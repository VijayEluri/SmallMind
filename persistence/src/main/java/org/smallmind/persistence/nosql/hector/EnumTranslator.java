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
package org.smallmind.persistence.nosql.hector;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.Composite;
import org.smallmind.persistence.PersistenceException;

public class EnumTranslator implements HectorTranslator<Enum<?>, String> {

  @Override
  public String getHectorType () {

    return "UTF8Type";
  }

  @Override
  public Serializer<String> getSerializer () {

    return StringSerializer.get();
  }

  @Override
  public String toHectorValue (Enum<?> value) {

    return value.name();
  }

  @Override
  public Enum<?> toEntityValue (Class<?> fieldType, int index, Composite columnName) {

    return fromName(fieldType, columnName.get(index, StringSerializer.get()));
  }

  @Override
  public Enum<?> toEntityValue (Class<?> fieldType, Composite columnName, ColumnFamilyResult<Composite, Composite> hectorResult) {

    return fromName(fieldType, hectorResult.getString(columnName));
  }

  private Enum<?> fromName (Class<?> fieldType, String enumName) {

    for (Object enumConstant : fieldType.getEnumConstants()) {
      if (((Enum)enumConstant).name().equals(enumName)) {

        return (Enum)enumConstant;
      }
    }

    throw new PersistenceException("Unable to locate matching enum constant(%s) for field of type(%s)", enumName, fieldType.getName());

  }
}
