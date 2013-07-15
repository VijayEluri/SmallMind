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
package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.util.LinkedList;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;

public final class ExtensionFileFilter extends FileFilter implements java.io.FileFilter {

  private LinkedList<Pattern> regExpList;
  private LinkedList<String> extensionList;
  private String description;

  public ExtensionFileFilter () {

    this("");
  }

  public ExtensionFileFilter (String description, String... extensions) {

    this.description = description;

    regExpList = new LinkedList<Pattern>();
    extensionList = new LinkedList<String>();

    if (extensions != null) {
      for (String extension : extensions) {
        addExtension(extension);
      }
    }
  }

  public void addExtension (String extension) {

    Pattern parsedPattern;

    parsedPattern = Pattern.compile(RegExpTranslator.translate("*." + extension));
    regExpList.add(parsedPattern);
    extensionList.add(extension);
  }

  public String getExtension () {

    return extensionList.getFirst();
  }

  public boolean accept (File file) {

    if (file.isDirectory()) {

      return true;
    }

    for (Pattern pattern : regExpList) {
      if (pattern.matcher(file.getName()).matches()) {

        return true;
      }
    }

    return false;
  }

  public void setDescription (String description) {

    this.description = description;
  }

  public String getDescription () {

    StringBuilder fullDescription = new StringBuilder();
    boolean first = true;

    fullDescription.append(description);
    fullDescription.append(" (");

    for (String extension : extensionList) {
      if (!first) {
        fullDescription.append(" ");
      }

      fullDescription.append("*.");
      fullDescription.append(extension);

      first = false;
    }

    fullDescription.append(")");

    return fullDescription.toString();
  }
}
