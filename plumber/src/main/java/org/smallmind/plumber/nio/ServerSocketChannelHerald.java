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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.plumber.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.Counter;
import org.smallmind.quorum.pool.simple.ComponentFactory;
import org.smallmind.quorum.pool.simple.ComponentPool;
import org.smallmind.quorum.pool.simple.SimplePoolConfig;
import org.smallmind.scribe.pen.Logger;

public class ServerSocketChannelHerald implements ComponentFactory<SocketChannelWorker>, Runnable {

  public static final int NO_THROTTLE = -1;

  private final Counter acceptCounter;

  private Logger logger;
  private CountDownLatch exitLatch;
  private CountDownLatch pulseLatch;
  private AtomicBoolean finished = new AtomicBoolean(false);
  private ComponentPool<SocketChannelWorker> workerPool;
  private SocketChannelWorkerFactory workerFactory;
  private Selector acceptSelector;
  private int maxAccepted;

  public ServerSocketChannelHerald (Logger logger, SocketChannelWorkerFactory workerFactory, ServerSocketChannel serverSocketChannel, int maxAccepted, int poolSize)
    throws IOException {

    SimplePoolConfig simplePoolConfig;

    this.logger = logger;
    this.workerFactory = workerFactory;
    this.maxAccepted = maxAccepted;

    serverSocketChannel.configureBlocking(false);

    acceptSelector = Selector.open();
    serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);

    acceptCounter = new Counter();
    pulseLatch = new CountDownLatch(1);
    exitLatch = new CountDownLatch(1);

    simplePoolConfig = new SimplePoolConfig();
    simplePoolConfig.setAcquireWaitTimeMillis(poolSize);

    workerPool = new ComponentPool<SocketChannelWorker>(this, simplePoolConfig);
  }

  public SocketChannelWorker createComponent ()
    throws Exception {

    return workerFactory.createWorker(logger, this);
  }

  public void finish ()
    throws InterruptedException {

    if (finished.compareAndSet(false, true)) {
      pulseLatch.countDown();
    }

    exitLatch.await();
  }

  public void run () {

    Thread workThread;
    SocketChannelWorker worker;
    ServerSocketChannel readyChannel;
    Set<SelectionKey> readyKeySet;
    Iterator<SelectionKey> readyKeyIter;
    SelectionKey readyKey;
    boolean accepted;

    try {
      while (!finished.get()) {
        try {
          if (acceptSelector.select(1000) > 0) {
            readyKeySet = acceptSelector.selectedKeys();
            readyKeyIter = readyKeySet.iterator();
            while (readyKeyIter.hasNext()) {
              if (finished.get()) {
                break;
              }

              accepted = false;
              synchronized (acceptCounter) {
                if ((maxAccepted < 0) || (acceptCounter.getCount() < maxAccepted)) {
                  acceptCounter.inc();
                  accepted = true;

                  readyKey = readyKeyIter.next();
                  readyKeyIter.remove();

                  readyChannel = (ServerSocketChannel)readyKey.channel();

                  worker = workerPool.getComponent();
                  worker.setChannel(readyChannel);
                  workThread = new Thread(worker);
                  workThread.setDaemon(true);
                  workThread.start();
                }
              }

              if (!accepted) {
                try {
                  pulseLatch.await(100, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException interruptedException) {
                  logger.error(interruptedException);
                }
              }
            }
          }
        }
        catch (Exception e) {
          logger.error(e);
        }
      }
    }
    finally {
      exitLatch.countDown();
    }
  }

  public void returnConnection (SocketChannelWorker worker) {

    workerPool.returnComponent(worker);

    synchronized (acceptCounter) {
      acceptCounter.dec();
    }
  }
}
