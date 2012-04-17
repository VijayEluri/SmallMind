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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import org.smallmind.quorum.pool.component.PooledComponent;
import org.smallmind.scribe.pen.LoggerManager;

public class MessageSender implements PooledComponent {

  private QueueSession queueSession;
  private QueueSender queueSender;
  private TemporaryQueue temporaryQueue;
  private QueueReceiver queueReceiver;
  private MessageStrategy messageStrategy;

  public MessageSender (QueueConnection queueConnection, Queue queue, MessageStrategy messageStrategy)
    throws JMSException {

    this.messageStrategy = messageStrategy;

    queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    queueSender = queueSession.createSender(queue);
    temporaryQueue = queueSession.createTemporaryQueue();
    queueReceiver = queueSession.createReceiver(temporaryQueue);
  }

  public Message wrapInMessage (Serializable serializable)
    throws Exception {

    return messageStrategy.wrapInMessage(queueSession, serializable);
  }

  public void sendMessage (Message message, String serviceSelector)
    throws JMSException {

    message.setStringProperty(MessageProperty.SERVICE.getKey(), serviceSelector);
    message.setJMSReplyTo(temporaryQueue);
    queueSender.send(message);
  }

  public Object getResult ()
    throws Exception {

    Message message;

    message = queueReceiver.receive();
    if (message.getBooleanProperty(MessageProperty.EXCEPTION.getKey())) {
      throw new InvocationTargetException((Exception)messageStrategy.unwrapFromMessage(message));
    }

    return messageStrategy.unwrapFromMessage(message);
  }

  public void close ()
    throws JMSException {

    queueSender.close();
    queueReceiver.close();
    temporaryQueue.delete();
    queueSession.close();
  }

  @Override
  public void terminate () {

    try {
      close();
    }
    catch (JMSException jmsException) {
      LoggerManager.getLogger(MessageSender.class).error(jmsException);
    }
  }
}
