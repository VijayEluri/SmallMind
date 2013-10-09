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
package org.smallmind.instrument;

import java.util.concurrent.ConcurrentHashMap;
import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import org.smallmind.instrument.context.NamedMetricFactory;
import org.smallmind.instrument.context.TracingOptions;
import org.smallmind.instrument.event.MetricEvent;
import org.smallmind.instrument.event.MetricEventListener;
import org.smallmind.instrument.jmx.ChronometerMonitor;
import org.smallmind.instrument.jmx.DefaultJMXNamingPolicy;
import org.smallmind.instrument.jmx.HistogramMonitor;
import org.smallmind.instrument.jmx.JMXNamingPolicy;
import org.smallmind.instrument.jmx.MeterMonitor;
import org.smallmind.instrument.jmx.SpeedometerMonitor;
import org.smallmind.instrument.jmx.TallyMonitor;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class MetricRegistry {

  private final ConcurrentHashMap<MetricKey, Metric<?>> metricMap = new ConcurrentHashMap<MetricKey, Metric<?>>();
  private MBeanServer server;
  private JMXNamingPolicy jmxNamingPolicy = new DefaultJMXNamingPolicy();
  private TracingOptions tracingOptions;
  private MetricEventListener[] metricEventListeners;

  public MBeanServer getServer () {

    return server;
  }

  public void setServer (MBeanServer server) {

    this.server = server;
  }

  public void setJmxNamingPolicy (JMXNamingPolicy jmxNamingPolicy) {

    this.jmxNamingPolicy = jmxNamingPolicy;
  }

  public TracingOptions getTracingOptions () {

    return tracingOptions;
  }

  public void setTracingOptions (TracingOptions tracingOptions) {

    this.tracingOptions = tracingOptions;
  }

  public void setMetricEventListeners (MetricEventListener[] metricEventListeners) {

    this.metricEventListeners = metricEventListeners;
  }

  public void register () {

    InstrumentationManager.register(this);
  }

  public void fireMetricEvent (MetricEvent metricEvent) {

    if ((metricEventListeners != null) && (metricEventListeners.length > 0)) {
      for (MetricEventListener metricEventListener : metricEventListeners) {
        metricEventListener.metricTriggered(metricEvent);
      }
    }
  }

  public <M extends Metric<M>> M instrument (Metrics.MetricBuilder<M> builder, String domain, MetricProperty... properties) {

    M metric;
    MetricKey metricKey = new MetricKey(builder.getType(), domain, properties);

    if ((metric = builder.getMetricClass().cast(metricMap.get(metricKey))) == null) {
      synchronized (metricMap) {
        if ((metric = builder.getMetricClass().cast(metricMap.get(metricKey))) == null) {

          metricMap.put(metricKey, metric = (tracingOptions != null) ? builder.getMetricClass().cast(NamedMetricFactory.createNamedMetric(builder.getType(), builder.construct(), domain, properties).getProxy()) : builder.construct());

          if (server != null) {

            DynamicMBean mBean;

            switch (builder.getType()) {
              case TALLY:
                mBean = new TallyMonitor((Tally)metric);
                break;
              case METER:
                mBean = new MeterMonitor((Meter)metric);
                break;
              case HISTOGRAM:
                mBean = new HistogramMonitor((Histogram)metric);
                break;
              case SPEEDOMETER:
                mBean = new SpeedometerMonitor((Speedometer)metric);
                break;
              case CHRONOMETER:
                mBean = new ChronometerMonitor((Chronometer)metric);
                break;
              default:
                throw new UnknownSwitchCaseException(builder.getType().name());
            }

            try {
              server.registerMBean(mBean, jmxNamingPolicy.createObjectName(builder.getType(), domain, properties));
            }
            catch (Exception exception) {
              throw new InstrumentationException(exception);
            }
          }
        }
      }
    }

    return metric;
  }
}
