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
package org.smallmind.websocket;

import java.util.concurrent.ThreadLocalRandom;

public class Frame {

  public static byte[] ping (byte[] message)
    throws WebsocketException {

    return control(OpCode.PING, message);
  }

  public static byte[] pong (byte[] message)
    throws WebsocketException {

    return control(OpCode.PONG, message);
  }

  private static byte[] control (OpCode opCode, byte[] message)
    throws WebsocketException {

    if (message.length > 125) {
      throw new WebsocketException("Control frame data length exceeds 125 bytes");
    }

    return data(opCode, message);
  }

  public static byte[] text (String message) {

    return data(OpCode.TEXT, message.getBytes());
  }

  public static byte[] binary (byte[] message) {

    return data(OpCode.BINARY, message);
  }

  private static byte[] data (OpCode opCode, byte[] message) {

    int start = (message.length < 126) ? 6 : (message.length < 65536) ? 8 : 14;
    byte[] out = new byte[message.length + start];
    byte[] mask = new byte[4];

    ThreadLocalRandom.current().nextBytes(mask);

    out[0] = (byte)(0x80 | opCode.getCode());

    if (message.length < 126) {
      out[1] = (byte)(0x80 | message.length);

      System.arraycopy(mask, 0, out, 2, 4);
    }
    else if (message.length < 65536) {
      out[1] = (byte)(0x80 | 126);
      out[2] = (byte)(message.length >>> 8);
      out[3] = (byte)(message.length & 0xFF);

      System.arraycopy(mask, 0, out, 4, 4);
    }
    else {
      out[1] = (byte)(0x80 | 127);
      // largest array will never be more than 2^31-1
      out[2] = 0;
      out[3] = 0;
      out[4] = 0;
      out[5] = 0;
      out[6] = (byte)(message.length >>> 24);
      out[7] = (byte)(message.length >>> 16);
      out[8] = (byte)(message.length >>> 8);
      out[9] = (byte)(message.length & 0xFF);

      System.arraycopy(mask, 0, out, 10, 4);
    }

    for (int index = 0; index < message.length; index++) {
      out[index + start] = (byte)(message[index] ^ mask[index % 4]);
    }

    return out;
  }

  public static Data decode (byte[] buffer)
    throws SyntaxException {

    OpCode opCode;
    boolean fin;
    int start;
    byte[] message;
    byte length;

    if ((opCode = OpCode.convert(buffer[0])) == null) {
      throw new SyntaxException("Unknown op code(%d)", buffer[0] & 0xF);
    }
    fin = (buffer[0] & 0x80) != 0;

    if ((length = (byte)(buffer[1] & 0x7F)) < 126) {
      start = 2;
      message = new byte[length];
    }
    else if (length == 126) {
      message = new byte[(buffer[2] << 8) + buffer[3]];
      start = 4;
    }
    else {
      message = new byte[(buffer[6] << 24) + (buffer[7] << 16) + (buffer[8] << 8) + buffer[9]];
      start = 10;
    }

    System.arraycopy(buffer, start, message, 0, message.length);

    return new Data(fin, opCode, message);
  }
}
