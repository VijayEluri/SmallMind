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
package org.smallmind.scribe.pen;

import java.io.File;
import java.util.Calendar;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class TimestampRollover extends Rollover {

  private TimestampQuantifier timestampQuantifier;

  public TimestampRollover () {

    this(TimestampQuantifier.TOP_OF_DAY);
  }

  public TimestampRollover (TimestampQuantifier timestampQuantifier) {

    super();

    this.timestampQuantifier = timestampQuantifier;
  }

  public TimestampRollover (TimestampQuantifier timestampQuantifier, char separator, Timestamp timestamp) {

    super(separator, timestamp);

    this.timestampQuantifier = timestampQuantifier;
  }

  public TimestampQuantifier getTimestampQuantifier () {

    return timestampQuantifier;
  }

  public void setTimestampQuantifier (TimestampQuantifier timestampQuantifier) {

    this.timestampQuantifier = timestampQuantifier;
  }

  public boolean willRollover (File logFile, long bytesToBeWritten) {

    Calendar now = Calendar.getInstance();
    Calendar lastMod = Calendar.getInstance();

    lastMod.setTimeInMillis(logFile.lastModified());
    switch (timestampQuantifier) {
      case TOP_OF_MINUTE:
        return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH)) || (now.get(Calendar.HOUR_OF_DAY) != lastMod.get(Calendar.HOUR_OF_DAY)) || (now.get(Calendar.MINUTE) != lastMod.get(Calendar.MINUTE));
      case TOP_OF_HOUR:
        return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH)) || (now.get(Calendar.HOUR_OF_DAY) != lastMod.get(Calendar.HOUR_OF_DAY));
      case HALF_DAY:
        return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH)) || (now.get(Calendar.AM_PM) != lastMod.get(Calendar.AM_PM));
      case TOP_OF_DAY:
        return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH));
      case TOP_OF_WEEK:
        return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.WEEK_OF_MONTH) != lastMod.get(Calendar.WEEK_OF_MONTH));
      case TOP_OF_MONTH:
        return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH));
      default:
        throw new UnknownSwitchCaseException(timestampQuantifier.name());
    }
  }
}