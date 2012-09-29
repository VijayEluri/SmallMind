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
package org.smallmind.swing.layout;

import java.awt.Component;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.Pair;
import org.smallmind.nutsnbolts.layout.ParaboxConstraint;
import org.smallmind.nutsnbolts.layout.ParaboxElement;
import org.smallmind.nutsnbolts.layout.Spec;

public class SwingParaboxElement extends ParaboxElement<Component> {

  public SwingParaboxElement (Component component) {

    super(component, ParaboxConstraint.immutable());
  }

  public SwingParaboxElement (Component component, ParaboxConstraint constraint) {

    super(component, (constraint == null) ? ParaboxConstraint.immutable() : constraint);
  }

  public SwingParaboxElement (Component component, Spec spec) {

    super(component, (spec == null) ? ParaboxConstraint.immutable() : spec.staticConstraint());
  }

  @Override
  public double getComponentMinimumWidth () {

    return getPart().getMinimumSize().getWidth();
  }

  @Override
  public double getComponentMinimumHeight () {

    return getPart().getMinimumSize().getHeight();
  }

  @Override
  public double getComponentPreferredWidth () {

    return getPart().getPreferredSize().getWidth();
  }

  @Override
  public double getComponentPreferredHeight () {

    return getPart().getPreferredSize().getHeight();
  }

  @Override
  public double getComponentMaximumWidth () {

    return getPart().getMaximumSize().getWidth();
  }

  @Override
  public double getComponentMaximumHeight () {

    return getPart().getMaximumSize().getHeight();
  }

  public double getBaseline (Bias bias, double measurement) {

    int swingBaseline;

    return ((swingBaseline = getPart().getBaseline((int)biasedPair.getFirst(), (int)biasedPair.getSecond())) < 0) ? measurement : swingBaseline;
  }

  @Override
  public void applyLayout (Pair position, Pair size) {

    getPart().setBounds((int)position.getFirst(), (int)position.getSecond(), (int)size.getFirst(), (int)size.getSecond());
  }
}
