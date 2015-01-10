/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.swing.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import org.smallmind.swing.ComponentUtilities;
import org.smallmind.swing.panel.OptionPanel;

public class ProgressOptionPanel extends OptionPanel implements ProgressOperator, DialogListener {

  private JProgressBar progressBar;
  private JLabel processLabel;
  private ProgressRunnable progressRunnable;
  private boolean withLabel;
  private boolean closeOnComplete;

  public ProgressOptionPanel (ProgressRunnable progressRunnable, int orientation, int min, int max, boolean withLabel, boolean closeOnComplete) {

    super(new GridBagLayout());

    GridBagConstraints constraint;

    this.progressRunnable = progressRunnable;
    this.withLabel = withLabel;
    this.closeOnComplete = closeOnComplete;

    if (withLabel) {
      processLabel = new JLabel("X");
      ComponentUtilities.setPreferredHeight(processLabel, processLabel.getPreferredSize().height);
      processLabel.setText("");
    }

    progressBar = new JProgressBar(orientation, min, max);
    progressBar.setStringPainted(true);
    progressBar.setBorderPainted(true);

    constraint = new GridBagConstraints();

    if (withLabel) {
      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(0, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 0;
      add(processLabel, constraint);
    }

    constraint.anchor = GridBagConstraints.WEST;
    constraint.fill = GridBagConstraints.HORIZONTAL;
    constraint.insets = new Insets((!withLabel) ? 0 : 3, 0, 0, 0);
    constraint.gridx = 0;
    constraint.gridy = (!withLabel) ? 0 : 1;
    constraint.weightx = 1;
    constraint.weighty = 0;
    add(progressBar, constraint);
  }

  public void initialize (OptionDialog optionDialog) {

    Thread progressThread;

    super.initialize(optionDialog);

    progressRunnable.initalize(this);
    progressThread = new Thread(progressRunnable);
    progressThread.start();

    optionDialog.addDialogListener(this);
  }

  public String validateOption (DialogState dialogState) {

    return null;
  }

  public synchronized void setProcessLabel (String name) {

    if (!withLabel) {
      throw new IllegalStateException("Progress bar has been requested without a label");
    }

    processLabel.setText(name);
  }

  public synchronized void setMinimum (int min) {

    progressBar.setMinimum(min);
  }

  public synchronized void setMaximum (int max) {

    progressBar.setMaximum(max);
  }

  public synchronized void setValue (int value) {

    progressBar.setValue(value);
    if (value == progressBar.getMaximum()) {
      if (closeOnComplete) {
        setDialogSate(DialogState.COMPLETE);
        closeParent();
      }
      else {
        getOptionDialog().replaceButtons(new OptionButton[] {new OptionButton("Continue", DialogState.CONTINUE)});
      }
    }
  }

  public synchronized void dialogHandler (DialogEvent dialogEvent) {

    if (!getDialogState().equals(DialogState.COMPLETE)) {
      progressRunnable.terminate();
    }
  }

}
