/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocketFactory;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public abstract class WebSocket implements AutoCloseable {

  private final Socket socket;
  private final MessageWorker messageWorker;
  private final ConcurrentLinkedQueue<String> pingKeyQueue = new ConcurrentLinkedQueue<>();
  private final AtomicReference<ConnectionState> connectionStateRef = new AtomicReference<>(ConnectionState.CONNECTING);
  private final String url;
  private final String negotiatedProtocol;
  private final boolean secure;
  private final byte[] rawBuffer = new byte[1024];
  private final int protocolVersion = 13;

  public WebSocket (URI uri, String... protocols)
    throws IOException, NoSuchAlgorithmException, WebSocketException {

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

    if (uri.getScheme().equals("wss")) {
      socket = SSLSocketFactory.getDefault().createSocket(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : 443);
      secure = true;
    } else {
      socket = new Socket(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : 80);
      secure = false;
    }
    socket.setTcpNoDelay(true);
    socket.setSoTimeout(1000);

    // initial handshake request
    socket.getOutputStream().write(Handshake.constructRequest(protocolVersion, uri, keyBytes, protocols));
    negotiatedProtocol = Handshake.validateResponse(new String(read()), keyBytes, protocols);
    connectionStateRef.set(ConnectionState.OPEN);

    workerThread = new Thread(messageWorker = new MessageWorker());
    workerThread.setDaemon(true);
    workerThread.start();
  }

  public abstract void onError (Exception exception);

  public abstract void onPong (byte[] message);

  public abstract void onText (String message);

  public abstract void onBinary (byte[] message);

  public synchronized void ping (byte[] buffer)
    throws IOException, WebSocketException {

    if (connectionStateRef.get().equals(ConnectionState.CLOSING) || connectionStateRef.get().equals(ConnectionState.CLOSED)) {
      throw new WebSocketException("The websocket has been closed");
    }

    try {
      pingKeyQueue.add(Base64Codec.encode(EncryptionUtility.hash(HashAlgorithm.SHA_1, buffer)));
      write(Frame.ping(buffer));
    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new WebSocketException(noSuchAlgorithmException);
    }
  }

  public void text (String message)
    throws IOException, WebSocketException {

    text(message, true);
  }

  public synchronized void text (String message, boolean fin)
    throws IOException, WebSocketException {

    if (connectionStateRef.get().equals(ConnectionState.CLOSING) || connectionStateRef.get().equals(ConnectionState.CLOSED)) {
      throw new WebSocketException("The websocket has been closed");
    }

    write(Frame.text(message, fin));
  }

  public void binary (byte[] buffer)
    throws IOException, WebSocketException {

    binary(buffer, true);
  }

  public synchronized void binary (byte[] buffer, boolean fin)
    throws IOException, WebSocketException {

    if (connectionStateRef.get().equals(ConnectionState.CLOSING) || connectionStateRef.get().equals(ConnectionState.CLOSED)) {
      throw new WebSocketException("The websocket has been closed");
    }

    write(Frame.binary(buffer, true));
  }

  @Override
  public void close ()
    throws IOException, WebSocketException, InterruptedException {

    close(CloseCode.NORMAL);
  }

  public void close (CloseCode closeCode)
    throws IOException, WebSocketException, InterruptedException {

    close(closeCode, null);
  }

  public void close (CloseCode closeCode, String reason)
    throws IOException, WebSocketException, InterruptedException {

    close(closeCode.getCodeAsBytes(), reason);
  }

  private void close (byte[] status, String reason)
    throws IOException, WebSocketException, InterruptedException {

    if (connectionStateRef.compareAndSet(ConnectionState.OPEN, ConnectionState.CLOSING)) {
      try {
        messageWorker.abort();
        write(Frame.close(status, reason));
      } finally {
        connectionStateRef.set(ConnectionState.CLOSED);
      }
    }
  }

  private void write (byte[] buffer)
    throws IOException {

    socket.getOutputStream().write(buffer);
  }

  private byte[] read ()
    throws IOException, WebSocketException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    boolean complete = false;

    // TODO: Should NOT need to do this
    socket.getOutputStream().write(Frame.pong(new byte[0]));

    do {
      do {

        int bytesRead;

        bytesRead = socket.getInputStream().read(rawBuffer);
        outputStream.write(rawBuffer, 0, bytesRead);
      } while (socket.getInputStream().available() > 0);

      if (connectionStateRef.get().equals(ConnectionState.CONNECTING)) {
        complete = true;
      } else {
        if (outputStream.size() >= 2) {

          byte length = (byte)(outputStream.toByteArray()[1] & 0x7F);

          if (length < 126) {
            complete = outputStream.size() == length + 2;
          } else if ((length == 126) && (outputStream.size() >= 4)) {

            byte[] currentArray = outputStream.toByteArray();

            complete = outputStream.size() == ((currentArray[2] & 0xFF) << 8) + (currentArray[3] & 0xFF) + 4;
          } else if (outputStream.size() >= 10) {

            byte[] currentArray = outputStream.toByteArray();

            complete = outputStream.size() == ((currentArray[6] & 0xFF) << 24) + ((currentArray[7] & 0xFF) << 16) + ((currentArray[8] & 0xFF) << 8) + (currentArray[9] & 0xFF) + 10;
          }
        }
      }
    } while (!complete);

    outputStream.close();

    return outputStream.toByteArray();
  }

  public int getProtocolVersion () {

    return protocolVersion;
  }

  public String getNegotiatedProtocol () {

    return negotiatedProtocol;
  }

  public boolean isSecure () {

    return secure;
  }

  public String url () {

    return url;
  }

  public ConnectionState getConnectionState () {

    return connectionStateRef.get();
  }

  public int connectionState () {

    return connectionStateRef.get().ordinal();
  }

  public String extensions () {

    return "";
  }

  private class MessageWorker implements Runnable {

    private CountDownLatch exitLatch = new CountDownLatch(1);
    private AtomicBoolean aborted = new AtomicBoolean(false);
    private LinkedList<Fragment> fragmentList = new LinkedList<>();

    public void abort ()
      throws InterruptedException {

      if (aborted.compareAndSet(false, true)) {
        // do nothing for now... maybe with a proper event based implementation
      }

      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!aborted.get()) {
          try {

            Fragment fragment;

            if ((fragment = Frame.decode(read())).isFinal()) {
              switch (fragment.getOpCode()) {
                case CONTINUATION:
                  if (fragmentList.isEmpty()) {
                    throw new WebSocketException("No continuation exists to terminate");
                  }

                  try {
                    ByteArrayOutputStream fragmentStream = new ByteArrayOutputStream();

                    for (Fragment storedFragment : fragmentList) {
                      fragmentStream.write(storedFragment.getMessage());
                    }
                    fragmentStream.write(fragment.getMessage());
                    fragmentStream.close();

                    switch (fragmentList.getFirst().getOpCode()) {
                      case TEXT:
                        onText(new String(fragmentStream.toByteArray()));
                        break;
                      case BINARY:
                        onBinary(fragmentStream.toByteArray());
                        break;
                      default:
                        throw new WebSocketException("The current continuation starts with an illegal op code(%s)", fragmentList.getFirst().getOpCode().name());
                    }
                  } finally {
                    fragmentList.clear();
                  }
                  break;
                case TEXT:
                  if (!fragmentList.isEmpty()) {
                    fragmentList.clear();
                    throw new WebSocketException("Expecting the final frame of a continuation");
                  }

                  onText(new String(fragment.getMessage()));
                  break;
                case BINARY:
                  if (!fragmentList.isEmpty()) {
                    fragmentList.clear();
                    throw new WebSocketException("Expecting the final frame of a continuation");
                  }

                  onBinary(fragment.getMessage());
                  break;
                case CLOSE:

                  byte[] status;

                  if (fragment.getMessage().length < 2) {
                    status = CloseCode.SERVER_ERROR.getCodeAsBytes();
                  } else {
                    status = new byte[2];
                    System.arraycopy(fragment.getMessage(), 0, status, 0, 2);
                  }

                  close(status, null);
                  break;
                case PING:
                  socket.getOutputStream().write(Frame.pong(fragment.getMessage()));
                  break;
                case PONG:

                  Iterator<String> pingKeyIter = pingKeyQueue.iterator();
                  String pongKey = Base64Codec.encode(EncryptionUtility.hash(HashAlgorithm.SHA_1, fragment.getMessage()));

                  while (pingKeyIter.hasNext()) {

                    String pingKey = pingKeyIter.next();

                    pingKeyIter.remove();
                    if (pongKey.equals(pingKey)) {
                      onPong(fragment.getMessage());
                      break;
                    }
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(fragment.getOpCode().name());
              }
            } else {
              if (!(fragment.getOpCode().equals(OpCode.CONTINUATION) || fragment.getOpCode().equals(OpCode.TEXT) || fragment.getOpCode().equals(OpCode.BINARY))) {
                throw new WebSocketException("All control frames must be marked as final");
              }
              if ((fragment.getOpCode().equals(OpCode.TEXT) || fragment.getOpCode().equals(OpCode.BINARY)) && (!fragmentList.isEmpty())) {
                fragmentList.clear();
                throw new WebSocketException("Starting a new continuation before the previous continuation has terminated");
              }
              if (fragment.getOpCode().equals(OpCode.CONTINUATION) && fragmentList.isEmpty()) {
                throw new WebSocketException("The first frame of a continuation must have an op code != 0");
              }

              fragmentList.add(fragment);
            }
          } catch (SocketTimeoutException socketTimeoutException) {
            // TODO: Do something here
          } catch (Exception exception) {
            onError(exception);
          }
        }
      } finally {
        exitLatch.countDown();
      }
    }
  }
}
