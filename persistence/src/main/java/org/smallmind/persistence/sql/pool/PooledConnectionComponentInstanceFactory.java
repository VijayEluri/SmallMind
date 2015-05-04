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

public class PooledConnectionComponentInstanceFactory<P extends PooledConnection> implements ComponentInstanceFactory<P> {

  private Juggler<ConnectionPoolDataSource, P> pooledConnectionJuggler;
  private String validationQuery = "select 1";

  public PooledConnectionComponentInstanceFactory (Class<P> pooledConnectionClass, ConnectionPoolDataSource... dataSources) {

    this(0, pooledConnectionClass, dataSources);
  }

  public PooledConnectionComponentInstanceFactory (int recoveryCheckSeconds, Class<P> pooledConnectionClass, ConnectionPoolDataSource... dataSources) {

    pooledConnectionJuggler = new Juggler<>(ConnectionPoolDataSource.class, pooledConnectionClass, recoveryCheckSeconds, new PooledConnectionJugglingPinFactory<P>(), dataSources);
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

  public ComponentInstance<P> createInstance (ComponentPool<P> componentPool)
    throws NoAvailableJugglerResourceException, SQLException {

    return new PooledConnectionComponentInstance<>(componentPool, pooledConnectionJuggler.pickResource(), validationQuery);
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
