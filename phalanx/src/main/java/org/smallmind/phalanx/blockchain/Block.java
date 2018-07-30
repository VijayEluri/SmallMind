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
import java.util.Date;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public class Block {

  private String data; //our data will be a simple message.
  private long timeStamp; //as number of milliseconds since 1/1/1970.
  private int nonce;
  public String hash;
  public String previousHash;

  //Block Constructor.
  public Block (String data, String previousHash)
    throws NoSuchAlgorithmException {

    this.data = data;
    this.previousHash = previousHash;
    this.timeStamp = new Date().getTime();

    this.hash = calculateHash(); //Making sure we do this after we set the other values.
  }

  //Calculate new hash based on blocks contents
  public String calculateHash ()
    throws NoSuchAlgorithmException {

    String calculatedhash = new String(EncryptionUtility.hash(HashAlgorithm.SHA_256, (previousHash +
                                                                                        Long.toString(timeStamp) +
                                                                                        Integer.toString(nonce) +
                                                                                        data).getBytes()
    ));

    return calculatedhash;
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