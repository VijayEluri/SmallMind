/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.persistence;

import java.io.Serializable;
import java.util.List;

public interface WideDurableDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> {

  public abstract void remove (String context, Class<? extends Durable<W>> parentClass, W parentId);

  public abstract void remove (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  public abstract List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId);

  public abstract List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  public abstract List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables);

  public abstract List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables);

  public abstract List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables);

  public abstract List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);

  public abstract void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables);

  public abstract void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables);

  public abstract void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables);

  public abstract void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);
}
