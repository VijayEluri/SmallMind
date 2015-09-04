/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.spark.singularity.boot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class SingularityClassGate {

  static {

    URL.setURLStreamHandlerFactory(new OneJarURLStreamHandlerFactory());
  }

  public static class OneJarURLConnection extends URLConnection {

    protected OneJarURLConnection (URL url) {

      super(url);
    }

    @Override
    public void connect ()
      throws IOException {
      // Do your job here. As of now it merely prints "Connected!".
      System.out.println("Connected!");
    }

    @Override
    public InputStream getInputStream ()
      throws IOException {

      return null;
    }

    @Override
    public int getContentLength () {

      return 0;
    }
  }

  public static class OneJarURLStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection (URL url)
      throws IOException {

      return new OneJarURLConnection(url);
    }
  }

  public static class OneJarURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler (String protocol) {

      if ("singularity".equals(protocol)) {
        return new OneJarURLStreamHandler();
      }

      return null;
    }
  }
}



