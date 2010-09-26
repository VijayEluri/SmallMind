/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.scribe.pen.adapter;

import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.probe.ProbeReport;

public interface LoggerAdapter {

   public abstract String getName ();

   public abstract boolean getAutoFillLogicalContext ();

   public abstract void setAutoFillLogigicalContext (boolean autoFillLogicalContext);

   public abstract void addFilter (Filter filter);

   public abstract void clearFilters ();

   public abstract void addAppender (Appender appender);

   public abstract void clearAppenders ();

   public abstract void addEnhancer (Enhancer enhancer);

   public abstract void clearEnhancers ();

   public abstract Level getLevel ();

   public abstract void setLevel (Level level);

   public abstract void logMessage (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args);

   public abstract void logProbe (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport);

   public abstract void logMessage (Discriminator discriminator, Level level, Throwable throwable, Object object);
}
