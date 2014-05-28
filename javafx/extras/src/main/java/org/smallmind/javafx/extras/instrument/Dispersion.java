/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.javafx.extras.instrument;

public class Dispersion {

  private double median;
  private double percentile_75;
  private double percentile_95;
  private double percentile_98;
  private double percentile_99;
  private double percentile_999;

  public Dispersion (double median, double percentile_75, double percentile_95, double percentile_98, double percentile_99, double percentile_999) {

    this.median = median;
    this.percentile_75 = percentile_75;
    this.percentile_95 = percentile_95;
    this.percentile_98 = percentile_98;
    this.percentile_99 = percentile_99;
    this.percentile_999 = percentile_999;
  }

  public double getMedian () {

    return median;
  }

  public double getPercentile_75 () {

    return percentile_75;
  }

  public double getPercentile_95 () {

    return percentile_95;
  }

  public double getPercentile_98 () {

    return percentile_98;
  }

  public double getPercentile_99 () {

    return percentile_99;
  }

  public double getPercentile_999 () {

    return percentile_999;
  }
}
