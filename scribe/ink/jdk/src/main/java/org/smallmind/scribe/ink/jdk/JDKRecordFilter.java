/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class JDKRecordFilter extends LogRecord implements RecordWrapper {

  private FilterRecord filterRecord;
  private Level level;

  public JDKRecordFilter (Record record, Level level) {

    super(JDKLevelTranslator.getLog4JLevel(level), record.getMessage());

    this.level = level;

    filterRecord = new FilterRecord(record, this);
  }

  public String getSourceClassName () {

    if (filterRecord.getLogicalContext() != null) {
      return filterRecord.getLogicalContext().getClassName();
    }

    return null;
  }

  public String getSourceMethodName () {

    if (filterRecord.getLogicalContext() != null) {
      return filterRecord.getLogicalContext().getMethodName();
    }

    return null;
  }

  public Record getRecord () {

    return filterRecord;
  }

  private class FilterRecord implements Record {

    private Record record;
    private LogRecord logRecord;

    public FilterRecord (Record record, LogRecord logRecord) {

      this.record = record;
      this.logRecord = logRecord;
    }

    @Override
    public Object getNativeLogEntry () {

      return logRecord;
    }

    @Override
    public String getLoggerName () {

      return record.getLoggerName();
    }

    @Override
    public Level getLevel () {

      return level;
    }

    @Override
    public Throwable getThrown () {

      return record.getThrown();
    }

    @Override
    public String getMessage () {

      return record.getMessage();
    }

    @Override
    public Parameter[] getParameters () {

      return record.getParameters();
    }

    @Override
    public LogicalContext getLogicalContext () {

      return record.getLogicalContext();
    }

    @Override
    public long getThreadID () {

      return record.getThreadID();
    }

    @Override
    public String getThreadName () {

      return record.getThreadName();
    }

    @Override
    public long getSequenceNumber () {

      return record.getSequenceNumber();
    }

    @Override
    public long getMillis () {

      return record.getMillis();
    }
  }
}