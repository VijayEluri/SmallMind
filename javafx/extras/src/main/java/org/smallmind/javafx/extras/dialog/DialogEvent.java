/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.javafx.extras.dialog;

import javafx.event.Event;
import javafx.event.EventType;

public class DialogEvent extends Event {

  public static final EventType<DialogEvent> ANY = new EventType<DialogEvent>(Event.ANY);
  public static final EventType<DialogEvent> DIALOG_COMPLETED = new EventType<DialogEvent>(DialogEvent.ANY, "DIALOG_COMPLETED");

  private final Object source;
  private final DialogState dialogState;

  protected DialogEvent (EventType<DialogEvent> eventType, Object source, DialogState dialogState) {

    super(eventType);

    this.source = source;
    this.dialogState = dialogState;
  }

  public Object getSource () {

    return source;
  }

  public DialogState getDialogState () {

    return dialogState;
  }
}
