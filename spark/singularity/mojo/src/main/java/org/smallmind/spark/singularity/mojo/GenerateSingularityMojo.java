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
package org.smallmind.spark.singularity.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

// Generates Singularity based one jar applications
@Mojo(name = "generate-singularity", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateSingularityMojo extends AbstractMojo {

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter(defaultValue = "singularity")
  private String singularityBuildDir;
  @Parameter
  private String mainClass;
  @Parameter(defaultValue = "false")
  private boolean verbose;

  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    Path buildPath;

    try {
      Files.createDirectories(buildPath = Paths.get(project.getBuild().getDirectory(), singularityBuildDir));
    } catch (IOException ioException) {
      throw new MojoExecutionException("Unable to create a build directory", ioException);
    }

    for (Artifact artifact : project.getRuntimeArtifacts()) {
      try {
        if (verbose) {
          getLog().info(String.format("Copying dependency(%s)...", artifact.getFile().getName()));
        }

        copyToDestination(artifact.getFile(), buildPath.resolve(artifact.getFile().getName()));
      } catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the application library", artifact), ioException);
      }
    }

    try {
      Files.walkFileTree(Paths.get(project.getBuild().getDirectory(), "classes"), new CopyFileVisitor(buildPath));
    } catch (IOException ioException) {
      throw new MojoExecutionException("Unable to copy the classes directory into the build path", ioException);
    }
  }

  public void copyToDestination (File file, Path destinationPath)
    throws IOException {

    FileInputStream inputStream;
    FileOutputStream outputStream;
    FileChannel readChannel;
    FileChannel writeChannel;
    long bytesTransferred;
    long currentPosition = 0;

    readChannel = (inputStream = new FileInputStream(file)).getChannel();
    writeChannel = (outputStream = new FileOutputStream(destinationPath.toFile())).getChannel();
    while ((currentPosition < readChannel.size()) && (bytesTransferred = readChannel.transferTo(currentPosition, 8192, writeChannel)) >= 0) {
      currentPosition += bytesTransferred;
    }
    outputStream.close();
    inputStream.close();
  }

  public class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path targetPath;
    private Path sourcePath;

    public CopyFileVisitor (Path targetPath) {

      this.targetPath = targetPath;
    }

    @Override
    public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs)
      throws IOException {

      if (sourcePath == null) {
        sourcePath = dir;
      }

      Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile (final Path file, final BasicFileAttributes attrs)
      throws IOException {

      Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));

      return FileVisitResult.CONTINUE;
    }
  }
}
