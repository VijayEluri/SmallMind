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
package org.smallmind.wrench.deploy;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class UpstartDecorator implements Decorator {

  private static final Template UPSTART_TEMPLATE;

  public void decorate (OperatingSystem operatingSystem, String appUser, File installDir, String nexusHost, String nexusUser, String nexusPassword, Repository repository, String groupId, String artifactId, String version, String classifier, String extension, String... envVars)
    throws IOException, TemplateException {

    if (operatingSystem.equals(OperatingSystem.LINUX)) {

      HashMap<String, Object> interpolationMap = new HashMap<>();

      interpolationMap.put("artifactId", artifactId);
      interpolationMap.put("applicationName", artifactId.toUpperCase());
      interpolationMap.put("installDir", installDir.getAbsolutePath());
      interpolationMap.put("batchExtension", operatingSystem.getBatchExtension());
      interpolationMap.put("envVars", envVars);

      UPSTART_TEMPLATE.process(interpolationMap, Files.newBufferedWriter(installDir.toPath().resolve(artifactId).resolve("bin").resolve(artifactId + ".install"), Charset.forName("UTF-8"), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }
  }

  static {

    Configuration freemarkerConf = new Configuration();

    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);

    try {
      UPSTART_TEMPLATE = new Template("upstart template", new InputStreamReader(UpstartDecorator.class.getClassLoader().getResourceAsStream("org/smallmind/wrench/deploy/upstart-install.freemarker.in")), freemarkerConf);
    } catch (IOException ioException) {
      throw new StaticInitializationError(ioException);
    }
  }
}
