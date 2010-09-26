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
package org.smallmind.nutsnbolts.http;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public final class Base64 {

   private static final String base64Bible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

   public static String decode (String encodedString)
      throws UnsupportedEncodingException {

      StringWriter transStringWriter = new StringWriter();
      int transChar;
      int maxOctets;
      int baseConv1;
      int baseConv2;
      int octet;
      int index;

      maxOctets = encodedString.length() / 4;
      for (octet = 0; octet < maxOctets; octet++) {
         for (index = 0; index < 3; index++) {
            baseConv1 = base64Bible.indexOf(encodedString.charAt(index + (octet * 4)));
            baseConv2 = base64Bible.indexOf(encodedString.charAt(index + 1 + (octet * 4)));
            if ((baseConv1 < 0) || (baseConv2 < 0)) {
               throw new UnsupportedEncodingException();
            }
            if (baseConv1 == 64) {
               baseConv1 = 0;
            }
            if (baseConv2 == 64) {
               return transStringWriter.toString();
            }
            switch (index) {
               case 0:
                  transChar = ((baseConv1 & 63) << 2) | ((baseConv2 & 48) >> 4);
                  break;
               case 1:
                  transChar = ((baseConv1 & 15) << 4) | ((baseConv2 & 60) >> 2);
                  break;
               case 2:
                  transChar = ((baseConv1 & 3) << 6) | (baseConv2 & 63);
                  break;
               default:
                  transChar = 0;
            }
            transStringWriter.write(transChar);
         }
      }
      return transStringWriter.toString();
   }

   public static String encode (String normalString)
      throws UnsupportedEncodingException {

      StringReader transStringReader = new StringReader(normalString);
      StringBuilder encodedString;
      int base64Conv;
      int transChar;
      int normalChar;
      int maxOctets;
      int padding;
      int octet;
      int index;

      maxOctets = normalString.length() / 3;
      if ((normalString.length() % 3) > 0) {
         maxOctets++;
      }
      encodedString = new StringBuilder(maxOctets * 4);
      padding = 0;
      for (octet = 0; octet < maxOctets; octet++) {
         transChar = 0;
         for (index = 0; index < 3; index++) {
            try {
               normalChar = transStringReader.read();
            }
            catch (IOException i) {
               throw new UnsupportedEncodingException();
            }
            transChar <<= 8;
            if (normalChar > 0) {
               transChar += (normalChar & 255);
            }
            else if (normalChar < 0) {
               padding++;
            }
         }
         for (index = 0; index < 4; index++) {
            base64Conv = (transChar & 16515072) >> 18;
            if ((base64Conv == 0) && (index == 2) && (padding == 2)) {
               encodedString.append(base64Bible.charAt(64));
               padding--;
            }
            else if ((base64Conv == 0) && (index == 3) && (padding == 1)) {
               encodedString.append(base64Bible.charAt(64));
               padding--;
            }
            else {
               encodedString.append(base64Bible.charAt(base64Conv));
            }
            transChar <<= 6;
         }
      }
      return encodedString.toString();
   }
}
