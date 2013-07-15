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
package org.smallmind.nutsnbolts.util;

import java.util.Random;

public class ThreadLocalRandom extends Random {

  private static final long serialVersionUID = -5851777807851030925L;

  // same constants as Random, but must be re-declared because private
  private final static long multiplier = 0x5DEECE66DL;
  private final static long addend = 0xBL;
  private final static long mask = (1L << 48) - 1;

  /**
   * The random seed. We can't use super.seed.
   */
  private long rnd;

  /**
   * Initialization flag to permit calls to setSeed to succeed only
   * while executing the Random constructor.  We can't allow others
   * since it would cause setting seed in one part of a program to
   * unintentionally impact other usages by the thread.
   */
  boolean initialized;

  // Padding to help avoid memory contention among seed updates in
  // different TLRs in the common case that they are located near
  // each other.
  private long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7;

  /**
   * The actual ThreadLocal
   */
  private static final ThreadLocal<ThreadLocalRandom> localRandom =
    new ThreadLocal<ThreadLocalRandom>() {

      protected ThreadLocalRandom initialValue () {

        return new ThreadLocalRandom();
      }
    };

  /**
   * Constructor called only by localRandom.initialValue.
   */
  ThreadLocalRandom () {

    super();
    initialized = true;
  }

  /**
   * Returns the current thread's {@code ThreadLocalRandom}.
   *
   * @return the current thread's {@code ThreadLocalRandom}
   */
  public static ThreadLocalRandom current () {

    return localRandom.get();
  }

  /**
   * Throws {@code UnsupportedOperationException}.  Setting seeds in
   * this generator is not supported.
   *
   * @throws UnsupportedOperationException always
   */
  public void setSeed (long seed) {

    if (initialized)
      throw new UnsupportedOperationException();
    rnd = (seed ^ multiplier) & mask;
  }

  protected int next (int bits) {

    rnd = (rnd * multiplier + addend) & mask;
    return (int)(rnd >>> (48 - bits));
  }

  /**
   * Returns a pseudorandom, uniformly distributed value between the
   * given least value (inclusive) and bound (exclusive).
   *
   * @param least the least value returned
   * @param bound the upper bound (exclusive)
   * @return the next value
   * @throws IllegalArgumentException if least greater than or equal
   *                                  to bound
   */
  public int nextInt (int least, int bound) {

    if (least >= bound)
      throw new IllegalArgumentException();

    return nextInt(bound - least) + least;
  }

  /**
   * Returns a pseudorandom, uniformly distributed value
   * between 0 (inclusive) and the specified value (exclusive).
   *
   * @param n the bound on the random number to be returned.  Must be
   *          positive.
   * @return the next value
   * @throws IllegalArgumentException if n is not positive
   */
  public long nextLong (long n) {

    if (n <= 0)
      throw new IllegalArgumentException("n must be positive");
    // Divide n by two until small enough for nextInt. On each
    // iteration (at most 31 of them but usually much less),
    // randomly choose both whether to include high bit in result
    // (offset) and whether to continue with the lower vs upper
    // half (which makes a difference only if odd).
    long offset = 0;
    while (n >= Integer.MAX_VALUE) {
      int bits = next(2);
      long half = n >>> 1;
      long nextn = ((bits & 2) == 0) ? half : n - half;
      if ((bits & 1) == 0)
        offset += n - nextn;
      n = nextn;
    }
    return offset + nextInt((int)n);
  }

  /**
   * Returns a pseudorandom, uniformly distributed value between the
   * given least value (inclusive) and bound (exclusive).
   *
   * @param least the least value returned
   * @param bound the upper bound (exclusive)
   * @return the next value
   * @throws IllegalArgumentException if least greater than or equal
   *                                  to bound
   */
  public long nextLong (long least, long bound) {

    if (least >= bound)
      throw new IllegalArgumentException();
    return nextLong(bound - least) + least;
  }

  /**
   * Returns a pseudorandom, uniformly distributed {@code double} value
   * between 0 (inclusive) and the specified value (exclusive).
   *
   * @param n the bound on the random number to be returned.  Must be
   *          positive.
   * @return the next value
   * @throws IllegalArgumentException if n is not positive
   */
  public double nextDouble (double n) {

    if (n <= 0)
      throw new IllegalArgumentException("n must be positive");
    return nextDouble() * n;
  }

  /**
   * Returns a pseudorandom, uniformly distributed value between the
   * given least value (inclusive) and bound (exclusive).
   *
   * @param least the least value returned
   * @param bound the upper bound (exclusive)
   * @return the next value
   * @throws IllegalArgumentException if least greater than or equal
   *                                  to bound
   */
  public double nextDouble (double least, double bound) {

    if (least >= bound)
      throw new IllegalArgumentException();
    return nextDouble() * (bound - least) + least;
  }
}
