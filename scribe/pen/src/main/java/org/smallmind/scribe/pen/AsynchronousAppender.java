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
package org.smallmind.scribe.pen;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsynchronousAppender implements Appender, Runnable {

  private CountDownLatch exitLatch;
  private AtomicBoolean finished = new AtomicBoolean(false);
  private Appender internalAppender;
  private LinkedBlockingQueue<Record> publishQueue;

  public AsynchronousAppender (Appender internalAppender) {

    this(internalAppender, Integer.MAX_VALUE);
  }

  public AsynchronousAppender (Appender internalAppender, int bufferSize) {

    Thread publishThread;

    this.internalAppender = internalAppender;

    publishQueue = new LinkedBlockingQueue<>(bufferSize);

    exitLatch = new CountDownLatch(1);

    publishThread = new Thread(this);
    publishThread.setDaemon(true);
    publishThread.start();
  }

  public String getName () {

    return internalAppender.getName();
  }

  public void setName (String name) {

    internalAppender.setName(name);
  }

  public void clearFilters () {

    internalAppender.clearFilters();
  }

  public synchronized void setFilter (Filter filter) {

    internalAppender.setFilter(filter);
  }

  public void addFilter (Filter filter) {

    internalAppender.addFilter(filter);
  }

  public Filter[] getFilters () {

    return internalAppender.getFilters();
  }

  public void setFilters (List<Filter> filterList) {

    internalAppender.setFilters(filterList);
  }

  public ErrorHandler getErrorHandler () {

    return internalAppender.getErrorHandler();
  }

  public void setErrorHandler (ErrorHandler errorHandler) {

    internalAppender.setErrorHandler(errorHandler);
  }

  public Formatter getFormatter () {

    return internalAppender.getFormatter();
  }

  public void setFormatter (Formatter formatter) {

    internalAppender.setFormatter(formatter);
  }

  public boolean isActive () {

    return internalAppender.isActive();
  }

  public void setActive (boolean active) {

    internalAppender.setActive(active);
  }

  public boolean requiresFormatter () {

    return internalAppender.requiresFormatter();
  }

  public void publish (Record record) {

    try {
      if (finished.get()) {
        throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
      }

      publishQueue.put(record);
    }
    catch (Exception exception) {
      if (internalAppender.getErrorHandler() == null) {
        exception.printStackTrace();
      }
      else {
        internalAppender.getErrorHandler().process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
      }
    }
  }

  public void close ()
    throws InterruptedException, LoggerException {

    finish();

    internalAppender.close();
  }

  public void finish ()
    throws InterruptedException {

    finished.compareAndSet(false, true);
    exitLatch.await();
  }

  protected void finalize ()
    throws InterruptedException {

    finish();
  }

  public void run () {

    Record record;

    try {
      while (!finished.get()) {
        if ((record = publishQueue.poll(1, TimeUnit.SECONDS)) != null) {
          internalAppender.publish(record);
        }
      }
    }
    catch (InterruptedException interruptedException) {
      finished.set(true);
      LoggerManager.getLogger(AsynchronousAppender.class).error(interruptedException);
    }
    finally {
      exitLatch.countDown();
    }
  }
}