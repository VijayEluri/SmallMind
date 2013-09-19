/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class Juggler<P, R> implements BlackList<R> {

  private final SecureRandom random = new SecureRandom();
  private final JugglingPinFactory<P, R> jugglingPinFactory;
  private final P[] providers;
  private final Class<P> managedClass;
  private final int recoveryCheckSeconds;
  private ProviderRecoveryWorker recoveryWorker = null;
  private ArrayList<JugglingPin<R>> sourcePins;
  private ArrayList<JugglingPin<R>> targetPins;
  private ConcurrentSkipListMap<Long, JugglingPin<R>> blackMap;
  private State state = State.DECONSTRUCTED;

  public Juggler (Class<P> managedClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P provider, int size) {

    this(managedClass, recoveryCheckSeconds, jugglingPinFactory, generateArray(provider, size));
  }

  public Juggler (Class<P> managedClass, int recoveryCheckSeconds, JugglingPinFactory<P, R> jugglingPinFactory, P... providers) {

    this.managedClass = managedClass;
    this.recoveryCheckSeconds = recoveryCheckSeconds;
    this.jugglingPinFactory = jugglingPinFactory;
    this.providers = providers;
  }

  private static <P> P[] generateArray (P provider, int size) {

    P[] array = (P[])new Object[size];

    Arrays.fill(array, provider);

    return array;
  }

  public synchronized void initialize ()
    throws JugglerResourceCreationException {

    if (state.equals(State.DECONSTRUCTED)) {
      sourcePins = new ArrayList<JugglingPin<R>>(providers.length);
      targetPins = new ArrayList<JugglingPin<R>>(providers.length);
      blackMap = new ConcurrentSkipListMap<Long, JugglingPin<R>>();

      for (P provider : providers) {
        targetPins.add(jugglingPinFactory.createJugglingPin(this, provider));
      }

      while (!targetPins.isEmpty()) {
        sourcePins.add(targetPins.remove(random.nextInt(targetPins.size())));
      }

      state = State.INITIALIZED;
    }
  }

  public synchronized void startup () {

    startup(null);
  }

  public synchronized void startup (Method method, Object... args) {

    if (state.equals(State.INITIALIZED)) {

      Thread recoveryThread;
      Iterator<JugglingPin<R>> sourcePinIter = sourcePins.iterator();

      while (sourcePinIter.hasNext()) {

        JugglingPin<R> pin = sourcePinIter.next();

        try {
          pin.start(method, args);
        }
        catch (JugglerResourceException jugglerResourceException) {
          try {
            LoggerManager.getLogger(Juggler.class).error(jugglerResourceException);
          }
          finally {
            sourcePinIter.remove();
            blackMap.put(System.currentTimeMillis(), pin);
          }
        }
      }

      if (recoveryCheckSeconds > 0) {
        recoveryThread = new Thread(recoveryWorker = new ProviderRecoveryWorker(recoveryCheckSeconds));
        recoveryThread.setDaemon(true);
        recoveryThread.start();
      }

      state = State.STARTED;
    }
  }

  public synchronized R pickResource ()
    throws NoAvailableJugglerResourceException {

    if (!(state.equals(State.INITIALIZED) || state.equals(State.STARTED))) {
      throw new IllegalStateException("Juggler must be in the initialized or started state");
    }

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

    throw new NoAvailableJugglerResourceException("All available resources(%s) have been black listed", managedClass.getSimpleName());
  }

  public synchronized void addToBlackList (JugglingPin<R> blackPin) {

    if (sourcePins.remove(blackPin)) {
      blackMap.put(System.currentTimeMillis(), blackPin);
      LoggerManager.getLogger(Juggler.class).info("Added resource(%s) to black list", blackPin.describe());
    }
    else if (targetPins.remove(blackPin)) {
      blackMap.put(System.currentTimeMillis(), blackPin);
      LoggerManager.getLogger(Juggler.class).info("Added resource(%s) to black list", blackPin.describe());
    }
  }

  public synchronized void shutdown () {

    shutdown(null);
  }

  public synchronized void shutdown (Method method, Object... args) {

    if (state.equals(State.STARTED)) {
      if (recoveryWorker != null) {
        try {
          recoveryWorker.abort();
        }
        catch (InterruptedException interruptedException) {
          LoggerManager.getLogger(Juggler.class).error(interruptedException);
        }
      }

      for (JugglingPin<R> pin : sourcePins) {
        try {
          pin.stop(method, args);
        }
        catch (Exception exception) {
          LoggerManager.getLogger(Juggler.class).error(exception);
        }
      }
      while (!targetPins.isEmpty()) {

        JugglingPin<R> pin = targetPins.remove(0);

        try {
          pin.stop(method, args);
        }
        catch (Exception exception) {
          LoggerManager.getLogger(Juggler.class).error(exception);
        }
        finally {
          sourcePins.add(pin);
        }
      }

      state = State.STOPPED;
    }
  }

  public synchronized void deconstruct () {

    deconstruct(null);
  }

  public synchronized void deconstruct (Method method, Object... args) {

    if (state.equals(State.STOPPED)) {
      for (JugglingPin<R> pin : sourcePins) {
        try {
          pin.close(method, args);
        }
        catch (Exception exception) {
          LoggerManager.getLogger(Juggler.class).error(exception);
        }
      }

      state = State.DECONSTRUCTED;
    }
  }

  private static enum State {DECONSTRUCTED, INITIALIZED, STARTED, STOPPED}

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
            if (firstEntry.getValue().recover()) {
              synchronized (Juggler.this) {

                JugglingPin<R> recoveredPin;

                if ((recoveredPin = blackMap.remove(firstEntry.getKey())) != null) {
                  targetPins.add(recoveredPin);
                  LoggerManager.getLogger(Juggler.class).info("Recovered resource(%s) from black list", recoveredPin.describe());
                }
                else {
                  LoggerManager.getLogger(ProviderRecoveryWorker.class).fatal("We've lost a resource(%s), which should never occur - please notify a system administrator", managedClass.getSimpleName());
                }
              }
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
