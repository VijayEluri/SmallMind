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
package org.smallmind.instrument;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.nutsnbolts.util.ThreadLocalRandom;

/**
 * Uses Cormode et al's forward-decayiny priority reservoir sampling method to produce a statistically
 * representative sample, exponentially biased towards newer entries.
 */
public class ExponentiallyDecayingSample implements Sample {

  private static final long RESCALE_THRESHOLD = TimeUnit.HOURS.toMillis(1);

  private volatile long startTime;

  private final ReentrantReadWriteLock lock;
  private final ConcurrentSkipListMap<Double, Long> values;
  private final Clock clock;
  private final AtomicLong count = new AtomicLong(0);
  private final AtomicLong nextScaleTime = new AtomicLong(0);
  private final double alpha;
  private final int reservoirSize;

  // alpha - the exponential decay factor; the higher this is, the more biased the sample will be towards newer values
  public ExponentiallyDecayingSample (int reservoirSize, double alpha) {

    this(reservoirSize, alpha, Clocks.NANO.getClock());
  }

  public ExponentiallyDecayingSample (int reservoirSize, double alpha, Clock clock) {

    this.reservoirSize = reservoirSize;
    this.alpha = alpha;
    this.clock = clock;

    values = new ConcurrentSkipListMap<Double, Long>();
    startTime = currentTimeInSeconds();
    nextScaleTime.set(clock.getTime() + RESCALE_THRESHOLD);

    lock = new ReentrantReadWriteLock();
  }

  @Override
  public Samples getType () {

    return Samples.BIASED;
  }

  @Override
  public void clear () {

    lock.writeLock().lock();
    try {
      values.clear();
      count.set(0);
      startTime = currentTimeInSeconds();
      nextScaleTime.set(clock.getTime() + RESCALE_THRESHOLD);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int size () {

    return (int)Math.min(reservoirSize, count.get());
  }

  @Override
  public void update (long value) {

    rescaleIfNeeded();

    lock.readLock().lock();
    try {

      final double priority = weight(currentTimeInSeconds() - startTime) / ThreadLocalRandom.current().nextDouble();
      final long newCount = count.incrementAndGet();

      if (newCount <= reservoirSize) {
        values.put(priority, value);
      }
      else {
        Double first = values.firstKey();
        if (first < priority) {
          if (values.putIfAbsent(priority, value) == null) {
            // ensure we always remove an item
            while (values.remove(first) == null) {
              first = values.firstKey();
            }
          }
        }
      }
    }
    finally {
      lock.readLock().unlock();
    }
  }

  private void rescaleIfNeeded () {

    long now = clock.getTime();
    long next = nextScaleTime.get();

    if (now >= next) {
      rescale(now, next);
    }
  }

  @Override
  public Snapshot getSnapshot () {

    lock.readLock().lock();
    try {
      return new Snapshot(values.values());
    }
    finally {
      lock.readLock().unlock();
    }
  }

  private long currentTimeInSeconds () {

    return TimeUnit.MILLISECONDS.toSeconds(clock.getTime());
  }

  private double weight (long t) {

    return Math.exp(alpha * t);
  }

  /* "A common feature of the above techniques—indeed, the key technique that
  * allows us to track the decayed weights efficiently—is that they maintain
  * counts and other quantities based on g(ti − L), and only scale by g(t − L)
  * at query time. But while g(ti −L)/g(t−L) is guaranteed to lie between zero
  * and one, the intermediate values of g(ti − L) could become very large. For
  * polynomial functions, these values should not grow too large, and should be
  * effectively represented in practice by floating point values without loss of
  * precision. For exponential functions, these values could grow quite large as
  * new values of (ti − L) become large, and potentially exceed the capacity of
  * common floating point types. However, since the values stored by the
  * algorithms are linear combinations of g values (scaled sums), they can be
  * rescaled relative to a new landmark. That is, by the analysis of exponential
  * decay in Section III-A, the choice of L does not affect the final result. We
  * can therefore multiply each value based on L by a factor of exp(−α(L′ − L)),
  * and obtain the correct value as if we had instead computed relative to a new
  * landmark L′ (and then use this new L′ at query time). This can be done with
  * a linear pass over whatever data structure is being used."
  */
  private void rescale (long now, long next) {

    if (nextScaleTime.compareAndSet(next, now + RESCALE_THRESHOLD)) {
      lock.writeLock().lock();
      try {
        final long oldStartTime = startTime;
        this.startTime = currentTimeInSeconds();
        final ArrayList<Double> keys = new ArrayList<Double>(values.keySet());
        for (Double key : keys) {
          final Long value = values.remove(key);
          values.put(key * Math.exp(-alpha * (startTime - oldStartTime)), value);
        }

        // make sure the counter is in sync with the number of stored samples.
        count.set(values.size());
      }
      finally {
        lock.writeLock().unlock();
      }
    }
  }
}
