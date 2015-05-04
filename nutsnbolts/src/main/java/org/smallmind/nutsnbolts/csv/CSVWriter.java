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
package org.smallmind.nutsnbolts.csv;

import java.io.IOException;
import java.io.OutputStream;

public class CSVWriter implements AutoCloseable {

  private static char[] ESCAPED_CHARS = {'"', ',', '\n', '\r', '\f'};

  private OutputStream outputStream;
  private int lineLength;

  public CSVWriter (OutputStream outputStream, String[] headers)
    throws IOException, CSVParseException {

    this.outputStream = outputStream;

    lineLength = headers.length;
    write(headers);
  }

  public CSVWriter (OutputStream outputStream, int lineLength) {

    this.outputStream = outputStream;
    this.lineLength = lineLength;
  }

  public void write (String... fields)
    throws IOException, CSVParseException {

    boolean init = false;

    if (fields.length != lineLength) {
      throw new CSVParseException("Line must contain the set number of fields(%d)", lineLength);
    }

    for (String field : fields) {
      if (init) {
        outputStream.write(',');
      }

      if (mustBeQuoted(field)) {
        outputStream.write('"');
        outputStream.write(doubleAllQuotes(field).getBytes());
        outputStream.write('"');
      }
      else {
        outputStream.write(doubleAllQuotes(field).getBytes());
      }

      init = true;
    }

    outputStream.write('\n');
  }

  private String doubleAllQuotes (String field) {

    if (field.indexOf("\"") >= 0) {
      return field.replaceAll("\"", "\"\"");
    }

    return field;
  }

  private boolean mustBeQuoted (String field) {

    for (char escapeChar : ESCAPED_CHARS) {
      if (field.indexOf(escapeChar) >= 0) {
        return true;
      }
    }

    return false;
  }

  public void close ()
    throws IOException {

    outputStream.close();
  }
}
