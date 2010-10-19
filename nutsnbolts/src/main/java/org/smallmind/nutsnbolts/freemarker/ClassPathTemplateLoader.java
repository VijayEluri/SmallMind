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
package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import freemarker.cache.TemplateLoader;

public class ClassPathTemplateLoader implements TemplateLoader {

   ClassLoader classLoader;
   private Class<?> anchorClass;

   public ClassPathTemplateLoader () {

      classLoader = Thread.currentThread().getContextClassLoader();
   }

   public ClassPathTemplateLoader (Class<?> anchorClass) {

      this.anchorClass = anchorClass;

      classLoader = anchorClass.getClassLoader();
   }

   public Class<?> getAnchorClass () {

      return anchorClass;
   }

   public ClassLoader getClassLoader () {

      return classLoader;
   }

   public Object findTemplateSource (String name)
      throws IOException {

      ClassPathTemplateSource source;

      if (name.startsWith("/")) {
         source = new ClassPathTemplateSource(classLoader, name);
      }
      else if (anchorClass != null) {

         StringBuilder pathBuilder = new StringBuilder(anchorClass.getPackage().getName().replace('.', '/'));

         pathBuilder.append('/').append(name);
         source = new ClassPathTemplateSource(classLoader, pathBuilder.toString());
      }
      else {
         source = new ClassPathTemplateSource(classLoader, name);
      }

      return (source.exists()) ? source : null;
   }

   public long getLastModified (Object templateSource) {

      return -1;
   }

   public Reader getReader (Object templateSource, String encoding)
      throws IOException {

      return new InputStreamReader(((ClassPathTemplateSource)templateSource).getInputStream(), encoding);
   }

   public void closeTemplateSource (Object templateSource)
      throws IOException {

      ((ClassPathTemplateSource)templateSource).close();
   }
}
