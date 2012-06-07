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

import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

public class TransmissionListener implements MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MessageTransmitter messageTransmitter;
  private final TopicConnection responseConnection;
  private final TopicSession responseSession;
  private final TopicSubscriber responseSubscriber;

  public TransmissionListener (MessageTransmitter messageTransmitter, TopicConnection responseConnection, Topic responseTopic, AcknowledgeMode acknowledgeMode)
    throws JMSException {

    this.messageTransmitter = messageTransmitter;
    this.responseConnection = responseConnection;

    responseSession = responseConnection.createTopicSession(false, acknowledgeMode.getJmsValue());
    responseSubscriber = responseSession.createSubscriber(responseTopic, MessageProperty.INSTANCE + " = '" + messageTransmitter.getInstanceId() + "'", false);
    responseSubscriber.setMessageListener(this);
    responseConnection.start();
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      responseConnection.stop();

      responseSubscriber.close();
      responseSession.close();
      responseConnection.close();
    }
  }

  @Override
  public void onMessage (Message message) {

    messageTransmitter.completeCallback(message);
  }
}
