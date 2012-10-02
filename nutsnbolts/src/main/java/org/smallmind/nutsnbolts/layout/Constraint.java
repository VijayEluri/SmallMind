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

public class Constraint {

  private static final Constraint IMMUTABLE_RIGID_INSTANCE = new Constraint();
  private static final Constraint IMMUTABLE_GROW_INSTANCE = new Constraint(0.5D, 0.0D);
  private static final Constraint IMMUTABLE_SHRINK_INSTANCE = new Constraint(0.0D, 0.5D);
  private static final Constraint IMMUTABLE_GROW_AND_SHRINK_INSTANCE = new Constraint(0.5D, 0.5D);

  private double grow;
  private double shrink;

  public static Constraint immutable () {

    return IMMUTABLE_RIGID_INSTANCE;
  }

  public static Constraint expand () {

    return IMMUTABLE_GROW_INSTANCE;
  }

  public static Constraint contract () {

    return IMMUTABLE_SHRINK_INSTANCE;
  }

  public static Constraint stretch () {

    return IMMUTABLE_GROW_AND_SHRINK_INSTANCE;
  }

  public static MutableConstraint create () {

    return new MutableConstraint();
  }

  public Constraint () {

  }

  public Constraint (double grow, double shrink) {

    this.grow = grow;
    this.shrink = shrink;
  }

  public double getGrow () {

    return grow;
  }

  private void setGrow (double grow) {

    this.grow = grow;
  }

  public double getShrink () {

    return shrink;
  }

  private void setShrink (double shrink) {

    this.shrink = shrink;
  }
}
