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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

public class ProxyUtility {

   private static final HashMap<String, Method> METHOD_MAP = new HashMap<String, Method>();
   private static final HashMap<String, Class> SIGNATURE_MAP = new HashMap<String, Class>();

   public static Object invoke (Object proxy, InvocationHandler invocationHandler, boolean isSubclass, String methodCode, String methodName, String resultSignature, String[] signatures, Object... args)
      throws Throwable {

      Method proxyMethod;

      if ((proxyMethod = METHOD_MAP.get(methodCode)) == null) {
         synchronized (METHOD_MAP) {
            if ((proxyMethod = METHOD_MAP.get(methodCode)) == null) {

               Class methodContainer = (isSubclass) ? proxy.getClass().getSuperclass() : proxy.getClass();

               METHOD_MAP.put(methodCode, proxyMethod = methodContainer.getMethod(methodName, assembleSignature(signatures)));
            }
         }
      }

      if (invocationHandler == null) {
         switch (resultSignature.charAt(0)) {
            case 'V':
               return null;
            case 'Z':
               return false;
            case 'B':
               return 0;
            case 'C':
               return (char)0;
            case 'S':
               return 0;
            case 'I':
               return 0;
            case 'J':
               return 0L;
            case 'F':
               return 0.0F;
            case 'D':
               return 0.0D;
            case 'L':
               return null;
            case '[':
               return null;
            default:
               throw new ByteCodeManipulationException("Unknown format for result signature(%s)", resultSignature);
         }
      }

      return invocationHandler.invoke(proxy, proxyMethod, args);
   }

   private static Class[] assembleSignature (String[] signatures) {

      Class[] parsedSignature;
      LinkedList<Class> parsedList;

      parsedList = new LinkedList<Class>();
      for (String signature : signatures) {
         switch (signature.charAt(0)) {
            case 'Z':
               parsedList.add(boolean.class);
            case 'B':
               parsedList.add(byte.class);
            case 'C':
               parsedList.add(char.class);
            case 'S':
               parsedList.add(short.class);
            case 'I':
               parsedList.add(int.class);
            case 'J':
               parsedList.add(long.class);
            case 'F':
               parsedList.add(float.class);
            case 'D':
               parsedList.add(double.class);
            case 'L':
               parsedList.add(getObjectType(signature.substring(1, signature.length() - 1).replace('/', '.')));
               break;
            case '[':
               parsedList.add(getObjectType(signature.replace('/', '.')));
               break;
            default:
               throw new ByteCodeManipulationException("Unknown format for parameter signature(%s)", signature);
         }
      }

      parsedSignature = new Class[parsedList.size()];
      parsedList.toArray(parsedSignature);

      return parsedSignature;
   }

   private static Class getObjectType (String type) {

      Class objectType;

      if ((objectType = SIGNATURE_MAP.get(type)) == null) {
         synchronized (SIGNATURE_MAP) {
            if ((objectType = SIGNATURE_MAP.get(type)) == null) {
               try {
                  SIGNATURE_MAP.put(type, objectType = Class.forName(type));
               }
               catch (ClassNotFoundException classNotFoundException) {
                  throw new ByteCodeManipulationException(classNotFoundException);
               }
            }
         }
      }

      return objectType;
   }
}
