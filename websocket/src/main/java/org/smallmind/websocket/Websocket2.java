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
package org.smallmind.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.util.ThreadLocalRandom;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class Websocket2 {

  private final Socket socket;
  private final MessageWorker messageWorker;
  private final AtomicReference<ReadyState> readyStateRef = new AtomicReference<>(ReadyState.CONNECTING);
  private final String url;

  public Websocket2 (URI uri, String... protocols)
    throws IOException, NoSuchAlgorithmException, SyntaxException {

    Thread workerThread;
    byte[] keyBytes = new byte[16];

    ThreadLocalRandom.current().nextBytes(keyBytes);

    if (!uri.isAbsolute()) {
      throw new SyntaxException("A websocket uri must be absolute");
    }
    if ((uri.getScheme() == null) || (!(uri.getScheme().equals("ws") || uri.getScheme().equals("wss")))) {
      throw new SyntaxException("A websocket requires a uri with either the 'ws' or 'wss' scheme");
    }
    if ((uri.getFragment() != null) && (uri.getFragment().length() > 0)) {
      throw new SyntaxException("A websocket uri may not contain a fragment");
    }

    if (!ProtocolValidator.validate(protocols)) {
      throw new SyntaxException("The provided protocols(%s) are not valid", Arrays.toString(protocols));
    }

    url = uri.toString();

    socket = new Socket(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : uri.getScheme().equals("ws") ? 80 : 443);
    socket.setTcpNoDelay(true);

    // initial handshake request
    socket.getOutputStream().write(ClientHandshake.constructRequest(uri, keyBytes, protocols));
    ClientHandshake.validateResponse(readData(), keyBytes, protocols);
    readyStateRef.set(ReadyState.OPEN);

    workerThread = new Thread(messageWorker = new MessageWorker());
    workerThread.setDaemon(true);
    workerThread.start();
  }

  public abstract void onMessage (String message);

  public void send (ByteBuffer buffer)
    throws IOException {

    socket.getOutputStream().write(buffer.array(), buffer.position(), buffer.limit() - buffer.position());
  }

  private String readData ()
    throws IOException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];

    socket.getOutputStream().write(Frame.pong());
    do {

      int bytesRead;

      bytesRead = socket.getInputStream().read(buffer);
      outputStream.write(buffer, 0, bytesRead);
    } while (socket.getInputStream().available() > 0);

    return new String(outputStream.toByteArray());
  }

  public String url () {

    return url;
  }

  public ReadyState getReadyState () {

    return readyStateRef.get();
  }

  public int readyState () {

    return readyStateRef.get().ordinal();
  }

  public String extensions () {

    return "";
  }

  private class MessageWorker implements Runnable {

    private CountDownLatch exitLatch = new CountDownLatch(1);
    private AtomicBoolean aborted = new AtomicBoolean(false);

    public void abort ()
      throws InterruptedException {

      if (aborted.compareAndSet(false, true)) {
      }

      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!aborted.get()) {
          onMessage(readData());
        }
      }
      catch (IOException ioException) {
        LoggerManager.getLogger(Websocket2.class).error(ioException);
      }
      finally {
        exitLatch.countDown();
      }
    }
  }
}
