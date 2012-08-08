/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.Metrics;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.quorum.transport.Transport;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.scribe.pen.LoggerManager;

public class TransmissionListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MessageTransmitter messageTransmitter;
  private final ConnectionFactor responseConnectionFactor;
  private final Topic responseTopic;
  private final String selector;

  public TransmissionListener (MessageTransmitter messageTransmitter, ConnectionFactor responseConnectionFactor, Topic responseTopic)
    throws JMSException {

    this.messageTransmitter = messageTransmitter;
    this.responseConnectionFactor = responseConnectionFactor;
    this.responseTopic = responseTopic;

    selector = MessageProperty.INSTANCE.getKey() + "='" + messageTransmitter.getInstanceId() + "'";

    responseConnectionFactor.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return responseTopic;
  }

  @Override
  public String getMessageSelector () {

    return selector;
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      responseConnectionFactor.stop();
      responseConnectionFactor.close();
    }
  }

  @Override
  public void onMessage (Message message) {

    Transport transport;
    MetricConfiguration metricConfiguration;

    if (((transport = TransportManager.getTransport()) != null) && ((metricConfiguration = transport.getMetricConfiguration()) != null) && metricConfiguration.isInstrumented()) {
      try {

        long timeInTopic = System.currentTimeMillis() - message.getJMSTimestamp();

        InstrumentationManager.getMetricRegistry().instrument(Metrics.buildChronometer(metricConfiguration.getSamples(), TimeUnit.MILLISECONDS, metricConfiguration.getTickInterval(), metricConfiguration.getTickTimeUnit(), Clocks.EPOCH), metricConfiguration.getMetricDomain().getDomain(), new MetricProperty("event", MetricEvent.RESPONSE_TOPIC.getDisplay())).update(timeInTopic);
      }
      catch (Exception exception) {
        LoggerManager.getLogger(ReceptionListener.class).error(exception);
      }
    }

    messageTransmitter.completeCallback(message);
  }
}
