package org.smallmind.phalanx.blockchain;

public enum BlockVersion {

  V1 {
    @Override
    public boolean validate (Block block, BlockChain blockChain) {

      return false;
    }
  };

  public abstract boolean validate (Block block, BlockChain blockChain);
}
