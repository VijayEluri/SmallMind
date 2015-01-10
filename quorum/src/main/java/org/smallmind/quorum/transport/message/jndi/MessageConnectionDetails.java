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
package org.smallmind.quorum.transport.message.jndi;

import javax.naming.Context;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class MessageConnectionDetails {

  private ComponentPool<Context> contextPool;
  private String destinationName;
  private String connectionFactoryName;
  private String userName;
  private String password;

  public MessageConnectionDetails (ComponentPool<Context> contextPool, String destinationName, String connectionFactoryName, String userName, String password) {

    this.contextPool = contextPool;
    this.destinationName = destinationName;
    this.connectionFactoryName = connectionFactoryName;
    this.userName = userName;
    this.password = password;
  }

  public ComponentPool<Context> getContextPool () {

    return contextPool;
  }

  public String getDestinationName () {

    return destinationName;
  }

  public String getConnectionFactoryName () {

    return connectionFactoryName;
  }

  public String getUserName () {

    return userName;
  }

  public String getPassword () {

    return password;
  }
}
