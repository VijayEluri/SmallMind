/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.scribe.ink.indigenous;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class IndigenousLoggerAdapter implements LoggerAdapter {

  private ConcurrentLinkedQueue<Filter> filterList;
  private ConcurrentLinkedQueue<Appender> appenderList;
  private ConcurrentLinkedQueue<Enhancer> enhancerList;
  private Level level = Level.INFO;
  private String name;
  private boolean autoFillLogicalContext = false;

  public IndigenousLoggerAdapter (String name) {

    this.name = name;

    filterList = new ConcurrentLinkedQueue<Filter>();
    appenderList = new ConcurrentLinkedQueue<Appender>();
    enhancerList = new ConcurrentLinkedQueue<Enhancer>();
  }

  public String getName () {

    return name;
  }

  public boolean getAutoFillLogicalContext () {

    return autoFillLogicalContext;
  }

  public void setAutoFillLogicalContext (boolean autoFillLogicalContext) {

    this.autoFillLogicalContext = autoFillLogicalContext;
  }

  public void addFilter (Filter filter) {

    filterList.add(filter);
  }

  public void clearFilters () {

    filterList.clear();
  }

  public void addAppender (Appender appender) {

    appenderList.add(appender);
  }

  public void clearAppenders () {

    appenderList.clear();
  }

  public void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);
  }

  public void clearEnhancers () {

    enhancerList.clear();
  }

  public Level getLevel () {

    return level;
  }

  public void setLevel (Level level) {

    this.level = level;
  }

  public void logMessage (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args) {

    IndigenousRecord indigenousRecord;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      indigenousRecord = new IndigenousRecord(name, discriminator, level, null, throwable, message, args);
      if (willLog(indigenousRecord)) {
        completeLogOperation(indigenousRecord);
      }
    }
  }

  public void logProbe (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport) {

    IndigenousRecord indigenousRecord;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      indigenousRecord = new IndigenousRecord(name, discriminator, level, probeReport, throwable, (probeReport.getTitle() == null) ? "Probe Report" : probeReport.getTitle());
      if (willLog(indigenousRecord)) {
        completeLogOperation(indigenousRecord);
      }
    }
  }

  public void logMessage (Discriminator discriminator, Level level, Throwable throwable, Object object) {

    IndigenousRecord indigenousRecord;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      indigenousRecord = new IndigenousRecord(name, discriminator, level, null, throwable, (object == null) ? null : object.toString());
      if (willLog(indigenousRecord)) {
        completeLogOperation(indigenousRecord);
      }
    }
  }

  private boolean willLog (IndigenousRecord indigenousRecord) {

    LogicalContext logicalContext;

    logicalContext = new DefaultLogicalContext();
    if (getAutoFillLogicalContext()) {
      logicalContext.fillIn();
    }

    indigenousRecord.setLogicalContext(logicalContext);

    if (!filterList.isEmpty()) {
      for (Filter filter : filterList) {
        if (!filter.willLog(indigenousRecord)) {
          return false;
        }
      }
    }

    return true;
  }

  private void completeLogOperation (Record record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }

    for (Appender appender : appenderList) {
      if (appender.isActive()) {
        appender.publish(record);
      }
    }
  }
}