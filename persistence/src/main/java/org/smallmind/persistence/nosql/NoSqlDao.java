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
package org.smallmind.persistence.nosql;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.smallmind.persistence.AbstractWideVectorAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.WideDurableDao;
import org.smallmind.persistence.cache.WideVectoredDao;

public abstract class NoSqlDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractWideVectorAwareManagedDao<W, I, D> implements WideDurableDao<W, I, D> {

  private boolean cacheEnabled;

  public NoSqlDao (String metricSource, WideVectoredDao<W, I, D> wideVectoredDao, boolean cacheEnabled) {

    super(metricSource, wideVectoredDao);

    this.cacheEnabled = cacheEnabled;
  }

  @Override
  public boolean isCacheEnabled () {

    return cacheEnabled;
  }

  @Override
  public List<D> get (Class<Durable<W>> parentClass, W parentId) {

    return get(parentClass, parentId, getManagedClass());
  }

  @Override
  public List<D> persist (Class<Durable<W>> parentClass, W parentId, D... durables) {

    return persist(parentClass, parentId, getManagedClass(), durables);
  }

  @Override
  public List<D> persist (Class<Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables) {

    return persist(parentClass, parentId, durableClass, Arrays.asList(durables));
  }

  @Override
  public void delete (Class<Durable<W>> parentClass, W parentId, D... durables) {

    delete(parentClass, parentId, getManagedClass(), durables);
  }

  @Override
  public void delete (Class<Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables) {

    delete(parentClass, parentId, durableClass, Arrays.asList(durables));
  }
}
