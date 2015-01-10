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
package org.smallmind.seda;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class StopWatch {

  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final boolean CURRENT_THREAD_CPU_TIME_SUPPORTED = THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported();

  private boolean initialized = false;
  private long startMillis;
  private long clickClockMillis;
  private long clickClockNanos;
  private long durationNanos;
  private long clickCPUNanos;
  private long cpuTimeNanos;

  public StopWatch click () {

    if (!initialized) {
      initialized = true;

      clickClockMillis = System.currentTimeMillis();
      clickClockNanos = System.nanoTime();
    }
    else {

      long stopNanos;

      startMillis = clickClockMillis;
      clickClockMillis = System.currentTimeMillis();

      durationNanos = (stopNanos = System.nanoTime()) - clickClockNanos;
      clickClockNanos = stopNanos;

      if (!CURRENT_THREAD_CPU_TIME_SUPPORTED) {
        cpuTimeNanos = durationNanos;
      }
      else {

        long stopCPUNanos;

        cpuTimeNanos = (stopCPUNanos = THREAD_MX_BEAN.getCurrentThreadCpuTime()) - clickCPUNanos;
        clickCPUNanos = stopCPUNanos;
      }
    }

    return this;
  }

  public long getStartMillis () {

    return startMillis;
  }

  public long getDurationNanos () {

    return durationNanos;
  }

  public long getCpuTimeNanos () {

    return cpuTimeNanos;
  }
}
