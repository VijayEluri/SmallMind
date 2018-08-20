package org.smallmind.phalanx.blockchain;

public class BlockChain {

  /*
  A timestamp is accepted as valid if it is greater than the median timestamp of previous 11 blocks, and less than the network-adjusted time + 2 hours.
  "Network-adjusted time" is the median of the timestamps returned by all nodes connected to you.

Whenever a node connects to another node, it gets a UTC timestamp from it, and stores its offset from node-local UTC. The network-adjusted time is then
the node-local UTC plus the median offset from all connected nodes. Network time is never adjusted more than 70 minutes from local system time, however.
   */
  /*
  When a miner (call him Pete) learns that a new block (call it X) has been mined by someone else, he will normally update the header of the block he is
  trying to mine, so that it lists X as the previous block. (That way, Pete is now trying to build on top of X, rather than trying to replace it.) At the
  same time, Pete will note which transactions were included in block X, and remove them from his "memory pool" of unconfirmed transactions that he was
  planning to include in the block he was trying to mine. He will rebuild a Merkle tree with the remaining transactions he wishes to include, and update
  his block headers to match.

In this way, when Pete does succeed in mining a block, it won't contain any transactions that have been contained in previously mined blocks.
   */

  public byte[] getTarget (double difficulty) {

    return null;
  }

  public double adjustDifficulty (long difficulty, long expectedTime, long actualTime) {

    return difficulty * expectedTime / ((double)actualTime);
  }
}
