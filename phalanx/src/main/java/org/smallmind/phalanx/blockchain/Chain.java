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
import java.util.ArrayList;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class Chain {

  public static ArrayList<Block> blockchain = new ArrayList<Block>();
  public static int difficulty = 3;

  public static void main (String[] args)
    throws NoSuchAlgorithmException, JsonProcessingException {

    //add our blocks to the blockchain ArrayList:

    blockchain.add(new Block("Hi im the first block", "0"));
    System.out.println("Trying to Mine block 1... ");
    blockchain.get(0).mineBlock(difficulty);

    blockchain.add(new Block("Yo im the second block", blockchain.get(blockchain.size() - 1).hash));
    System.out.println("Trying to Mine block 2... ");
    blockchain.get(1).mineBlock(difficulty);

    blockchain.add(new Block("Hey im the third block", blockchain.get(blockchain.size() - 1).hash));
    System.out.println("Trying to Mine block 3... ");
    blockchain.get(2).mineBlock(difficulty);

    System.out.println("\nBlockchain is Valid: " + isChainValid());

    String blockchainJson = JsonCodec.writeAsString(blockchain);
    System.out.println("\nThe block chain: ");
    System.out.println(blockchainJson);
  }

  public static Boolean isChainValid ()
    throws NoSuchAlgorithmException {

    Block currentBlock;
    Block previousBlock;
    String hashTarget = new String(new char[difficulty]).replace('\0', '0');

    //loop through blockchain to check hashes:
    for (int i = 1; i < blockchain.size(); i++) {
      currentBlock = blockchain.get(i);
      previousBlock = blockchain.get(i - 1);
      //compare registered hash and calculated hash:
      if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
        System.out.println("Current Hashes not equal");
        return false;
      }
      //compare previous hash and registered previous hash
      if (!previousBlock.hash.equals(currentBlock.previousHash)) {
        System.out.println("Previous Hashes not equal");
        return false;
      }
      //check if hash is solved
      if (!currentBlock.hash.substring(0, difficulty).equals(hashTarget)) {
        System.out.println("This block hasn't been mined");
        return false;
      }
    }
    return true;
  }
}