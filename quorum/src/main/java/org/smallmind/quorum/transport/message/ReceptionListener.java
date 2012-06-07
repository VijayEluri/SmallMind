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

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import org.smallmind.scribe.pen.LoggerManager;

public class ReceptionListener implements MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final QueueConnection requestConnection;
  private final QueueSession requestSession;
  private final QueueReceiver requestReceiver;
  private final SynchronousQueue<Message> messageRendezvous;

  public ReceptionListener (QueueConnection requestConnection, Queue requestQueue, AcknowledgeMode acknowledgeMode, SynchronousQueue<Message> messageRendezvous)
    throws JMSException {

    this.requestConnection = requestConnection;
    this.messageRendezvous = messageRendezvous;

    requestSession = requestConnection.createQueueSession(false, acknowledgeMode.getJmsValue());
    requestReceiver = requestSession.createReceiver(requestQueue);
    requestReceiver.setMessageListener(this);
    requestConnection.start();
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestConnection.stop();

      requestReceiver.close();
      requestSession.close();
      requestConnection.close();
    }
  }

  @Override
  public synchronized void onMessage (Message message) {

    try {

      messageRendezvous.put(message);
    }
    catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(ReceptionListener.class).error(interruptedException);
    }
  }
}
