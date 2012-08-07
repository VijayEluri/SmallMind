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
package org.smallmind.instrument.config;

import java.util.concurrent.TimeUnit;
import org.smallmind.instrument.Samples;

// This class has no direct use in the instrumentation API, but is made available for use by instrumentation clients
public class MetricConfiguration {

  private MetricDomain metricDomain = new UnsetMetricDomain();
  private Samples samples = Samples.BIASED;
  private TimeUnit tickTimeUnit = TimeUnit.SECONDS;
  private boolean instrumented = true;
  private long tickInterval = 10;

  public boolean isInstrumented () {

    return instrumented;
  }

  public void setInstrumented (boolean instrumented) {

    this.instrumented = instrumented;
  }

  public MetricDomain getMetricDomain () {

    return metricDomain;
  }

  public void setMetricDomain (MetricDomain metricDomain) {

    this.metricDomain = metricDomain;
  }

  public Samples getSamples () {

    return samples;
  }

  public void setSamples (Samples samples) {

    this.samples = samples;
  }

  public long getTickInterval () {

    return tickInterval;
  }

  public void setTickInterval (long tickInterval) {

    this.tickInterval = tickInterval;
  }

  public TimeUnit getTickTimeUnit () {

    return tickTimeUnit;
  }

  public void setTickTimeUnit (TimeUnit tickTimeUnit) {

    this.tickTimeUnit = tickTimeUnit;
  }
}
