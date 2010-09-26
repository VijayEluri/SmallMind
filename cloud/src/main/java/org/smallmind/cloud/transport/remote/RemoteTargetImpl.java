/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.cloud.transport.remote;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.cloud.transport.MethodInvoker;

public class RemoteTargetImpl extends UnicastRemoteObject implements RemoteTarget {

   private MethodInvoker methodInvoker;

   public RemoteTargetImpl (RemoteEndpoint remoteEndpoint, String registryName)
      throws NoSuchMethodException, MalformedURLException, RemoteException {

      Naming.rebind(registryName, this);
      methodInvoker = new MethodInvoker(remoteEndpoint, remoteEndpoint.getProxyInterfaces());
   }

   public Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception {

      return methodInvoker.remoteInvocation(invocationSignal);
   }
}
