package org.smallmind.license;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Pattern;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.JavaDocStencil;
import org.smallmind.license.stencil.Stencil;

/**
 * @goal generate-notice-headers
 * @phase process-sources
 * @description Generates and/or replaces notice headers in source files
 */
public class SourceNoticeMojo extends AbstractMojo {

   private static final Stencil[] DEFAULT_STENCILS = new Stencil[] {new JavaDocStencil()};

   private static enum NoticeState {

      FIRST, LAST, COMPLETED, TERMINATED
   }

   /**
    * @parameter expression="${project}"
    * @readonly
    */
   private MavenProject project;

   /**
    * @parameter
    */
   private Stencil[] stencils;

   /**
    * @parameter
    */
   private Rule[] rules;

   /**
    * @parameter default-value=false
    */
   private boolean allowNoticeRemoval;

   /**
    * @parameter default-value=true
    */
   private boolean includeResources;

   /**
    * @parameter default-value=false
    */
   private boolean includeTests;

   /**
    * @parameter default-value=false
    */
   private boolean verbose;

   //TODO: Excludes, Seek/Process Optimization

   @Override
   public void execute ()
      throws MojoExecutionException, MojoFailureException {

      MavenProject rootProject;
      Stencil[] mergedStencils;
      char[] buffer = new char[8192];

      rootProject = project;
      while (!rootProject.isExecutionRoot()) {
         rootProject = rootProject.getParent();
      }

      mergedStencils = new Stencil[(stencils != null) ? stencils.length + DEFAULT_STENCILS.length : DEFAULT_STENCILS.length];
      System.arraycopy(DEFAULT_STENCILS, 0, mergedStencils, 0, DEFAULT_STENCILS.length);
      if (stencils != null) {
         System.arraycopy(stencils, 0, mergedStencils, DEFAULT_STENCILS.length, stencils.length);
      }

      for (Rule rule : rules) {

         FileFilter[] fileFilters;
         String[] noticeArray;
         boolean stenciled;
         long noticeModTime;

         if (verbose) {
            getLog().info(String.format("Processing rule(%s)...", rule.getId()));
         }

         if (rule.getNotice() == null) {
            if (!allowNoticeRemoval) {
               throw new MojoExecutionException("No notice was provided for rule(" + rule.getId() + "), but notice removal has not been enabled");
            }

            noticeArray = null;
            noticeModTime = -1;
         }
         else {

            File noticeFile;

            noticeFile = new File(rule.getNotice());
            noticeArray = getFileAsLineArray(noticeFile.isAbsolute() ? noticeFile.getAbsolutePath() : rootProject.getBasedir() + System.getProperty("file.separator") + noticeFile.getPath());
            noticeModTime = noticeFile.lastModified();
         }

         if ((rule.getFileTypes() == null) || (rule.getFileTypes().length == 0)) {
            throw new MojoExecutionException("No file types were specified for rule(" + rule.getId() + ")");
         }

         fileFilters = new FileFilter[rule.getFileTypes().length];
         for (int count = 0; count < fileFilters.length; count++) {
            fileFilters[count] = new FileTypeFilenameFilter(rule.getFileTypes()[count]);
         }

         stenciled = false;
         for (Stencil stencil : mergedStencils) {
            if (stencil.getId().equals(rule.getStencilId())) {
               stenciled = true;

               updateNotice(stencil, noticeArray, noticeModTime, buffer, project.getBuild().getSourceDirectory(), fileFilters);
               updateNotice(stencil, noticeArray, noticeModTime, buffer, project.getBuild().getScriptSourceDirectory(), fileFilters);

               if (includeResources) {
                  for (Resource resource : project.getBuild().getResources()) {
                     updateNotice(stencil, noticeArray, noticeModTime, buffer, resource.getDirectory(), fileFilters);
                  }
               }

               if (includeTests) {
                  updateNotice(stencil, noticeArray, noticeModTime, buffer, project.getBuild().getTestSourceDirectory(), fileFilters);

                  if (includeResources) {
                     for (Resource testResource : project.getBuild().getTestResources()) {
                        updateNotice(stencil, noticeArray, noticeModTime, buffer, testResource.getDirectory(), fileFilters);
                     }
                  }
               }

               break;
            }
         }

         if (!stenciled) {
            throw new MojoExecutionException("No stencil found with id(" + rule.getStencilId() + ") for rule(" + rule.getId() + ")");
         }
      }
   }

   private void updateNotice (Stencil stencil, String[] noticeArray, long noticeModTime, char[] buffer, String directoryPath, FileFilter... fileFilters)
      throws MojoExecutionException {

      File tempFile;
      BufferedReader fileReader;
      FileWriter fileWriter;
      Pattern skipPattern = null;
      String unprocessedLine;
      int charsRead;

      if (stencil.getSkipLines() != null) {
         skipPattern = Pattern.compile(stencil.getSkipLines());
      }

      for (File licensedFile : new LicensedFileIterator(new File(directoryPath), fileFilters)) {
         if (verbose) {
            getLog().info(String.format(((noticeArray == null) ? "Removing" : "Updating") + " license notice for file(%s)...", licensedFile.getAbsolutePath()));
         }

         try {
            fileWriter = new FileWriter(tempFile = new File(licensedFile.getParent() + System.getProperty("file.separator") + "license.temp"));

            try {
               fileReader = new BufferedReader(new FileReader(licensedFile));

               unprocessedLine = seekNotice(stencil, skipPattern, fileReader, fileWriter);

               if (noticeArray != null) {
                  applyNotice(stencil, noticeArray, fileWriter);
               }

               if (unprocessedLine != null) {
                  fileWriter.write(unprocessedLine);
                  fileWriter.write(System.getProperty("line.separator"));
               }

               while ((charsRead = fileReader.read(buffer)) >= 0) {
                  fileWriter.write(buffer, 0, charsRead);
               }

               fileWriter.close();
               fileReader.close();

               if (!licensedFile.delete()) {
                  throw new MojoFailureException("Unable to delete file(" + licensedFile.getAbsolutePath() + ")");
               }
            }
            catch (Exception exception) {
               tempFile.delete();
               throw new MojoExecutionException("Exception during notice processing", exception);
            }

            if (!tempFile.renameTo(licensedFile)) {
               throw new MojoFailureException("Unable to rename temp file(" + tempFile.getAbsolutePath() + ") to processed file(" + licensedFile.getAbsolutePath() + ")");
            }
         }
         catch (MojoExecutionException mojoExecutionException) {
            throw mojoExecutionException;
         }
         catch (Exception exception) {
            throw new MojoExecutionException("Exception during notice processing", exception);
         }
      }
   }

   private String[] getFileAsLineArray (String noticePath)
      throws MojoExecutionException {

      BufferedReader noticeReader;
      LinkedList<String> lineList;
      String[] lineArray;
      String singleLine;

      try {
         noticeReader = new BufferedReader(new FileReader(noticePath));
         lineList = new LinkedList<String>();
         while ((singleLine = noticeReader.readLine()) != null) {
            lineList.add(singleLine);
         }
      }
      catch (IOException ioException) {
         throw new MojoExecutionException("Unable to acquire the notice file(" + noticePath + ")", ioException);
      }

      lineArray = new String[lineList.size()];
      lineList.toArray(lineArray);

      return lineArray;
   }

   private String seekNotice (Stencil stencil, Pattern skipPattern, BufferedReader fileReader, FileWriter fileWriter)
      throws IOException, MojoFailureException {

      NoticeState noticeState;
      String singleLine = null;

      noticeState = (stencil.getFirstLine() != null) ? NoticeState.FIRST : NoticeState.LAST;
      while ((!(noticeState.equals(NoticeState.COMPLETED) || noticeState.equals(NoticeState.TERMINATED))) && ((singleLine = fileReader.readLine()) != null)) {
         if ((skipPattern == null) || (!skipPattern.matcher(singleLine).matches())) {
            switch (noticeState) {
               case FIRST:
                  if (singleLine.length() > 0) {
                     noticeState = singleLine.equals(stencil.getFirstLine()) ? NoticeState.LAST : NoticeState.TERMINATED;
                  }
                  break;
               case LAST:
                  if ((stencil.getLastLine() != null) && singleLine.equals(stencil.getLastLine())) {
                     noticeState = NoticeState.COMPLETED;
                  }
                  else if ((singleLine.length() > 0) && (!singleLine.startsWith(stencil.getBeforeEachLine()))) {
                     noticeState = NoticeState.TERMINATED;
                  }
                  else if ((singleLine.length() == 0) && stencil.willPrefixBlankLines()) {
                     noticeState = NoticeState.TERMINATED;
                  }
                  break;
               default:
                  throw new MojoFailureException("Unknown or inappropriate notice seeking state(" + noticeState.name() + ")");
            }
         }
         else {
            fileWriter.write(singleLine);
            fileWriter.write(System.getProperty("line.separator"));
         }
      }

      if (noticeState.equals(NoticeState.COMPLETED) || ((singleLine != null) && (singleLine.length() == 0))) {
         do {
            singleLine = fileReader.readLine();
         } while ((singleLine != null) && (singleLine.length() == 0));
      }

      return singleLine;
   }

   private void applyNotice (Stencil stencil, String[] noticeArray, FileWriter fileWriter)
      throws IOException {

      for (int count = 0; count < stencil.getBlankLinesBefore(); count++) {
         fileWriter.write(System.getProperty("line.separator"));
      }

      if (stencil.getFirstLine() != null) {
         fileWriter.write(stencil.getFirstLine());
         fileWriter.write(System.getProperty("line.separator"));
      }

      for (String noticeLine : noticeArray) {
         if ((stencil.getBeforeEachLine() != null) && ((noticeLine.length() > 0) || stencil.willPrefixBlankLines())) {
            fileWriter.write(stencil.getBeforeEachLine());
         }
         fileWriter.write(noticeLine);
         fileWriter.write(System.getProperty("line.separator"));
      }

      if (stencil.getLastLine() != null) {
         fileWriter.write(stencil.getLastLine());
         fileWriter.write(System.getProperty("line.separator"));
      }

      for (int count = 0; count < stencil.getBlankLinesAfter(); count++) {
         fileWriter.write(System.getProperty("line.separator"));
      }
   }
}
