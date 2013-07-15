/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.wicket.component.radio;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;

public abstract class AjaxRadio<E> extends Radio<E> {

  public AjaxRadio (String id) {

    this(id, null, null);
  }

  public AjaxRadio (String id, IModel<E> model) {

    this(id, model, null);
  }

  public AjaxRadio (String id, RadioGroup<E> group) {

    this(id, null, group);
  }

  public AjaxRadio (String id, IModel<E> model, RadioGroup<E> group) {

    super(id, model, group);

    add(new OnClickAjaxEventBehavior());
  }

  public abstract void onClick (E selection, AjaxRequestTarget target);

  private class OnClickAjaxEventBehavior extends AjaxEventBehavior {

    public OnClickAjaxEventBehavior () {

      super("onClick");
    }

    protected void onEvent (AjaxRequestTarget target) {

      if (AjaxRadio.this.isEnabled()) {
        onClick(AjaxRadio.this.getModel().getObject(), target);
      }
    }
  }
}
