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
package org.smallmind.swing.file;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.TreeCellRenderer;

public class DirectoryTreeCellRenderer implements TreeCellRenderer {

   private static ImageIcon DRIVE;
   private static ImageIcon FOLDER;
   private static ImageIcon FOLDERS;

   private static Border SELECTED_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getDefaults().getColor("textHighlight").darker()), BorderFactory.createEmptyBorder(1, 1, 1, 1));
   private static Border INVISIBLE_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getDefaults().getColor("text")), BorderFactory.createEmptyBorder(1, 1, 1, 1));

   private HashMap<DirectoryNode, JLabel> directoryLabelMap;

   static {

      DRIVE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/harddisk.png"));
      FOLDER = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/folder.png"));
      FOLDERS = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/folders.png"));
   }

   public DirectoryTreeCellRenderer () {

      directoryLabelMap = new HashMap<DirectoryNode, JLabel>();
   }

   public Component getTreeCellRendererComponent (JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

      JLabel directoryLabel;

      if ((directoryLabel = directoryLabelMap.get(value)) == null) {
         if (row == 0) {
            directoryLabel = new JLabel(((Directory)((DirectoryNode)value).getUserObject()).getAbsolutePath(), DRIVE, SwingConstants.LEFT);
         }
         else if (leaf) {
            directoryLabel = new JLabel(((Directory)((DirectoryNode)value).getUserObject()).getName(), FOLDER, SwingConstants.LEFT);
         }
         else {
            directoryLabel = new JLabel(((Directory)((DirectoryNode)value).getUserObject()).getName(), FOLDERS, SwingConstants.LEFT);
         }

         directoryLabel.setBorder(INVISIBLE_BORDER);
         directoryLabel.setOpaque(true);
         directoryLabelMap.put((DirectoryNode)value, directoryLabel);
      }

      if (selected) {
         directoryLabel.setBackground(UIManager.getDefaults().getColor("textHighlight"));
         directoryLabel.setBorder(SELECTED_BORDER);
      }
      else {
         directoryLabel.setBackground(UIManager.getDefaults().getColor("text"));
         directoryLabel.setBorder(INVISIBLE_BORDER);
      }

      return directoryLabel;
   }

}
