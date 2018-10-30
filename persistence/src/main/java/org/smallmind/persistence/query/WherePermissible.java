/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public interface WherePermissible<W extends WherePermissible<W>> {

  Set<String> fieldNames ();

  default W validate (WherePermit... permits) {

    if ((permits != null) && (permits.length > 0)) {

      Set<String> fieldNameSet = fieldNames();
      HashSet<String> allowedNameSet = new HashSet<>();
      HashSet<String> requiredNameSet = new HashSet<>();
      HashSet<String> excludedNameSet = new HashSet<>();

      for (WherePermit permit : permits) {
        switch (permit.getType()) {
          case ALLOWED:
            allowedNameSet.addAll(Arrays.asList(permit.getFields()));
            break;
          case REQUIRED:
            allowedNameSet.addAll(Arrays.asList(permit.getFields()));
            requiredNameSet.addAll(Arrays.asList(permit.getFields()));
            break;
          case EXCLUDED:
            excludedNameSet.addAll(Arrays.asList(permit.getFields()));
            break;
          default:
            throw new UnknownSwitchCaseException(permit.getType().name());
        }
      }

      for (String fieldName : fieldNameSet) {
        if (excludedNameSet.contains(fieldName)) {
          throw new WhereValidationException("The field(%s) is not permitted in %s clauses for this query", fieldName, this.getClass().getSimpleName());
        }
        if ((!allowedNameSet.isEmpty()) && (!allowedNameSet.contains(fieldName))) {
          throw new WhereValidationException("The field(%s) is not permitted in %s clauses for this query", fieldName, this.getClass().getSimpleName());
        }
      }
      if (!fieldNameSet.containsAll(requiredNameSet)) {
        throw new WhereValidationException("The fields(%s) are required in %s clauses for this query", Arrays.toString(requiredNameSet.toArray()), this.getClass().getSimpleName());
      }
    }

    return (W)this;
  }
}