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
package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileProcessorQueue {

   private static enum State {

      STARTED, STOPPED
   }

   private ReentrantLock exclusiveLock;
   private Condition emptyCondition;
   private FileProcessorGopher fileProcessorGopher;
   private LinkedList<File> waitingQueue;
   private HashSet<File> waitingSet;
   private HashSet<File> processingSet;
   private File directory;
   private FileFilter fileFilter;
   private TimeUnit timeUnit;
   private AtomicReference<State> atomicState = new AtomicReference<State>(State.STOPPED);
   private boolean removeEmptyDirectories = false;
   private long pulse;

   public FileProcessorQueue () {

      waitingQueue = new LinkedList<File>();
      waitingSet = new HashSet<File>();
      processingSet = new LinkedHashSet<File>();

      exclusiveLock = new ReentrantLock();
      emptyCondition = exclusiveLock.newCondition();
   }

   public FileProcessorQueue (File directory, long pulse, TimeUnit timeUnit) {

      this(directory, null, false, pulse, timeUnit);
   }

   public FileProcessorQueue (File directory, boolean removeEmptyDirectories, long pulse, TimeUnit timeUnit) {

      this(directory, null, removeEmptyDirectories, pulse, timeUnit);
   }

   public FileProcessorQueue (File directory, FileFilter fileFilter, long pulse, TimeUnit timeUnit) {

      this(directory, fileFilter, false, pulse, timeUnit);
   }

   public FileProcessorQueue (File directory, FileFilter fileFilter, boolean removeEmptyDirectories, long pulse, TimeUnit timeUnit) {

      this();

      this.directory = directory;
      this.fileFilter = fileFilter;
      this.removeEmptyDirectories = removeEmptyDirectories;
      this.pulse = pulse;
      this.timeUnit = timeUnit;
   }

   public void setDirectory (File directory) {

      this.directory = directory;
   }

   public File getDirectory () {

      return directory;
   }

   public void setFileFilter (FileFilter fileFilter) {

      this.fileFilter = fileFilter;
   }

   public void setRemoveEmptyDirectories (boolean removeEmptyDirectories) {

      this.removeEmptyDirectories = removeEmptyDirectories;
   }

   public void setPulse (long pulse) {

      this.pulse = pulse;
   }

   public void setTimeUnit (TimeUnit timeUnit) {

      this.timeUnit = timeUnit;
   }

   public void push (File file) {

      push(file, false);
   }

   public void push (File file, boolean forced) {

      exclusiveLock.lock();
      try {
         if ((forced || atomicState.get().equals(State.STARTED)) && file.exists()) {
            if (!(waitingSet.contains(file) || processingSet.contains(file))) {
               waitingQueue.addLast(file);
               waitingSet.add(file);

               emptyCondition.signal();
            }
         }
      }
      finally {
         exclusiveLock.unlock();
      }
   }

   public File poll ()
      throws InterruptedException {

      return poll(0, TimeUnit.MILLISECONDS);
   }

   public File poll (long timeout, TimeUnit timeUnit)
      throws InterruptedException {

      File file;
      boolean unexpired = true;

      exclusiveLock.lock();
      try {
         while (unexpired && atomicState.get().equals(State.STARTED) && waitingQueue.isEmpty()) {
            unexpired = emptyCondition.await(timeout, timeUnit);
         }

         if (unexpired && atomicState.get().equals(State.STARTED)) {
            waitingSet.remove(file = waitingQueue.removeFirst());
            processingSet.add(file);

            return file;
         }

         return null;
      }
      finally {
         exclusiveLock.unlock();
      }
   }

   public void delete (File file)
      throws FileProcessingException {

      exclusiveLock.lock();
      try {
         if (!processingSet.contains(file)) {
            throw new FileProcessingException("File(%s) is not currently marked for processing", file.getAbsolutePath());
         }

         processingSet.remove(file);
         if (!file.delete()) {
            throw new FileProcessingException("Unable to delete the file(%s)", file.getAbsolutePath());
         }

         if (removeEmptyDirectories && (file.getParentFile().list().length == 0)) {
            if (!file.getParentFile().delete()) {
               throw new FileProcessingException("Unable to delete the empty directory(%s)", file.getParentFile().getAbsolutePath());
            }
         }
      }
      finally {
         exclusiveLock.unlock();
      }
   }

   public void start () {

      if (atomicState.compareAndSet(State.STOPPED, State.STARTED)) {
         new Thread(fileProcessorGopher = new FileProcessorGopher(this, directory, fileFilter, pulse, timeUnit)).start();
      }
   }

   public void stop ()
      throws InterruptedException {

      if (atomicState.compareAndSet(State.STARTED, State.STOPPED)) {
         fileProcessorGopher.finish();
      }
   }
}