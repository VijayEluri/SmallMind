/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.wicket.validator;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

public class PasswordComplexityValidator extends AbstractValidator {

   private static final PasswordComplexityValidator STATIC_INSTANCE = new PasswordComplexityValidator();

   public static PasswordComplexityValidator getInstance () {

      return STATIC_INSTANCE;
   }

   protected void onValidate (IValidatable iValidatable) {

      String password;
      int digitCount = 0;
      int punctuationCount = 0;

      password = (String)iValidatable.getValue();

      if (password.length() < 6) {
         error(iValidatable, "error.password.complexity.length");
      }

      for (int count = 0; count < password.length(); count++) {
         if (Character.isDigit(password.charAt(count))) {
            digitCount++;
         }
         else if (!Character.isLetter(password.charAt(count))) {
            punctuationCount++;
         }
      }

      if ((digitCount < 1) && (punctuationCount < 1)) {
         error(iValidatable, "error.password.complexity.safety");
      }
   }

}