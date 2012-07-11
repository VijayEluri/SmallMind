/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.persistence.cache.aop;

import org.aspectj.lang.JoinPoint;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.aop.AOPUtility;
import org.smallmind.persistence.cache.VectorIndex;

public class VectorIndices {

  // Called from CachedWithAspect
  public static VectorIndex[] getVectorIndexes (Vector vector, Durable durable) {

    VectorIndex[] indices;

    indices = new VectorIndex[vector.value().length];
    for (int count = 0; count < vector.value().length; count++) {

      Object indexValue;

      indexValue = (!vector.value()[count].constant().equals("")) ? vector.value()[count].constant() : getValue(durable, vector.value()[count].on(), vector.value()[count].nullable());
      indices[count] = new VectorIndex(vector.value()[count].with(), vector.value()[count].on(), indexValue, vector.value()[count].alias());
    }

    return indices;
  }

  // Called from CacheAsAspect
  public static VectorIndex[] getVectorIndexes (Vector vector, JoinPoint joinPoint) {

    VectorIndex[] indices;

    indices = new VectorIndex[vector.value().length];
    for (int count = 0; count < vector.value().length; count++) {

      Object indexValue;

      indexValue = (!vector.value()[count].constant().equals("")) ? vector.value()[count].constant() : getValue(joinPoint, vector.value()[count].on(), vector.value()[count].nullable());
      indices[count] = new VectorIndex(vector.value()[count].with(), vector.value()[count].on(), indexValue, vector.value()[count].alias());
    }

    return indices;
  }

  public static Object getValue (JoinPoint joinPoint, String parameterName, boolean nullable) {

    try {
      return AOPUtility.getParameterValue(joinPoint, parameterName, nullable);
    }
    catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }

  public static Object getValue (Durable durable, String fieldName, boolean nullable) {

    try {

      return BeanUtility.executeGet(durable, fieldName, nullable);
    }
    catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }
}
