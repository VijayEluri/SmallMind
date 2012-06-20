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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.Message;
import org.smallmind.quorum.transport.TransportException;

public class AsynchronousTransmissionCallback implements TransmissionCallback {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<Message> responseMessageRef = new AtomicReference<Message>();
  private final MessageStrategy messageStrategy;

  public AsynchronousTransmissionCallback (MessageStrategy messageStrategy) {

    this.messageStrategy = messageStrategy;
  }

  @Override
  public void destroy () {

    resultLatch.countDown();
  }

  @Override
  public Object getResult ()
    throws Exception {

    Message responseMessage;

    resultLatch.await();

    if ((responseMessage = responseMessageRef.get()) == null) {
      throw new TransportException("Timeout exceeded while waiting for a response");
    }

    if (responseMessage.getBooleanProperty(MessageProperty.EXCEPTION.getKey())) {
      throw (Exception)messageStrategy.unwrapFromMessage(responseMessage);
    }

    return messageStrategy.unwrapFromMessage(responseMessage);
  }

  public void setResponseMessage (Message responseMessage) {

    responseMessageRef.set(responseMessage);

    resultLatch.countDown();
  }
}