/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.json.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DirectionalMap {

  private final HashMap<String, PropertyMap> internalMap = new HashMap<>();
  private final Direction direction;

  public DirectionalMap (Direction direction) {

    this.direction = direction;
  }

  public DirectionalMap (Direction direction, DirectionalMap directionalMap) {

    this(direction);

    internalMap.putAll(directionalMap.getInternalMap());
  }

  private HashMap<String, PropertyMap> getInternalMap () {

    return internalMap;
  }

  public void put (List<String> purposes, String fieldName, PropertyInformation propertyInformation)
    throws DtoDefinitionException {

    if ((purposes == null) || purposes.isEmpty()) {
      purposes = Collections.singletonList("");
    }

    for (String purpose : purposes) {

      PropertyMap propertyMap;

      if ((propertyMap = internalMap.get(purpose)) == null) {
        internalMap.put(purpose, propertyMap = new PropertyMap());
      }

      if (propertyMap.containsKey(fieldName)) {
        throw new DtoDefinitionException("The field(name=%s, purpose=%s, direction=%s) has already been processed", fieldName, (purpose.isEmpty()) ? "n/a" : purpose, direction.name());
      } else {
        propertyMap.put(fieldName, propertyInformation);
      }
    }
  }

  public Set<Map.Entry<String, PropertyMap>> entrySet () {

    return internalMap.entrySet();
  }
}
