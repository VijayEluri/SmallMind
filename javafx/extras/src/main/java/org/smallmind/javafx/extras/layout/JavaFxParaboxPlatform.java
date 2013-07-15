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
package org.smallmind.javafx.extras.layout;

import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.Flow;
import org.smallmind.nutsnbolts.layout.Orientation;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.Perimeter;

public class JavaFxParaboxPlatform implements ParaboxPlatform {

  private static final Perimeter PERIMETER = new Perimeter(10.0D, 10.0D, 10.0D, 10.0D);
  private static final Orientation ORIENTATION = new Orientation(Bias.HORIZONTAL, Flow.FIRST_TO_LAST);

  @Override
  public double getRelatedGap () {

    return 5.0D;
  }

  @Override
  public double getUnrelatedGap () {

    return 10.0D;
  }

  @Override
  public Perimeter getFramePerimeter () {

    return PERIMETER;
  }

  @Override
  public Orientation getOrientation () {

    return ORIENTATION;
  }
}
