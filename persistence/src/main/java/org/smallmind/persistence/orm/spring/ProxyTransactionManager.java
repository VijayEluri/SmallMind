/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.spring;

import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionUsageException;
import org.springframework.transaction.support.ResourceTransactionManager;

public class ProxyTransactionManager implements PlatformTransactionManager, ResourceTransactionManager {

  private ProxySession proxySession;

  public ProxyTransactionManager (ProxySession proxySession) {

    this.proxySession = proxySession;
  }

  @Override
  public Object getResourceFactory () {

    return proxySession.getNativeSession();
  }

  public TransactionStatus getTransaction (TransactionDefinition transactionDefinition)
    throws TransactionException {

    if (transactionDefinition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
      throw new TransactionUsageException("Timeouts are not supported");
    }

    return new ProxyTransactionStatus(proxySession.beginTransaction());
  }

  public void commit (TransactionStatus transactionStatus)
    throws TransactionException {

    ((ProxyTransactionStatus)transactionStatus).getProxyTransaction().commit();
  }

  public void rollback (TransactionStatus transactionStatus)
    throws TransactionException {

    ((ProxyTransactionStatus)transactionStatus).getProxyTransaction().rollback();
  }

  private class ProxyTransactionStatus implements TransactionStatus {

    private ProxyTransaction proxyTransaction;

    public ProxyTransactionStatus (ProxyTransaction proxyTransaction) {

      this.proxyTransaction = proxyTransaction;
    }

    protected ProxyTransaction getProxyTransaction () {

      return proxyTransaction;
    }

    public boolean isNewTransaction () {

      return false;
    }

    public boolean hasSavepoint () {

      return false;
    }

    public void setRollbackOnly () {

      proxyTransaction.setRollbackOnly();
    }

    public boolean isRollbackOnly () {

      return proxyTransaction.isRollbackOnly();
    }

    public boolean isCompleted () {

      return proxyTransaction.isCompleted();
    }

    public void flush () {

      proxyTransaction.flush();
    }

    public Object createSavepoint ()
      throws TransactionException {

      throw new TransactionUsageException("Savepoints are not supported");
    }

    public void rollbackToSavepoint (Object o)
      throws TransactionException {

      throw new TransactionUsageException("Savepoints are not supported");
    }

    public void releaseSavepoint (Object o)
      throws TransactionException {

      throw new TransactionUsageException("Savepoints are not supported");
    }
  }
}
