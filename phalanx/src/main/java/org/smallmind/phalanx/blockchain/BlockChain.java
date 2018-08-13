package org.smallmind.phalanx.blockchain;

public class BlockChain {

  public byte[] getTarget (double difficulty) {

    return null;
  }

  public double adjustDifficulty (long difficulty, long expectedTime, long actualTime) {

    return difficulty * expectedTime / ((double)actualTime);
  }
}
