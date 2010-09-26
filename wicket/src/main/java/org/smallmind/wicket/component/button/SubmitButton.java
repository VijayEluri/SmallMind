/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.wicket.component.button;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.smallmind.wicket.skin.SkinManager;

public class SubmitButton extends Button {

   public SubmitButton (String id, IModel labelModel, Form form, SkinManager skinManager) {

      super(id, labelModel, skinManager);

      addButtonBehavior(new AttributeModifier("onclick", true, new FormSubmitActionModel(form)));
   }

   private class FormSubmitActionModel extends AbstractReadOnlyModel {

      private Form form;

      public FormSubmitActionModel (Form form) {

         this.form = form;
      }

      public Object getObject () {

         if (!SubmitButton.this.isEnabled()) {
            return "";
         }

         return "SMALLMIND.component.button.submit('" + form.getMarkupId() + "')";
      }
   }
}