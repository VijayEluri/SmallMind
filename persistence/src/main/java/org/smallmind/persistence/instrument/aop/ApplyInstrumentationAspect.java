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
package org.smallmind.persistence.instrument.aop;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.MetricRegistryFactory;
import org.smallmind.instrument.Metrics;
import org.smallmind.persistence.Persistence;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.orm.VectorAwareORMDao;

@Aspect
public class ApplyInstrumentationAspect {

  private static final Persistence PERSISTENCE;
  private static final MetricRegistry METRIC_REGISTRY;

  static {

    if (((PERSISTENCE = PersistenceManager.getPersistence()) == null) || (!PERSISTENCE.getMetricConfiguration().isInstrumented())) {
      METRIC_REGISTRY = null;
    }
    else {
      if ((METRIC_REGISTRY = MetricRegistryFactory.getMetricRegistry()) == null) {
        throw new ExceptionInInitializerError("No MetricRegistry instance has been registered with the MetricRegistryFactory");
      }
    }
  }

  @Around(value = "@within(instrumented) && execution(@org.smallmind.persistence.instrument.aop.ApplyInstrumentation * * (..)) && this(ormDao)", argNames = "thisJoinPoint, instrumented, ormDao")
  public Object aroundApplyStatisticsMethod (ProceedingJoinPoint thisJoinPoint, Instrumented instrumented, VectorAwareORMDao ormDao)
    throws Throwable {

    Method executedMethod;
    boolean timingEnabled;
    long start = 0;
    long stop;

    if (timingEnabled = (METRIC_REGISTRY != null) && instrumented.value()) {
      start = System.currentTimeMillis();
    }

    try {

      return thisJoinPoint.proceed();
    }
    finally {
      if (timingEnabled) {
        stop = System.currentTimeMillis();
        executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();

        METRIC_REGISTRY.ensure(Metrics.buildChronometer(PERSISTENCE.getMetricConfiguration().getChronometerSamples(), TimeUnit.MILLISECONDS, PERSISTENCE.getMetricConfiguration().getTickInterval(), PERSISTENCE.getMetricConfiguration().getTickTimeUnit(), Clocks.NANO), PERSISTENCE.getMetricConfiguration().getMetricDomain(), new MetricProperty("durable", ormDao.getManagedClass().getSimpleName()), new MetricProperty("method", executedMethod.getName()), new MetricProperty("source", ormDao.getMetricSource())).update(stop - start, TimeUnit.MILLISECONDS);
      }
    }
  }
}