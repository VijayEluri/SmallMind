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
package org.smallmind.web.grizzly;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.BindException;
import java.util.EnumSet;
import java.util.LinkedList;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import org.smallmind.web.jersey.jackson.JsonResourceConfig;
import org.smallmind.web.jersey.spring.ExposedApplicationContext;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.jaxws.JaxwsHandler;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.smallmind.nutsnbolts.lang.web.PerApplicationContextFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class GrizzlyInitializingBean implements DisposableBean, ApplicationContextAware, ApplicationListener, BeanPostProcessor {

  private static final Class[] NO_ARG_SIGNATURE = new Class[0];
  private HttpServer httpServer;
  private LinkedList<WebService> serviceList = new LinkedList<>();
  private LinkedList<ServletInstaller> servletInstallerList = new LinkedList<>();
  private String host;
  private String contextPath;
  private String restPath;
  private String soapPath;
  private int port;
  private boolean debug = false;

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setContextPath (String contextPath) {

    this.contextPath = contextPath;
  }

  public void setRestPath (String restPath) {

    this.restPath = restPath;
  }

  public void setSoapPath (String soapPath) {

    this.soapPath = soapPath;
  }

  public void setDebug (boolean debug) {

    this.debug = debug;
  }

  @Override
  public synchronized void onApplicationEvent (ApplicationEvent event) {

    if (event instanceof ContextRefreshedEvent) {

      if (debug) {
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
      }
      httpServer = new HttpServer();
      httpServer.addListener(new NetworkListener("grizzly2", host, port));

      WebappContext webappContext = new WebappContext("Grizzly Application Context");
      webappContext.addServlet("JAX-RS Application", new ServletContainer(new JsonResourceConfig(ExposedApplicationContext.getApplicationContext()))).addMapping(restPath + "/*");
      webappContext.addFilter("per-application-data", new PerApplicationContextFilter()).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), restPath + "/*");
      webappContext.addListener("org.springframework.web.context.request.RequestContextListener");

      for (ServletInstaller servletInstaller : servletInstallerList) {
        try {

          Constructor<? extends Servlet> servletConstructor;
          Servlet servlet;
          String urlPattern;

          servletConstructor = servletInstaller.getServletClass().getConstructor(NO_ARG_SIGNATURE);
          servlet = servletConstructor.newInstance();

          webappContext.addServlet(servletInstaller.getDisplayName(), servlet).addMapping((urlPattern = servletInstaller.getUrlPattern()) == null ? "/" : urlPattern);
        }
        catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }

      webappContext.deploy(httpServer);

      for (WebService webService : serviceList) {

        HttpHandler httpHandler = new JaxwsHandler(webService.getService(), false);

        httpServer.getServerConfiguration().addHttpHandler(httpHandler, soapPath + webService.getPath());
      }

      try {
        httpServer.start();
      }
      catch (IOException ioException) {
        if (!(ioException instanceof BindException)) {
          throw new GrizzlyInitializationException(ioException);
        }
      }
    }
  }

  @Override
  public void setApplicationContext (ApplicationContext applicationContext) {

    ExposedApplicationContext.register(applicationContext);
  }

  @Override
  public Object postProcessBeforeInitialization (Object bean, String beanName) throws BeansException {

    return bean;
  }

  @Override
  public Object postProcessAfterInitialization (Object bean, String beanName) throws BeansException {

    ServicePath servicePath;
    if (bean instanceof ServletInstaller) {
      servletInstallerList.add((ServletInstaller)bean);
    }

    if ((servicePath = bean.getClass().getAnnotation(ServicePath.class)) != null) {
      serviceList.add(new WebService(servicePath.value(), bean));
    }

    return bean;
  }

  @Override
  public synchronized void destroy () {

    if (httpServer != null) {
      httpServer.shutdown();
    }
  }
}
