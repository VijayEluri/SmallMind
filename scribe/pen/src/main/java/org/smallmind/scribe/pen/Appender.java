/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.scribe.pen;

import java.util.List;

public interface Appender {

  public abstract void setName (String name);

  public abstract String getName ();

  public abstract void setFilter (Filter filter);

  public abstract void setFilters (List<Filter> filterList);

  public abstract void clearFilters ();

  public abstract void addFilter (Filter filter);

  public abstract Filter[] getFilters ();

  public abstract void setErrorHandler (ErrorHandler errorHandler);

  public abstract ErrorHandler getErrorHandler ();

  public abstract void setFormatter (Formatter formatter);

  public abstract Formatter getFormatter ();

  public abstract boolean isActive ();

  public abstract void setActive (boolean active);

  public abstract boolean requiresFormatter ();

  public abstract void publish (Record record);

  public abstract void close ()
    throws InterruptedException, LoggerException;
}
