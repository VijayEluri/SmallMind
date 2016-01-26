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
package org.smallmind.persistence.orm.aop;

import java.util.LinkedList;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;

public class TransactionalState {

  private static final ThreadLocal<LinkedList<RollbackAwareBoundarySet<ProxyTransaction>>> TRANSACTION_SET_STACK_LOCAL = new ThreadLocal<LinkedList<RollbackAwareBoundarySet<ProxyTransaction>>>();

  public static boolean isInTransaction () {

    return isInTransaction(null);
  }

  public static boolean isInTransaction (String sessionSourceKey) {

    return currentTransaction(sessionSourceKey) != null;
  }

  public static ProxyTransaction currentTransaction (String sessionSourceKey) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
      for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
        for (ProxyTransaction proxyTransaction : transactionSet) {
          if (sessionSourceKey == null) {
            if (proxyTransaction.getSession().getSessionSourceKey() == null) {

              return proxyTransaction;
            }
          }
          else if (sessionSourceKey.equals(proxyTransaction.getSession().getSessionSourceKey())) {

            return proxyTransaction;
          }
        }
      }
    }

    return null;
  }

  public static boolean withinBoundary (ProxySession proxySession) {

    return withinBoundary(proxySession.getSessionSourceKey());
  }

  public static boolean withinBoundary (String sessionSourceKey) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
      for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
        if (transactionSet.allows(sessionSourceKey)) {
          return true;
        }
      }
    }

    return false;
  }

  public static RollbackAwareBoundarySet<ProxyTransaction> obtainBoundary (ProxySession proxySession) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
      for (RollbackAwareBoundarySet<ProxyTransaction> transactionSet : transactionSetStack) {
        if (transactionSet.allows(proxySession)) {
          if (NonTransactionalState.containsSession(proxySession)) {
            throw new StolenTransactionError("Attempt to steal the session - a non-transactional boundary is already enforced");
          }

          return transactionSet;
        }
      }
    }

    return null;
  }

  protected static void startBoundary (Transactional transactional) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) {
      TRANSACTION_SET_STACK_LOCAL.set(transactionSetStack = new LinkedList<RollbackAwareBoundarySet<ProxyTransaction>>());
    }

    transactionSetStack.addLast(new RollbackAwareBoundarySet<ProxyTransaction>(transactional.dataSources(), transactional.implicit(), transactional.rollbackOnly()));
  }

  protected static void commitBoundary ()
    throws TransactionError {

    commitBoundary(null);
  }

  protected static void commitBoundary (Throwable throwable)
    throws TransactionError {

    if ((throwable == null) || (!(throwable instanceof TransactionError)) || ((TRANSACTION_SET_STACK_LOCAL.get() != null) && (TRANSACTION_SET_STACK_LOCAL.get().size() != ((TransactionError)throwable).getClosure()))) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;
      RollbackAwareBoundarySet<ProxyTransaction> transactionSet;
      IncompleteTransactionError incompleteTransactionError = null;

      if (((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) || transactionSetStack.isEmpty()) {
        throw new TransactionBoundaryError(0, "No transaction boundary has been enforced");
      }

      try {
        for (ProxyTransaction proxyTransaction : transactionSet = transactionSetStack.removeLast()) {
          try {
            if (transactionSet.isRollbackOnly() || proxyTransaction.isRollbackOnly()) {
              proxyTransaction.rollback();
            }
            else {
              proxyTransaction.commit();
            }
          }
          catch (Throwable unexpectedThrowable) {
            if (incompleteTransactionError == null) {
              incompleteTransactionError = new IncompleteTransactionError(transactionSetStack.size(), unexpectedThrowable);
            }
          }
        }

        if (incompleteTransactionError != null) {
          throw incompleteTransactionError;
        }
      }
      finally {
        if (transactionSetStack.isEmpty()) {
          TRANSACTION_SET_STACK_LOCAL.remove();
        }
      }
    }
    else {
      TRANSACTION_SET_STACK_LOCAL.remove();
    }
  }

  protected static void rollbackBoundary (Throwable throwable)
    throws TransactionError {

    if ((throwable == null) || (!(throwable instanceof TransactionError)) || ((TRANSACTION_SET_STACK_LOCAL.get() != null) && (TRANSACTION_SET_STACK_LOCAL.get().size() != ((TransactionError)throwable).getClosure()))) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction>> transactionSetStack;
      IncompleteTransactionError incompleteTransactionError = null;

      if (((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) || transactionSetStack.isEmpty()) {
        throw new TransactionBoundaryError(0, throwable, "No transaction boundary has been enforced");
      }

      try {
        for (ProxyTransaction proxyTransaction : transactionSetStack.removeLast()) {
          try {
            proxyTransaction.rollback();
          }
          catch (Throwable unexpectedThrowable) {
            if (incompleteTransactionError == null) {
              incompleteTransactionError = new IncompleteTransactionError(transactionSetStack.size(), unexpectedThrowable);
            }
          }
        }

        if (incompleteTransactionError != null) {
          throw incompleteTransactionError;
        }
      }
      finally {
        if (transactionSetStack.isEmpty()) {
          TRANSACTION_SET_STACK_LOCAL.remove();
        }
      }
    }
    else {
      TRANSACTION_SET_STACK_LOCAL.remove();
    }
  }
}
