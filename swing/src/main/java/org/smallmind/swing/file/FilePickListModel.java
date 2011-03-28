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
package org.smallmind.swing.file;

import java.io.File;
import java.util.Arrays;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class FilePickListModel implements ListModel {

  private static final FileComparator FILE_COMPARATOR = new FileComparator();

  private WeakEventListenerList<ListDataListener> listenerList = new WeakEventListenerList<ListDataListener>();
  private FileFilter filter;
  private File directory;
  private File[] files;

  public FilePickListModel (File directory) {

    this(directory, null);
  }

  public FilePickListModel (File directory, FileFilter filter) {

    this.filter = filter;

    setDirectory(directory);
  }

  public synchronized FileFilter getFilter () {

    return filter;
  }

  public synchronized void setFilter (FileFilter filter) {

    this.filter = filter;
    setDirectory(directory);
  }

  public synchronized File getDirectory () {

    return directory;
  }

  public synchronized void setDirectory (File directory) {

    int prevSize = (files == null) ? 0 : files.length;

    this.directory = (directory == null) ? new File(System.getProperty("user.home")) : directory;

    files = this.directory.listFiles((java.io.FileFilter)filter);
    Arrays.sort(files, FILE_COMPARATOR);

    fireContentsChanges(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(files.length, prevSize)));
  }

  @Override
  public int getSize () {

    return files.length;
  }

  @Override
  public Object getElementAt (int index) {

    return files[index];
  }

  private synchronized void fireContentsChanges (ListDataEvent listDataEvent) {

    for (ListDataListener listDataListener : listenerList) {
      listDataListener.contentsChanged(listDataEvent);
    }
  }

  @Override
  public synchronized void addListDataListener (ListDataListener listDataListener) {

    listenerList.addListener(listDataListener);
  }

  @Override
  public synchronized void removeListDataListener (ListDataListener listDataListener) {

    listenerList.removeListener(listDataListener);
  }
}
