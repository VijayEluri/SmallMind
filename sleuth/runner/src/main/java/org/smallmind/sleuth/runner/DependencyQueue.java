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
package org.smallmind.sleuth.runner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class DependencyQueue<T> {

  private LinkedList<Dependency<T>> dependencyList;
  private HashSet<String> completedSet = new HashSet<>();

  public DependencyQueue (LinkedList<Dependency<T>> dependencyList) {

    this.dependencyList = dependencyList;
  }

  public synchronized Dependency<T> poll () {

    while (!dependencyList.isEmpty()) {

      Iterator<Dependency<T>> dependencyIter = dependencyList.iterator();

      while (dependencyIter.hasNext()) {

        Dependency<T> dependency;

        if (isComplete(dependency = dependencyIter.next())) {
          dependencyIter.remove();

          return dependency;
        }
      }

      try {
        wait();
      } catch (InterruptedException interruptedException) {
        throw new RuntimeException(interruptedException);
      }
    }

    return null;
  }

  public synchronized void complete (Dependency<T> dependency) {

    completedSet.add(dependency.getName());
    notifyAll();
  }

  private boolean isComplete (Dependency<T> dependency) {

    if ((dependency.getDependsOn() != null) && (dependency.getDependsOn().length > 0)) {
      for (String requirement : dependency.getDependsOn()) {
        if (!completedSet.contains(requirement)) {

          return false;
        }
      }
    }
    if ((dependency.getPriorityOn() != null) && (dependency.getPriorityOn().length > 0)) {
      for (String requirement : dependency.getPriorityOn()) {
        if (!completedSet.contains(requirement)) {

          return false;
        }
      }
    }

    return true;
  }
}
