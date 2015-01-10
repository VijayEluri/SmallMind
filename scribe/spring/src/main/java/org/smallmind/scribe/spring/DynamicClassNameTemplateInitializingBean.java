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
package org.smallmind.scribe.spring;

import java.util.HashMap;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.ClassNameTemplate;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerException;
import org.springframework.beans.factory.InitializingBean;

public class DynamicClassNameTemplateInitializingBean implements InitializingBean {

  private Filter[] filters = new Filter[0];
  private Appender[] appenders = new Appender[0];
  private Enhancer[] enhancers = new Enhancer[0];
  private boolean autoFillLogicalContext = false;

  public void setFilters (Filter[] filters) {

    this.filters = filters;
  }

  public void setAppenders (Appender[] appenders) {

    this.appenders = appenders;
  }

  public void setEnhancers (Enhancer[] enhancers) {

    this.enhancers = enhancers;
  }

  public void setAutoFillLogicalContext (boolean autoFillLogicalContext) {

    this.autoFillLogicalContext = autoFillLogicalContext;
  }

  @Override
  public void afterPropertiesSet () throws Exception {

    HashMap<String, String> templateMap = new HashMap<>();
    HashMap<String, String> levelMap = new HashMap<>();

    SpringPropertyAccessor springPropertyAccessor = SpringPropertyAccessorManager.getSpringPropertyAccessor();

    for (String propertyKey : springPropertyAccessor.getKeySet()) {
      if (propertyKey.startsWith("log.template.")) {
        templateMap.put(propertyKey.substring("log.template.".length()), springPropertyAccessor.asString(propertyKey));
      }
      if (propertyKey.startsWith("log.level.")) {
        levelMap.put(propertyKey.substring("log.level.".length()), springPropertyAccessor.asString(propertyKey));
      }
    }

    for (String logKey : templateMap.keySet()) {

      ClassNameTemplate classNameTemplate;
      String levelValue;

      if ((levelValue = levelMap.get(logKey)) == null) {
        throw new LoggerException("Undefined level for log key(%s)", logKey);
      }

      classNameTemplate = new ClassNameTemplate(filters, appenders, enhancers, Level.valueOf(levelValue), autoFillLogicalContext, templateMap.get(logKey));
      classNameTemplate.register();
    }
  }
}