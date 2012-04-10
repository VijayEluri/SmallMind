/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.juggler;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class Juggler<P, R> {

  private final SecureRandom random = new SecureRandom();
  private final ConcurrentSkipListMap<Long, JugglingPin<R>> blackMap;
  private final Class<P> managedClass;

  private ProviderRecoveryWorker recoveryWorker = null;
  private ArrayList<JugglingPin<R>> sourcePins;
  private ArrayList<JugglingPin<R>> targetPins;

  public Juggler (Class<P> managedClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P... providers) {

    Thread recoveryThread;

    this.managedClass = managedClass;

    sourcePins = new ArrayList<JugglingPin<R>>(providers.length);
    targetPins = new ArrayList<JugglingPin<R>>(providers.length);
    blackMap = new ConcurrentSkipListMap<Long, JugglingPin<R>>();

    for (P provider : providers) {
      targetPins.add(jugglingPinFactory.createJugglingPin(provider));
    }

    while (!targetPins.isEmpty()) {
      sourcePins.add(targetPins.remove(random.nextInt(targetPins.size())));
    }

    if (recoveryCheckSeconds > 0) {
      recoveryThread = new Thread(recoveryWorker = new ProviderRecoveryWorker(recoveryCheckSeconds));
      recoveryThread.setDaemon(true);
      recoveryThread.start();
    }
  }

  public R pickResource ()
    throws NoAvailableResourceException {

    synchronized (this) {
      while (!(sourcePins.isEmpty() && targetPins.isEmpty())) {

        R resource;
        JugglingPin<R> pin;

        if (sourcePins.isEmpty()) {

          ArrayList<JugglingPin<R>> tempPins = sourcePins;

          sourcePins = targetPins;
          targetPins = tempPins;
        }

        pin = sourcePins.remove(random.nextInt(sourcePins.size()));
        try {
          resource = pin.obtain();
          targetPins.add(pin);

          return resource;
        }
        catch (Exception exception) {
          try {
            LoggerManager.getLogger(Juggler.class).error(exception);
          }
          finally {
            blackMap.put(System.currentTimeMillis(), pin);
          }
        }
      }

      throw new NoAvailableResourceException("All available resources(%s) have been black listed", managedClass.getSimpleName());
    }
  }

  public void shutdown ()
    throws InterruptedException {

    if (recoveryWorker != null) {
      recoveryWorker.abort();
    }
  }

  private class ProviderRecoveryWorker implements Runnable {

    private CountDownLatch terminationLatch;
    private CountDownLatch exitLatch;
    private long recoveryCheckMillis;

    public ProviderRecoveryWorker (int recoveryCheckSeconds) {

      terminationLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
      recoveryCheckMillis = recoveryCheckSeconds * 1000;
    }

    public void abort ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!terminationLatch.await(3, TimeUnit.SECONDS)) {

          Map.Entry<Long, JugglingPin<R>> firstEntry;

          while (((firstEntry = blackMap.firstEntry()) != null) && ((firstEntry.getKey() + recoveryCheckMillis) <= System.currentTimeMillis())) {
            try {
              firstEntry.getValue().obtain();

              synchronized (Juggler.this) {

                JugglingPin<R> recoveredPin;

                if ((recoveredPin = blackMap.remove(firstEntry.getKey())) != null) {
                  targetPins.add(recoveredPin);
                }
                else {
                  LoggerManager.getLogger(ProviderRecoveryWorker.class).fatal("We've lost a resource(%s), which should never occur - please notify a system administrator", managedClass.getSimpleName());
                }
              }
            }
            catch (Exception exception) {
              // We can bury this exception because it just means that the resource provider is still broke
            }
          }
        }
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ProviderRecoveryWorker.class).error(interruptedException);
      }

      exitLatch.countDown();
    }
  }
}
