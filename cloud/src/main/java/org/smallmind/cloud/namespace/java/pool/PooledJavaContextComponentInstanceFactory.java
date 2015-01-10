/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.cloud.namespace.java.pool;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.smallmind.cloud.namespace.java.JavaContext;
import org.smallmind.cloud.namespace.java.PooledJavaContext;
import org.smallmind.cloud.namespace.java.backingStore.NamingConnectionDetails;
import org.smallmind.cloud.namespace.java.backingStore.StorageType;
import org.smallmind.quorum.pool.complex.AbstractComponentInstanceFactory;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class PooledJavaContextComponentInstanceFactory extends AbstractComponentInstanceFactory<PooledJavaContext> {

  private StorageType storageType;
  private String contextPath;
  private String host;
  private String rootNamespace;
  private String userContext;
  private String password;
  private boolean tls;
  private int port;

  public PooledJavaContextComponentInstanceFactory (StorageType storageType, String contextPath, String host, int port, boolean tls, String rootNamespace, String userContext, String password) {

    this.storageType = storageType;
    this.contextPath = contextPath;
    this.host = host;
    this.port = port;
    this.tls = tls;
    this.rootNamespace = rootNamespace;
    this.userContext = userContext;
    this.password = password;
  }

  public ComponentInstance<PooledJavaContext> createInstance (ComponentPool<PooledJavaContext> componentPool)
    throws Exception {

    PooledJavaContext pooledJavaContext;
    InitialContext initContext;
    Hashtable<String, Object> env;

    env = new Hashtable<String, Object>();
    env.put(Context.URL_PKG_PREFIXES, "org.smallmind.cloud.namespace");
    env.put(JavaContext.CONNECTION_DETAILS, new NamingConnectionDetails(host, port, tls, rootNamespace, userContext, password));
    env.put(JavaContext.CONTEXT_STORE, storageType.getBackingStore());
    env.put(JavaContext.CONTEXT_MODIFIABLE, "true");
    env.put(JavaContext.POOLED_CONNECTION, "true");

    initContext = new InitialContext(env);

    pooledJavaContext = (PooledJavaContext)initContext.lookup(contextPath);
    initContext.close();

    return new JavaContextComponentInstance(componentPool, pooledJavaContext);
  }
}
