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
package org.smallmind.persistence.sql.pool.context;

import org.smallmind.quorum.pool.ComponentPoolException;

public class DefaultContextualPoolNameTranslator implements ContextualPoolNameTranslator {

  private String baseName;
  private char separator;

  public DefaultContextualPoolNameTranslator (String baseName, char separator) {

    this.baseName = baseName;
    this.separator = separator;
  }

  @Override
  public String getBaseName () {

    return baseName;
  }

  @Override
  public String getPoolName (String contextualPart) {

    return baseName + separator + contextualPart;
  }

  @Override
  public String getContextualPartFromPoolName (String poolName)
    throws ComponentPoolException {

    if (!poolName.startsWith(baseName)) {
      throw new ComponentPoolException("Unable to parse pool name(%s)", poolName);
    }

    if (poolName.length() == baseName.length()) {

      return null;
    }
    else if (!(poolName.charAt(baseName.length()) == separator)) {
      throw new ComponentPoolException("Unable to parse pool name(%s)", poolName);
    }

    return poolName.substring(baseName.length() + 1);
  }
}
