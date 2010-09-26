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
package org.smallmind.persistence.orm.type;

import java.util.Date;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class UTC {

   private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

   public static DateTime isoParse (String date) {

      return ISO_DATE_TIME_FORMATTER.parseDateTime(date);
   }

   public static String isoFormat (Date date) {

      return ISO_DATE_TIME_FORMATTER.print(date.getTime());
   }

   public static Date now () {

      DateTime now;

      return (now = new DateTime()).minusMillis(now.getZone().getOffset(now)).toDate();
   }

   public static Date then (Date date) {

      DateTime then;

      return (then = new DateTime(date.getTime())).minusMillis(then.getZone().getOffset(then)).toDate();
   }

   public static Date local (Date date, int offset) {

      return new DateTime(date.getTime()).withZoneRetainFields(DateTimeZone.forOffsetHours(offset)).plusHours(offset).toDate();
   }
}
