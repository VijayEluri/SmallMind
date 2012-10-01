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
package org.smallmind.nutsnbolts.layout;

import java.util.List;

public class BaselineCalculations {

  private Pair[] elementAscentsDescents;
  private double idealizedBaseline;

  public BaselineCalculations (Bias bias, Double maximumOverrideMeasurement, double containerMeasurement, List<ParaboxElement<?>> elements, LayoutTailor tailor) {

    elementAscentsDescents = new Pair[elements.size()];
    double maxAscent = 0;
    double maxDescent = 0;
    int index = 0;

    for (ParaboxElement<?> element : elements) {

      double currentMeasurement = Math.min(containerMeasurement, (maximumOverrideMeasurement != null) ? maximumOverrideMeasurement : element.getMaximumMeasurement(bias, tailor));
      double currentAscent = element.getBaseline(bias, currentMeasurement);
      double currentDescent;

      if (currentAscent > maxAscent) {
        maxAscent = currentAscent;
      }
      if ((currentDescent = (currentMeasurement - currentAscent)) > maxDescent) {
        maxDescent = currentDescent;
      }

      elementAscentsDescents[index++] = new Pair(currentAscent, currentDescent);
    }

    idealizedBaseline = maxAscent + (Math.max(0, containerMeasurement - (maxAscent + maxDescent)) / 2);
  }

  public double getIdealizedBaseline () {

    return idealizedBaseline;
  }

  public Pair[] getElementAscentsDescents () {

    return elementAscentsDescents;
  }
}
