package org.smallmind.phalanx.blockchain;

public enum BlockVersion {

  PROOF_OF_WORK_V1 {
    @Override
    public boolean validate (Block block, BlockChain blockChain) {

      return false;
    }
  };

  public abstract boolean validate (Block block, BlockChain blockChain);
}
