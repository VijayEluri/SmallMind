/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.quorum.pool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class PoolConfig<P extends PoolConfig> {

  private final AtomicLong acquireWaitTimeMillis = new AtomicLong(0);
  private final AtomicInteger maxPoolSize = new AtomicInteger(10);

  public PoolConfig () {

  }

  public PoolConfig (PoolConfig<?> poolConfig) {

    setAcquireWaitTimeMillis(poolConfig.getAcquireWaitTimeMillis());
    setMaxPoolSize(poolConfig.getMaxPoolSize());
  }

  public abstract Class<P> getConfigurationClass ();

  public int getMaxPoolSize () {

    return maxPoolSize.get();
  }

  public P setMaxPoolSize (int maxPoolSize) {

    if (maxPoolSize < 0) {
      throw new IllegalArgumentException("Maximum pool size must be >= 0");
    }

    this.maxPoolSize.set(maxPoolSize);

    return getConfigurationClass().cast(this);
  }

  public long getAcquireWaitTimeMillis () {

    return acquireWaitTimeMillis.get();
  }

  public P setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    if (acquireWaitTimeMillis < 0) {
      throw new IllegalArgumentException("Acquire wait time must be >= 0");
    }

    this.acquireWaitTimeMillis.set(acquireWaitTimeMillis);

    return getConfigurationClass().cast(this);
  }
}
