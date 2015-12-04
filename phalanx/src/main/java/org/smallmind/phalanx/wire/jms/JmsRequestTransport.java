/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.jms;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.AsynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.SynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.TransmissionCallback;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;

public class JmsRequestTransport implements MetricConfigurationProvider, RequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MetricConfiguration metricConfiguration;
  private final SignalCodec signalCodec;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final LinkedBlockingQueue<MessageHandler> talkQueue;
  private final LinkedBlockingQueue<MessageHandler> whisperQueue;
  private final ConnectionManager[] talkRequestConnectionManagers;
  private final ConnectionManager[] whisperRequestConnectionManagers;
  private final ResponseListener[] responseListeners;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  public JmsRequestTransport (MetricConfiguration metricConfiguration, RoutingFactories routingFactories, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, int maximumMessageLength, int defaultTimeoutSeconds)
    throws IOException, JMSException, TransportException {

    int talkIndex = 0;
    int whisperIndex = 0;

    this.metricConfiguration = metricConfiguration;
    this.signalCodec = signalCodec;

    callbackMap = new SelfDestructiveMap<>(new Duration(defaultTimeoutSeconds, TimeUnit.SECONDS));

    talkRequestConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < talkRequestConnectionManagers.length; index++) {
      talkRequestConnectionManagers[index] = new ConnectionManager(routingFactories.getRequestQueueFactory(), messagePolicy, reconnectionPolicy);
    }
    whisperRequestConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < whisperRequestConnectionManagers.length; index++) {
      whisperRequestConnectionManagers[index] = new ConnectionManager(routingFactories.getRequestTopicFactory(), messagePolicy, reconnectionPolicy);
    }

    talkQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      talkQueue.add(new QueueOperator(talkRequestConnectionManagers[talkIndex], (Queue)routingFactories.getRequestQueueFactory().getDestination()));
      if (++talkIndex == talkRequestConnectionManagers.length) {
        talkIndex = 0;
      }
    }
    whisperQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      whisperQueue.add(new TopicOperator(whisperRequestConnectionManagers[whisperIndex], (Topic)routingFactories.getRequestTopicFactory().getDestination()));
      if (++whisperIndex == whisperRequestConnectionManagers.length) {
        whisperIndex = 0;
      }
    }

    responseListeners = new ResponseListener[clusterSize];
    for (int index = 0; index < responseListeners.length; index++) {
      responseListeners[index] = new ResponseListener(this, new ConnectionManager(routingFactories.getResponseTopicFactory(), messagePolicy, reconnectionPolicy), (Topic)routingFactories.getResponseTopicFactory().getDestination(), signalCodec, callerId, maximumMessageLength);
    }
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  @Override
  public void transmitInOnly (String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    transmit(true, serviceGroup, instanceId, 0, address, arguments, contexts);
  }

  @Override
  public Object transmitInOut (String serviceGroup, String instanceId, int timeoutSeconds, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    TransmissionCallback transmissionCallback;

    if ((transmissionCallback = transmit(false, serviceGroup, instanceId, timeoutSeconds, address, arguments, contexts)) != null) {

      return transmissionCallback.getResult(signalCodec);
    }

    return null;
  }

  private TransmissionCallback transmit (boolean inOnly, String serviceGroup, String instanceId, int timeoutSeconds, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    LinkedBlockingQueue<MessageHandler> messageQueue = (instanceId == null) ? talkQueue : whisperQueue;
    final MessageHandler messageHandler = acquireMessageHandler(messageQueue);

    try {

      Message requestMessage;

      messageHandler.send(requestMessage = constructMessage(messageHandler, inOnly, serviceGroup, instanceId, address, arguments, contexts));

      if (!inOnly) {

        AsynchronousTransmissionCallback asynchronousCallback = new AsynchronousTransmissionCallback(address.getService(), address.getFunction().getName());
        SynchronousTransmissionCallback previousCallback;

        if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(requestMessage.getJMSMessageID(), asynchronousCallback, (timeoutSeconds > 0) ? new Duration(timeoutSeconds, TimeUnit.SECONDS) : null)) != null) {

          return previousCallback;
        }

        return asynchronousCallback;
      }

      return null;
    } finally {
      messageQueue.put(messageHandler);
    }
  }

  private MessageHandler acquireMessageHandler (final LinkedBlockingQueue<MessageHandler> messageHandlerQueue)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<MessageHandler>(this, new MetricProperty("event", MetricType.ACQUIRE_REQUEST_DESTINATION.getDisplay())) {

      @Override
      public MessageHandler withChronometer ()
        throws TransportException, InterruptedException {

        MessageHandler messageHandler;

        do {
          messageHandler = messageHandlerQueue.poll(1, TimeUnit.SECONDS);
        } while ((!closed.get()) && (messageHandler == null));

        if (messageHandler == null) {
          throw new TransportException("Message transmission has been closed");
        }

        return messageHandler;
      }
    });
  }

  private Message constructMessage (final MessageHandler messageHandler, final boolean inOnly, final String serviceGroup, final String instanceId, final Address address, final Map<String, Object> arguments, final WireContext... contexts)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<Message>(this, new MetricProperty("event", MetricType.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public Message withChronometer ()
        throws Exception {

        BytesMessage requestMessage;

        requestMessage = messageHandler.createMessage();

        requestMessage.writeBytes(signalCodec.encode(new InvocationSignal(inOnly, address, arguments, contexts)));

        if (!inOnly) {
          requestMessage.setStringProperty(WireProperty.CALLER_ID.getKey(), callerId);
        }

        requestMessage.setStringProperty(WireProperty.CONTENT_TYPE.getKey(), signalCodec.getContentType());
        requestMessage.setLongProperty(WireProperty.CLOCK.getKey(), System.currentTimeMillis());
        requestMessage.setStringProperty(WireProperty.SERVICE_GROUP.getKey(), serviceGroup);

        if (instanceId != null) {
          requestMessage.setStringProperty(WireProperty.INSTANCE_ID.getKey(), instanceId);
        }

        return requestMessage;
      }
    });
  }

  public void completeCallback (String correlationId, ResultSignal resultSignal) {

    TransmissionCallback previousCallback;

    if ((previousCallback = callbackMap.get(correlationId)) == null) {
      if ((previousCallback = callbackMap.putIfAbsent(correlationId, new SynchronousTransmissionCallback(resultSignal))) != null) {
        if (previousCallback instanceof AsynchronousTransmissionCallback) {
          ((AsynchronousTransmissionCallback)previousCallback).setResultSignal(resultSignal);
        }
      }
    } else if (previousCallback instanceof AsynchronousTransmissionCallback) {
      ((AsynchronousTransmissionCallback)previousCallback).setResultSignal(resultSignal);
    }
  }

  @Override
  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (ConnectionManager requestConnectionManager : whisperRequestConnectionManagers) {
        requestConnectionManager.stop();
      }
      for (ConnectionManager requestConnectionManager : talkRequestConnectionManagers) {
        requestConnectionManager.stop();
      }

      for (ConnectionManager requestConnectionManager : whisperRequestConnectionManagers) {
        requestConnectionManager.close();
      }
      for (ConnectionManager requestConnectionManager : talkRequestConnectionManagers) {
        requestConnectionManager.close();
      }

      for (ResponseListener responseListener : responseListeners) {
        responseListener.close();
      }

      callbackMap.shutdown();
    }
  }
}
