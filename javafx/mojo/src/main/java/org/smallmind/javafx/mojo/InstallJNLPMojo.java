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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.mojo;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

// Installs A Webstart Javafx-based Project
@Mojo(name = "install-jnlp", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class InstallJNLPMojo extends AbstractMojo {

  @Component
  ArtifactFactory artifactFactory;
  @Component
  ArtifactInstaller artifactInstaller;
  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter(readonly = true)
  private ArtifactRepository localRepository;
  @Parameter(property = "project.artifactId")
  private String artifactName;
  @Parameter(defaultValue = "true")
  private boolean engaged;

  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    if (engaged) {

      Artifact applicationArtifact;
      StringBuilder pathBuilder;

      applicationArtifact = artifactFactory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), "jar", (project.getArtifact().getClassifier() == null) ? "jnlp" : project.getArtifact().getClassifier() + "-jnlp");
      pathBuilder = new StringBuilder(project.getBuild().getDirectory()).append(System.getProperty("file.separator")).append(artifactName).append('-').append(project.getVersion());

      if (project.getArtifact().getClassifier() != null) {
        pathBuilder.append('-');
        pathBuilder.append(project.getArtifact().getClassifier());
      }

      pathBuilder.append("-jnlp").append(".jar");

      try {
        artifactInstaller.install(new File(pathBuilder.toString()), applicationArtifact, localRepository);
      }
      catch (ArtifactInstallationException artifactInstallationException) {
        throw new MojoExecutionException("Unable to install the jnlp artifact(" + artifactName + ")", artifactInstallationException);
      }
    }
  }
}