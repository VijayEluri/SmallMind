/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.persistence.orm;

import java.io.Serializable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.CacheAwareDurableDao;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.instrument.MetricSource;

public abstract class AbstractOrmDao<I extends Serializable & Comparable<I>, D extends Durable<I>, N> extends CacheAwareDurableDao<I, D> implements ORMDao<I, D, N> {

  private ProxySession<N> proxySession;

  public AbstractOrmDao (ProxySession<N> proxySession, VectoredDao<I, D> vectoredDao) {

    super(vectoredDao);

    this.proxySession = proxySession;
  }

  public void register () {

    DaoManager.register(getManagedClass(), this);
  }

  @Override
  public String getMetricSource () {

    return MetricSource.ORM.getDisplay();
  }

  @Override
  public String getDataSource () {

    return proxySession.getDataSource();
  }

  @Override
  public ProxySession<N> getSession () {

    return proxySession;
  }

  @Override
  public boolean isCacheEnabled () {

    return proxySession.isCacheEnabled();
  }

  @Override
  public D get (I id) {

    return get(getManagedClass(), id);
  }

  @Override
  public D persist (D durable) {

    return persist(getManagedClass(), durable);
  }

  @Override
  public void delete (D durable) {

    delete(getManagedClass(), durable);
  }
}
