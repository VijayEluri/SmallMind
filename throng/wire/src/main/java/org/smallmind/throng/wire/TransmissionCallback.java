package org.smallmind.throng.wire;

import org.smallmind.nutsnbolts.util.SelfDestructive;

public interface TransmissionCallback extends SelfDestructive {

  public abstract Object getResult (SignalCodec signalCodec)
    throws Throwable;
}