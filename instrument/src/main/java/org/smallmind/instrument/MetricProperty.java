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
package org.smallmind.instrument;

import java.io.Serializable;

public class MetricProperty implements Serializable {

  private String key;
  private String value;

  public MetricProperty (String key, String value) {

    this.key = key;
    this.value = value;
  }

  public String getKey () {

    return key;
  }

  public String getValue () {

    return value;
  }

  @Override
  public String toString () {

    return new StringBuilder(key).append('=').append(value).toString();
  }

  @Override
  public int hashCode () {

    return key.hashCode() ^ value.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof MetricProperty) && ((MetricProperty)obj).getKey().equals(key) && ((MetricProperty)obj).getValue().equals(value);
  }
}
