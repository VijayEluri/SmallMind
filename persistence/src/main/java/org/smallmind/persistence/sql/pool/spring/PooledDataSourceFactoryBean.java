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
package org.smallmind.persistence.sql.pool.spring;

import java.sql.SQLException;
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.persistence.sql.pool.DataSourceFactory;
import org.smallmind.persistence.sql.pool.PooledDataSourceFactory;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class PooledDataSourceFactoryBean<D extends CommonDataSource, P extends PooledConnection> implements FactoryBean<CommonDataSource>, InitializingBean, DisposableBean {

  private AbstractPooledDataSource dataSource;
  private DataSourceFactory<D, P> dataSourceFactory;
  private ComplexPoolConfig poolConfig;
  private DatabaseConnection[] connections;
  private String poolName;
  private String validationQuery;
  private int maxStatements;

  public void setPoolName (String poolName) {

    this.poolName = poolName;
  }

  public void setDataSourceFactory (DataSourceFactory<D, P> dataSourceFactory) {

    this.dataSourceFactory = dataSourceFactory;
  }

  public void setConnections (DatabaseConnection[] connections) {

    this.connections = connections;
  }

  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  public void setMaxStatements (int maxStatements) {

    this.maxStatements = maxStatements;
  }

  public void setPoolConfig (ComplexPoolConfig poolConfig) {

    this.poolConfig = poolConfig;
  }

  @Override
  public void afterPropertiesSet ()
    throws SQLException, ComponentPoolException {

    dataSource = PooledDataSourceFactory.createPooledDataSource(poolName, dataSourceFactory, validationQuery, maxStatements, poolConfig, connections);
    dataSource.startup();
  }

  @Override
  public void destroy ()
    throws ComponentPoolException {

    dataSource.shutdown();
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return CommonDataSource.class;
  }

  @Override
  public CommonDataSource getObject () {

    return dataSource;
  }
}