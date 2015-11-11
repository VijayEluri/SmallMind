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
package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class MessageRouter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicStampedReference<Channel> channelRef = new AtomicStampedReference<>(null, 0);
  private final AtomicInteger version = new AtomicInteger(0);
  private final RabbitMQConnector connector;
  private final NameConfiguration nameConfiguration;

  public MessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration) {

    this.connector = connector;
    this.nameConfiguration = nameConfiguration;
  }

  public abstract void bindQueues (Channel channel)
    throws IOException;

  public abstract void installConsumer (Channel channel)
    throws IOException;

  public void initialize ()
    throws IOException {

    ensureChannel(0);
  }

  public String getRequestExchangeName () {

    return nameConfiguration.getRequestExchange();
  }

  public String getResponseExchangeName () {

    return nameConfiguration.getResponseExchange();
  }

  public String getResponseQueueName () {

    return nameConfiguration.getResponseQueue();
  }

  public String getTalkQueueName () {

    return nameConfiguration.getTalkQueue();
  }

  public String getWhisperQueueName () {

    return nameConfiguration.getWhisperQueue();
  }

  public void ensureChannel (int stamp)
    throws IOException {

    synchronized (channelRef) {
      if (channelRef.getStamp() == stamp) {

        Channel channel;
        final int nextStamp;

        channel = connector.getConnection().createChannel();
        channel.exchangeDeclare(getRequestExchangeName(), "direct", false, false, null);
        channel.exchangeDeclare(getResponseExchangeName(), "direct", false, false, null);

        bindQueues(channel);

        channelRef.set(channel, nextStamp = version.incrementAndGet());

        channel.addShutdownListener(new ShutdownListener() {

          @Override
          public void shutdownCompleted (ShutdownSignalException cause) {

            try {
              if (!closed.get()) {
                ensureChannel(nextStamp);
              }
            } catch (IOException ioException) {
              LoggerManager.getLogger(RabbitMQConnector.class).error(ioException);
            }
          }
        });

        installConsumer(channel);
      }
    }
  }

  public void send (String routingKey, String exchangeName, AMQP.BasicProperties properties, byte[] body)
    throws IOException {

    boolean sent = false;

    do {

      int[] stampHolder = new int[1];
      Channel channel = channelRef.get(stampHolder);

      try {
        channel.basicPublish(exchangeName, routingKey, true, false, properties, body);
        sent = true;
      } catch (AlreadyClosedException exception) {
        ensureChannel(stampHolder[0]);
      }
    } while (!sent);
  }

  public long getTimestamp (AMQP.BasicProperties properties) {

    Date date;

    if ((date = properties.getTimestamp()) != null) {

      return date.getTime();
    }

    return Long.MAX_VALUE;
  }

  public void close ()
    throws IOException {

    if (closed.compareAndSet(false, true)) {
      channelRef.getReference().close();
    }
  }
}
