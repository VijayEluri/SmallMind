package org.smallmind.quorum.pool.connection;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

public class DeconstructionQueue {

  private final ConcurrentSkipListMap<IgnitionKey, DeconstructionFuse> fuseMap = new ConcurrentSkipListMap<IgnitionKey, DeconstructionFuse>();
  private final AtomicReference<CountDownLatch> terminationLatch = new AtomicReference<CountDownLatch>();
  private final AtomicReference<CountDownLatch> exitLatch = new AtomicReference<CountDownLatch>();
  private final AtomicInteger ordinal = new AtomicInteger(0);

  public void startup () {

    terminationLatch.set(new CountDownLatch(1));
    exitLatch.set(new CountDownLatch(1));
    new Thread(new IgnitionWorker()).start();
  }

  public int nextOrdinal () {

    return ordinal.incrementAndGet();
  }

  public void add (DeconstructionFuse deconstructionFuse) {

    fuseMap.put(new IgnitionKey(deconstructionFuse), deconstructionFuse);
  }

  public void remove (DeconstructionFuse deconstructionFuse) {

    fuseMap.remove(new IgnitionKey(deconstructionFuse));
  }

  public void shutdown ()
    throws InterruptedException {

    terminationLatch.get().countDown();
    exitLatch.get().await();
  }

  private class IgnitionWorker implements Runnable {

    @Override
    public void run () {

      try {
        while (!terminationLatch.get().await(1, TimeUnit.SECONDS)) {

          IgnitionKey nowKey = new IgnitionKey(Integer.MAX_VALUE, System.currentTimeMillis());
          IgnitionKey fuseKey;

          while (((fuseKey = fuseMap.firstKey()) != null) && fuseKey.compareTo(nowKey) < 0) {
            fuseMap.remove(fuseKey).ignite();
          }
        }
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(DeconstructionQueue.class).error(interruptedException);
      }

      exitLatch.get().countDown();
    }
  }

  private class IgnitionKey implements Comparable<IgnitionKey> {

    private final long ignitionTime;
    private final int ordinal;

    private IgnitionKey (DeconstructionFuse deconstructionFuse) {

      this(deconstructionFuse.getOrdinal(), deconstructionFuse.getIgnitionTime());
    }

    private IgnitionKey (int ordinal, long ignitionTime) {

      this.ordinal = ordinal;
      this.ignitionTime = ignitionTime;
    }

    public int getOrdinal () {

      return ordinal;
    }

    public long getIgnitionTime () {

      return ignitionTime;
    }

    @Override
    public int compareTo (IgnitionKey key) {

      long comparison;

      if ((comparison = ignitionTime - key.getIgnitionTime()) == 0) {

        return ordinal - key.getOrdinal();
      }

      return (int)comparison;
    }

    @Override
    public int hashCode () {

      return ordinal;
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof IgnitionKey) && (ordinal == ((IgnitionKey)obj).getOrdinal());
    }
  }
}