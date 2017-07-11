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
package org.smallmind.sleuth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class DependencyAnalysis<T> {

  private HashMap<String, Dependency<T>> dependencyMap = new HashMap<>();
  private Class<T> clazz;

  public DependencyAnalysis (Class<T> clazz) {

    this.clazz = clazz;
  }

  public void add (Dependency<T> dependency) {

    Dependency<T> mappedDependency;

    if ((mappedDependency = dependencyMap.putIfAbsent(dependency.getName(), dependency)) == null) {
      mappedDependency = dependency;
    } else {
      mappedDependency.align(dependency);
    }

    if ((mappedDependency.getDependsOn() != null) && (mappedDependency.getDependsOn().length > 0)) {
      for (String parentName : mappedDependency.getDependsOn()) {

        Dependency<T> parentDependency;

        if ((parentDependency = dependencyMap.get(parentName)) == null) {
          dependencyMap.put(parentName, parentDependency = new Dependency<>(parentName));
        }
        parentDependency.addChild(mappedDependency);
      }
    }
  }

  public LinkedList<Dependency<T>> calculate () {

    LinkedList<Dependency<T>> dependencyList = new LinkedList<>();

    while (!dependencyMap.isEmpty()) {

      HashSet<String> completedSet = new HashSet<>();

      for (Dependency<T> dependency : dependencyMap.values()) {
        visit(dependency, dependencyList, completedSet);
      }
      for (String name : completedSet) {
        dependencyMap.remove(name);
      }
    }

    return dependencyList;
  }

  private void visit (Dependency<T> dependency, LinkedList<Dependency<T>> dependencyList, HashSet<String> completedSet) {

    if (dependency.isTemporary()) {
      throw new TestDependencyException("Cyclic dependency(%s) detected involving node(%s)", clazz.getSimpleName(), dependency.getName());
    }
    if (!(dependency.isTemporary() || dependency.isPermanent())) {
      dependency.setTemporary();
      for (Dependency<T> childDependency : dependency.getChildren()) {
        visit(childDependency, dependencyList, completedSet);
      }
      if (!dependency.isCompleted()) {
        throw new TestDependencyException("Missing dependency(%s) on node(%s)", clazz.getSimpleName(), dependency.getName());
      }
      dependency.setPermanent();
      dependency.unsetTemporary();
      dependencyList.addFirst(dependency);
      completedSet.add(dependency.getName());
    }
  }
}