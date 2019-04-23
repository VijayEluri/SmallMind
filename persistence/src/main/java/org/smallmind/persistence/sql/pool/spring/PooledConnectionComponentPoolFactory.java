/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.persistence.sql.pool.spring;

import java.sql.SQLException;
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.pool.ConnectionEndpoint;
import org.smallmind.persistence.sql.pool.DataSourceComponentInstanceFactory;
import org.smallmind.persistence.sql.pool.DataSourceFactory;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class PooledConnectionComponentPoolFactory {

  public static <D extends CommonDataSource> ComponentPool<? extends PooledConnection> constructComponentPool (String poolName, DataSourceFactory<D, ? extends PooledConnection> dataSourceFactory, String validationQuery, int maxStatements, ComplexPoolConfig poolConfig, DatabaseConnection... connections)
    throws SQLException, ComponentPoolException {

    DataSourceComponentInstanceFactory<D, ? extends PooledConnection> connectionInstanceFactory = new DataSourceComponentInstanceFactory<>(dataSourceFactory, maxStatements, createConnectionEndpoints(connections));

    if (validationQuery != null) {
      connectionInstanceFactory.setValidationQuery(validationQuery);
    }

    return new ComponentPool<>(poolName, connectionInstanceFactory).setComplexPoolConfig(poolConfig);
  }

  private static ConnectionEndpoint[] createConnectionEndpoints (DatabaseConnection... databaseConnections) {

    if (databaseConnections == null) {

      return new ConnectionEndpoint[0];
    } else {

      ConnectionEndpoint[] connectionEndpoints = new ConnectionEndpoint[databaseConnections.length];

      for (int count = 0; count < connectionEndpoints.length; count++) {
        connectionEndpoints[count] = new ConnectionEndpoint(databaseConnections[count].getJdbcUrl(), databaseConnections[count].getUser(), databaseConnections[count].getPassword());
      }

      return connectionEndpoints;
    }
  }
}
