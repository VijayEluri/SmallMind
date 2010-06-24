package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.smallmind.nutsnbolts.swing.event.DialogEvent;
import org.smallmind.nutsnbolts.swing.event.DialogListener;
import org.smallmind.nutsnbolts.swing.event.DirectoryChoiceEvent;
import org.smallmind.nutsnbolts.swing.event.DirectoryChoiceListener;
import org.smallmind.nutsnbolts.swing.file.DirectoryChooser;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DirectoryChooserDialog extends JDialog implements WindowListener, DirectoryChoiceListener {

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
   private static final FlowLayout FLOW_LAYOUT = new FlowLayout(FlowLayout.RIGHT);

   private WeakEventListenerList<DialogListener> listenerList;
   private CancelAction cancelAction;
   private JButton okButton;
   private File directory;
   boolean initialized = false;

   public static File createShowDialog (Dialog parentDialog) {

      DirectoryChooserDialog directoryChooserDialog;

      directoryChooserDialog = new DirectoryChooserDialog(parentDialog);
      directoryChooserDialog.showDialog();

      return directoryChooserDialog.getChosenDirectory();
   }

   public static File createShowDialog (Frame parentFrame) {

      DirectoryChooserDialog directoryChooserDialog;

      directoryChooserDialog = new DirectoryChooserDialog(parentFrame);
      directoryChooserDialog.showDialog();

      return directoryChooserDialog.getChosenDirectory();
   }

   public DirectoryChooserDialog (Dialog parentDialog) {

      super(parentDialog, "Choose Directory...");

      buildDialog(parentDialog);
   }

   public DirectoryChooserDialog (Frame parentFrame) {

      super(parentFrame, "Choose Directory...");

      buildDialog(parentFrame);
   }

   private void buildDialog (Window parentWindow) {

      GridBagConstraints constraints = new GridBagConstraints();
      Container contentPane;
      JPanel buttonPanel;
      JButton cancelButton;
      OKAction okAction;
      DirectoryChooser directoryChooser;

      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

      contentPane = getContentPane();
      contentPane.setLayout(GRID_BAG_LAYOUT);

      okAction = new OKAction();
      cancelAction = new CancelAction();

      okButton = new JButton(okAction);
      okButton.setEnabled(false);
      okButton.registerKeyboardAction(okAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

      cancelButton = new JButton(cancelAction);
      cancelButton.registerKeyboardAction(cancelAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

      buttonPanel = new JPanel(FLOW_LAYOUT);
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);

      directoryChooser = new DirectoryChooser();

      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.fill = GridBagConstraints.BOTH;
      constraints.insets = new Insets(5, 5, 0, 5);
      constraints.weightx = 1;
      constraints.weighty = 1;
      contentPane.add(directoryChooser, constraints);

      constraints.gridx = 0;
      constraints.gridy = 1;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.insets = new Insets(5, 5, 5, 5);
      constraints.weightx = 1;
      constraints.weighty = 0;
      contentPane.add(buttonPanel, constraints);

      pack();
      setLocationRelativeTo(parentWindow);
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

      directoryChooser.addDirectoryChoiceListener(this);
      addWindowListener(this);

      listenerList = new WeakEventListenerList<DialogListener>();
   }

   public void showDialog () {

      setModal(true);
      setVisible(true);
   }

   public synchronized void addDialogListener (DialogListener dialogListener) {

      listenerList.addListener(dialogListener);
   }

   public synchronized void removeDialogListener (DialogListener dialogListener) {

      listenerList.removeListener(dialogListener);
   }

   public synchronized File getChosenDirectory () {

      return directory;
   }

   public synchronized void rootChosen (DirectoryChoiceEvent directoryChoiceEvent) {

      directory = null;
      okButton.setEnabled(false);
      initialized = false;
   }

   public synchronized void directoryChosen (DirectoryChoiceEvent directoryChoiceEvent) {

      directory = directoryChoiceEvent.getChosenDirectory();

      if (!initialized) {
         okButton.setEnabled(true);
         initialized = true;
      }
   }

   public synchronized void fireDialogHandler (DialogState dialogState) {

      Iterator<DialogListener> listenerIter = listenerList.getListeners();
      DialogEvent dialogEvent;

      dialogEvent = new DialogEvent(this, dialogState);
      while (listenerIter.hasNext()) {
         listenerIter.next().dialogHandler(dialogEvent);
      }
   }

   public void windowOpened (WindowEvent windowEvent) {
   }

   public synchronized void windowClosing (WindowEvent windowEvent) {

      cancelAction.actionPerformed(null);
   }

   public void windowClosed (WindowEvent windowEvent) {
   }

   public void windowIconified (WindowEvent windowEvent) {
   }

   public void windowDeiconified (WindowEvent windowEvent) {
   }

   public void windowActivated (WindowEvent windowEvent) {
   }

   public void windowDeactivated (WindowEvent windowEvent) {
   }

   public class OKAction extends AbstractAction {

      public OKAction () {

         super();

         putValue(Action.NAME, "OK");
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         setVisible(false);
         dispose();
         fireDialogHandler(DialogState.OK);
      }

   }

   public class CancelAction extends AbstractAction {

      public CancelAction () {

         super();

         putValue(Action.NAME, "Cancel");
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         directory = null;

         setVisible(false);
         dispose();
         fireDialogHandler(DialogState.CANCEL);
      }

   }

}
