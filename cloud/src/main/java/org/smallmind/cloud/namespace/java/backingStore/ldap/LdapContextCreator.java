/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.cloud.namespace.java.backingStore.ldap;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.smallmind.cloud.namespace.java.backingStore.ContextCreator;
import org.smallmind.cloud.namespace.java.backingStore.NamingConnectionDetails;

public class LdapContextCreator extends ContextCreator {

  public static void insureContext (DirContext dirContext, String namingPath)
    throws NamingException {

    StringBuilder pathSoFar;
    String[] pathArray;

    pathArray = namingPath.split(",", -1);
    pathSoFar = new StringBuilder();
    for (int count = pathArray.length - 1; count >= 0; count--) {
      if (pathSoFar.length() > 0) {
        pathSoFar.insert(0, ',');
      }
      pathSoFar.insert(0, pathArray[count]);
      try {
        dirContext.lookup(pathSoFar.toString());
      }
      catch (NameNotFoundException n) {
        dirContext.createSubcontext(pathSoFar.toString());
      }
    }
  }

  public LdapContextCreator (NamingConnectionDetails connectionDetails) {

    super(connectionDetails);
  }

  public String getRoot () {

    return getConnectionDetails().getRootNamespace();
  }

  public DirContext getInitialContext ()
    throws NamingException {

    InitialDirContext initLdapContext;
    DirContext rootLdapContext;
    Hashtable<String, String> env;
    String rootUrl;

    env = new Hashtable<String, String>();
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, getConnectionDetails().getUserName());
    env.put(Context.SECURITY_CREDENTIALS, getConnectionDetails().getPassword());

    initLdapContext = new InitialDirContext(env);
    rootUrl = "ldap://" + getConnectionDetails().getHost() + ":" + getConnectionDetails().getPort() + "/" + getConnectionDetails().getRootNamespace();

    try {
      rootLdapContext = (DirContext)initLdapContext.lookup(rootUrl);
    }
    catch (NamingException originalNamingException) {
      throw (NamingException)new NamingException(rootUrl).initCause(originalNamingException);
    }

    return rootLdapContext;
  }
}
