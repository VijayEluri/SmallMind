/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.popup;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyEvent;

public class DropDownButton extends ButtonBase {

  public DropDownButton () {

    super();

    initialize();
  }

  public DropDownButton (String text) {

    super(text);

    initialize();
  }

  public DropDownButton (String text, Node image) {

    super(text, image);

    initialize();
  }

  private void initialize () {

    getStylesheets().add(DropDownButton.class.getResource("DropDownButton.css").toExternalForm());
    getStyleClass().add("drop-down-box");
    setFocusTraversable(true);

    focusedProperty().addListener(new ChangeListener<Boolean>() {

      @Override
      public void changed (ObservableValue<? extends Boolean> observableValue, Boolean oldBoolean, Boolean newBoolean) {

        if (!newBoolean) {
          System.out.println("focus lost****************************");
        }
      }
    });

    onKeyTypedProperty().set(new EventHandler<KeyEvent>() {

      @Override
      public void handle (KeyEvent keyEvent) {

        if (!(keyEvent.isAltDown() || keyEvent.isControlDown() || keyEvent.isMetaDown() || keyEvent.isShiftDown() || keyEvent.isShortcutDown())) {
          if ((keyEvent.getCharacter().length() == 1) && (keyEvent.getCharacter().charAt(0) == '\u001B')) {
            keyEvent.consume();
            System.out.println("escaped***********************");
          }
        }
      }
    });
  }

  @Override
  public void fire () {

    System.out.println("fire************************************");
  }
}
