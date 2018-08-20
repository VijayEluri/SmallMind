package org.smallmind.phalanx.blockchain;

public interface BlockHeader<D extends Data> extends Hashable {

  BlockVersion getVersion ();
}
