/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.orm.spring.jdo.antique;

import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class JdoTransactionManager extends AbstractPlatformTransactionManager
  implements ResourceTransactionManager, InitializingBean {

  private PersistenceManagerFactory persistenceManagerFactory;

  private DataSource dataSource;

  private boolean autodetectDataSource = true;

  private JdoDialect jdoDialect;

  /**
   * Create a new JdoTransactionManager instance.
   * A PersistenceManagerFactory has to be set to be able to use it.
   */
  public JdoTransactionManager () {

  }

  /**
   * Create a new JdoTransactionManager instance.
   */
  public JdoTransactionManager (PersistenceManagerFactory pmf) {

    this.persistenceManagerFactory = pmf;
    afterPropertiesSet();
  }

  /**
   * Return the PersistenceManagerFactory that this instance should manage transactions for.
   */
  public PersistenceManagerFactory getPersistenceManagerFactory () {

    return this.persistenceManagerFactory;
  }

  /**
   * Set the PersistenceManagerFactory that this instance should manage transactions for.
   * <p>The PersistenceManagerFactory specified here should be the target
   * PersistenceManagerFactory to manage transactions for, not a
   * TransactionAwarePersistenceManagerFactoryProxy. Only data access
   * code may work with TransactionAwarePersistenceManagerFactoryProxy, while the
   * transaction manager needs to work on the underlying target PersistenceManagerFactory.
   */
  public void setPersistenceManagerFactory (PersistenceManagerFactory pmf) {

    this.persistenceManagerFactory = pmf;
  }

  /**
   * Return the JDBC DataSource that this instance manages transactions for.
   */
  public DataSource getDataSource () {

    return this.dataSource;
  }

  /**
   * Set the JDBC DataSource that this instance should manage transactions for.
   * The DataSource should match the one used by the JDO PersistenceManagerFactory:
   * for example, you could specify the same JNDI DataSource for both.
   * <p>If the PersistenceManagerFactory uses a DataSource as connection factory,
   * the DataSource will be autodetected: You can still explicitly specify the
   * DataSource, but you don't need to in this case.
   * <p>A transactional JDBC Connection for this DataSource will be provided to
   * application code accessing this DataSource directly via DataSourceUtils
   * or JdbcTemplate. The Connection will be taken from the JDO PersistenceManager.
   * <p>Note that you need to use a JDO dialect for a specific JDO provider to
   * allow for exposing JDO transactions as JDBC transactions.
   * <p>The DataSource specified here should be the target DataSource to manage
   * transactions for, not a TransactionAwareDataSourceProxy. Only data access
   * code may work with TransactionAwareDataSourceProxy, while the transaction
   * manager needs to work on the underlying target DataSource. If there's
   * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
   * unwrapped to extract its target DataSource.
   */
  public void setDataSource (DataSource dataSource) {

    if (dataSource instanceof TransactionAwareDataSourceProxy) {
      // If we got a TransactionAwareDataSourceProxy, we need to perform transactions
      // for its underlying target DataSource, else data access code won't see
      // properly exposed transactions (i.e. transactions for the target DataSource).
      this.dataSource = ((TransactionAwareDataSourceProxy)dataSource).getTargetDataSource();
    } else {
      this.dataSource = dataSource;
    }
  }

  /**
   * Set whether to autodetect a JDBC DataSource used by the JDO PersistenceManagerFactory,
   * as returned by the {@code getConnectionFactory()} method. Default is "true".
   * <p>Can be turned off to deliberately ignore an available DataSource,
   * to not expose JDO transactions as JDBC transactions for that DataSource.
   */
  public void setAutodetectDataSource (boolean autodetectDataSource) {

    this.autodetectDataSource = autodetectDataSource;
  }

  /**
   * Return the JDO dialect to use for this transaction manager.
   * <p>Creates a default one for the specified PersistenceManagerFactory if none set.
   */
  public JdoDialect getJdoDialect () {

    if (this.jdoDialect == null) {
      this.jdoDialect = new DefaultJdoDialect();
    }
    return this.jdoDialect;
  }

  /**
   * Set the JDO dialect to use for this transaction manager.
   * <p>The dialect object can be used to retrieve the underlying JDBC connection
   * and thus allows for exposing JDO transactions as JDBC transactions.
   */
  public void setJdoDialect (JdoDialect jdoDialect) {

    this.jdoDialect = jdoDialect;
  }

  /**
   * Eagerly initialize the JDO dialect, creating a default one
   * for the specified PersistenceManagerFactory if none set.
   * Auto-detect the PersistenceManagerFactory's DataSource, if any.
   */
  @Override
  public void afterPropertiesSet () {

    if (getPersistenceManagerFactory() == null) {
      throw new IllegalArgumentException("Property 'persistenceManagerFactory' is required");
    }
    // Build default JdoDialect if none explicitly specified.
    if (this.jdoDialect == null) {
      this.jdoDialect = new DefaultJdoDialect(getPersistenceManagerFactory().getConnectionFactory());
    }

    // Check for DataSource as connection factory.
    if (this.autodetectDataSource && getDataSource() == null) {
      Object pmfcf = getPersistenceManagerFactory().getConnectionFactory();
      if (pmfcf instanceof DataSource) {
        // Use the PersistenceManagerFactory's DataSource for exposing transactions to JDBC code.
        this.dataSource = (DataSource)pmfcf;
        if (logger.isInfoEnabled()) {
          logger.info("Using DataSource [" + this.dataSource +
                        "] of JDO PersistenceManagerFactory for JdoTransactionManager");
        }
      }
    }
  }

  @Override
  public Object getResourceFactory () {

    return getPersistenceManagerFactory();
  }

  @Override
  protected Object doGetTransaction () {

    JdoTransactionObject txObject = new JdoTransactionObject();
    txObject.setSavepointAllowed(isNestedTransactionAllowed());

    PersistenceManagerHolder pmHolder = (PersistenceManagerHolder)
                                          TransactionSynchronizationManager.getResource(getPersistenceManagerFactory());
    if (pmHolder != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found thread-bound PersistenceManager [" +
                       pmHolder.getPersistenceManager() + "] for JDO transaction");
      }
      txObject.setPersistenceManagerHolder(pmHolder, false);
    }

    if (getDataSource() != null) {
      ConnectionHolder conHolder = (ConnectionHolder)
                                     TransactionSynchronizationManager.getResource(getDataSource());
      txObject.setConnectionHolder(conHolder);
    }

    return txObject;
  }

  @Override
  protected boolean isExistingTransaction (Object transaction) {

    return ((JdoTransactionObject)transaction).hasTransaction();
  }

  @Override
  protected void doBegin (Object transaction, TransactionDefinition definition) {

    JdoTransactionObject txObject = (JdoTransactionObject)transaction;

    if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
      throw new IllegalTransactionStateException(
        "Pre-bound JDBC Connection found! JdoTransactionManager does not support " +
          "running within DataSourceTransactionManager if told to manage the DataSource itself. " +
          "It is recommended to use a single JdoTransactionManager for all transactions " +
          "on a single DataSource, no matter whether JDO or JDBC access.");
    }

    PersistenceManager pm;

    try {
      if (txObject.getPersistenceManagerHolder() == null ||
            txObject.getPersistenceManagerHolder().isSynchronizedWithTransaction()) {
        PersistenceManager newPm = getPersistenceManagerFactory().getPersistenceManager();
        if (logger.isDebugEnabled()) {
          logger.debug("Opened new PersistenceManager [" + newPm + "] for JDO transaction");
        }
        txObject.setPersistenceManagerHolder(new PersistenceManagerHolder(newPm), true);
      }

      pm = txObject.getPersistenceManagerHolder().getPersistenceManager();

      // Delegate to JdoDialect for actual transaction begin.
      final int timeoutToUse = determineTimeout(definition);
      Object transactionData = getJdoDialect().beginTransaction(pm.currentTransaction(),
        new DelegatingTransactionDefinition(definition) {

          @Override
          public int getTimeout () {

            return timeoutToUse;
          }
        });
      txObject.setTransactionData(transactionData);

      // Register transaction timeout.
      if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
        txObject.getPersistenceManagerHolder().setTimeoutInSeconds(timeoutToUse);
      }

      // Register the JDO PersistenceManager's JDBC Connection for the DataSource, if set.
      if (getDataSource() != null) {
        ConnectionHandle conHandle = getJdoDialect().getJdbcConnection(pm, definition.isReadOnly());
        if (conHandle != null) {
          ConnectionHolder conHolder = new ConnectionHolder(conHandle);
          if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
            conHolder.setTimeoutInSeconds(timeoutToUse);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("Exposing JDO transaction as JDBC transaction [" +
                           conHolder.getConnectionHandle() + "]");
          }
          TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
          txObject.setConnectionHolder(conHolder);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Not exposing JDO transaction [" + pm + "] as JDBC transaction because " +
                           "JdoDialect [" + getJdoDialect() + "] does not support JDBC Connection retrieval");
          }
        }
      }

      // Bind the persistence manager holder to the thread.
      if (txObject.isNewPersistenceManagerHolder()) {
        TransactionSynchronizationManager.bindResource(
          getPersistenceManagerFactory(), txObject.getPersistenceManagerHolder());
      }
      txObject.getPersistenceManagerHolder().setSynchronizedWithTransaction(true);
    } catch (TransactionException ex) {
      closePersistenceManagerAfterFailedBegin(txObject);
      throw ex;
    } catch (Throwable ex) {
      closePersistenceManagerAfterFailedBegin(txObject);
      throw new CannotCreateTransactionException("Could not open JDO PersistenceManager for transaction", ex);
    }
  }

  /**
   * Close the current transaction's EntityManager.
   * Called after a transaction begin attempt failed.
   */
  protected void closePersistenceManagerAfterFailedBegin (JdoTransactionObject txObject) {

    if (txObject.isNewPersistenceManagerHolder()) {
      PersistenceManager pm = txObject.getPersistenceManagerHolder().getPersistenceManager();
      try {
        if (pm.currentTransaction().isActive()) {
          pm.currentTransaction().rollback();
        }
      } catch (Throwable ex) {
        logger.debug("Could not rollback PersistenceManager after failed transaction begin", ex);
      } finally {
        PersistenceManagerFactoryUtils.releasePersistenceManager(pm, getPersistenceManagerFactory());
      }
      txObject.setPersistenceManagerHolder(null, false);
    }
  }

  @Override
  protected Object doSuspend (Object transaction) {

    JdoTransactionObject txObject = (JdoTransactionObject)transaction;
    txObject.setPersistenceManagerHolder(null, false);
    PersistenceManagerHolder persistenceManagerHolder = (PersistenceManagerHolder)
                                                          TransactionSynchronizationManager.unbindResource(getPersistenceManagerFactory());
    txObject.setConnectionHolder(null);
    ConnectionHolder connectionHolder = null;
    if (getDataSource() != null && TransactionSynchronizationManager.hasResource(getDataSource())) {
      connectionHolder = (ConnectionHolder)TransactionSynchronizationManager.unbindResource(getDataSource());
    }
    return new SuspendedResourcesHolder(persistenceManagerHolder, connectionHolder);
  }

  @Override
  protected void doResume (Object transaction, Object suspendedResources) {

    SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder)suspendedResources;
    TransactionSynchronizationManager.bindResource(
      getPersistenceManagerFactory(), resourcesHolder.getPersistenceManagerHolder());
    if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
      TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
    }
  }

  /**
   * This implementation returns "true": a JDO commit will properly handle
   * transactions that have been marked rollback-only at a global level.
   */
  @Override
  protected boolean shouldCommitOnGlobalRollbackOnly () {

    return true;
  }

  @Override
  protected void doCommit (DefaultTransactionStatus status) {

    JdoTransactionObject txObject = (JdoTransactionObject)status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Committing JDO transaction on PersistenceManager [" +
                     txObject.getPersistenceManagerHolder().getPersistenceManager() + "]");
    }
    try {
      Transaction tx = txObject.getPersistenceManagerHolder().getPersistenceManager().currentTransaction();
      tx.commit();
    } catch (JDOException ex) {
      // Assumably failed to flush changes to database.
      throw convertJdoAccessException(ex);
    }
  }

  @Override
  protected void doRollback (DefaultTransactionStatus status) {

    JdoTransactionObject txObject = (JdoTransactionObject)status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Rolling back JDO transaction on PersistenceManager [" +
                     txObject.getPersistenceManagerHolder().getPersistenceManager() + "]");
    }
    try {
      Transaction tx = txObject.getPersistenceManagerHolder().getPersistenceManager().currentTransaction();
      if (tx.isActive()) {
        tx.rollback();
      }
    } catch (JDOException ex) {
      throw new TransactionSystemException("Could not roll back JDO transaction", ex);
    }
  }

  @Override
  protected void doSetRollbackOnly (DefaultTransactionStatus status) {

    JdoTransactionObject txObject = (JdoTransactionObject)status.getTransaction();
    if (status.isDebug()) {
      logger.debug("Setting JDO transaction on PersistenceManager [" +
                     txObject.getPersistenceManagerHolder().getPersistenceManager() + "] rollback-only");
    }
    txObject.setRollbackOnly();
  }

  @Override
  protected void doCleanupAfterCompletion (Object transaction) {

    JdoTransactionObject txObject = (JdoTransactionObject)transaction;

    // Remove the persistence manager holder from the thread.
    if (txObject.isNewPersistenceManagerHolder()) {
      TransactionSynchronizationManager.unbindResource(getPersistenceManagerFactory());
    }
    txObject.getPersistenceManagerHolder().clear();

    // Remove the JDBC connection holder from the thread, if exposed.
    if (txObject.hasConnectionHolder()) {
      TransactionSynchronizationManager.unbindResource(getDataSource());
      try {
        getJdoDialect().releaseJdbcConnection(txObject.getConnectionHolder().getConnectionHandle(),
          txObject.getPersistenceManagerHolder().getPersistenceManager());
      } catch (Throwable ex) {
        // Just log it, to keep a transaction-related exception.
        logger.debug("Could not release JDBC connection after transaction", ex);
      }
    }

    getJdoDialect().cleanupTransaction(txObject.getTransactionData());

    if (txObject.isNewPersistenceManagerHolder()) {
      PersistenceManager pm = txObject.getPersistenceManagerHolder().getPersistenceManager();
      if (logger.isDebugEnabled()) {
        logger.debug("Closing JDO PersistenceManager [" + pm + "] after transaction");
      }
      PersistenceManagerFactoryUtils.releasePersistenceManager(pm, getPersistenceManagerFactory());
    } else {
      logger.debug("Not closing pre-bound JDO PersistenceManager after transaction");
    }
  }

  /**
   * Convert the given JDOException to an appropriate exception from the
   * {@code org.springframework.dao} hierarchy.
   * <p>The default implementation delegates to the JdoDialect.
   * May be overridden in subclasses.
   */
  protected DataAccessException convertJdoAccessException (JDOException ex) {

    return getJdoDialect().translateException(ex);
  }

  /**
   * JDO transaction object, representing a PersistenceManagerHolder.
   * Used as transaction object by JdoTransactionManager.
   */
  private class JdoTransactionObject extends JdbcTransactionObjectSupport {

    private PersistenceManagerHolder persistenceManagerHolder;

    private boolean newPersistenceManagerHolder;

    private Object transactionData;

    public void setPersistenceManagerHolder (
      PersistenceManagerHolder persistenceManagerHolder, boolean newPersistenceManagerHolder) {

      this.persistenceManagerHolder = persistenceManagerHolder;
      this.newPersistenceManagerHolder = newPersistenceManagerHolder;
    }

    public PersistenceManagerHolder getPersistenceManagerHolder () {

      return this.persistenceManagerHolder;
    }

    public boolean isNewPersistenceManagerHolder () {

      return this.newPersistenceManagerHolder;
    }

    public boolean hasTransaction () {

      return (this.persistenceManagerHolder != null && this.persistenceManagerHolder.isTransactionActive());
    }

    public Object getTransactionData () {

      return this.transactionData;
    }

    public void setTransactionData (Object transactionData) {

      this.transactionData = transactionData;
      this.persistenceManagerHolder.setTransactionActive(true);
    }

    public void setRollbackOnly () {

      Transaction tx = this.persistenceManagerHolder.getPersistenceManager().currentTransaction();
      if (tx.isActive()) {
        tx.setRollbackOnly();
      }
      if (hasConnectionHolder()) {
        getConnectionHolder().setRollbackOnly();
      }
    }

    @Override
    public boolean isRollbackOnly () {

      Transaction tx = this.persistenceManagerHolder.getPersistenceManager().currentTransaction();
      return tx.getRollbackOnly();
    }

    @Override
    public void flush () {

      try {
        this.persistenceManagerHolder.getPersistenceManager().flush();
      } catch (JDOException ex) {
        throw convertJdoAccessException(ex);
      }
    }
  }

  /**
   * Holder for suspended resources.
   * Used internally by {@code doSuspend} and {@code doResume}.
   */
  private static class SuspendedResourcesHolder {

    private final PersistenceManagerHolder persistenceManagerHolder;

    private final ConnectionHolder connectionHolder;

    private SuspendedResourcesHolder (PersistenceManagerHolder pmHolder, ConnectionHolder conHolder) {

      this.persistenceManagerHolder = pmHolder;
      this.connectionHolder = conHolder;
    }

    private PersistenceManagerHolder getPersistenceManagerHolder () {

      return this.persistenceManagerHolder;
    }

    private ConnectionHolder getConnectionHolder () {

      return this.connectionHolder;
    }
  }
}
