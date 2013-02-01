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
package org.smallmind.scribe.ink.indigenous;

import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprints;

public class IndigenousLoggingBlueprints extends LoggingBlueprints {

  public LoggerAdapter getLoggingAdapter (String name) {

    return new IndigenousLoggerAdapter(name);
  }

  public Record filterRecord (Record record, Discriminator discriminator, Level level) {

    return new IndigenousRecordFilter(record, discriminator, level).getRecord();
  }

  public Record errorRecord (Record record, Throwable throwable, String message, Object... args) {

    IndigenousRecord indigenousRecord;
    LogicalContext logicalContext;

    indigenousRecord = new IndigenousRecord(record.getLoggerName(), null, Level.FATAL, null, throwable, message, args);
    logicalContext = new DefaultLogicalContext();
    logicalContext.fillIn();
    indigenousRecord.setLogicalContext(logicalContext);

    return indigenousRecord.getRecord();
  }
}