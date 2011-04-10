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
package org.smallmind.persistence.orm.sql;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;
import org.smallmind.nutsnbolts.lang.Existential;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class DriverManagerPooledConnection implements PooledConnection, Existential, InvocationHandler {

  private static final Method CLOSE_METHOD;
  private static final Method GET_EXISTENTIAL_STACK_TRACE_METHOD;

  private final DriverManagerPreparedStatementCache statementCache;
  private final AtomicReference<StackTraceElement[]> stackTraceReference = new AtomicReference<StackTraceElement[]>();

  private DataSource dataSource;
  private Connection actualConnection;
  private Connection proxyConnection;
  private ConcurrentLinkedQueue<ConnectionEventListener> connectionEventListenerQueue;
  private ConcurrentLinkedQueue<StatementEventListener> statementEventListenerQueue;
  private AtomicBoolean closed = new AtomicBoolean(false);
  private boolean existential;
  private long creationMilliseconds;

  static {

    try {
      CLOSE_METHOD = Connection.class.getMethod("close");
      GET_EXISTENTIAL_STACK_TRACE_METHOD = Existential.class.getMethod("getExistentialStackTrace");
    }
    catch (NoSuchMethodException noSuchMethodException) {
      throw new StaticInitializationError(noSuchMethodException);
    }
  }

  public DriverManagerPooledConnection (DriverManagerDataSource dataSource, int maxStatements)
    throws SQLException {

    this(dataSource, null, null, maxStatements, false);
  }

  public DriverManagerPooledConnection (DriverManagerDataSource dataSource, int maxStatements, boolean existential)
    throws SQLException {

    this(dataSource, null, null, maxStatements, existential);
  }

  public DriverManagerPooledConnection (DriverManagerDataSource dataSource, String user, String password, int maxStatements)
    throws SQLException {

    this(dataSource, user, password, maxStatements, false);
  }

  public DriverManagerPooledConnection (DriverManagerDataSource dataSource, String user, String password, int maxStatements, boolean existential)
    throws SQLException {

    this.dataSource = dataSource;
    this.existential = existential;

    if (maxStatements < 0) {
      throw new SQLException("The maximum number of cached statements for this connection must be >= 0");
    }

    creationMilliseconds = System.currentTimeMillis();

    if ((user != null) && (password != null)) {
      actualConnection = dataSource.getConnection(user, password);
    }
    else {
      actualConnection = dataSource.getConnection();
    }

    proxyConnection = (Connection)Proxy.newProxyInstance(DriverManagerDataSource.class.getClassLoader(), new Class[] {Connection.class, Existential.class}, this);

    connectionEventListenerQueue = new ConcurrentLinkedQueue<ConnectionEventListener>();
    statementEventListenerQueue = new ConcurrentLinkedQueue<StatementEventListener>();

    if (maxStatements == 0) {
      statementCache = null;
    }
    else {
      addStatementEventListener(statementCache = new DriverManagerPreparedStatementCache(maxStatements));
    }
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return stackTraceReference.get();
  }

  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable {

    if (GET_EXISTENTIAL_STACK_TRACE_METHOD.equals(method)) {

      return getExistentialStackTrace();
    }
    if (CLOSE_METHOD.equals(method)) {
      if (existential) {
        stackTraceReference.set(null);
      }

      ConnectionEvent event = new ConnectionEvent(this);

      for (ConnectionEventListener listener : connectionEventListenerQueue) {
        listener.connectionClosed(event);
      }

      return null;
    }
    else {
      try {
        if ((statementCache != null) && PreparedStatement.class.isAssignableFrom(method.getReturnType())) {

          PreparedStatement preparedStatement;

          synchronized (statementCache) {
            if ((preparedStatement = statementCache.getPreparedStatement(args)) == null) {
              preparedStatement = statementCache.cachePreparedStatement(args, new DriverManagerPooledPreparedStatement(this, (PreparedStatement)method.invoke(actualConnection, args)));
            }
          }

          return preparedStatement;
        }
        else {

          return method.invoke(actualConnection, args);
        }
      }
      catch (Throwable throwable) {

        Throwable closestCause;

        closestCause = ((throwable instanceof UndeclaredThrowableException) && (throwable.getCause() != null)) ? throwable.getCause() : throwable;

        if (closestCause instanceof SQLException) {

          ConnectionEvent event = new ConnectionEvent(this, (SQLException)closestCause);

          for (ConnectionEventListener listener : connectionEventListenerQueue) {
            listener.connectionErrorOccurred(event);
          }
        }

        throw new PooledConnectionException(closestCause, "Connection encountered an exception after operation for %d milliseconds", System.currentTimeMillis() - creationMilliseconds);
      }
    }
  }

  public PrintWriter getLogWriter ()
    throws SQLException {

    return dataSource.getLogWriter();
  }

  public Connection getConnection ()
    throws SQLException {

    if (existential) {
      stackTraceReference.set(Thread.currentThread().getStackTrace());
    }

    return proxyConnection;
  }

  public void close ()
    throws SQLException {

    if (closed.compareAndSet(false, true)) {
      try {
        if (statementCache != null) {
          try {
            statementCache.close();
          }
          finally {
            removeStatementEventListener(statementCache);
          }
        }
      }
      finally {
        actualConnection.close();
      }
    }
  }

  public void finalize ()
    throws SQLException {

    try {
      close();
    }
    catch (SQLException sqlExecption) {

      PrintWriter logWriter;

      if ((logWriter = getLogWriter()) != null) {
        sqlExecption.printStackTrace(logWriter);
      }
    }
  }

  public void addConnectionEventListener (ConnectionEventListener listener) {

    connectionEventListenerQueue.add(listener);
  }

  public void removeConnectionEventListener (ConnectionEventListener listener) {

    connectionEventListenerQueue.remove(listener);
  }

  protected Iterable<StatementEventListener> getStatementEventListeners () {

    return statementEventListenerQueue;
  }

  public void addStatementEventListener (StatementEventListener listener) {

    statementEventListenerQueue.add(listener);
  }

  public void removeStatementEventListener (StatementEventListener listener) {

    statementEventListenerQueue.remove(listener);
  }
}
