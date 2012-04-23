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

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

public class MessageDistributor implements MessageListener, Runnable {

  private CountDownLatch exitLatch;
  private AtomicBoolean stopped = new AtomicBoolean(false);
  private QueueSession queueSession;
  private QueueReceiver queueReceiver;
  private MessageObjectStrategy messageObjectStrategy;
  private HashMap<String, MessageTarget> targetMap;

  public MessageDistributor (QueueConnection queueConnection, Queue queue, MessageObjectStrategy messageObjectStrategy, HashMap<String, MessageTarget> targetMap)
    throws TransportException, JMSException {

    this.messageObjectStrategy = messageObjectStrategy;
    this.targetMap = targetMap;

    queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

    queueReceiver = queueSession.createReceiver(queue);
    queueReceiver.setMessageListener(this);
    queueConnection.start();

    exitLatch = new CountDownLatch(1);
  }

  public void close ()
    throws JMSException {

    if (stopped.compareAndSet(false, true)) {
      try {
        queueReceiver.close();
        queueSession.close();
      }
      finally {
        exitLatch.countDown();
      }
    }
  }

  @Override
  public void run () {

    try {
      exitLatch.await();
    }
    catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(MessageDistributor.class).error(interruptedException);
    }
  }

  public synchronized void onMessage (Message message) {

    Message responseMessage;
    QueueSender queueSender;

    try {
      try {

        MessageTarget messageTarget;
        String serviceSelector;

        if ((serviceSelector = message.getStringProperty(MessageProperty.SERVICE.getKey())) == null) {
          throw new TransportException("Missing message property(%s)", MessageProperty.SERVICE.getKey());
        }
        else if ((messageTarget = targetMap.get(serviceSelector)) == null) {
          throw new TransportException("Unknown service selector(%s)", serviceSelector);
        }

        responseMessage = messageTarget.handleMessage(queueSession, messageObjectStrategy, message);
      }
      catch (Exception exception) {
        responseMessage = messageObjectStrategy.wrapInMessage(queueSession, exception);
        responseMessage.setBooleanProperty(MessageProperty.EXCEPTION.getKey(), true);
      }

      responseMessage.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);

      queueSender = queueSession.createSender((Queue)message.getJMSReplyTo());

      try {
        queueSender.send(responseMessage);
      }
      finally {
        queueSender.close();
      }
    }
    catch (Throwable throwable) {
      LoggerManager.getLogger(MessageDistributor.class).error(throwable);
    }
  }
}
