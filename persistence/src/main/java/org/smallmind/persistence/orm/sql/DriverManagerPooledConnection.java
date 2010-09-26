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
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

public class DriverManagerPooledConnection implements PooledConnection, InvocationHandler {

   private final DriverManagerPreparedStatementCache statementCache;

   private DataSource dataSource;
   private Connection actualConnection;
   private Connection proxyConnection;
   private LinkedList<ConnectionEventListener> connectionEventListenerList;
   private LinkedList<StatementEventListener> statementEventListenerList;
   private AtomicBoolean closed = new AtomicBoolean(false);
   private long creationMilliseconds;

   public DriverManagerPooledConnection (DriverManagerDataSource dataSource, int maxStatements)
      throws SQLException {

      this(dataSource, null, null, maxStatements);
   }

   public DriverManagerPooledConnection (DriverManagerDataSource dataSource, String user, String password, int maxStatements)
      throws SQLException {

      this.dataSource = dataSource;

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

      proxyConnection = (Connection)Proxy.newProxyInstance(DriverManagerDataSource.class.getClassLoader(), new Class[] {Connection.class}, this);

      connectionEventListenerList = new LinkedList<ConnectionEventListener>();
      statementEventListenerList = new LinkedList<StatementEventListener>();

      if (maxStatements == 0) {
         statementCache = null;
      }
      else {
         addStatementEventListener(statementCache = new DriverManagerPreparedStatementCache(maxStatements));
      }
   }

   public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      if (method.getName().equals("close")) {

         ConnectionEvent event = new ConnectionEvent(this);

         for (ConnectionEventListener listener : connectionEventListenerList) {
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

               for (ConnectionEventListener listener : connectionEventListenerList) {
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

      return proxyConnection;
   }

   public void close ()
      throws SQLException {

      if (closed.compareAndSet(false, true)) {
         if (statementCache != null) {
            statementCache.close();
         }

         actualConnection.close();
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

      connectionEventListenerList.add(listener);
   }

   public void removeConnectionEventListener (ConnectionEventListener listener) {

      connectionEventListenerList.remove(listener);
   }

   protected Iterable<StatementEventListener> getStatementEventListeners () {

      return statementEventListenerList;
   }

   public void addStatementEventListener (StatementEventListener listener) {

      statementEventListenerList.add(listener);
   }

   public void removeStatementEventListener (StatementEventListener listener) {

      statementEventListenerList.remove(listener);
   }
}
