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
package org.smallmind.scribe.ink.log4j;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class Log4JRecordFilter extends LoggingEvent implements RecordWrapper {

  private FilterRecord filterRecord;
  private AtomicReference<LocationInfo> locationInfoReference;
  private Discriminator discriminator;
  private Level level;

  public Log4JRecordFilter (Record record, Discriminator discriminator, Level level) {

    this(record, (LoggingEvent)record.getNativeLogEntry(), discriminator, level);
  }

  private Log4JRecordFilter (Record record, LoggingEvent loggingEvent, Discriminator discriminator, Level level) {

    super(loggingEvent.getFQNOfLoggerClass(), loggingEvent.getLogger(), loggingEvent.getTimeStamp(), Log4JLevelTranslator.getLog4JLevel(level), loggingEvent.getRenderedMessage(), loggingEvent.getThrowableInformation().getThrowable());

    this.discriminator = discriminator;
    this.level = level;

    filterRecord = new FilterRecord(record, this);

    locationInfoReference = new AtomicReference<LocationInfo>();
  }

  public Record getRecord () {

    return filterRecord;
  }

  public LocationInfo getLocationInformation () {

    if (locationInfoReference.get() == null) {
      synchronized (this) {
        if ((locationInfoReference.get() == null) && (filterRecord.getLogicalContext() != null)) {
          locationInfoReference.set(new LocationInfo(filterRecord.getLogicalContext().getFileName(), filterRecord.getLogicalContext().getClassName(), filterRecord.getLogicalContext().getMethodName(), String.valueOf(filterRecord.getLogicalContext().getLineNumber())));
        }
      }
    }

    return locationInfoReference.get();
  }

  private class FilterRecord implements Record {

    private Record record;
    private LoggingEvent loggingEvent;

    public FilterRecord (Record record, LoggingEvent loggingEvent) {

      this.record = record;
      this.loggingEvent = loggingEvent;
    }

    public Object getNativeLogEntry () {

      return loggingEvent;
    }

    public ProbeReport getProbeReport () {

      return record.getProbeReport();
    }

    public String getLoggerName () {

      return record.getLoggerName();
    }

    public Discriminator getDiscriminator () {

      return discriminator;
    }

    public Level getLevel () {

      return level;
    }

    public Throwable getThrown () {

      return record.getThrown();
    }

    public String getMessage () {

      return record.getMessage();
    }

    public void addParameter (String key, Serializable value) {

      throw new UnsupportedOperationException();
    }

    public Parameter[] getParameters () {

      return record.getParameters();
    }

    public LogicalContext getLogicalContext () {

      return record.getLogicalContext();
    }

    public long getThreadID () {

      return record.getThreadID();
    }

    public String getThreadName () {

      return record.getThreadName();
    }

    public long getSequenceNumber () {

      return record.getSequenceNumber();
    }

    public long getMillis () {

      return record.getMillis();
    }
  }
}