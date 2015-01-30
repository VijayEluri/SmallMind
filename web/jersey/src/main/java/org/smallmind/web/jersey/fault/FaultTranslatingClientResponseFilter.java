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
package org.smallmind.web.jersey.fault;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import org.smallmind.web.jersey.util.JsonCodec;

public class FaultTranslatingClientResponseFilter implements ClientResponseFilter {

  @Override
  public void filter (ClientRequestContext requestContext, ClientResponseContext responseContext)
    throws IOException {

    if ((responseContext.getStatus() == 500) && MediaType.APPLICATION_JSON_TYPE.equals(responseContext.getMediaType()) && responseContext.hasEntity()) {

      Fault fault;
      NativeObject nativeObject;

      if (((nativeObject = (fault = JsonCodec.read(responseContext.getEntityStream(), Fault.class)).getNativeObject()) != null) && nativeObject.getLanguage().equals(NativeLanguage.JAVA)) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(nativeObject.getBytes()); ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
          try {
            throw new ResourceInvocationException((Exception)objectInputStream.readObject());
          } catch (ClassNotFoundException classNotFoundException) {
            throw new ObjectInstantiationException(classNotFoundException);
          }
        }
      }

      throw new FaultWrappingException(fault);
    }
  }
}

