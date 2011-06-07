/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.orm.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.BoundarySet;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.RollbackAwareBoundarySet;
import org.smallmind.persistence.orm.aop.TransactionalState;

public class HibernateProxySession extends ProxySession {

  private final ThreadLocal<Session> managerThreadLocal = new ThreadLocal<Session>();
  private final ThreadLocal<HibernateProxyTransaction> transactionThreadLocal = new ThreadLocal<HibernateProxyTransaction>();

  private SessionFactory sessionFactory;

  public HibernateProxySession (String dataSourceKey, SessionFactory sessionFactory, boolean boundaryEnforced, boolean cacheEnabled) {

    super(dataSourceKey, boundaryEnforced, cacheEnabled);

    this.sessionFactory = sessionFactory;
  }

  public ClassMetadata getClassMetadata (Class entityClass) {

    return sessionFactory.getClassMetadata(entityClass);
  }

  public HibernateProxyTransaction beginTransaction () {

    HibernateProxyTransaction proxyTransaction;

    if ((proxyTransaction = transactionThreadLocal.get()) == null) {

      Session session = getSession();

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {
        proxyTransaction = new HibernateProxyTransaction(this, session.beginTransaction());
        transactionThreadLocal.set(proxyTransaction);
      }
    }

    return proxyTransaction;
  }

  public ProxyTransaction currentTransaction () {

    return transactionThreadLocal.get();
  }

  public void flush () {

    Session session;

    (session = getSession()).flush();
    session.clear();
  }

  public boolean isClosed () {

    Session session;

    return ((session = managerThreadLocal.get()) == null) || (!session.isOpen());
  }

  public Object getNativeSession () {

    return getSession();
  }

  public Session getSession () {

    Session session;

    if ((session = managerThreadLocal.get()) == null) {
      session = sessionFactory.openSession();
      managerThreadLocal.set(session);

      RollbackAwareBoundarySet<ProxyTransaction> transactionSet;
      BoundarySet<ProxySession> sessionSet;

      if ((transactionSet = TransactionalState.obtainBoundary(this)) != null) {
        try {
          transactionSet.add(beginTransaction());
        }
        catch (Throwable throwable) {
          close();
          throw new SessionEnforcementException(throwable);
        }
      }
      else if ((sessionSet = NonTransactionalState.obtainBoundary(this)) != null) {
        sessionSet.add(this);
      }
      else if (isBoundaryEnforced()) {
        close();
        throw new SessionEnforcementException("Session was requested outside of any boundary enforcement (@NonTransactional or @Transactional)");
      }
    }

    return session;
  }

  public void close () {

    Session session;

    try {
      if ((session = managerThreadLocal.get()) != null) {
        session.close();
      }
    }
    finally {
      managerThreadLocal.set(null);
      transactionThreadLocal.set(null);
    }
  }
}