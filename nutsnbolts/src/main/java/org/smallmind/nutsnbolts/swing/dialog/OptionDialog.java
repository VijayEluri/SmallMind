package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.smallmind.nutsnbolts.swing.event.DialogEvent;
import org.smallmind.nutsnbolts.swing.event.DialogListener;
import org.smallmind.nutsnbolts.swing.panel.OptionPanel;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class OptionDialog extends JDialog implements WindowListener {

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
   private static final int BUTTON_HEIGHT = 23;

   private WeakEventListenerList<DialogListener> listenerList;
   private DialogState dialogState;
   private OptionPanel optionPanel;
   private JPanel buttonPanel;

   public OptionDialog (Frame parentFrame, String optionText, OptionType optionType) {

      this(parentFrame, optionText, optionType, null, null);
   }

   public OptionDialog (Frame parentFrame, String optionText, OptionType optionType, OptionPanel optionPanel) {

      this(parentFrame, optionText, optionType, null, optionPanel);
   }

   public OptionDialog (Dialog parentDialog, String optionText, OptionType optionType) {

      this(parentDialog, optionText, optionType, null, null);
   }

   public OptionDialog (Dialog parentDialog, String optionText, OptionType optionType, OptionPanel optionPanel) {

      this(parentDialog, optionText, optionType, null, optionPanel);
   }

   public OptionDialog (Frame parentFrame, String optionText, OptionType optionType, OptionButton[] buttonList) {

      super(parentFrame, optionType.getTitle() + "...");

      buildDialog(parentFrame, optionText, optionType, buttonList, null);
   }

   public OptionDialog (Frame parentFrame, String optionText, OptionType optionType, OptionButton[] buttonList, OptionPanel optionPanel) {

      super(parentFrame, optionType.getTitle() + "...");

      buildDialog(parentFrame, optionText, optionType, buttonList, optionPanel);
   }

   public OptionDialog (Dialog parentDialog, String optionText, OptionType optionType, OptionButton[] buttonList) {

      super(parentDialog, optionType.getTitle() + "...");

      buildDialog(parentDialog, optionText, optionType, buttonList, null);
   }

   public OptionDialog (Dialog parentDialog, String optionText, OptionType optionType, OptionButton[] buttonList, OptionPanel optionPanel) {

      super(parentDialog, optionType.getTitle() + "...");

      buildDialog(parentDialog, optionText, optionType, buttonList, optionPanel);
   }

   private void buildDialog (Window parentWindow, String optionText, OptionType optionType, OptionButton[] buttonList, OptionPanel optionPanel) {

      GridBagConstraints constraint;
      Container contentPane;
      JPanel dialogPanel;
      JPanel infoPanel;
      JPanel expressionPanel;
      JLabel optionIconLabel;
      JLabel optionTextLabel;
      ImageIcon optionImage;

      this.optionPanel = optionPanel;

      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      listenerList = new WeakEventListenerList<DialogListener>();

      try {
         optionImage = new ImageIcon(ClassLoader.getSystemResource("public/images/dialog" + optionType.getImageType() + ".png"));
      }
      catch (Exception i) {
         optionImage = null;
      }

      dialogPanel = new JPanel(GRID_BAG_LAYOUT);
      infoPanel = new JPanel(GRID_BAG_LAYOUT);
      expressionPanel = new JPanel(GRID_BAG_LAYOUT);

      buttonPanel = new JPanel(GRID_BAG_LAYOUT);
      addButtons(buttonList);

      optionIconLabel = new JLabel(optionImage);

      optionTextLabel = new JLabel(optionText);
      optionTextLabel.setFont(optionTextLabel.getFont().deriveFont(Font.BOLD));

      constraint = new GridBagConstraints();

      constraint.anchor = GridBagConstraints.NORTH;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(0, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 0;
      infoPanel.add(optionTextLabel, constraint);

      if (optionPanel != null) {
         constraint.anchor = GridBagConstraints.NORTH;
         constraint.fill = GridBagConstraints.HORIZONTAL;
         constraint.insets = new Insets(10, 0, 0, 0);
         constraint.gridx = 0;
         constraint.gridy = 1;
         constraint.weightx = 1;
         constraint.weighty = 0;
         infoPanel.add(optionPanel, constraint);
      }

      constraint.anchor = GridBagConstraints.NORTH;
      constraint.fill = GridBagConstraints.NONE;
      constraint.insets = new Insets(0, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 0;
      expressionPanel.add(optionIconLabel, constraint);

      constraint.anchor = GridBagConstraints.NORTH;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(10, 10, 0, 0);
      constraint.gridx = 1;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 0;
      expressionPanel.add(infoPanel, constraint);

      constraint.anchor = GridBagConstraints.NORTH;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(5, 5, 0, 5);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 0;
      dialogPanel.add(expressionPanel, constraint);

      constraint.anchor = GridBagConstraints.SOUTH;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(10, 5, 5, 5);
      constraint.gridx = 0;
      constraint.gridy = 1;
      constraint.weightx = 1;
      constraint.weighty = 0;
      dialogPanel.add(buttonPanel, constraint);

      contentPane = getContentPane();
      contentPane.setLayout(new GridLayout(1, 0));
      contentPane.add(dialogPanel);

      pack();
      setResizable(false);
      setLocationRelativeTo(parentWindow);

      dialogState = DialogState.INCOMPLETE;

      if (optionPanel != null) {
         optionPanel.initalize(this);
      }

      addWindowListener(this);
   }

   private void addButtons (OptionButton[] buttonList) {

      int count;

      buttonPanel.removeAll();

      if (buttonList == null) {
         placeButton(buttonPanel, "Continue", DialogState.CONTINUE, true, 0);
      }
      else {
         for (count = 0; count < buttonList.length; count++) {
            placeButton(buttonPanel, buttonList[count].getName(), buttonList[count].getButtonState(), false, count);
         }
      }

      buttonPanel.revalidate();
      buttonPanel.repaint();
   }

   private void placeButton (JPanel buttonPanel, String buttonName, DialogState dialogState, boolean defaultAction, int buttonIndex) {

      GridBagConstraints constraint;
      JButton button;
      Action buttonAction;
      int buttonWidth;

      buttonAction = new OptionAction(buttonName, dialogState);
      button = new JButton(buttonAction);
      if (defaultAction) {
         button.registerKeyboardAction(buttonAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
      }
      buttonWidth = (int)button.getPreferredSize().getWidth();
      button.setPreferredSize(new Dimension(buttonWidth, BUTTON_HEIGHT));

      constraint = new GridBagConstraints();

      constraint.anchor = GridBagConstraints.EAST;
      constraint.fill = GridBagConstraints.NONE;
      constraint.insets = new Insets(0, (buttonIndex == 0) ? 0 : 5, 0, 0);
      constraint.gridx = buttonIndex;
      constraint.gridy = 0;
      constraint.weightx = (buttonIndex == 0) ? 1 : 0;
      constraint.weighty = 0;
      buttonPanel.add(button, constraint);
   }

   public synchronized void addDialogListener (DialogListener dialogListener) {

      listenerList.addListener(dialogListener);
   }

   public synchronized void removeDialogListener (DialogListener dialogListener) {

      listenerList.removeListener(dialogListener);
   }

   public synchronized DialogState getDialogState () {

      return dialogState;
   }

   public synchronized void setDialogState (DialogState dialogState) {

      this.dialogState = dialogState;
   }

   public OptionPanel getOptionPanel () {

      return optionPanel;
   }

   public synchronized void replaceButtons (OptionButton[] buttonList) {

      addButtons(buttonList);
   }

   public synchronized void fireDialogEvent () {

      DialogEvent dialogEvent;
      Iterator<DialogListener> listenerIter = listenerList.getListeners();

      dialogEvent = new DialogEvent(this, dialogState);

      while (listenerIter.hasNext()) {
         listenerIter.next().dialogHandler(dialogEvent);
      }
   }

   public void windowOpened (WindowEvent windowEvent) {
   }

   public synchronized void windowClosing (WindowEvent windowEvent) {

      String validationMessage;
      WarningDialog warningDialog;

      if ((optionPanel != null) && ((validationMessage = optionPanel.validateOption(dialogState)) != null)) {
         warningDialog = new WarningDialog(this, validationMessage);
         warningDialog.setModal(true);
         warningDialog.setVisible(true);
      }
      else {
         fireDialogEvent();
         setVisible(false);
         dispose();
      }
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

   private class OptionAction extends AbstractAction {

      private DialogState actionState;

      public OptionAction (String name, DialogState actionState) {

         super(name);

         this.actionState = actionState;
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         dialogState = actionState;
         windowClosing(null);
      }
   }

}
