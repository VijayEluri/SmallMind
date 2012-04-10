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
package org.smallmind.swing.io;

import java.io.IOException;
import java.io.InputStream;
import org.smallmind.swing.progress.ProgressDataHandler;
import org.smallmind.swing.progress.ProgressPanel;

public class ProgressInputStream extends InputStream implements ProgressDataHandler {

   private ProgressPanel progressPanel;
   private InputStream inputStream;
   private long length;
   private long index;
   private long markIndex;

   public ProgressInputStream (InputStream inputStream, long length, long pulseTime) {

      this.inputStream = inputStream;
      this.length = length;

      index = 0;
      markIndex = 0;

      progressPanel = new ProgressPanel(this, pulseTime);
   }

   public long getLength () {

      return length;
   }

   public synchronized long getIndex () {

      return index;
   }

   public ProgressPanel getIOProgressPanel () {

      return progressPanel;
   }

   public synchronized int read ()
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read()) >= 0) {
         index++;
      }

      return readValue;
   }

   public synchronized int read (byte buf[])
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read(buf)) >= 0) {
         index += readValue;
      }

      return readValue;
   }

   public synchronized int read (byte buf[], int off, int len)
      throws IOException {

      int readValue;

      if ((readValue = inputStream.read(buf, off, len)) >= 0) {
         index += readValue;
      }

      return readValue;
   }

   public synchronized long skip (long n)
      throws IOException {

      long skipValue;

      skipValue = inputStream.skip(n);
      index += skipValue;

      return skipValue;
   }

   public synchronized void mark (int readAheadLimit) {

      inputStream.mark(readAheadLimit);
      markIndex = index;
   }

   public synchronized void reset ()
      throws IOException {

      inputStream.reset();
      index = markIndex;
   }

   public void close ()
      throws IOException {

      inputStream.close();
   }

}
