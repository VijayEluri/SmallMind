/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.nutsnbolts.spring.property;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.yaml.snakeyaml.Yaml;

public enum PropertyFileType {

  PROPERTIES("properties") {
    @Override
    public PropertyHandler<?> getPropertyHandler (InputStream inputStream)
      throws IOException {

      Properties properties = new Properties();

      properties.load(inputStream);

      return new PropertiesPropertyHandler(properties);
    }
  },
  YAML("yaml") {
    @Override
    public PropertyHandler<?> getPropertyHandler (InputStream inputStream) {

      Yaml yaml = new Yaml();

      return new YamlPropertyHandler((Map<String, Object>)yaml.load(inputStream));
    }
  };
  private String extension;

  private PropertyFileType (String extension) {

    this.extension = extension;
  }

  public static PropertyFileType forExtension (String extension) {

    for (PropertyFileType propertyFileType : PropertyFileType.values()) {
      if (propertyFileType.getExtension().equals(extension)) {

        return propertyFileType;
      }
    }

    return null;
  }

  public abstract PropertyHandler<?> getPropertyHandler (InputStream inputStream)
    throws IOException;

  public String getExtension () {

    return extension;
  }
}
