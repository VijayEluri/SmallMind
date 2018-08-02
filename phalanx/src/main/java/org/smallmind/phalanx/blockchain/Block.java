/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.phalanx.blockchain;

import java.security.NoSuchAlgorithmException;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;
import org.smallmind.nutsnbolts.util.Bytes;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class Block<D extends Data> {

  private D data;
  private Block<D> parent;
  private byte[] hash;
  private long created;

  Block (D data)
    throws NoSuchAlgorithmException {

    this(null, data);
  }

  public Block (Block<D> parent, D data)
    throws NoSuchAlgorithmException {

    byte[] snowflakeBytes = SnowflakeId.newInstance().asByteArray();
    byte[] dataHash = data.getHash();
    byte[] parentHash = (parent == null) ? null : parent.getHash();
    byte[] hashBytes = new byte[8 + snowflakeBytes.length + dataHash.length + ((parentHash == null) ? 0 : parentHash.length)];

    this.parent = parent;

    created = System.currentTimeMillis();

    System.arraycopy(Bytes.getBytes(created), 0, hashBytes, 0, 8);
    System.arraycopy(snowflakeBytes, 0, hashBytes, hashBytes.length, snowflakeBytes.length);
    System.arraycopy(dataHash, 0, hashBytes, hashBytes.length, dataHash.length);
    if (parentHash != null) {
      System.arraycopy(parentHash, 0, hashBytes, hashBytes.length, parentHash.length);
    }

    hash = EncryptionUtility.hash(HashAlgorithm.SHA_256, hashBytes);
  }

  private byte[] getHash () {

    return hash;
  }

  public void mineBlock (int difficulty)
    throws NoSuchAlgorithmException {

    String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
    while (!hash.substring(0, difficulty).equals(target)) {
      nonce++;
      hash = calculateHash();
    }
    System.out.println("Block Mined!!! : " + hash);
  }
}