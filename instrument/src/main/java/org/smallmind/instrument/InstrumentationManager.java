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

import org.smallmind.nutsnbolts.lang.StaticManager;

public class InstrumentationManager implements StaticManager {

  private static InheritableThreadLocal<MetricRegistry> METRIC_REGISTRY_LOCAL = new InheritableThreadLocal<MetricRegistry>();

  public static void register (MetricRegistry metricRegistry) {

    METRIC_REGISTRY_LOCAL.set(metricRegistry);
  }

  public static MetricRegistry getMetricRegistry () {

    return METRIC_REGISTRY_LOCAL.get();
  }

  public static <M extends Metric> void execute (Instrument<M> instrument)
    throws Exception {

    MetricRegistry metricRegistry;
    InstrumentationArguments<M> arguments;

    instrument.with((((metricRegistry = METRIC_REGISTRY_LOCAL.get()) == null) || ((arguments = instrument.getArguments()) == null)) ? null : metricRegistry.instrument(arguments.getBuilder(), arguments.getDomain(), arguments.getProperties()));
  }

  public static <M extends Metric, T> T execute (InstrumentAndReturn<M, T> instrumentAndReturn)
    throws Exception {

    MetricRegistry metricRegistry;
    InstrumentationArguments<M> arguments;

    return instrumentAndReturn.with((((metricRegistry = METRIC_REGISTRY_LOCAL.get()) == null) || ((arguments = instrumentAndReturn.getArguments()) == null)) ? null : metricRegistry.instrument(arguments.getBuilder(), arguments.getDomain(), arguments.getProperties()));
  }
}