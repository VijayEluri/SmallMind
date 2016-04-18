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
package org.smallmind.persistence.orm.morphia;

import java.util.Arrays;
import java.util.LinkedList;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.orm.ORMOperationException;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereConjunction;
import org.smallmind.persistence.query.WhereCriterion;
import org.smallmind.persistence.query.WhereField;
import org.smallmind.persistence.query.WhereOperandTransformer;
import org.smallmind.persistence.query.WhereOperation;

public class QueryUtility {

  public static Query<?> apply (Query<?> query, Where where) {

    return apply(query, where, new WhereOperandTransformer() {

      @Override
      public Class<? extends Enum> getEnumType (String type) {

        throw new ORMOperationException("Translation of enum(%s) requires an implementation of a WhereOperandTransformer", type);
      }
    });
  }

  public static Query<?> apply (Query<?> query, Where where, WhereOperandTransformer transformer) {

    if (where != null) {
      walkConjunction(query, where.getRootConjunction(), transformer);
    }

    return query;
  }

  private static Criteria walkConjunction (Query<?> query, WhereConjunction whereConjunction, WhereOperandTransformer transformer) {

    if ((whereConjunction == null) || whereConjunction.isEmpty()) {

      return null;
    }

    LinkedList<Criteria> criteriaList = new LinkedList<>();

    for (WhereCriterion whereCriterion : whereConjunction.getCriteria()) {

      Criteria criteria;

      switch (whereCriterion.getCriterionType()) {
        case CONJUNCTION:
          if ((criteria = walkConjunction(query, (WhereConjunction)whereCriterion, transformer)) != null) {
            criteriaList.add(criteria);
          }
          break;
        case FIELD:
          if ((criteria = walkField(query, (WhereField)whereCriterion, transformer)) != null) {
            criteriaList.add(criteria);
          }
          break;
        default:
          throw new UnknownSwitchCaseException(whereCriterion.getCriterionType().name());
      }
    }

    if (criteriaList.isEmpty()) {

      return null;
    } else {

      Criteria[] criteria;

      criteria = new Criteria[criteriaList.size()];
      criteriaList.toArray(criteria);

      switch (whereConjunction.getConjunctionType()) {
        case AND:
          return query.and(criteria);
        case OR:
          return query.or(criteria);
        default:
          throw new UnknownSwitchCaseException(whereConjunction.getConjunctionType().name());
      }
    }
  }

  private static Criteria walkField (Query<?> query, WhereField whereField, WhereOperandTransformer transformer) {

    FieldEnd<? extends CriteriaContainerImpl> fieldEnd = query.criteria(whereField.getName());

    switch (whereField.getOperation()) {
      case LT:
        return fieldEnd.lessThan(whereField.getOperand().extract(transformer));
      case LE:
        return fieldEnd.lessThanOrEq(whereField.getOperand().extract(transformer));
      case EQ:

        Object equalValue;

        return ((equalValue = whereField.getOperand().extract(transformer)) == null) ? fieldEnd.doesNotExist() : fieldEnd.equal(equalValue);
      case GE:
        return fieldEnd.greaterThanOrEq(whereField.getOperand().extract(transformer));
      case GT:
        return fieldEnd.equal(whereField.getOperand().extract(transformer));
      case LIKE:

        Object likeValue;

        if ((likeValue = whereField.getOperand().extract(transformer)) == null) {

          return fieldEnd.doesNotExist();
        } else if (!(likeValue instanceof String)) {

          throw new ORMOperationException("The operation(%s) requires a String operand", WhereOperation.LIKE.name());
        } else {
          switch (((String)(likeValue)).length()) {
            case 0:
              return fieldEnd.equal("");
            case 1:
              return likeValue.equals("%") ? fieldEnd.exists() : fieldEnd.equal(likeValue);
            case 2:
              return likeValue.equals("%%") ? fieldEnd.exists() : (((String)likeValue).charAt(0) == '%') ? fieldEnd.startsWith(((String)likeValue).substring(1)) : (((String)likeValue).charAt(1) == '%') ? fieldEnd.endsWith(((String)likeValue).substring(0, 1)) : fieldEnd.equal(likeValue);
            default:
              if (((String)likeValue).substring(1, ((String)likeValue).length() - 1).indexOf('%') >= 0) {
                throw new ORMOperationException("The operation(%s) allows wildcards('%') only at the  start or end of the operand", WhereOperation.LIKE.name());
              } else if (((String)likeValue).startsWith("%") && ((String)likeValue).endsWith("%")) {

                return fieldEnd.contains(((String)likeValue).substring(1, ((String)likeValue).length() - 1));
              } else if (((String)likeValue).startsWith("%")) {

                return fieldEnd.startsWith(((String)likeValue).substring(1));
              } else if (((String)likeValue).endsWith("%")) {

                return fieldEnd.endsWith(((String)likeValue).substring(0, ((String)likeValue).length() - 1));
              } else {

                return fieldEnd.equal(likeValue);
              }
          }
        }
      case IN:
        return fieldEnd.in(Arrays.asList((Object[])whereField.getOperand().extract(transformer)));
      default:
        throw new UnknownSwitchCaseException(whereField.getOperation().name());
    }
  }
}
