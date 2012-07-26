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

import java.util.concurrent.TimeUnit;
import org.smallmind.instrument.config.ConfigurationProvider;
import org.smallmind.nutsnbolts.time.TimeUtilities;

public abstract class ChronometerInstrument extends Instrument<Chronometer> {

  public ChronometerInstrument (ConfigurationProvider provider, MetricProperty... properties) {

    super(((provider == null) || (!provider.getMetricConfiguration().isInstrumented())) ? null : new InstrumentationArguments<Chronometer>(Metrics.buildChronometer(provider.getMetricConfiguration().getChronometerSamples(), TimeUnit.MILLISECONDS, provider.getMetricConfiguration().getTickInterval(), provider.getMetricConfiguration().getTickTimeUnit()), provider.getMetricConfiguration().getMetricDomain().getDomain(), properties));
  }

  public ChronometerInstrument (Metrics.MetricBuilder<Chronometer> builder, String domain, MetricProperty... properties) {

    super(new InstrumentationArguments<Chronometer>(builder, domain, properties));
  }

  public abstract void withChronometer ()
    throws Exception;

  @Override
  public final void with (Chronometer chronometer)
    throws Exception {

    long startTime = 0;

    if (chronometer != null) {
      startTime = chronometer.getClock().getTimeNanoseconds();
    }

    withChronometer();

    if (chronometer != null) {
      chronometer.update(TimeUtilities.convertToLong(chronometer.getClock().getTimeNanoseconds() - startTime, TimeUnit.NANOSECONDS, chronometer.getLatencyTimeUnit()));
    }
  }
}