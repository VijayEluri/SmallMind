/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.SystemColor;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

public class Separator extends JComponent {

   public static int HORIZONTAL = SwingConstants.HORIZONTAL;
   public static int VERTICAL = SwingConstants.VERTICAL;

   private int orientation;
   private int length;

   public Separator () {

      this(0, HORIZONTAL);
   }

   public Separator (int length, int orientation) {

      this.length = length;
      this.orientation = orientation;

      setFocusable(false);

      setForeground(SystemColor.controlShadow);
      setBackground(SystemColor.controlLtHighlight);
   }

   public int getOrientation () {

      return orientation;
   }

   public void setOrientation (int orientation) {

      if (this.orientation != orientation) {
         this.orientation = orientation;
         revalidate();
         repaint();
      }
   }

   public Dimension getPreferredSize () {

      if (orientation == VERTICAL) {
         return new Dimension(2, length);
      }
      else {
         return new Dimension(length, 2);
      }
   }

   public void paint (Graphics graphics) {

      Dimension currentSize = getSize();

      if (orientation == VERTICAL) {
         graphics.setColor(getForeground());
         graphics.drawLine(0, 0, 0, currentSize.height);

         graphics.setColor(getBackground());
         graphics.drawLine(1, 0, 1, currentSize.height);
      }
      else {
         graphics.setColor(getForeground());
         graphics.drawLine(0, 0, currentSize.width, 0);

         graphics.setColor(getBackground());
         graphics.drawLine(0, 1, currentSize.width, 1);
      }
   }

}
