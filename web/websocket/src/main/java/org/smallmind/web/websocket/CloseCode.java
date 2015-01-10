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
package org.smallmind.web.websocket;

public enum CloseCode {

  NORMAL(1000), GOING_AWAY(1001), PROTOCOL_ERROR(1002), UNKNOWN_DATA_TYPE(1003), DATA_TYPE_CONVERSION_ERROR(1007), POLICY_VIOLATION(1008), MESSAGE_TOO_LARGE(1009), MISSING_EXTENSION(1010), SERVER_ERROR(1011);
  private int code;

  private CloseCode (int code) {

    this.code = code;
  }

  public int getCode () {

    return code;
  }

  public byte[] getCodeAsBytes () {

    byte[] out = new byte[2];

    out[0] = (byte)(code >>> 8);
    out[1] = (byte)(code & 0xFF);

    return out;
  }
}
