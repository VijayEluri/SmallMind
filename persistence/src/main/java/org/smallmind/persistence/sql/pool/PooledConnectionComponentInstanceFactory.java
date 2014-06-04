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

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.juggler.Juggler;
import org.smallmind.quorum.juggler.JugglerResourceCreationException;
import org.smallmind.quorum.juggler.JugglerResourceException;
import org.smallmind.quorum.juggler.NoAvailableJugglerResourceException;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentInstanceFactory;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class PooledConnectionComponentInstanceFactory implements ComponentInstanceFactory<PooledConnection> {

  private Juggler<ConnectionPoolDataSource, PooledConnection> pooledConnectionJuggler;
  private String validationQuery = "select 1";

  public PooledConnectionComponentInstanceFactory (ConnectionPoolDataSource... dataSources) {

    this(0, dataSources);
  }

  public PooledConnectionComponentInstanceFactory (int recoveryCheckSeconds, ConnectionPoolDataSource... dataSources) {

    pooledConnectionJuggler = new Juggler<>(ConnectionPoolDataSource.class, recoveryCheckSeconds, new PooledConnectionJugglingPinFactory(), dataSources);
  }

  @Override
  public void initialize ()
    throws JugglerResourceCreationException {

    pooledConnectionJuggler.initialize();
  }

  @Override
  public void startup ()
    throws JugglerResourceException {

    pooledConnectionJuggler.startup();
  }

  public String getValidationQuery () {

    return validationQuery;
  }

  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  public ComponentInstance<PooledConnection> createInstance (ComponentPool<PooledConnection> componentPool)
    throws NoAvailableJugglerResourceException, SQLException {

    return new PooledConnectionComponentInstance(componentPool, pooledConnectionJuggler.pickResource(), validationQuery);
  }

  @Override
  public void shutdown () {

    pooledConnectionJuggler.shutdown();
  }

  @Override
  public void deconstruct () throws Exception {

    pooledConnectionJuggler.deconstruct();
  }
}
