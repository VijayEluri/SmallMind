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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import org.smallmind.persistence.Durable;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class WideDurableKey<W extends Serializable & Comparable<W>, D extends Durable<?>> implements Serializable {

  private Class<D> durableClass;
  private String key;

  public WideDurableKey (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass) {

    this.durableClass = durableClass;

    StringBuilder keyBuilder = new StringBuilder(context);

    keyBuilder.append('.').append((parentClass.getSimpleName())).append('[').append(durableClass.getSimpleName()).append(']').append('=').append(parentId);

    key = keyBuilder.toString();
  }

  public Class<D> getDurableClass () {

    return durableClass;
  }

  public String getKey () {

    return key;
  }

  public String getParentIdAsString () {

    return key.substring(key.indexOf('=') + 1);
  }

  public String toString () {

    return key;
  }

  public int hashCode () {

    return key.hashCode();
  }

  public boolean equals (Object obj) {

    return (obj instanceof WideDurableKey) && key.equals(((WideDurableKey)obj).getKey());
  }
}
