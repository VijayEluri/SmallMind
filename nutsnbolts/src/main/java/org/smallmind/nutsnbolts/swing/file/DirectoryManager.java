package org.smallmind.nutsnbolts.swing.file;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.swing.SmallMindScrollPane;
import org.smallmind.nutsnbolts.swing.dialog.DirectoryChooserDialog;

public class DirectoryManager extends JPanel {

   private static enum ParentType {

      FRAME, DIALOG
   }

   ;

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
   private static final Dimension PREFERRED_DIMENSION = new Dimension(300, 500);

   private static ImageIcon DIRECTORY_ADD;
   private static ImageIcon DIRECTORY_REMOVE;

   private ParentType parentType;
   private Window parentWindow;
   private JList directoryDisplayList;
   private DirectoryManagerListModel listModel;

   static {

      DIRECTORY_ADD = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/24x24/plain/folder_add.png"));
      DIRECTORY_REMOVE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/24x24/plain/folder_delete.png"));
   }

   public DirectoryManager (Dialog parentDialog, List<File> directoryList) {

      this(directoryList);

      parentType = ParentType.DIALOG;
      parentWindow = parentDialog;
   }

   public DirectoryManager (Frame parentFrame, List<File> directoryList) {

      this(directoryList);

      parentType = ParentType.FRAME;
      parentWindow = parentFrame;
   }

   private DirectoryManager (List<File> directoryList) {

      super(GRID_BAG_LAYOUT);

      Box buttonBox;
      JScrollPane directoryDisplayListScrollPane;
      JButton addDirectoryButton;
      JButton removeDirectoryButton;
      RemoveDirectoryAction removeDirectoryAction;

      GridBagConstraints constraints = new GridBagConstraints();

      listModel = new DirectoryManagerListModel(directoryList);

      directoryDisplayList = new JList(listModel);
      directoryDisplayList.setDragEnabled(false);
      directoryDisplayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      directoryDisplayListScrollPane = new SmallMindScrollPane(directoryDisplayList);

      removeDirectoryAction = new RemoveDirectoryAction();
      removeDirectoryButton = new JButton(removeDirectoryAction);
      removeDirectoryButton.setFocusable(false);
      removeDirectoryButton.registerKeyboardAction(removeDirectoryAction, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

      if (listModel.getSize() == 0) {
         removeDirectoryAction.setEnabled(false);
      }
      else {
         directoryDisplayList.setSelectedIndex(0);
      }

      addDirectoryButton = new JButton(new AddDirectoryAction(removeDirectoryAction));
      addDirectoryButton.setFocusable(false);

      buttonBox = new Box(BoxLayout.Y_AXIS);
      buttonBox.add(addDirectoryButton);
      buttonBox.add(Box.createVerticalStrut(5));
      buttonBox.add(removeDirectoryButton);
      buttonBox.add(Box.createVerticalGlue());

      constraints.fill = GridBagConstraints.BOTH;
      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 1;
      add(directoryDisplayListScrollPane, constraints);

      constraints.fill = GridBagConstraints.BOTH;
      constraints.insets = new Insets(0, 5, 0, 0);
      constraints.gridx = 1;
      constraints.gridy = 0;
      constraints.weightx = 0;
      constraints.weighty = 1;
      add(buttonBox, constraints);
   }

   public Dimension getPreferredSize () {

      return PREFERRED_DIMENSION;
   }

   public class AddDirectoryAction extends AbstractAction {

      private RemoveDirectoryAction removeDirectoryAction;

      public AddDirectoryAction (RemoveDirectoryAction removeDirectoryAction) {

         super();

         this.removeDirectoryAction = removeDirectoryAction;

         putValue(Action.SMALL_ICON, DIRECTORY_ADD);
         putValue(Action.SHORT_DESCRIPTION, "Add a media directory");
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         File addedDirectory;

         switch (parentType) {
            case DIALOG:
               addedDirectory = DirectoryChooserDialog.createShowDialog((Dialog)parentWindow);
               break;
            case FRAME:
               addedDirectory = DirectoryChooserDialog.createShowDialog((Frame)parentWindow);
               break;
            default:
               throw new UnknownSwitchCaseException(parentType.name());
         }

         if (addedDirectory != null) {
            listModel.addDirectory(addedDirectory);
            removeDirectoryAction.setEnabled(true);

            if (directoryDisplayList.getSelectedIndex() < 0) {
               directoryDisplayList.setSelectedIndex(0);
            }
         }
      }

   }

   public class RemoveDirectoryAction extends AbstractAction {

      public RemoveDirectoryAction () {

         super();

         putValue(Action.SMALL_ICON, DIRECTORY_REMOVE);
         putValue(Action.SHORT_DESCRIPTION, "Remove a media directory");
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         int selectedIndex;

         listModel.removeDirectory(selectedIndex = directoryDisplayList.getSelectedIndex());

         if (listModel.getSize() == 0) {
            setEnabled(false);
         }
         if (selectedIndex < listModel.getSize()) {
            directoryDisplayList.setSelectedIndex(selectedIndex);
         }
         else {
            directoryDisplayList.setSelectedIndex(listModel.getSize() - 1);
         }
      }

   }

}
