/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.scribe.pen;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatTimestamp implements Timestamp {

   private static final DateFormatTimestamp STANDARD_TIMESTAMP = new DateFormatTimestamp();

   private DateFormat dateFormat;

   public static DateFormatTimestamp getDefaultInstance () {

      return STANDARD_TIMESTAMP;
   }

   public DateFormatTimestamp () {

      this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
   }

   public DateFormatTimestamp (DateFormat dateFormat) {

      this.dateFormat = dateFormat;
   }

   public DateFormat getDateFormat () {

      return dateFormat;
   }

   public void setDateFormat (DateFormat dateFormat) {

      this.dateFormat = dateFormat;
   }

   public synchronized String getTimestamp (Date date) {

      return dateFormat.format(date);
   }
}
