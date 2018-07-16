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
package org.smallmind.web.json.dto;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.smallmind.nutsnbolts.apt.AptUtility;

public class PropertyParser implements Iterable<PropertyBox> {

  private final LinkedList<PropertyBox> entryList = new LinkedList<>();

  public PropertyParser (AnnotationMirror propertyAnnotationMirror, TypeMirror type, boolean virtual) {

    boolean hasIdioms = false;

    for (AnnotationMirror idiomAnnotationMirror : AptUtility.extractAnnotationValueAsList(propertyAnnotationMirror, "idioms", AnnotationMirror.class)) {

      IdiomInformation idiomInformation = new IdiomInformation(idiomAnnotationMirror);

      hasIdioms = true;
      if (idiomInformation.getPurposeList().isEmpty()) {
        entryList.add(new PropertyBox(idiomInformation.getVisibility(), "", new PropertyInformation(propertyAnnotationMirror, idiomInformation.getConstraintList(), type, virtual)));
      } else {
        for (String purpose : idiomInformation.getPurposeList()) {
          entryList.add(new PropertyBox(idiomInformation.getVisibility(), purpose, new PropertyInformation(propertyAnnotationMirror, idiomInformation.getConstraintList(), type, virtual)));
        }
      }
    }
    if (!hasIdioms) {
      entryList.add(new PropertyBox(Visibility.BOTH, "", new PropertyInformation(propertyAnnotationMirror, Collections.emptyList(), type, virtual)));
    }
  }

  @Override
  public Iterator<PropertyBox> iterator () {

    return entryList.iterator();
  }
}
