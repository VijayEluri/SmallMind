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
package org.smallmind.javafx.mojo;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public enum JavaFXRuntimeVersion {

  TWO_POINT_ONE_PLUS("2.1+") {
    @Override
    public String getLocation (OSType osType) {

      switch (osType) {
        case WINDOWS_X86_64:
          return "http://javadl.sun.com/webapps/download/GetFile/javafx-latest/windows-i586/javafx2.jnlp";
        default:
          throw new UnknownSwitchCaseException(osType.name());
      }
    }
  };

  private String code;

  private JavaFXRuntimeVersion (String code) {

    this.code = code;
  }

  public abstract String getLocation (OSType osType);

  public String getCode () {

    return code;
  }

  public static JavaFXRuntimeVersion fromCode (String code) {

    for (JavaFXRuntimeVersion runtimeVersion : JavaFXRuntimeVersion.values()) {
      if (runtimeVersion.getCode().equals(code)) {

        return runtimeVersion;
      }
    }

    throw new IllegalArgumentException(code);
  }

  public static String[] getValidCodes () {

    String[] validCodes = new String[JavaFXRuntimeVersion.values().length];
    int index = 0;

    for (JavaFXRuntimeVersion runtimeVersion : JavaFXRuntimeVersion.values()) {
      validCodes[index++] = runtimeVersion.getCode();
    }

    return validCodes;
  }
}
