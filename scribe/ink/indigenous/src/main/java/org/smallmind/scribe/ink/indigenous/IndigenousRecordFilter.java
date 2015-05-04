/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.ink.indigenous;

import java.io.Serializable;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class IndigenousRecordFilter implements Record, RecordWrapper {

  private Record record;
  private Discriminator discriminator;
  private Level level;

  public IndigenousRecordFilter (Record record, Discriminator discriminator, Level level) {

    this.record = record;
    this.discriminator = discriminator;
    this.level = level;
  }

  public Record getRecord () {

    return this;
  }

  public Object getNativeLogEntry () {

    return this;
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

  public void addParameter (String key, Serializable value) {

    throw new UnsupportedOperationException();
  }

  public Parameter[] getParameters () {

    return record.getParameters();
  }

  public String getMessage () {

    return record.getMessage();
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