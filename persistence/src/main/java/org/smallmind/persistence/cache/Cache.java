/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.cache;

public interface Cache<K, V> {

  public abstract long getDefaultTimeToLiveMilliseconds ();

  public abstract V get (K key)
    throws CacheOperationException;

  public abstract void set (K key, V value, long timeToLiveMilliseconds)
    throws CacheOperationException;

  public abstract V putIfAbsent (K key, V value, long timeToLiveMilliseconds)
    throws CacheOperationException;

  public abstract CASValue<V> getViaCas (K key);

  public abstract boolean putViaCas (K key, V value, long version, long timeToLiveMilliseconds)
    throws CacheOperationException;

  public abstract void remove (K key)
    throws CacheOperationException;
}
