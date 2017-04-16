/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jaxws;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.jws.WebService;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

abstract class AbstractWsGenMojo extends AbstractJaxwsMojo {

  /**
   * Specify that a WSDL file should be generated in <code>${resourceDestDir}</code>.
   */
  @Parameter(defaultValue = "false")
  protected boolean genWsdl;

  /**
   * Service endpoint implementation class name.
   */
  @Parameter
  private String sei;

  /**
   * Used in conjunction with <code>genWsdl<code> to specify the protocol to use in the
   * <code>wsdl:binding</code>. Valid values are "<code>soap1.1</code>" or "<code>Xsoap1.2</code>",
   * default is "<code>soap1.1</code>". "<code>Xsoap1.2</code>" is not standard
   * and can only be used in conjunction with the <code>extension</code> option.
   */
  @Parameter
  private String protocol;

  /**
   * Specify the Service name to use in the generated WSDL.
   * Used in conjunction with the <code>genWsdl</code> option.
   */
  @Parameter
  private String servicename;

  /**
   * Specify the Port name to use in the generated WSDL.
   * Used in conjunction with the <code>genWsdl</code> option.
   */
  @Parameter
  private String portname;

  /**
   * Inline schemas in the generated WSDL.
   * Used in conjunction with the <code>genWsdl</code> option.
   */
  @Parameter(defaultValue = "false")
  private boolean inlineSchemas;

  /**
   * Turn off compilation after code generation and let generated sources be
   * compiled by maven during compilation phase; keep is turned on with this option.
   */
  @Parameter(defaultValue = "false")
  private boolean xnocompile;

  /**
   */
  @Parameter(defaultValue = "false")
  private boolean xdonotoverwrite;

  /**
   * Metadata file for wsgen. See <a href="https://jax-ws.java.net/2.2.8/docs/ch03.html#users-guide-external-metadata">the JAX-WS Guide</a>
   * for the description of this feature.
   * Unmatched files will be ignored.
   *
   * @see <a href="https://jax-ws.java.net/2.2.8/docs/ch03.html#users-guide-external-metadata">External Web Service Metadata</a>
   * @since 2.3
   */
  @Parameter
  private File metadata;

  protected abstract File getResourceDestDir ();

  protected abstract File getClassesDir ();

  @Override
  public void execute () throws MojoExecutionException, MojoFailureException {

    Set<String> seis = new HashSet<String>();
    if (sei != null) {
      seis.add(sei);
    }
    else {
      //find all SEIs within current classes
      seis.addAll(getSEIs(getClassesDir()));
    }
    if (seis.isEmpty()) {
      throw new MojoFailureException("No @javax.jws.WebService found.");
    }
    for (String aSei : seis) {
      processSei(aSei);
    }
  }

  protected void processSei (String sei) throws MojoExecutionException {

    getLog().info("Processing: " + sei);
    ArrayList<String> args = getWsGenArgs(sei);
    getLog().info("jaxws:wsgen args: " + args);
    exec(args);
    if (metadata != null) {
      try {
        FileUtils.copyFileToDirectory(metadata, getClassesDir());
      }
      catch (IOException ioe) {
        throw new MojoExecutionException(ioe.getMessage(), ioe);
      }
    }
  }

  @Override
  protected String getMain () {

    return "com.sun.tools.ws.wscompile.WsgenTool";
  }

  @Override
  @SuppressWarnings("unchecked")
  protected String getExtraClasspath () {

    StringBuilder buf = new StringBuilder();
    buf.append(getClassesDir().getAbsolutePath());
    for (Artifact a : (Set<Artifact>)project.getArtifacts()) {
      buf.append(File.pathSeparatorChar);
      buf.append(a.getFile().getAbsolutePath());
    }
    return buf.toString();
  }

  @Override
  protected boolean getXnocompile () {

    return xnocompile;
  }

  /**
   * Construct wsgen arguments
   *
   * @return a list of arguments
   * @throws MojoExecutionException
   */
  private ArrayList<String> getWsGenArgs (String aSei) throws MojoExecutionException {

    ArrayList<String> args = new ArrayList<String>();
    args.addAll(getCommonArgs());

    if (this.genWsdl) {
      if (this.protocol != null) {
        args.add("-wsdl:" + this.protocol);
      }
      else {
        args.add("-wsdl");
      }

      if (inlineSchemas) {
        maybeUnsupportedOption("-inlineSchemas", null, args);
      }

      if (servicename != null) {
        args.add("-servicename");
        args.add(servicename);
      }

      if (portname != null) {
        args.add("-portname");
        args.add(portname);
      }

      File resourceDir = getResourceDestDir();
      if (!resourceDir.mkdirs() && !resourceDir.exists()) {
        getLog().warn("Cannot create directory: " + resourceDir.getAbsolutePath());
      }
      args.add("-r");
      args.add("'" + resourceDir.getAbsolutePath() + "'");
      if (!"war".equals(project.getPackaging())) {
        Resource r = new Resource();
        r.setDirectory(getRelativePath(project.getBasedir(), getResourceDestDir()));
        project.addResource(r);
      }
    }

    if (xdonotoverwrite) {
      args.add("-Xdonotoverwrite");
    }

    if (metadata != null && isArgSupported("-x")) {
      maybeUnsupportedOption("-x", "'" + metadata.getAbsolutePath() + "'", args);
    }

    args.add(aSei);

    getLog().debug("jaxws:wsgen args: " + args);

    return args;
  }

  private String getRelativePath (File root, File f) {

    return root.toURI().relativize(f.toURI()).getPath();
  }

  private Set<String> getSEIs (File directory) throws MojoExecutionException {

    Set<String> seis = new HashSet<String>();
    if (!directory.exists() || directory.isFile()) {
      return seis;
    }
    ClassLoader cl = null;
    try {
      cl = new URLClassLoader(new URL[] {directory.toURI().toURL()});
      for (Object s : FileUtils.getFileAndDirectoryNames(directory, "**/*.class", null, false, true, true, false)) {
        try {
          String clsName = ((String)s).replace(File.separator, ".");
          Class<?> c = cl.loadClass(clsName.substring(0, clsName.length() - 6));
          WebService ann = c.getAnnotation(WebService.class);
          if (!c.isInterface() && ann != null) {
            //more sophisticated checks are done by wsgen itself
            seis.add(c.getName());
          }
        }
        catch (ClassNotFoundException ex) {
          throw new MojoExecutionException(ex.getMessage(), ex);
        }
      }
    }
    catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
    finally {
      if (cl != null && cl instanceof Closeable) {
        try {
          ((Closeable)cl).close();
        }
        catch (IOException ex) {
          //ignore
        }
      }
    }
    return seis;
  }
}