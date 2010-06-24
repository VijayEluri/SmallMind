package org.smallmind.spark.wrapper.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import freemarker.template.Template;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.smallmind.nutsnbolts.io.FileIterator;

/**
 * @goal generate-os-wrapper
 * @phase package
 * @requiresDependencyResolution runtime
 * @description Generates Tanukisoft based os service wrappers
 */
public class OSWrapperMojo extends AbstractMojo {

  private static final String[] NOTHING = new String[0];
  private static final String RESOURCE_BASE_PATH = OSWrapperMojo.class.getPackage().getName().replace('.', '/');

  /**
   * @parameter expression="${project}"
   * @readonly
   */
  private MavenProject project;

  /**
   * @parameter
   */
  private File licenseFile;

  /**
   * @parameter
   */
  private Dependency[] dependencies;

  /**
   * @parameter default-value="application"
   */
  private String applicationDir;

  /**
   * @parameter
   * @required
   */
  private String operatingSystem;

  /**
   * @parameter
   * @required
   */
  private String wrapperListener;

  /**
   * @parameter expression="${project.artifactId}"
   */
  private String applicationName;

  /**
   * @parameter expression="${project.name}"
   */
  private String applicationLongName;

  /**
   * @parameter expression="${project.description}"
   */
  private String applicationDescription;

  /**
   * @parameter
   */
  private String[] jvmArgs;

  /**
   * @parameter default-value=0
   */
  private int jvmInitMemoryMB;

  /**
   * @parameter default-value=0
   */
  private int jvmMaxMemoryMB;

  /**
   * @parameter default-value="java"
   */
  private String javaCommand;

  /**
   * @parameter
   */
  private String[] appParameters;

  /**
   * @parameter
   */
  private String[] serviceDependencies;

  /**
   * @parameter
   */
  private String[] configurations;

  /**
   * @parameter default-value=true
   */
  private boolean createJar;

  /**
   * @parameter default-value=false
   */
  private boolean verbose;

  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    File binDirectory;
    File libDirectory;
    File confDirectory;
    OSType osType;
    HashMap<String, Object> freemarkerMap;
    LinkedList<String> classpathElementList;
    List<Dependency> additionalDependencies;
    Iterator<Dependency> aditionalDependencyIter;

    try {
      osType = OSType.valueOf(operatingSystem.replace('-', '_').toUpperCase());
    }
    catch (Throwable throwable) {
      throw new MojoExecutionException(String.format("Unknown operating system type(%s) - valid choices are %s", operatingSystem, Arrays.toString(OSType.values())), throwable);
    }

    createDirectory("bin", binDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir + System.getProperty("file.separator") + "bin"));
    createDirectory("lib", libDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir + System.getProperty("file.separator") + "lib"));
    createDirectory("conf", confDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir + System.getProperty("file.separator") + "conf"));

    if (licenseFile != null) {
      try {
        if (verbose) {
          getLog().info(String.format("Copying license file(%s)...", licenseFile.getAbsolutePath()));
        }

        copyToDestination(new FileInputStream(licenseFile), confDirectory.getAbsolutePath(), licenseFile.getName());
      }
      catch (IOException ioException) {
        throw new MojoExecutionException("Problem in copying your license file into the application conf directory", ioException);
      }
    }

    if (configurations != null) {
      for (String configuration : configurations) {

        File configurationFile = new File(configuration);

        try {
          if (verbose) {
            getLog().info(String.format("Copying configuration(%s)...", configurationFile.getName()));
          }

          copyToDestination(configurationFile, confDirectory.getAbsolutePath(), configurationFile.getName());
        }
        catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in copying the configuration(%s) into the application conf directory", configurationFile.getAbsolutePath()), ioException);
        }
      }
    }

    freemarkerMap = new HashMap<String, Object>();
    freemarkerMap.put("applicationName", applicationName);
    freemarkerMap.put("applicationLongName", applicationLongName);
    freemarkerMap.put("applicationDescription", (applicationDescription != null) ? applicationDescription : String.format("%s generated project", OSWrapperMojo.class.getSimpleName()));
    freemarkerMap.put("javaCommand", javaCommand);
    freemarkerMap.put("wrapperListener", wrapperListener);
    freemarkerMap.put("jvmArgs", (jvmArgs != null) ? jvmArgs : NOTHING);

    if (jvmInitMemoryMB > 0) {
      freemarkerMap.put("jvmInitMemoryMB", jvmInitMemoryMB);
    }

    if (jvmMaxMemoryMB > 0) {
      freemarkerMap.put("jvmMaxMemoryMB", jvmMaxMemoryMB);
    }

    if (appParameters == null) {
      freemarkerMap.put("appParameters", new String[] {wrapperListener});
    }
    else {

      String[] modifiedAppParameters = new String[appParameters.length + 1];

      modifiedAppParameters[0] = wrapperListener;
      System.arraycopy(appParameters, 0, modifiedAppParameters, 1, appParameters.length);
      freemarkerMap.put("appParameters", modifiedAppParameters);
    }

    freemarkerMap.put("serviceDependencies", (serviceDependencies != null) ? serviceDependencies : NOTHING);

    classpathElementList = new LinkedList<String>();
    freemarkerMap.put("classpathElements", classpathElementList);

    additionalDependencies = (dependencies != null) ? Arrays.asList(dependencies) : null;
    for (Object artifact : project.getRuntimeArtifacts()) {
      try {
        if (verbose) {
          getLog().info(String.format("Copying dependency(%s)...", ((org.apache.maven.artifact.Artifact)artifact).getFile().getName()));
        }

        if (additionalDependencies != null) {
          aditionalDependencyIter = additionalDependencies.iterator();
          while (aditionalDependencyIter.hasNext()) {
            if (aditionalDependencyIter.next().matchesArtifact((Artifact)artifact)) {
              aditionalDependencyIter.remove();
            }
          }
        }

        classpathElementList.add(((Artifact)artifact).getFile().getName());
        copyToDestination(((Artifact)artifact).getFile(), libDirectory.getAbsolutePath(), ((Artifact)artifact).getFile().getName());
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the application library", artifact), ioException);
      }
    }

    if (additionalDependencies != null) {
      for (Dependency dependency : additionalDependencies) {
        for (Object artifact : project.getDependencyArtifacts()) {
          if (dependency.matchesArtifact(((Artifact)artifact))) {
            try {
              if (verbose) {
                getLog().info(String.format("Copying additional dependency(%s)...", ((org.apache.maven.artifact.Artifact)artifact).getFile().getName()));
              }

              classpathElementList.add(((Artifact)artifact).getFile().getName());
              copyToDestination(((Artifact)artifact).getFile(), libDirectory.getAbsolutePath(), ((Artifact)artifact).getFile().getName());
            }
            catch (IOException ioException) {
              throw new MojoExecutionException(String.format("Problem in copying an additional dependency(%s) into the application library", artifact), ioException);
            }
          }
        }
      }
    }

    if (!project.getArtifact().getType().equals("jar")) {

      File jarFile;

      jarFile = new File(createJarArtifactName(project.getBuild().getDirectory(), false));

      try {
        if (verbose) {
          getLog().info(String.format("Creating and copying output jar(%s)...", jarFile.getName()));
        }

        createJar(jarFile, new File(project.getBuild().getOutputDirectory()));
        classpathElementList.add(jarFile.getName());
        copyToDestination(jarFile, libDirectory.getAbsolutePath(), jarFile.getName());
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in creating or copying the output jar(%s) into the application library", jarFile.getName()), ioException);
      }
    }
    else {
      try {
        if (verbose) {
          getLog().info(String.format("Copying build artifact(%s)...", project.getArtifact().getFile().getName()));
        }

        classpathElementList.add(project.getArtifact().getFile().getName());
        copyToDestination(project.getArtifact().getFile(), libDirectory.getAbsolutePath(), project.getArtifact().getFile().getName());
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying the build artifact(%s) into the application library", project.getArtifact()), ioException);
      }
    }

    try {
      if (verbose) {
        getLog().info(String.format("Copying wrapper library(%s)...", osType.getLibrary()));
      }

      copyToDestination(OSWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("lib", osType.getLibrary())), libDirectory.getAbsolutePath(), osType.getLibrary());
      copyToDestination(OSWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("lib", osType.getLibrary())), libDirectory.getAbsolutePath(), osType.getOsStyle().getLibrary());
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in copying the wrapper library(%s) into the application library", osType.getLibrary()), ioException);
    }

    try {
      if (verbose) {
        getLog().info(String.format("Copying wrapper executable(%s)...", osType.getExecutable()));
      }

      copyToDestination(OSWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", osType.getExecutable())), binDirectory.getAbsolutePath(), osType.getExecutable());
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in copying the wrapper executable(%s) into the application binaries", osType.getExecutable()), ioException);
    }

    try {
      if (verbose) {
        getLog().info("Copying wrapper scripts...");
      }

      switch (osType.getOsStyle()) {
        case UNIX:
          processFreemarkerTemplate(getWrapperFilePath("bin", "freemarker.sh.script.in"), binDirectory, applicationName, freemarkerMap);
          break;
        case WINDOWS:
          copyToDestination(OSWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", "App.bat.in")), binDirectory.getAbsolutePath(), applicationName + ".bat");
          copyToDestination(OSWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", "InstallApp-NT.bat.in")), binDirectory.getAbsolutePath(), "Install" + applicationName + "-NT.bat");
          copyToDestination(OSWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", "UninstallApp-NT.bat.in")), binDirectory.getAbsolutePath(), "Uninstall" + applicationName + "-NT.bat");
          break;
        default:
          throw new MojoExecutionException(String.format("Unknown os style(%s)", osType.getOsStyle().name()));
      }
    }
    catch (IOException ioException) {
      throw new MojoExecutionException("Problem in copying the wrapper scripts into the application binaries", ioException);
    }

    if (verbose) {
      getLog().info("Processing the configuration template...");
    }

    processFreemarkerTemplate(getWrapperFilePath("conf", "freemarker.wrapper.conf.in"), confDirectory, "wrapper.conf", freemarkerMap);

    if (createJar) {

      File jarFile;

      jarFile = new File(createJarArtifactName(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir, true));

      try {
        if (verbose) {
          getLog().info(String.format("Creating application jar(%s)...", jarFile.getName()));
        }

        createJar(jarFile, new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir));
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in creating the application jar(%s)", jarFile.getName()), ioException);
      }
    }
  }

  private String createJarArtifactName (String outputPath, boolean applicationArtifact) {

    StringBuilder nameBuilder;

    nameBuilder = new StringBuilder(outputPath);
    nameBuilder.append(System.getProperty("file.separator"));
    nameBuilder.append(applicationName);
    nameBuilder.append('-');
    nameBuilder.append(project.getVersion());

    if (project.getArtifact().getClassifier() != null) {
      nameBuilder.append('-');
      nameBuilder.append(project.getArtifact().getClassifier());
    }

    if (applicationArtifact) {
      nameBuilder.append("-app");
    }

    nameBuilder.append(".jar");

    return nameBuilder.toString();
  }

  private void createJar (File jarFile, File directoryToJar)
    throws IOException {

    FileOutputStream fileOutputStream;
    JarOutputStream jarOutputStream;
    JarEntry jarEntry;

    fileOutputStream = new FileOutputStream(jarFile);
    jarOutputStream = new JarOutputStream(fileOutputStream, new Manifest());
    for (File outputFile : new FileIterator(directoryToJar)) {
      if (!outputFile.equals(jarFile)) {
        jarEntry = new JarEntry(outputFile.getCanonicalPath().substring(directoryToJar.getAbsolutePath().length() + 1).replace(System.getProperty("file.separator"), "/"));
        jarEntry.setTime(outputFile.lastModified());
        jarOutputStream.putNextEntry(jarEntry);
        squeezeFile(jarOutputStream, outputFile);
      }
    }
    jarOutputStream.close();
    fileOutputStream.close();
  }

  private void squeezeFile (JarOutputStream jarOutputStream, File outputFile)
    throws IOException {

    FileInputStream inputStream;
    byte[] buffer = new byte[8192];
    int bytesRead;

    inputStream = new FileInputStream(outputFile);
    while ((bytesRead = inputStream.read(buffer)) >= 0) {
      jarOutputStream.write(buffer, 0, bytesRead);
    }
    inputStream.close();
  }

  private void processFreemarkerTemplate (String templatePath, File outputDir, String destinationName, HashMap<String, Object> interpolationMap)
    throws MojoExecutionException {

    freemarker.template.Configuration freemarkerConf;
    Template freemarkerTemplate;
    FileWriter fileWriter;

    freemarkerConf = new freemarker.template.Configuration();
    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    freemarkerConf.setTemplateLoader(new ClassPathTemplateLoader(OSWrapperMojo.class));

    try {
      freemarkerTemplate = freemarkerConf.getTemplate(templatePath);
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Unable to load template(%s) for translation", destinationName), ioException);
    }

    try {
      fileWriter = new FileWriter(outputDir.getAbsolutePath() + System.getProperty("file.separator") + destinationName);
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in creating a writer for the template(%s) file", destinationName), ioException);
    }

    try {
      freemarkerTemplate.process(interpolationMap, fileWriter);
    }
    catch (Exception exception) {
      throw new MojoExecutionException(String.format("Problem in processing the template(%s)", destinationName), exception);
    }

    try {
      fileWriter.close();
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in closing the template(%s) writer", destinationName), ioException);
    }
  }

  private void createDirectory (String dirType, File dirFile)
    throws MojoExecutionException {

    if (!dirFile.isDirectory()) {
      if (!dirFile.mkdirs()) {
        throw new MojoExecutionException(String.format("Unable to create the '%s' application directory(%s)", dirType, dirFile.getAbsolutePath()));
      }
    }
  }

  private String getWrapperFilePath (String dirType, String fileName) {

    StringBuilder pathBuilder;

    pathBuilder = new StringBuilder(RESOURCE_BASE_PATH);
    pathBuilder.append('/');
    pathBuilder.append(dirType);
    pathBuilder.append('/');
    pathBuilder.append(fileName);

    return pathBuilder.toString();
  }

  public void copyToDestination (File file, String destinationPath, String destinationName)
    throws IOException {

    FileInputStream inputStream;
    FileOutputStream outputStream;
    FileChannel readChannel;
    FileChannel writeChannel;
    long bytesTransferred;
    long currentPosition = 0;

    readChannel = (inputStream = new FileInputStream(file)).getChannel();
    writeChannel = (outputStream = new FileOutputStream(destinationPath + System.getProperty("file.separator") + destinationName)).getChannel();
    while ((currentPosition < readChannel.size()) && (bytesTransferred = readChannel.transferTo(currentPosition, 8192, writeChannel)) >= 0) {
      currentPosition += bytesTransferred;
    }
    outputStream.close();
    inputStream.close();
  }

  public void copyToDestination (InputStream inputStream, String destinationPath, String destinationName)
    throws IOException {

    FileOutputStream outputStream;
    byte[] buffer = new byte[8192];
    int bytesRead;

    outputStream = new FileOutputStream(destinationPath + System.getProperty("file.separator") + destinationName);
    while ((bytesRead = inputStream.read(buffer)) >= 0) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.close();
    inputStream.close();
  }
}