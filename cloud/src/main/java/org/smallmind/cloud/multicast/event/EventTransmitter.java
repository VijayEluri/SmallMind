/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.cloud.multicast.event;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.cloud.multicast.EventMessageException;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.UniqueId;
import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.KeyLock;
import org.smallmind.quorum.cache.indigenous.UnorderedCache;
import org.smallmind.scribe.pen.Logger;

public class EventTransmitter implements Runnable {

   private static final int SO_TIMEOUT = 1000;
   private static final int TTL = 3;
   private static final byte[] EMPTY_ID = new byte[UniqueId.byteSize()];

   private Logger logger;
   private CountDownLatch exitLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private UnorderedCache<EventMessageKey, EventMessageMold, EventMessageCacheEntry> messageCache;
   private MulticastEventHandler eventHandler;
   private MulticastSocket multicastSocket;
   private InetAddress multicastInetAddr;
   private int multicastPort;
   private int messageSegmentSize;
   private int messageBufferSize;

   static {

      Arrays.fill(EMPTY_ID, (byte)0);
   }

   public EventTransmitter (MulticastEventHandler eventHandler, Logger logger, InetAddress multicastInetAddr, int multicastPort, int messageSegmentSize)
      throws IOException, CacheException {

      Thread receiverThread;

      this.eventHandler = eventHandler;
      this.logger = logger;
      this.multicastInetAddr = multicastInetAddr;
      this.multicastPort = multicastPort;
      this.messageSegmentSize = messageSegmentSize;

      messageBufferSize = messageSegmentSize + EventMessage.MESSAGE_HEADER_SIZE;
      messageCache = new UnorderedCache<EventMessageKey, EventMessageMold, EventMessageCacheEntry>(EventTransmitter.class.getSimpleName(), new EventMessageCacheSource(), new EventMessageCacheExpirationPolicy(60));

      multicastSocket = new MulticastSocket(multicastPort);
      multicastSocket.setReuseAddress(true);
      multicastSocket.setSoTimeout(SO_TIMEOUT);
      multicastSocket.setTimeToLive(TTL);
      multicastSocket.joinGroup(multicastInetAddr);

      exitLatch = new CountDownLatch(1);

      receiverThread = new Thread(this);
      receiverThread.setDaemon(true);
      receiverThread.start();
   }

   public synchronized void fireEvent (MulticastEvent multicastEvent)
      throws EventMessageException {

      EventMessageHeader messageHeader;
      byte[] messageId;
      byte[] bodyBuffer;

      try {
         bodyBuffer = objectToByteArray(multicastEvent);

         if (multicastEvent != null) {
            messageId = UniqueId.newInstance().asByteArray();
         }
         else {
            messageId = EMPTY_ID;
         }

         messageHeader = new EventMessageHeader(messageId, bodyBuffer.length);
         sendDatagram(messageHeader.getByteBuffer());
         sendBody(messageId, bodyBuffer);
      }
      catch (Exception e) {
         throw new EventMessageException(e);
      }
   }

   private byte[] objectToByteArray (Object body)
      throws IOException {

      ByteArrayOutputStream byteOutputStream;
      ObjectOutputStream objectOutputStream;
      byte[] bodyBuffer;

      if (body != null) {
         byteOutputStream = new ByteArrayOutputStream();
         objectOutputStream = new ObjectOutputStream(byteOutputStream);
         objectOutputStream.writeObject(body);
         objectOutputStream.close();

         bodyBuffer = byteOutputStream.toByteArray();
      }
      else {
         bodyBuffer = new byte[0];
      }

      return bodyBuffer;
   }

   private void sendBody (byte[] messageId, byte[] bodyBuffer)
      throws IOException {

      EventMessageBody messageBody;
      int messageIndex;
      int bufferPos;
      int bytesToSend;
      byte[] bodySegment;

      bufferPos = 0;
      messageIndex = 0;
      while (bufferPos < bodyBuffer.length) {
         bytesToSend = Math.min(messageSegmentSize, bodyBuffer.length - bufferPos);
         bodySegment = new byte[bytesToSend];
         System.arraycopy(bodyBuffer, bufferPos, bodySegment, 0, bytesToSend);
         messageBody = new EventMessageBody(messageId, messageIndex++, bodySegment);
         sendDatagram(messageBody.getByteBuffer());
         bufferPos += bytesToSend;
      }
   }

   private void sendDatagram (ByteBuffer dataBuffer)
      throws IOException {

      dataBuffer.flip();
      multicastSocket.send(new DatagramPacket(dataBuffer.array(), dataBuffer.position(), dataBuffer.limit() - dataBuffer.position(), multicastInetAddr, multicastPort));
   }

   public void logError (Throwable throwable) {

      logger.error(throwable);
   }

   public synchronized void finish () {

      if (finished.compareAndSet(false, true)) {
         try {
            exitLatch.await();
         }
         catch (InterruptedException interruptedException) {
            logError(interruptedException);
         }

         try {
            multicastSocket.leaveGroup(multicastInetAddr);
            multicastSocket.close();
         }
         catch (IOException ioException) {
            logError(ioException);
         }
      }
   }

   public void run () {

      KeyLock keyLock = new KeyLock();
      EventMessageMold messageMold;
      MulticastEvent multicastEvent;
      DatagramPacket messagePacket;
      ByteBuffer translationBuffer;
      EventMessageKey messageKey;
      MessageType messageType;
      boolean packetReceived;
      int messageLength;
      int messageIndex;
      byte[] messageKeyBuffer = new byte[UniqueId.byteSize()];
      byte[] messageBuffer = new byte[messageBufferSize];
      byte[] messageSegment;

      translationBuffer = ByteBuffer.wrap(messageBuffer);
      messagePacket = new DatagramPacket(messageBuffer, messageBuffer.length);

      while (!finished.get()) {
         try {
            try {
               multicastSocket.receive(messagePacket);
               packetReceived = true;
            }
            catch (SocketTimeoutException s) {
               packetReceived = false;
            }

            if (packetReceived) {
               translationBuffer.rewind();
               translationBuffer.getInt();
               translationBuffer.get(messageKeyBuffer);
               messageKey = new EventMessageKey(messageKeyBuffer);
               messageType = MessageType.getMessageType(translationBuffer.getInt());
               messageLength = translationBuffer.getInt();

               messageMold = messageCache.get(keyLock, messageKey);
               switch (messageType) {
                  case HEADER:
                     messageMold.setMessageLength(messageLength);
                     break;
                  case DATA:
                     messageIndex = translationBuffer.getInt();
                     messageSegment = new byte[messageLength];
                     translationBuffer.get(messageSegment);
                     messageMold.addData(messageIndex, messageSegment);
                     break;
                  default:
                     throw new UnknownSwitchCaseException(messageType.name());
               }

               if (messageMold.isComplete()) {
                  messageCache.remove(keyLock, messageKey);
                  multicastEvent = (MulticastEvent)messageMold.unmoldMessageBody();
                  eventHandler.deliverEvent(multicastEvent);
               }
            }
         }
         catch (Exception e) {
            logger.error(e);
         }
      }

      exitLatch.countDown();
   }

   public void finalize () {

      finish();
   }
}
