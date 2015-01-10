/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.web.jersey.spring;

import java.util.LinkedList;
import javax.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class HK2ResourceBeanPostProcessor implements BeanPostProcessor {

  private LinkedList<Object> unregisteredBeanList = new LinkedList<>();

  public synchronized void registerResourceConfig (ResourceConfig resourceConfig) {

    for (Object bean : unregisteredBeanList) {
      resourceConfig.register(bean);
    }
  }

  @Override
  public Object postProcessBeforeInitialization (Object bean, String beanName)
    throws BeansException {

    return bean;
  }

  @Override
  public synchronized Object postProcessAfterInitialization (Object bean, String beanName)
    throws BeansException {

    if (bean.getClass().getAnnotation(Path.class) != null) {
      unregisteredBeanList.add(bean);
    }

    return bean;
  }
}