/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.persistence.sql;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class OmnivorousConnectionPoolDataSource<D extends CommonDataSource, P extends PooledConnection> implements ConnectionPoolDataSource {

  private D dataSource;
  private Class<P> pooledConnectionClass;
  private int maxStatements = 0;

  public OmnivorousConnectionPoolDataSource (D dataSource, Class<P> pooledConnectionClass) {

    this.dataSource = dataSource;
    this.pooledConnectionClass = pooledConnectionClass;
  }

  public OmnivorousConnectionPoolDataSource (D dataSource, Class<P> pooledConnectionClass, int maxStatements) {

    this(dataSource, pooledConnectionClass);

    this.maxStatements = maxStatements;
  }

  public P getPooledConnection ()
    throws SQLException {

    return pooledConnectionClass.cast(PooledConnectionFactory.createPooledConnection(dataSource, maxStatements));
  }

  public P getPooledConnection (String user, String password)
    throws SQLException {

    return pooledConnectionClass.cast(PooledConnectionFactory.createPooledConnection(dataSource, user, password, maxStatements));
  }

  public Logger getParentLogger () throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  public PrintWriter getLogWriter ()
    throws SQLException {

    return dataSource.getLogWriter();
  }

  public void setLogWriter (PrintWriter out)
    throws SQLException {

    dataSource.setLogWriter(out);
  }

  public void setLoginTimeout (int seconds)
    throws SQLException {

    dataSource.setLoginTimeout(seconds);
  }

  public int getLoginTimeout ()
    throws SQLException {

    return dataSource.getLoginTimeout();
  }
}