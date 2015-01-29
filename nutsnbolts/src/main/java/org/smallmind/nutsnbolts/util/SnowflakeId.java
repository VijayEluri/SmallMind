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
package org.smallmind.nutsnbolts.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeId implements Comparable<SnowflakeId> {

  private static final int[] DOT_OFFSET_0 = {0, 0, 0};
  private static final int[] DOT_OFFSET_1 = {0, 0, 0};
  private static final int[] DOT_OFFSET_2 = {0, 1, 0};
  private static final int[] DOT_OFFSET_3 = {0, 1, 1};

  private static final int CODE_TEMPLATE_BITS = 5;
  private static final String CODE_TEMPLATE = "ABCDEFGHIJKMNPQRSTUVWXYZ23456789";

  private static final int COMPACT_CODE_TEMPLATE_BITS = 6;
  private static final String COMPACT_CODE_TEMPLATE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890$*";

  private static final int[] POWER_ARRAY = {1, 2, 4, 8, 16, 32, 64, 128};

  private static final SecureRandom RANDOM = new SecureRandom(Bytes.getBytes(System.currentTimeMillis()));

  private static final byte[] MAC_BYTES = new byte[6];
  private static final byte[] JVM_BYTES = new byte[2];

  private static final AtomicLong ATOMIC_TIME;
  private static final AtomicInteger ATOMIC_COUNT;

  private final byte[] uniqueArray;

  static {

    byte[] macAddress;

    try {
      macAddress = MacAddress.getBytes();
    } catch (Exception exception) {
      RANDOM.nextBytes(macAddress = new byte[6]);
    }

    System.arraycopy(macAddress, 0, MAC_BYTES, 0, 6);

    ATOMIC_TIME = new AtomicLong(System.currentTimeMillis());
    ATOMIC_COUNT = new AtomicInteger(Short.MIN_VALUE);
    RANDOM.nextBytes(JVM_BYTES);
  }

  public SnowflakeId () {

    uniqueArray = generateByteArray();
  }

  public SnowflakeId (byte[] uniqueArray) {

    this.uniqueArray = uniqueArray;
  }

  public static int byteSize () {

    return 18;
  }

  public static SnowflakeId newInstance () {

    return new SnowflakeId();
  }

  private byte[] generateByteArray () {

    byte[] bytes = new byte[18];
    long currentTime = 0;
    int currentCount;

    do {
      if ((currentCount = ATOMIC_COUNT.incrementAndGet()) < Short.MAX_VALUE) {
        currentTime = ATOMIC_TIME.get();
      } else if (currentCount == Short.MAX_VALUE) {
        ATOMIC_TIME.set(currentTime = Math.max(ATOMIC_TIME.get() + 1, System.currentTimeMillis()));
        ATOMIC_COUNT.set(currentCount = Short.MIN_VALUE);
      }
    } while (currentCount > Short.MAX_VALUE);

    if (currentTime == 0) {
      throw new IllegalStateException("Current time value should never be '0'");
    }

    System.arraycopy(Bytes.getBytes(currentTime), 0, bytes, 0, 8);
    System.arraycopy(MAC_BYTES, 0, bytes, 8, 6);
    System.arraycopy(JVM_BYTES, 0, bytes, 14, 2);
    System.arraycopy(Bytes.getBytes((short)currentCount), 0, bytes, 16, 2);

    return bytes;
  }

  public byte[] asByteArray () {

    return uniqueArray;
  }

  public BigInteger generateBigInteger () {

    return new BigInteger(uniqueArray);
  }

  public String generateCompactString () {

    return generateTemplateString(COMPACT_CODE_TEMPLATE, COMPACT_CODE_TEMPLATE_BITS).toString();
  }

  public String generateDottedString () {

    StringBuilder dottedIdBuilder = generateTemplateString(CODE_TEMPLATE, CODE_TEMPLATE_BITS);
    int dashSize;

    dashSize = dottedIdBuilder.length() / 4;
    switch (dottedIdBuilder.length() % 4) {
      case 0:
        return insertDots(dottedIdBuilder, DOT_OFFSET_0, dashSize);
      case 1:
        return insertDots(dottedIdBuilder, DOT_OFFSET_1, dashSize);
      case 2:
        return insertDots(dottedIdBuilder, DOT_OFFSET_2, dashSize);
      default:
        return insertDots(dottedIdBuilder, DOT_OFFSET_3, dashSize);
    }
  }

  private String insertDots (StringBuilder dottedIdBuilder, int[] offsets, int dashSize) {

    for (int count = 0; count < offsets.length; count++) {
      dottedIdBuilder.insert((dashSize * (count + 1)) + count + offsets[count], '.');
    }

    return dottedIdBuilder.toString();
  }

  private StringBuilder generateTemplateString (String template, int templateBits) {

    StringBuilder uniqueIdBuilder = new StringBuilder();

    int codeIndex = 0;
    int codeValue = 0;

    for (byte codeByte : uniqueArray) {
      for (int count = 0; count < 8; count++) {
        if ((codeByte & POWER_ARRAY[count]) != 0) {
          codeValue += POWER_ARRAY[codeIndex];
        }

        if (++codeIndex == templateBits) {
          uniqueIdBuilder.append(template.charAt(codeValue));
          codeIndex = 0;
          codeValue = 0;
        }
      }
    }

    if (codeIndex > 0) {
      uniqueIdBuilder.append(CODE_TEMPLATE.charAt(codeValue));
    }

    return uniqueIdBuilder;
  }

  @Override
  public int hashCode () {

    return Arrays.hashCode(uniqueArray);
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof SnowflakeId) && Arrays.equals(uniqueArray, ((SnowflakeId)obj).asByteArray());
  }

  public int compareTo (SnowflakeId snowflakeId) {

    int comparison;

    if ((comparison = compareTimeBytes(snowflakeId)) == 0) {
      if ((comparison = compareCountBytes(snowflakeId)) == 0) {
        if ((comparison = compareMacBytes(snowflakeId)) == 0) {

          return compareJVMBytes(snowflakeId);
        }
      }
    }

    return comparison;
  }

  private int compareMacBytes (SnowflakeId snowflakeId) {

    int comparison;

    for (int count = 8; count < 14; count++) {
      if ((comparison = this.asByteArray()[count] - snowflakeId.asByteArray()[count]) != 0) {

        return comparison;
      }
    }

    return 0;
  }

  private int compareJVMBytes (SnowflakeId snowflakeId) {

    int comparison;

    for (int count = 14; count < 16; count++) {
      if ((comparison = this.asByteArray()[count] - snowflakeId.asByteArray()[count]) != 0) {

        return comparison;
      }
    }

    return 0;
  }

  private int compareTimeBytes (SnowflakeId snowflakeId) {

    return Long.compare(Bytes.getLong(Arrays.copyOfRange(this.asByteArray(), 0, 8)), Bytes.getLong(Arrays.copyOfRange(snowflakeId.asByteArray(), 0, 8)));
  }

  private int compareCountBytes (SnowflakeId snowflakeId) {

    return Short.compare(Bytes.getShort(Arrays.copyOfRange(this.asByteArray(), 16, 18)), Bytes.getShort(Arrays.copyOfRange(snowflakeId.asByteArray(), 16, 18)));
  }
}