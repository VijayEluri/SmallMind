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
package org.smallmind.quorum.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool<C> {

   private ConnectionInstanceFactory<C> connectionFactory;
   private ConcurrentLinkedQueue<ConnectionPin<C>> freeConnectionPinQueue;
   private ConcurrentLinkedQueue<ConnectionPin<C>> processingConnectionPinQueue;
   private String poolName;
   private PoolMode poolMode = PoolMode.BLOCKING_POOL;
   private AtomicInteger poolCount = new AtomicInteger(0);
   private AtomicBoolean startupFlag = new AtomicBoolean(false);
   private AtomicBoolean shutdownFlag = new AtomicBoolean(false);
   private boolean testOnConnect = false;
   private boolean testOnAcquire = false;
   private long connectionTimeoutMillis = 0;
   private int initialPoolSize = 0;
   private int minPoolSize = 1;
   private int maxPoolSize = 10;
   private int acquireRetryAttempts = 0;
   private int acquireRetryDelayMillis = 0;
   private int leaseTimeSeconds = 0;
   private int maxIdleTimeSeconds = 0;
   private int unreturnedConnectionTimeoutSeconds = 0;

   public ConnectionPool (String poolName, ConnectionInstanceFactory<C> connectionFactory)
      throws ConnectionPoolException {

      this.poolName = poolName;
      this.connectionFactory = connectionFactory;

      ConnectionPoolManager.register(this);
   }

   public void startup()
      throws ConnectionPoolException {

      if (startupFlag.compareAndSet(false, true)) {
         freeConnectionPinQueue = new ConcurrentLinkedQueue<ConnectionPin<C>>();
         processingConnectionPinQueue = new ConcurrentLinkedQueue<ConnectionPin<C>>();

         for (int count = 0; count < initialPoolSize; count++) {
            freeConnectionPinQueue.add(createConnectionPin());
         }
      }
   }

   public void shutdown () {

      if (shutdownFlag.compareAndSet(false, true)) {
         for (ConnectionPin<C> connectionPin : processingConnectionPinQueue) {
            destroyConnectionPin(connectionPin);
         }
         for (ConnectionPin<C> connectionPin : freeConnectionPinQueue) {
            destroyConnectionPin(connectionPin);
         }
      }
   }

   public void setPoolMode (PoolMode poolMode) {

      this.poolMode = poolMode;
   }

   public void setTestOnConnect (boolean testOnConnect) {

      this.testOnConnect = testOnConnect;
   }

   public void setTestOnAcquire (boolean testOnAcquire) {

      this.testOnAcquire = testOnAcquire;
   }

   public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

      this.connectionTimeoutMillis = connectionTimeoutMillis;
   }

   public void setInitialPoolSize (int initialPoolSize) {

      this.initialPoolSize = initialPoolSize;
   }

   public void setMinPoolSize (int minPoolSize) {

      this.minPoolSize = minPoolSize;
   }

   public void setMaxPoolSize (int maxPoolSize) {

      this.maxPoolSize = maxPoolSize;
   }

   public void setAcquireRetryAttempts (int acquireRetryAttempts) {

      this.acquireRetryAttempts = acquireRetryAttempts;
   }

   public void setAcquireRetryDelayMillis (int acquireRetryDelayMillis) {

      this.acquireRetryDelayMillis = acquireRetryDelayMillis;
   }

   public void setLeaseTimeSeconds (int leaseTimeSeconds) {

      this.leaseTimeSeconds = leaseTimeSeconds;
   }

   public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

      this.maxIdleTimeSeconds = maxIdleTimeSeconds;
   }

   public void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

      this.unreturnedConnectionTimeoutSeconds = unreturnedConnectionTimeoutSeconds;
   }

   public String getPoolName () {

      return poolName;
   }

   public Object rawConnection ()
      throws ConnectionCreationException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      try {
         return connectionFactory.rawInstance();
      }
      catch (Exception exception) {
         throw new ConnectionCreationException(exception);
      }
   }

   private ConnectionPin<C> createConnectionPin ()
      throws ConnectionPoolException {

      ConnectionInstance<C> connectionInstance;

      if (connectionTimeoutMillis > 0) {

         ConnectionWorker<C> connectionWorker;
         Thread workerThread;
         CountDownLatch workerInitLatch = new CountDownLatch(1);

         connectionWorker = new ConnectionWorker<C>(this, connectionFactory, workerInitLatch);
         workerThread = new Thread(connectionWorker);
         workerThread.start();

         try {
            workerInitLatch.await();
            workerThread.join(connectionTimeoutMillis);
            connectionWorker.abort();
         }
         catch (InterruptedException interruptedException) {
            throw new ConnectionPoolException(interruptedException);
         }

         if (connectionWorker.hasBeenAborted()) {
            throw new ConnectionCreationException("Exceeded connection timeout(%d) waiting on connection creation", connectionTimeoutMillis);
         }
         else if (connectionWorker.hasException()) {
            throw connectionWorker.getException();
         }
         else {
            connectionInstance = connectionWorker.getConnectionInstance();
         }
      }
      else {
         try {
            connectionInstance = connectionFactory.createInstance(this);
         }
         catch (ConnectionPoolException connectionPoolException) {
            throw connectionPoolException;
         }
         catch (Exception otherException) {
            throw new ConnectionPoolException(otherException);
         }
      }

      if (testOnConnect && (!connectionInstance.validate())) {
         throw new InvalidConnectionException("A new connection was required by failed to validate");
      }
      else {
         poolCount.incrementAndGet();

         return new ConnectionPin<C>(this, connectionInstance, maxIdleTimeSeconds, leaseTimeSeconds, unreturnedConnectionTimeoutSeconds);
      }
   }

   private void destroyConnectionPin (ConnectionPin<C> connectionPin) {

      poolCount.decrementAndGet();

      try {
         connectionPin.close();
      }
      catch (Exception exception) {
         ConnectionPoolManager.logError(exception);
      }
      finally {
         connectionPin.abort();
      }
   }

   private C useConnectionPin ()
      throws Exception {

      ConnectionPin<C> connectionPin;
      int blockedAttempts = 0;

      do {
         while ((connectionPin = freeConnectionPinQueue.poll()) != null) {
            synchronized (connectionPin) {
               if (connectionPin.isComissioned() && connectionPin.isFree()) {
                  if (testOnAcquire && (!connectionPin.validate())) {
                     destroyConnectionPin(connectionPin);
                  }
                  else {
                     try {
                        return connectionPin.serve();
                     }
                     finally {
                        processingConnectionPinQueue.add(connectionPin);
                     }
                  }
               }
               else {
                  poolCount.decrementAndGet();
               }
            }
         }

         if (poolMode.equals(PoolMode.EXPANDING_POOL) || (poolCount.get() < maxPoolSize)) {
            synchronized (connectionPin = createConnectionPin()) {
               try {
                  return connectionPin.serve();
               }
               finally {
                  processingConnectionPinQueue.add(connectionPin);
               }
            }
         }

         if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            if ((acquireRetryAttempts == 0) || (++blockedAttempts < acquireRetryAttempts)) {
               Thread.sleep(acquireRetryDelayMillis);
            }
            else {
               throw new ConnectionPoolException("Blocking ConnectionPool (%s) has exceeded its maximum attempts (%d)", poolName, acquireRetryAttempts);
            }
         }
      } while (poolMode.equals(PoolMode.BLOCKING_POOL));

      throw new ConnectionPoolException("Fixed ConnectionPool (%s) is completely booked", poolName);
   }

   public C getConnection ()
      throws ConnectionPoolException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      startup();

      try {
         return useConnectionPin();
      }
      catch (Exception exception) {
         throw new ConnectionPoolException(exception);
      }
   }

   public void returnInstance (ConnectionInstance connectionInstance)
      throws Exception {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      releaseInstance(connectionInstance, false);
   }

   public void terminateInstance (ConnectionInstance connectionInstance)
      throws Exception {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      releaseInstance(connectionInstance, true);
   }

   private void releaseInstance (ConnectionInstance connectionInstance, boolean terminate)
      throws Exception {

      for (ConnectionPin<C> connectionPin : processingConnectionPinQueue) {
         if (connectionPin.contains(connectionInstance)) {
            if (processingConnectionPinQueue.remove(connectionPin)) {
               synchronized (connectionPin) {
                  if (terminate || (poolCount.get() > minPoolSize)) {
                     destroyConnectionPin(connectionPin);

                     if (poolCount.get() < minPoolSize) {
                        freeConnectionPinQueue.add(createConnectionPin());
                     }
                  }
                  else if (connectionPin.isServed()) {
                     connectionPin.free();
                     freeConnectionPinQueue.add(connectionPin);
                  }
                  else {
                     poolCount.decrementAndGet();
                  }
               }
            }

            return;
         }
      }

      throw new ConnectionPoolException("Could not find connection (%s) within ConnectionPool (%s)", connectionInstance, poolName);
   }

   protected void removePin (ConnectionPin connectionPin) {

      if (processingConnectionPinQueue.remove(connectionPin) || freeConnectionPinQueue.remove(connectionPin)) {
         poolCount.decrementAndGet();
      }

      if (poolCount.get() < minPoolSize) {
         try {
            freeConnectionPinQueue.add(createConnectionPin());
         }
         catch (ConnectionPoolException connectionPoolException) {
            ConnectionPoolManager.logError(connectionPoolException);
         }
      }
   }

   public int getPoolSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return poolCount.get();
   }

   public int getFreeSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return freeConnectionPinQueue.size();
   }

   public int getProcessingSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return processingConnectionPinQueue.size();
   }
}