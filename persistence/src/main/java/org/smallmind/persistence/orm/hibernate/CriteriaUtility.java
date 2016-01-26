/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.orm.hibernate;

import java.util.LinkedList;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.query.Sort;
import org.smallmind.persistence.query.SortField;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereConjunction;
import org.smallmind.persistence.query.WhereCriterion;
import org.smallmind.persistence.query.WhereField;

public class CriteriaUtility {

  public static Criteria apply (Where where, Criteria criteria) {

    if (where != null) {

      Criterion walkedCriterion;

      if ((walkedCriterion = walkConjunction(where.getRootConjunction())) != null) {
        return criteria.add(walkedCriterion);
      }
    }

    return criteria;
  }

  private static Criterion walkConjunction (WhereConjunction whereConjunction) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Criterion> criterionList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {
      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:

          Criterion walkedCriterion;

          if ((walkedCriterion = walkConjunction((WhereConjunction)whereCriterion)) != null) {
            criterionList.add(walkedCriterion);
          }
          break;
        case FIELD:
          criterionList.add(walkField((WhereField)whereCriterion));
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }

    if (criterionList.isEmpty()) {

      return null;
    } else {

      Criterion[] criteria;

      criteria = new Criterion[criterionList.size()];
      criterionList.toArray(criteria);

      switch (whereConjunction.getConjunctionType()) {
        case AND:
          return Restrictions.and(criteria);
        case OR:
          return Restrictions.or(criteria);
        default:
          throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
      }
    }
  }

  private static Criterion walkField (WhereField whereField) {

    switch (whereField.getOperation()) {
      case LT:
        return Restrictions.lt(whereField.getName(), whereField.getValue().getValue());
      case LE:
        return Restrictions.le(whereField.getName(), whereField.getValue().getValue());
      case EQ:

        Object value;

        return ((value = whereField.getValue().getValue()) == null) ? Restrictions.isNull(whereField.getName()) : Restrictions.eq(whereField.getName(), value);
      case GE:
        return Restrictions.ge(whereField.getName(), whereField.getValue().getValue());
      case GT:
        return Restrictions.gt(whereField.getName(), whereField.getValue().getValue());
      case LIKE:
        return Restrictions.like(whereField.getName(), whereField.getValue().getValue());
      case IN:
        return Restrictions.in(whereField.getName(), (Object[])whereField.getValue().getValue());
      default:
        throw new UnknownSwitchCaseException(whereField.getOperation().name());
    }
  }

  public static Criteria apply (Sort sort, Criteria criteria) {

    if ((sort != null) && (!sort.isEmpty())) {
      for (SortField sortField : sort.getFields()) {
        switch (sortField.getDirection()) {
          case ASC:
            criteria.addOrder(Order.asc(sortField.getName()));
            break;
          case DESC:
            criteria.addOrder(Order.desc(sortField.getName()));
            break;
          default:
            throw new UnknownSwitchCaseException(sortField.getDirection().name());
        }
      }
    }

    return criteria;
  }
}
