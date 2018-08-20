package org.smallmind.phalanx.blockchain;

public class BlockHeader<D extends Data> implements Hashable {

  /*
The Block Header consists of the hash of the current block, the hash of the previous block,
timestamp of when the current block was hashed, the target difficulty of the block (more on this later)
, and the nonce (more on this later)
   */

  private BlockVersion version;
  private byte[] parent;
  private double difficulty;
  private long timestamp;

  public BlockHeader (BlockVersion version, D data, BlockChain chain) {

    this.version = version;
  }

  @Override
  public byte[] getHash () {

    return new byte[0];
  }

  public BlockVersion getVersion () {

    return version;
  }
}
