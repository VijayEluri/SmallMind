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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

public class MessageReceiver {

  private AtomicBoolean stopped = new AtomicBoolean(false);
  private MessageDistributor[] messageDistributors;

  public MessageReceiver (ManagedObjects managedObjects, MessageStrategy messageStrategy, int concurrencyLimit, MessageTarget... messageTargets)
    throws JMSException, TransportException {

    HashMap<String, MessageTarget> targetMap = new HashMap<String, MessageTarget>();
    for (MessageTarget messageTarget : messageTargets) {
      targetMap.put(messageTarget.getServiceInterface().getName(), messageTarget);
    }

    messageDistributors = new MessageDistributor[concurrencyLimit];
    for (int count = 0; count < messageDistributors.length; count++) {
      new Thread(messageDistributors[count] = new MessageDistributor(managedObjects, messageStrategy, targetMap)).start();
    }
  }

  public synchronized void close () {

    if (stopped.compareAndSet(false, true)) {
      try {
        for (MessageDistributor messageDistributor : messageDistributors) {
          messageDistributor.close();
        }
      }
      catch (JMSException jmsException) {
        LoggerManager.getLogger(MessageReceiver.class).error(jmsException);
      }
    }
  }
}
