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
package org.smallmind.nutsnbolts.resource;

import java.lang.reflect.Constructor;
import java.util.LinkedList;

public class ResourceTypeFactory implements ResourceFactory {

   private static final Class[] SIGNATURE = new Class[] {String.class};

   private static ResourceSchemes VALID_SCHEMES;

   static {

      String[] schemes;
      LinkedList<String> schemeList;

      schemeList = new LinkedList<String>();
      for (ResourceType resourceType : ResourceType.values()) {
         schemeList.add(resourceType.getResourceScheme());
      }

      schemes = new String[schemeList.size()];
      schemeList.toArray(schemes);
      VALID_SCHEMES = new ResourceSchemes(schemes);
   }

   public ResourceSchemes getValidSchemes () {

      return VALID_SCHEMES;
   }

   public Resource createResource (String scheme, String path)
      throws ResourceException {

      Constructor<? extends Resource> resourceConstructor;

      for (ResourceType resourceType : ResourceType.values()) {
         if (resourceType.getResourceScheme().equals(scheme)) {
            try {
               resourceConstructor = resourceType.getResourceClass().getConstructor(SIGNATURE);
               return resourceConstructor.newInstance(path);
            }
            catch (Exception exception) {
               throw new ResourceException(exception);
            }
         }
      }

      throw new ResourceException("This factory does not handle the references scheme(%s)", scheme);
   }
}
