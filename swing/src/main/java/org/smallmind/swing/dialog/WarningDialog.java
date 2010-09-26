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
package org.smallmind.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class WarningDialog extends OptionDialog {

   public static void showWarningDialog (Frame parentFrame, String warningText) {

      WarningDialog warningDialog = new WarningDialog(parentFrame, warningText);

      warningDialog.setModal(true);
      warningDialog.setVisible(true);
   }

   public static void showWarningDialog (Dialog parentDialog, String warningText) {

      WarningDialog warningDialog = new WarningDialog(parentDialog, warningText);

      warningDialog.setModal(true);
      warningDialog.setVisible(true);
   }

   public WarningDialog (Frame parentFrame, String warningText) {

      super(parentFrame, warningText, OptionType.WARNING);
   }

   public WarningDialog (Dialog parentDialog, String warningText) {

      super(parentDialog, warningText, OptionType.WARNING);
   }

}
