package org.smallmind.quorum.pool2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.scribe.pen.LoggerManager;

public class ConnectionPinManager<C> {

  private static enum State {STOPPED, STARTING, STARTED, STOPPING}

  private final ConnectionPool<C> connectionPool;
  private final HashMap<ConnectionInstance<C>, ConnectionPin<C>> backingMap = new HashMap<ConnectionInstance<C>, ConnectionPin<C>>();
  private final LinkedBlockingQueue<ConnectionPin<C>> freeQueue = new LinkedBlockingQueue<ConnectionPin<C>>();
  private final ReentrantReadWriteLock backingLock = new ReentrantReadWriteLock();
  private final AtomicReference<State> stateRef = new AtomicReference<State>(State.STOPPED);
  private final AtomicInteger size = new AtomicInteger(0);

  public ConnectionPinManager (ConnectionPool<C> connectionPool) {

    this.connectionPool = connectionPool;
  }

  public void startup ()
    throws ConnectionPoolException {

    if (stateRef.compareAndSet(State.STOPPED, State.STARTING)) {
      backingLock.writeLock().lock();
      try {
        while (backingMap.size() < Math.max(connectionPool.getConnectionPoolConfig().getMinPoolSize(), connectionPool.getConnectionPoolConfig().getInitialPoolSize())) {

          ConnectionPin<C> connectionPin;
          ConnectionInstance<C> connectionInstance;

          backingMap.put(connectionInstance = connectionPool.getConnectionInstanceFactory().createInstance(connectionPool), connectionPin = new ConnectionPin<C>(connectionPool, connectionInstance));
          freeQueue.put(connectionPin);
        }

        size.set(backingMap.size());
        stateRef.set(State.STARTED);
      }
      catch (Exception exception) {
        freeQueue.clear();
        backingMap.clear();
        size.set(0);
        stateRef.set(State.STOPPED);

        throw new ConnectionPoolException(exception);
      }
      finally {
        backingLock.writeLock().unlock();
      }
    }
    else {
      try {
        while (State.STARTING.equals(stateRef.get())) {
          Thread.sleep(100);
        }
      }
      catch (InterruptedException interruptedException) {
        throw new ConnectionPoolException(interruptedException);
      }
    }
  }

  public ConnectionPin<C> serve ()
    throws ConnectionPoolException {

    if (!State.STARTED.equals(stateRef.get())) {
      throw new ConnectionPoolException("ConnectionPool has not been started");
    }

    ConnectionPin<C> connectionPin;

    if ((connectionPin = freeQueue.poll()) != null) {

      if (connectionPool.getConnectionPoolConfig().isTestOnAcquire() && (!connectionPin.getConnectionInstance().validate())) {
        throw new ConnectionValidationException("A free connection was acquired, but failed to validate");
      }

      return connectionPin;
    }

    if ((connectionPin = addConnectionPin()) != null) {

      return connectionPin;
    }

    try {
      if ((connectionPin = freeQueue.poll(connectionPool.getConnectionPoolConfig().getAcquireWaitTimeMillis(), TimeUnit.MILLISECONDS)) != null) {

        if (connectionPool.getConnectionPoolConfig().isTestOnAcquire() && (!connectionPin.getConnectionInstance().validate())) {
          throw new ConnectionValidationException("A free connection was acquired, but failed to validate");
        }

        return connectionPin;
      }
    }
    catch (InterruptedException interruptedException) {
      throw new ConnectionPoolException(interruptedException);
    }

    throw new ConnectionPoolException("Exceeded the maximum acquire wait time(%d)", connectionPool.getConnectionPoolConfig().getAcquireWaitTimeMillis());
  }

  private ConnectionPin<C> addConnectionPin ()
    throws ConnectionCreationException, ConnectionValidationException {

    if (State.STARTED.equals(stateRef.get())) {

      int maxPoolSize;

      if (((maxPoolSize = connectionPool.getConnectionPoolConfig().getMaxPoolSize()) == 0) || (size.get() < maxPoolSize)) {
        backingLock.writeLock().lock();
        try {
          if ((maxPoolSize == 0) || (size.get() < maxPoolSize)) {

            ConnectionPin<C> connectionPin;
            ConnectionInstance<C> connectionInstance;

            backingMap.put(connectionInstance = manufactureConnectionInstance(), connectionPin = new ConnectionPin<C>(connectionPool, connectionInstance));
            size.incrementAndGet();

            return connectionPin;
          }
        }
        finally {
          backingLock.writeLock().unlock();
        }
      }
    }

    return null;
  }

  private ConnectionInstance<C> manufactureConnectionInstance ()
    throws ConnectionCreationException, ConnectionValidationException {

    ConnectionInstance<C> connectionInstance;

    try {
      if (connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis() > 0) {

        ConnectionWorker<C> connectionWorker;
        Thread workerThread;

        connectionWorker = new ConnectionWorker<C>(connectionPool);
        workerThread = new Thread(connectionWorker);
        workerThread.start();

        workerThread.join(connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis());
        if (connectionWorker.abort()) {
          throw new ConnectionCreationException("Exceeded connection timeout(%d) waiting on connection creation", connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis());
        }
        else {
          connectionInstance = connectionWorker.getConnectionInstance();
        }
      }
      else {
        connectionInstance = connectionPool.getConnectionInstanceFactory().createInstance(connectionPool);
      }
    }
    catch (ConnectionCreationException connectionCreationException) {
      throw connectionCreationException;
    }
    catch (Exception exception) {
      throw new ConnectionCreationException(exception);
    }

    if (connectionPool.getConnectionPoolConfig().isTestOnConnect() && (!connectionInstance.validate())) {
      throw new ConnectionValidationException("A new connection was required, but failed to validate");
    }

    return connectionInstance;
  }

  public void remove (ConnectionPin<C> connectionPin) {

    if (freeQueue.remove(connectionPin)) {
      terminate(connectionPin.getConnectionInstance());
    }
  }

  public void process (ConnectionInstance<C> connectionInstance) {

    ConnectionPin<C> connectionPin;

    backingLock.readLock().lock();
    try {
      connectionPin = backingMap.get(connectionInstance);
    }
    finally {
      backingLock.readLock().unlock();
    }

    if (connectionPin != null) {
      if (connectionPin.isTerminated()) {
        terminate(connectionPin.getConnectionInstance());
      }
      else {
        connectionPin.free();

        if (State.STARTED.equals(stateRef.get())) {
          try {
            freeQueue.put(connectionPin);
          }
          catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ConnectionPinManager.class).error(interruptedException);
          }
        }
      }
    }
  }

  public void terminate (ConnectionInstance<C> connectionInstance) {

    ConnectionPin<C> connectionPin;

    backingLock.writeLock().lock();
    try {
      connectionPin = backingMap.remove(connectionInstance);
    }
    finally {
      backingLock.writeLock().unlock();
    }

    if (connectionPin != null) {
      size.decrementAndGet();
      connectionPin.fizzle();

      try {
        connectionPin.getConnectionInstance().close();
      }
      catch (Exception exception) {
        LoggerManager.getLogger(ConnectionPinManager.class).error(exception);
      }

      try {

        ConnectionPin<C> replacementConnectionPin;

        if ((replacementConnectionPin = addConnectionPin()) != null) {
          freeQueue.put(replacementConnectionPin);
        }
      }
      catch (Exception exception) {
        LoggerManager.getLogger(ConnectionPinManager.class).error(exception);
      }
    }
  }

  public void shutdown ()
    throws ConnectionPoolException {

    if (stateRef.compareAndSet(State.STARTED, State.STOPPING)) {

      while (size.get() > 0) {

        Set<ConnectionInstance<C>> keySet;

        backingLock.readLock().lock();
        try {
          keySet = backingMap.keySet();
        }
        finally {
          backingLock.readLock().unlock();
        }

        for (ConnectionInstance<C> connectionInstance : keySet) {
          terminate(connectionInstance);
        }

        freeQueue.clear();
      }
    }
    else {
      try {
        while (State.STOPPING.equals(stateRef.get())) {
          Thread.sleep(100);
        }
      }
      catch (InterruptedException interruptedException) {
        throw new ConnectionPoolException(interruptedException);
      }
    }
  }

  public int getPoolSize () {

    return size.get();
  }

  public int getFreeSize () {

    return freeQueue.size();
  }

  public int getProcessingSize () {

    return getPoolSize() - getFreeSize();
  }

  public StackTrace[] getExistentialStackTraces () {

    LinkedList<StackTrace> stackTraceList = new LinkedList<StackTrace>();
    StackTrace[] stackTraces;

    backingLock.readLock().lock();
    try {
      for (ConnectionPin<C> connectionPin : backingMap.values()) {
        if (!freeQueue.contains(connectionPin)) {
          stackTraceList.add(new StackTrace(connectionPin.getExistentialStackTrace()));
        }
      }
    }
    finally {
      backingLock.readLock().unlock();
    }

    stackTraces = new StackTrace[stackTraceList.size()];
    stackTraceList.toArray(stackTraces);

    return stackTraces;
  }
}
