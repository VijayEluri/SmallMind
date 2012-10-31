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

import java.util.Arrays;
import java.util.List;

public class ParaboxLayout {

  private final ParaboxContainer container;
  private final Perimeter perimeter;

  private Box horizontalBox;
  private Box verticalBox;

  public ParaboxLayout (ParaboxContainer container) {

    this(container.getPlatform().getFramePerimeter(), container);
  }

  public ParaboxLayout (Perimeter perimeter, ParaboxContainer container) {

    this.perimeter = perimeter;
    this.container = container;
  }

  public ParaboxContainer getContainer () {

    return container;
  }

  public Box<?> getHorizontalBox () {

    return horizontalBox;
  }

  public ParaboxLayout setHorizontalBox (Box<?> horizontalBox) {

    this.horizontalBox = horizontalBox;

    return this;
  }

  public Box<?> getVerticalBox () {

    return verticalBox;
  }

  public ParaboxLayout setVerticalBox (Box<?> verticalBox) {

    this.verticalBox = verticalBox;

    return this;
  }

  public double calculateMinimumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMinimumMeasurement(Bias.HORIZONTAL, null);
  }

  public double calculateMinimumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMinimumMeasurement(Bias.VERTICAL, null);
  }

  public Pair calculateMinimumSize () {

    return new Pair(calculateMinimumWidth(), calculateMinimumHeight());
  }

  public double calculatePreferredWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculatePreferredMeasurement(Bias.HORIZONTAL, null);
  }

  public double calculatePreferredHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculatePreferredMeasurement(Bias.VERTICAL, null);
  }

  public Pair calculatePreferredSize () {

    return new Pair(calculatePreferredWidth(), calculatePreferredHeight());
  }

  public double calculateMaximumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMaximumMeasurement(Bias.HORIZONTAL, null);
  }

  public double calculateMaximumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMaximumMeasurement(Bias.VERTICAL, null);
  }

  public Pair calculateMaximumSize () {

    return new Pair(calculateMaximumWidth(), calculateMaximumHeight());
  }

  private double getHorizontalPerimeterRequirements () {

    return perimeter.getLeft() + perimeter.getRight();
  }

  private double getVerticalPerimeterRequirements () {

    return perimeter.getTop() + perimeter.getBottom();
  }

  public void doLayout (double width, double height, Object... components) {

    doLayout(width, height, Arrays.asList(components));
  }

  public void doLayout (double width, double height, List componentList) {

    LayoutTailor tailor;

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }
    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    horizontalBox.doLayout(Bias.HORIZONTAL, perimeter.getLeft(), width - getHorizontalPerimeterRequirements(), tailor = new LayoutTailor(componentList));
    verticalBox.doLayout(Bias.VERTICAL, perimeter.getTop(), height - getVerticalPerimeterRequirements(), tailor);

    tailor.cleanup();
  }

  public ParallelBox parallelBox () {

    return new ParallelBox(this);
  }

  public ParallelBox parallelBox (Alignment alignment) {

    return new ParallelBox(this, alignment);
  }

  public SequentialBox sequentialBox () {

    return new SequentialBox(this);
  }

  public SequentialBox sequentialBox (boolean greedy) {

    return new SequentialBox(this, greedy);
  }

  public SequentialBox sequentialBox (Gap gap) {

    return new SequentialBox(this, gap);
  }

  public SequentialBox sequentialBox (Gap gap, boolean greedy) {

    return new SequentialBox(this, gap, greedy);
  }

  public SequentialBox sequentialBox (double gap) {

    return new SequentialBox(this, gap);
  }

  public SequentialBox sequentialBox (double gap, boolean greedy) {

    return new SequentialBox(this, gap, greedy);
  }

  public SequentialBox sequentialBox (Justification justification) {

    return new SequentialBox(this, justification);
  }

  public SequentialBox sequentialBox (Justification justification, boolean greedy) {

    return new SequentialBox(this, justification, greedy);
  }

  public SequentialBox sequentialBox (Gap gap, Justification justification) {

    return new SequentialBox(this, gap, justification);
  }

  public SequentialBox sequentialBox (Gap gap, Justification justification, boolean greedy) {

    return new SequentialBox(this, gap, justification, greedy);
  }

  public SequentialBox sequentialBox (double gap, Justification justification) {

    return new SequentialBox(this, gap, justification);
  }

  public SequentialBox sequentialBox (double gap, Justification justification, boolean greedy) {

    return new SequentialBox(this, gap, justification, greedy);
  }
}