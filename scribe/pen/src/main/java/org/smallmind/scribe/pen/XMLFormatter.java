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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.adapter.LoggingBlueprintsFactory;
import org.smallmind.scribe.pen.probe.CompleteOrAbortProbeEntry;
import org.smallmind.scribe.pen.probe.Correlator;
import org.smallmind.scribe.pen.probe.MetricMilieu;
import org.smallmind.scribe.pen.probe.ProbeEntry;
import org.smallmind.scribe.pen.probe.ProbeReport;
import org.smallmind.scribe.pen.probe.Statement;
import org.smallmind.scribe.pen.probe.UpdateProbeEntry;

public class XMLFormatter implements Formatter {

  private Timestamp timestamp;
  private XMLElement[] xmlElements;
  private String newLine;
  private int indent;

  public XMLFormatter () {

    this(3, System.getProperty("line.separator"), DateFormatTimestamp.getDefaultInstance(), XMLElement.values());
  }

  public XMLFormatter (String newLine) {

    this(3, newLine, DateFormatTimestamp.getDefaultInstance(), XMLElement.values());
  }

  public XMLFormatter (Timestamp timestamp) {

    this(3, System.getProperty("line.separator"), timestamp, XMLElement.values());
  }

  public XMLFormatter (String newLine, Timestamp timestamp) {

    this(3, newLine, timestamp, XMLElement.values());
  }

  public XMLFormatter (int indent) {

    this(indent, System.getProperty("line.separator"), DateFormatTimestamp.getDefaultInstance(), XMLElement.values());
  }

  public XMLFormatter (int indent, String newLine) {

    this(indent, newLine, DateFormatTimestamp.getDefaultInstance(), XMLElement.values());
  }

  public XMLFormatter (int indent, Timestamp timestamp) {

    this(indent, System.getProperty("line.separator"), timestamp, XMLElement.values());
  }

  public XMLFormatter (int indent, String newLine, Timestamp timestamp) {

    this(indent, newLine, timestamp, XMLElement.values());
  }

  public XMLFormatter (XMLElement... xmlElements) {

    this(3, System.getProperty("line.separator"), DateFormatTimestamp.getDefaultInstance(), xmlElements);
  }

  public XMLFormatter (String newLine, XMLElement... xmlElements) {

    this(3, newLine, DateFormatTimestamp.getDefaultInstance(), xmlElements);
  }

  public XMLFormatter (Timestamp timestamp, XMLElement... xmlElements) {

    this(3, System.getProperty("line.separator"), timestamp, xmlElements);
  }

  public XMLFormatter (String newLine, Timestamp timestamp, XMLElement... xmlElements) {

    this(3, newLine, timestamp, xmlElements);
  }

  public XMLFormatter (int indent, XMLElement... xmlElements) {

    this(indent, System.getProperty("line.separator"), DateFormatTimestamp.getDefaultInstance(), xmlElements);
  }

  public XMLFormatter (int indent, String newLine, XMLElement... xmlElements) {

    this(indent, newLine, DateFormatTimestamp.getDefaultInstance(), xmlElements);
  }

  public XMLFormatter (int indent, String newLine, Timestamp timestamp, XMLElement... xmlElements) {

    this.indent = indent;
    this.newLine = newLine;
    this.xmlElements = xmlElements;
    this.timestamp = timestamp;
  }

  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {
        return prevStackTrace.length - count;
      }
    }

    return -1;
  }

  public int getIndent () {

    return indent;
  }

  public void setIndent (int indent) {

    this.indent = indent;
  }

  public String getNewLine () {

    return newLine;
  }

  public void setNewLine (String newLine) {

    this.newLine = newLine;
  }

  public Timestamp getTimestamp () {

    return timestamp;
  }

  public void setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;
  }

  public XMLElement[] getXmlElements () {

    return xmlElements;
  }

  public void setXmlElements (XMLElement[] xmlElements) {

    this.xmlElements = xmlElements;
  }

  public String format (Record record, Collection<Filter> filterCollection) {

    StringBuilder formatBuilder = new StringBuilder();

    appendLine(formatBuilder, "<log-record>", 0);

    for (XMLElement xmlElement : xmlElements) {
      switch (xmlElement) {
        case DATE:
          appendElement(formatBuilder, "date", timestamp.getTimestamp(new Date(record.getMillis())), 1);
          break;
        case MILLISECONDS:
          appendElement(formatBuilder, "milliseconds", String.valueOf(record.getMillis()), 1);
          break;
        case LOGGER_NAME:
          appendElement(formatBuilder, "logger", record.getLoggerName(), 1);
          break;
        case LEVEL:
          appendElement(formatBuilder, "level", record.getLevel().name(), 1);
          break;
        case MESSAGE:

          String message;

          if ((message = record.getMessage()) == null) {

            Throwable throwable;

            if ((throwable = record.getThrown()) != null) {
              message = throwable.getMessage();
            }
          }

          appendElement(formatBuilder, "message", message, 1);
          break;
        case THREAD:
          appendThreadInfo(formatBuilder, record.getThreadName(), record.getThreadID(), 1);
          break;
        case LOGICAL_CONTEXT:
          appendLogicalContext(formatBuilder, record.getLogicalContext(), 1);
          break;
        case PARAMETERS:
          appendParameters(formatBuilder, record.getParameters(), 1);
          break;
        case STACK_TRACE:
          appendStackTrace(formatBuilder, record.getThrown(), 1);
          break;
        case PROBE_REPORT:
          appendProbeReport(formatBuilder, record, record.getProbeReport(), filterCollection, 1);
          break;
        default:
          throw new UnknownSwitchCaseException(xmlElement.name());
      }
    }

    appendFinalLine(formatBuilder, "</log-record>", 0);

    return formatBuilder.toString();
  }

  private void appendThreadInfo (StringBuilder formatBuilder, String threadName, long threadId, int level) {

    if ((threadName != null) || (threadId > 0)) {
      appendLine(formatBuilder, "<thread>", level);
      appendElement(formatBuilder, "name", threadName, level + 1);
      appendElement(formatBuilder, "id", (threadId > 0) ? String.valueOf(threadId) : null, level + 1);
      appendLine(formatBuilder, "</thread>", level);
    }
  }

  private void appendLogicalContext (StringBuilder formatBuilder, LogicalContext logicalContext, int level) {

    if ((logicalContext != null) && (logicalContext.isFilled())) {
      appendLine(formatBuilder, "<context>", level);
      appendElement(formatBuilder, "class", logicalContext.getClassName(), level + 1);
      appendElement(formatBuilder, "method", logicalContext.getMethodName(), level + 1);
      appendElement(formatBuilder, "native", String.valueOf(logicalContext.isNativeMethod()), level + 1);
      appendElement(formatBuilder, "line", ((!logicalContext.isNativeMethod()) && (logicalContext.getLineNumber() > 0)) ? String.valueOf(logicalContext.getLineNumber()) : null, level + 1);
      appendElement(formatBuilder, "file", logicalContext.getFileName(), level + 1);
      appendLine(formatBuilder, "</context>", level);
    }
  }

  private void appendParameters (StringBuilder formatBuilder, Parameter[] parameters, int level) {

    if (parameters.length > 0) {
      appendLine(formatBuilder, "<parameters>", level);
      for (Parameter parameter : parameters) {
        appendElement(formatBuilder, parameter.getKey(), parameter.getValue().toString(), level + 1);
      }
      appendLine(formatBuilder, "</parameters>", level);
    }
  }

  private void appendStackTrace (StringBuilder formatBuilder, Throwable throwable, int level) {

    StackTraceElement[] prevStackTrace = null;
    int repeatedElements;

    if (throwable != null) {
      appendLine(formatBuilder, "<stack-trace>", level);

      do {
        appendIndent(formatBuilder, level + 1);

        if (prevStackTrace == null) {
          formatBuilder.append("Exception in thread ");
        }
        else {
          formatBuilder.append("Caused by: ");
        }

        formatBuilder.append(throwable.getClass().getCanonicalName());
        formatBuilder.append(": ");
        formatBuilder.append(throwable.getMessage());
        formatBuilder.append(newLine);

        for (StackTraceElement singleElement : throwable.getStackTrace()) {

          appendIndent(formatBuilder, level + 1);

          if (prevStackTrace != null) {
            if ((repeatedElements = findRepeatedStackElements(singleElement, prevStackTrace)) >= 0) {
              formatBuilder.append("   ... ");
              formatBuilder.append(repeatedElements);
              formatBuilder.append(" more");
              formatBuilder.append(newLine);
              break;
            }
          }

          formatBuilder.append("   at ");
          formatBuilder.append(singleElement.toString());
          formatBuilder.append(newLine);
        }

        prevStackTrace = throwable.getStackTrace();
      } while ((throwable = throwable.getCause()) != null);

      appendLine(formatBuilder, "</stack-trace>", level);
    }
  }

  private void appendProbeReport (StringBuilder formatBuilder, Record record, ProbeReport probeReport, Collection<Filter> filterCollection, int level) {

    if (probeReport != null) {
      appendLine(formatBuilder, "<probe-report>", level);
      appendElement(formatBuilder, "first", String.valueOf(probeReport.isFirst()), level + 1);
      appendCorrelator(formatBuilder, probeReport.getCorrelator(), level + 1);
      appendProbeEntry(formatBuilder, record, probeReport.getProbeEntry(), filterCollection, level + 1);
      appendLine(formatBuilder, "</probe-report>", level);
    }
  }

  private void appendCorrelator (StringBuilder formatBuilder, Correlator correlator, int level) {

    appendLine(formatBuilder, "<correlator>", level);
    appendElement(formatBuilder, "thread-identifier", Arrays.toString(correlator.getThreadIdentifier()), level + 1);
    appendElement(formatBuilder, "parent-identifier", Arrays.toString(correlator.getParentIdentifier()), level + 1);
    appendElement(formatBuilder, "identifier", Arrays.toString(correlator.getIdentifier()), level + 1);
    appendElement(formatBuilder, "frame", String.valueOf(correlator.getFrame()), level + 1);
    appendElement(formatBuilder, "instance", String.valueOf(correlator.getInstance()), level + 1);
    appendLine(formatBuilder, "</correlator>", level);
  }

  private void appendProbeEntry (StringBuilder formatBuilder, Record record, ProbeEntry probeEntry, Collection<Filter> filterCollection, int level) {

    Record filterRecord;

    if (probeEntry != null) {
      appendLine(formatBuilder, "<probe-entry>", level);
      appendElement(formatBuilder, "status", probeEntry.getProbeStatus().name(), level + 1);

      if (probeEntry instanceof UpdateProbeEntry) {
        appendElement(formatBuilder, "updated", String.valueOf(((UpdateProbeEntry)probeEntry).getUpdateTime()), level + 1);
        appendElement(formatBuilder, "count", String.valueOf(((UpdateProbeEntry)probeEntry).getUpdateCount()), level + 1);
      }
      else if (probeEntry instanceof CompleteOrAbortProbeEntry) {
        appendElement(formatBuilder, "started", String.valueOf(((CompleteOrAbortProbeEntry)probeEntry).getStartTime()), level + 1);
        appendElement(formatBuilder, "stopped", String.valueOf(((CompleteOrAbortProbeEntry)probeEntry).getStopTime()), level + 1);
        appendElement(formatBuilder, "elapsed", String.valueOf(((CompleteOrAbortProbeEntry)probeEntry).getStopTime() - ((CompleteOrAbortProbeEntry)probeEntry).getStartTime()), level + 1);
      }
      else {
        throw new IllegalArgumentException("Unknown instance type of ProbeEntry(" + probeEntry.getClass().getCanonicalName() + ")");
      }

      for (Statement statement : probeEntry.getStatements()) {

        boolean skipStatement = false;

        if (!filterCollection.isEmpty()) {
          filterRecord = LoggingBlueprintsFactory.getLoggingBlueprints().filterRecord(record, statement.getDiscriminator(), statement.getLevel());
          for (Filter filter : filterCollection) {
            if (!filter.willLog(filterRecord)) {
              skipStatement = true;
              break;
            }
          }
        }

        if (!skipStatement) {
          appendElement(formatBuilder, "statement", statement.getMessage(), level + 1);
        }
      }

      for (MetricMilieu metricMilieu : probeEntry.getMetricMilieus()) {

        boolean skipMetric = false;

        if (!filterCollection.isEmpty()) {
          filterRecord = LoggingBlueprintsFactory.getLoggingBlueprints().filterRecord(record, metricMilieu.getDiscriminator(), metricMilieu.getLevel());
          for (Filter filter : filterCollection) {
            if (!filter.willLog(filterRecord)) {
              skipMetric = true;
              break;
            }
          }
        }

        if (!skipMetric) {
          appendLine(formatBuilder, "<" + metricMilieu.getMetric().getTitle() + ">", level + 1);
          for (String key : metricMilieu.getMetric().getKeys()) {
            appendElement(formatBuilder, key, metricMilieu.getMetric().getData(key).toString(), level + 2);
          }
          appendLine(formatBuilder, "</" + metricMilieu.getMetric().getTitle() + ">", level + 1);
        }
      }

      appendLine(formatBuilder, "</probe-entry>", level);
    }
  }

  private void appendElement (StringBuilder formatBuilder, String tagName, String value, int level) {

    if (value != null) {
      appendIndent(formatBuilder, level);
      formatBuilder.append("<");
      formatBuilder.append(tagName);
      formatBuilder.append(">");
      formatBuilder.append(value);
      formatBuilder.append("</");
      formatBuilder.append(tagName);
      formatBuilder.append(">");
      formatBuilder.append(newLine);
    }
  }

  private void appendLine (StringBuilder formatBuilder, String content, int level) {

    appendIndent(formatBuilder, level);
    formatBuilder.append(content);
    formatBuilder.append(newLine);
  }

  private void appendFinalLine (StringBuilder formatBuilder, String content, int level) {

    appendIndent(formatBuilder, level);
    formatBuilder.append(content);
    formatBuilder.append(System.getProperty("line.separator"));
  }

  private void appendIndent (StringBuilder formatBuilder, int level) {

    for (int count = 0; count < (level * indent); count++) {
      formatBuilder.append(" ");
    }
  }
}