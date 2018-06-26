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
package org.smallmind.persistence.orm.spring.jdo.antique;

import java.sql.SQLException;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * SPI strategy that allows for customizing integration with a specific JDO provider,
 * in particular regarding transaction management and exception translation. To be
 * implemented for specific JDO providers such as JPOX, Kodo, Lido, Versant Open Access.
 *
 * <p>JDO 3.0 defines standard ways for most of the functionality covered here.
 * Hence, Spring's {@link DefaultJdoDialect} uses the corresponding JDO 3.0 methods
 * by default, to be overridden in a vendor-specific fashion if necessary.
 * Vendor-specific subclasses of {@link DefaultJdoDialect} are still required for special
 * transaction semantics and more sophisticated exception translation (if needed).
 *
 * <p>In general, it is recommended to derive from {@link DefaultJdoDialect} instead
 * of implementing this interface directly. This allows for inheriting common
 * behavior (present and future) from {@link DefaultJdoDialect}, only overriding
 * specific hooks to plug in concrete vendor-specific behavior.
 *
 * @author Juergen Hoeller
 * @since 02.11.2003
 * @see JdoTransactionManager#setJdoDialect
 * @see DefaultJdoDialect
 */
public interface JdoDialect {

	//-------------------------------------------------------------------------
	// Hooks for transaction management (used by JdoTransactionManager)
	//-------------------------------------------------------------------------

	/**
	 * Begin the given JDO transaction, applying the semantics specified by the
	 * given Spring transaction definition (in particular, an isolation level
	 * and a timeout). Invoked by JdoTransactionManager on transaction begin.
	 * <p>An implementation can configure the JDO Transaction object and then
	 * invoke {@code begin}, or invoke a special begin method that takes,
	 * for example, an isolation level.
	 * <p>An implementation can also apply read-only flag and isolation level to the
	 * underlying JDBC Connection before beginning the transaction. In that case,
	 * a transaction data object can be returned that holds the previous isolation
	 * level (and possibly other data), to be reset in {@code cleanupTransaction}.
	 * <p>Implementations can also use the Spring transaction name, as exposed by the
	 * passed-in TransactionDefinition, to optimize for specific data access use cases
	 * (effectively using the current transaction name as use case identifier).
	 * @param transaction the JDO transaction to begin
	 * @param definition the Spring transaction definition that defines semantics
	 * @return an arbitrary object that holds transaction data, if any
	 * (to be passed into cleanupTransaction)
	 * @throws JDOException if thrown by JDO methods
	 * @throws SQLException if thrown by JDBC methods
	 * @throws TransactionException in case of invalid arguments
	 * @see #cleanupTransaction
	 * @see javax.jdo.Transaction#begin
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#prepareConnectionForTransaction
	 */
	Object beginTransaction(Transaction transaction, TransactionDefinition definition)
			throws JDOException, SQLException, TransactionException;

	/**
	 * Clean up the transaction via the given transaction data.
	 * Invoked by JdoTransactionManager on transaction cleanup.
	 * <p>An implementation can, for example, reset read-only flag and
	 * isolation level of the underlying JDBC Connection. Furthermore,
	 * an exposed data access use case can be reset here.
	 * @param transactionData arbitrary object that holds transaction data, if any
	 * (as returned by beginTransaction)
	 * @see #beginTransaction
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#resetConnectionAfterTransaction
	 */
	void cleanupTransaction(Object transactionData);

	/**
	 * Retrieve the JDBC Connection that the given JDO PersistenceManager uses underneath,
	 * if accessing a relational database. This method will just get invoked if actually
	 * needing access to the underlying JDBC Connection, usually within an active JDO
	 * transaction (for example, by JdoTransactionManager). The returned handle will
	 * be passed into the {@code releaseJdbcConnection} method when not needed anymore.
	 * <p>Implementations are encouraged to return an unwrapped Connection object, i.e.
	 * the Connection as they got it from the connection pool. This makes it easier for
	 * application code to get at the underlying native JDBC Connection, like an
	 * OracleConnection, which is sometimes necessary for LOB handling etc. We assume
	 * that calling code knows how to properly handle the returned Connection object.
	 * <p>In a simple case where the returned Connection will be auto-closed with the
	 * PersistenceManager or can be released via the Connection object itself, an
	 * implementation can return a SimpleConnectionHandle that just contains the
	 * Connection. If some other object is needed in {@code releaseJdbcConnection},
	 * an implementation should use a special handle that references that other object.
	 * @param pm the current JDO PersistenceManager
	 * @param readOnly whether the Connection is only needed for read-only purposes
	 * @return a handle for the JDBC Connection, to be passed into
	 * {@code releaseJdbcConnection}, or {@code null}
	 * if no JDBC Connection can be retrieved
	 * @throws JDOException if thrown by JDO methods
	 * @throws SQLException if thrown by JDBC methods
	 * @see #releaseJdbcConnection
	 * @see org.springframework.jdbc.datasource.ConnectionHandle#getConnection
	 * @see org.springframework.jdbc.datasource.SimpleConnectionHandle
	 * @see JdoTransactionManager#setDataSource
	 * @see org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor
	 */
	ConnectionHandle getJdbcConnection(PersistenceManager pm, boolean readOnly)
			throws JDOException, SQLException;

	/**
	 * Release the given JDBC Connection, which has originally been retrieved
	 * via {@code getJdbcConnection}. This should be invoked in any case,
	 * to allow for proper release of the retrieved Connection handle.
	 * <p>An implementation might simply do nothing, if the Connection returned
	 * by {@code getJdbcConnection} will be implicitly closed when the JDO
	 * transaction completes or when the PersistenceManager is closed.
	 * @param conHandle the JDBC Connection handle to release
	 * @param pm the current JDO PersistenceManager
	 * @throws JDOException if thrown by JDO methods
	 * @throws SQLException if thrown by JDBC methods
	 * @see #getJdbcConnection
	 */
	void releaseJdbcConnection(ConnectionHandle conHandle, PersistenceManager pm)
			throws JDOException, SQLException;


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JdoTransactionManager)
	//-----------------------------------------------------------------------------------

	/**
	 * Translate the given JDOException to a corresponding exception from Spring's
	 * generic DataAccessException hierarchy. An implementation should apply
	 * PersistenceManagerFactoryUtils' standard exception translation if can't do
	 * anything more specific.
	 * <p>Of particular importance is the correct translation to
	 * DataIntegrityViolationException, for example on constraint violation.
	 * Unfortunately, standard JDO does not allow for portable detection of this.
	 * <p>Can use a SQLExceptionTranslator for translating underlying SQLExceptions
	 * in a database-specific fashion.
	 * @param ex the JDOException thrown
	 * @return the corresponding DataAccessException (must not be {@code null})
	 * @see JdoTransactionManager#convertJdoAccessException
	 * @see PersistenceManagerFactoryUtils#convertJdoAccessException
	 * @see org.springframework.dao.DataIntegrityViolationException
	 * @see org.springframework.jdbc.support.SQLExceptionTranslator
	 */
	DataAccessException translateException(JDOException ex);

}
