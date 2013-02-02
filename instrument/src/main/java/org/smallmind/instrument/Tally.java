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
package org.smallmind.instrument;

import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.instrument.context.MetricItem;
import org.smallmind.instrument.context.MetricSnapshot;

public class Tally extends Metric implements Countable {

  private final AtomicLong count;

  public Tally () {

    this(0);
  }

  public Tally (int initialCount) {

    this.count = new AtomicLong(initialCount);
  }

  @Override
  public void clear () {

    MetricSnapshot snapshot;

    count.set(0);

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("count", 0L));
    }
  }

  public void inc () {

    MetricSnapshot snapshot;
    long current;

    current = count.incrementAndGet();

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("count", current));
    }
  }

  public void inc (long n) {

    MetricSnapshot snapshot;
    long current;

    current = count.addAndGet(n);

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("count", current));
    }
  }

  public void dec () {

    MetricSnapshot snapshot;
    long current;

    current = count.decrementAndGet();

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("count", current));
    }
  }

  public void dec (long n) {

    MetricSnapshot snapshot;
    long current;

    current = count.addAndGet(0 - n);

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("count", current));
    }
  }

  @Override
  public long getCount () {

    return count.get();
  }
}
