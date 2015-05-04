/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.multicast.event;

import java.nio.ByteBuffer;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public abstract class EventMessage {

  public static final int MESSAGE_HEADER_SIZE = SnowflakeId.byteSize() + 16;

  private ByteBuffer translationBuffer;

  public EventMessage (byte[] messageId, MessageType messageType, int messageLength, int extraSize) {

    byte[] messageArray;

    messageArray = new byte[messageId.length + 12 + extraSize];
    translationBuffer = ByteBuffer.wrap(messageArray);

    translationBuffer.putInt(MessageStatus.MULTICAST.ordinal());
    translationBuffer.put(messageId);
    translationBuffer.putInt(messageType.ordinal());
    translationBuffer.putInt(messageLength);
  }

  public void put (byte[] b) {

    translationBuffer.put(b);
  }

  public void putInt (int i) {

    translationBuffer.putInt(i);
  }

  public void putLong (long l) {

    translationBuffer.putLong(l);
  }

  public ByteBuffer getByteBuffer () {

    return translationBuffer;
  }
}
