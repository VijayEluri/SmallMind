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
package org.smallmind.cloud.cluster.protocol.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.smallmind.cloud.cluster.ClusterEndpoint;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManager;

public class SocketClusterManager implements ClusterManager<SocketClusterProtocolDetails> {

  private ClusterHub clusterHub;
  private SocketClusterHandle socketClusterHandle;
  private ClusterInterface<SocketClusterProtocolDetails> clusterInterface;

  public SocketClusterManager (ClusterHub clusterHub, ClusterInterface<SocketClusterProtocolDetails> clusterInterface) {

    this.clusterHub = clusterHub;
    this.clusterInterface = clusterInterface;

    socketClusterHandle = new SocketClusterHandle(this);
  }

  public ClusterInterface<SocketClusterProtocolDetails> getClusterInterface () {

    return clusterInterface;
  }

  public ClusterHandle getClusterHandle () {

    return socketClusterHandle;
  }

  public void updateClusterStatus (ClusterEndpoint<SocketClusterProtocolDetails> clusterEndpoint, int calibratedFreeCapacity) {

    clusterInterface.getClusterPivot().updateClusterStatus(clusterEndpoint, calibratedFreeCapacity);
  }

  public void removeClusterMember (ClusterEndpoint<SocketClusterProtocolDetails> clusterEndpoint) {

    clusterInterface.getClusterPivot().removeClusterMember(clusterEndpoint);
  }

  private int getServicePort (String instanceId)
    throws ClusterManagementException {

    return clusterInterface.getClusterProtocolDetails().getPortMapper().mapPort(instanceId);
  }

  public SocketChannel connect (Object[] parameters)
    throws ClusterManagementException {

    ClusterEndpoint clusterEndpoint = null;
    SocketChannel serviceSocketChannel;
    SocketAddress serviceSocketAddress;

    while (true) {
      if ((clusterEndpoint = clusterInterface.getClusterPivot().nextRequestAddress(parameters, clusterEndpoint)) == null) {
        throw new ClusterManagementException("No server is currently available for requests to %s (%s)", ClusterInterface.class.getSimpleName(), clusterInterface);
      }

      serviceSocketAddress = new InetSocketAddress(clusterEndpoint.getHostAddress(), getServicePort(clusterEndpoint.getClusterInstance().getInstanceId()));
      try {
        serviceSocketChannel = SocketChannel.open(serviceSocketAddress);
        return serviceSocketChannel;
      }
      catch (IOException ioException) {
        clusterHub.logError(ioException);
        removeClusterMember(clusterEndpoint);
      }
    }
  }

}
