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

import java.util.concurrent.TimeUnit;

public class Metrics {

  public static TallyBuilder buildTally (int initialCount) {

    return new TallyBuilder(initialCount);
  }

  public static MeterBuilder buildMeter (long tickInterval, TimeUnit tickTimeUnit) {

    return new MeterBuilder(tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static MeterBuilder buildMeter (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new MeterBuilder(tickInterval, tickTimeUnit, clocks);
  }

  public static HistogramBuilder buildHistogram (Samples samples) {

    return new HistogramBuilder(samples);
  }

  public static SpeedometerBuilder buildSpeedometer (long tickInterval, TimeUnit tickTimeUnit) {

    return new SpeedometerBuilder(tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static SpeedometerBuilder buildSpeedometer (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new SpeedometerBuilder(tickInterval, tickTimeUnit, clocks);
  }

  public static ChronometerBuilder buildChronometer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit) {

    return new ChronometerBuilder(Samples.BIASED, durationUnit, tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static ChronometerBuilder buildChronometer (Samples samples, TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit) {

    return new ChronometerBuilder(samples, durationUnit, tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static ChronometerBuilder buildChronometer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new ChronometerBuilder(Samples.BIASED, durationUnit, tickInterval, tickTimeUnit, clocks);
  }

  public static ChronometerBuilder buildChronometer (Samples samples, TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new ChronometerBuilder(samples, durationUnit, tickInterval, tickTimeUnit, clocks);
  }

  public static interface MetricBuilder<M extends Metric> {

    public abstract Class<M> getMetricClass ();

    public abstract MetricType getType ();

    public abstract M construct ();
  }

  private static class TallyBuilder implements MetricBuilder<TallyImpl> {

    private int initialCount;

    public TallyBuilder (int initialCount) {

      this.initialCount = initialCount;
    }

    @Override
    public Class<TallyImpl> getMetricClass () {

      return TallyImpl.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.TALLY;
    }

    @Override
    public TallyImpl construct () {

      return new TallyImpl(initialCount);
    }
  }

  private static class MeterBuilder implements MetricBuilder<MeterImpl> {

    private Clocks clocks;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private MeterBuilder (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Class<MeterImpl> getMetricClass () {

      return MeterImpl.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.METER;
    }

    @Override
    public MeterImpl construct () {

      return new MeterImpl(tickInterval, tickTimeUnit, clocks.getClock());
    }
  }

  private static class HistogramBuilder implements MetricBuilder<HistogramImpl> {

    private Samples samples;

    private HistogramBuilder (Samples samples) {

      this.samples = samples;
    }

    @Override
    public Class<HistogramImpl> getMetricClass () {

      return HistogramImpl.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.HISTOGRAM;
    }

    @Override
    public HistogramImpl construct () {

      return new HistogramImpl(samples);
    }
  }

  private static class SpeedometerBuilder implements MetricBuilder<SpeedometerImpl> {

    private Clocks clocks;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private SpeedometerBuilder (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Class<SpeedometerImpl> getMetricClass () {

      return SpeedometerImpl.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.SPEEDOMETER;
    }

    @Override
    public SpeedometerImpl construct () {

      return new SpeedometerImpl(tickInterval, tickTimeUnit, clocks.getClock());
    }
  }

  private static class ChronometerBuilder implements MetricBuilder<ChronometerImpl> {

    private Samples samples;
    private Clocks clocks;
    private TimeUnit durationUnit;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private ChronometerBuilder (Samples samples, TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.samples = samples;
      this.durationUnit = durationUnit;
      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Class<ChronometerImpl> getMetricClass () {

      return ChronometerImpl.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.CHRONOMETER;
    }

    @Override
    public ChronometerImpl construct () {

      return new ChronometerImpl(samples, durationUnit, tickInterval, tickTimeUnit, clocks.getClock());
    }
  }
}
