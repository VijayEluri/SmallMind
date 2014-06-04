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
package org.smallmind.persistence.sql.pool;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.DataSourcePerApplicationManager;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class DefaultPooledDataSource extends AbstractPooledDataSource {

  private ComponentPool<PooledConnection> componentPool;
  private String key;

  public DefaultPooledDataSource (ComponentPool<PooledConnection> componentPool) {

    this(componentPool.getPoolName(), componentPool);
  }

  public DefaultPooledDataSource (String key, ComponentPool<PooledConnection> componentPool) {

    this.key = key;
    this.componentPool = componentPool;
  }

  public void register () {

    DataSourcePerApplicationManager.register(key, this);
  }

  public String getKey () {

    return key;
  }

  public Connection getConnection ()
    throws SQLException {

    try {
      return componentPool.getComponent().getConnection();
    }
    catch (ComponentPoolException componentPoolException) {
      throw new SQLException(componentPoolException);
    }
  }

  @Override
  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  @Override
  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }
}
