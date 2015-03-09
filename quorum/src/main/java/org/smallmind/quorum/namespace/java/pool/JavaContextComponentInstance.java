/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.namespace.java.pool;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.NamingException;
import org.smallmind.quorum.namespace.java.PooledJavaContext;
import org.smallmind.quorum.namespace.java.event.JavaContextEvent;
import org.smallmind.quorum.namespace.java.event.JavaContextListener;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.scribe.pen.LoggerManager;

public class JavaContextComponentInstance implements ComponentInstance<PooledJavaContext>, JavaContextListener {

  private final ComponentPool<PooledJavaContext> componentPool;
  private final PooledJavaContext pooledJavaContext;
  private final AtomicReference<StackTraceElement[]> stackTraceReference = new AtomicReference<StackTraceElement[]>();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public JavaContextComponentInstance (ComponentPool<PooledJavaContext> componentPool, PooledJavaContext pooledJavaContext)
    throws NamingException {

    this.componentPool = componentPool;
    this.pooledJavaContext = pooledJavaContext;

    pooledJavaContext.addJavaContextListener(this);
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return stackTraceReference.get();
  }

  public boolean validate () {

    try {
      pooledJavaContext.lookup("");
    } catch (NamingException namingException) {

      return false;
    }

    return true;
  }

  public void contextClosed (JavaContextEvent javaContextEvent) {

    try {
      componentPool.returnInstance(this);
    } catch (Exception exception) {
      LoggerManager.getLogger(JavaContextComponentInstance.class).error(exception);
    }
  }

  public void contextAborted (JavaContextEvent javaContextEvent) {

    Exception reportedException = javaContextEvent.getCommunicationException();

    try {
      componentPool.terminateInstance(this);
    } catch (Exception exception) {
      if ((reportedException != null) && (exception.getCause() == exception)) {
        exception.initCause(reportedException);
      }

      reportedException = exception;
    } finally {
      if (reportedException != null) {
        componentPool.reportErrorOccurred(reportedException);
        LoggerManager.getLogger(JavaContextComponentInstance.class).error(reportedException);
      }
    }
  }

  public PooledJavaContext serve ()
    throws Exception {

    if (componentPool.getComplexPoolConfig().isExistentiallyAware()) {
      stackTraceReference.set(Thread.currentThread().getStackTrace());
    }

    return pooledJavaContext;
  }

  public void close ()
    throws Exception {

    if (closed.compareAndSet(false, true)) {
      if (componentPool.getComplexPoolConfig().isExistentiallyAware()) {
        stackTraceReference.set(null);
      }

      if (pooledJavaContext != null) {
        pooledJavaContext.close(true);
      }
    }
  }
}