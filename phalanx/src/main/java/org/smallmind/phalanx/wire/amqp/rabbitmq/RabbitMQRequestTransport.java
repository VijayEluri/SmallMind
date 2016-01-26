/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.SynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.TransmissionCallback;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.WireContext;

public class RabbitMQRequestTransport implements MetricConfigurationProvider, RequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MetricConfiguration metricConfiguration;
  private final SignalCodec signalCodec;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final LinkedBlockingQueue<RequestMessageRouter> routerQueue;
  private final RequestMessageRouter[] requestMessageRouters;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  public RabbitMQRequestTransport (MetricConfiguration metricConfiguration, RabbitMQConnector rabbitMQConnector, NameConfiguration nameConfiguration, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, int defaultTimeoutSeconds, int messageTTLSeconds)
    throws IOException, InterruptedException {

    int routerIndex = 0;

    this.metricConfiguration = metricConfiguration;
    this.signalCodec = signalCodec;

    callbackMap = new SelfDestructiveMap<>(new Duration(defaultTimeoutSeconds, TimeUnit.SECONDS));

    requestMessageRouters = new RequestMessageRouter[clusterSize];
    for (int index = 0; index < requestMessageRouters.length; index++) {
      requestMessageRouters[index] = new RequestMessageRouter(rabbitMQConnector, nameConfiguration, this, signalCodec, callerId, index, messageTTLSeconds);
      requestMessageRouters[index].initialize();
    }

    routerQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      routerQueue.add(requestMessageRouters[routerIndex]);
      if (++routerIndex == requestMessageRouters.length) {
        routerIndex = 0;
      }
    }
  }

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  @Override
  public void transmitInOnly (String serviceGroup, Voice voice, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    transmit(true, serviceGroup, voice, 0, address, arguments, contexts);
  }

  @Override
  public Object transmitInOut (String serviceGroup, Voice voice, int timeoutSeconds, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    TransmissionCallback transmissionCallback;

    if ((transmissionCallback = transmit(false, serviceGroup, voice, timeoutSeconds, address, arguments, contexts)) != null) {

      return transmissionCallback.getResult(signalCodec);
    }

    return null;
  }

  private TransmissionCallback transmit (boolean inOnly, String serviceGroup, Voice voice, int timeoutSeconds, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    final RequestMessageRouter requestMessageRouter = acquireRequestMessageRouter();

    try {

      String messageId;

      messageId = requestMessageRouter.publish(inOnly, serviceGroup, voice, address, arguments, contexts);

      if (!inOnly) {

        AsynchronousTransmissionCallback asynchronousCallback = new AsynchronousTransmissionCallback(address.getService(), address.getFunction().getName());
        SynchronousTransmissionCallback previousCallback;

        if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(messageId, asynchronousCallback, (timeoutSeconds > 0) ? new Duration(timeoutSeconds, TimeUnit.SECONDS) : null)) != null) {

          return previousCallback;
        }

        return asynchronousCallback;
      }

      return null;
    } finally {
      routerQueue.put(requestMessageRouter);
    }
  }

  private RequestMessageRouter acquireRequestMessageRouter ()
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<RequestMessageRouter>(this, new MetricProperty("event", MetricType.ACQUIRE_REQUEST_DESTINATION.getDisplay())) {

      @Override
      public RequestMessageRouter withChronometer ()
        throws TransportException, InterruptedException {

        RequestMessageRouter messageTransmitter;

        do {
          messageTransmitter = routerQueue.poll(1, TimeUnit.SECONDS);
        } while ((!closed.get()) && (messageTransmitter == null));

        if (messageTransmitter == null) {
          throw new TransportException("Message transmission has been closed");
        }

        return messageTransmitter;
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
    throws IOException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (RequestMessageRouter requestMessageRouter : requestMessageRouters) {
        requestMessageRouter.close();
      }

      callbackMap.shutdown();
    }
  }
}
