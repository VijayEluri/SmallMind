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
package org.smallmind.persistence.query;

import java.util.Arrays;
import java.util.HashSet;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class WhereConjunction implements WhereCriterion {

  private HashSet<WhereCriterion> criterionSet;

  public WhereConjunction () {

  }

  public WhereConjunction (WhereCriterion... criteria) {

    criterionSet = new HashSet<>(Arrays.asList(criteria));
  }

  public abstract ConjunctionType getConjunctionType ();

  @Override
  @XmlTransient
  public CriterionType getCriterionType () {

    return CriterionType.CONJUNCTION;
  }

  @XmlTransient
  public synchronized boolean isEmpty () {

    return (criterionSet == null) || criterionSet.isEmpty();
  }

  @XmlElement(name = "criteria", required = false, nillable = false)
  @XmlElementRefs({@XmlElementRef(type = WhereField.class), @XmlElementRef(type = AndWhereConjunction.class), @XmlElementRef(type = OrWhereConjunction.class)})
  public synchronized WhereCriterion[] getCriteria () {

    WhereCriterion[] criteria = new WhereCriterion[criterionSet == null ? 0 : criterionSet.size()];

    if (criterionSet != null) {
      criterionSet.toArray(criteria);
    }

    return criteria;
  }

  public synchronized void setCriteria (WhereCriterion... criteria) {

    this.criterionSet = new HashSet<>(Arrays.asList(criteria));
  }
}