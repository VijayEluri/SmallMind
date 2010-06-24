package org.smallmind.plumber.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.smallmind.nutsnbolts.util.concurrent.Lock;

public class SocketPipe {

   private static final int UPSTREAM = 0;
   private static final int DOWNSTREAM = 1;

   private IOException threadException = null;
   private Socket downstreamSocket;
   private Socket upstreamSocket;
   private int bufferSize;

   public SocketPipe (Socket downstreamSocket, Socket upstreamSocket, int bufferSize) {

      this.downstreamSocket = downstreamSocket;
      this.upstreamSocket = upstreamSocket;
      this.bufferSize = bufferSize;
   }

   public void startPipe ()
      throws IOException {

      Thread lockThread;
      Lock lock;

      lockThread = Thread.currentThread();
      lock = new Lock(2);

      synchronized (lock) {
         startTransfer(lockThread, lock, UPSTREAM, downstreamSocket, upstreamSocket);
         startTransfer(lockThread, lock, DOWNSTREAM, upstreamSocket, downstreamSocket);

         try {
            lock.wait();
         }
         catch (InterruptedException i) {
            if (threadException != null) {
               throw threadException;
            }
         }
      }
   }

   private void startTransfer (Thread lockThread, Lock lock, int condition, Socket inputSocket, Socket outputSocket)
      throws IOException {

      Thread transferThread;

      transferThread = new Thread(new TransferHandler(lockThread, lock, condition, inputSocket, outputSocket));
      transferThread.setDaemon(true);

      transferThread.start();
   }

   public class TransferHandler implements Runnable {

      private final Thread lockThread;

      private Lock lock;
      private Socket inputSocket;
      private Socket outputSocket;
      private InputStream inputStream;
      private OutputStream outputStream;
      private int condition;
      private byte[] buffer;

      public TransferHandler (Thread lockThread, Lock lock, int condition, Socket inputSocket, Socket outputSocket)
         throws IOException {

         this.lockThread = lockThread;
         this.lock = lock;
         this.condition = condition;
         this.inputSocket = inputSocket;
         this.outputSocket = outputSocket;

         inputStream = inputSocket.getInputStream();
         outputStream = outputSocket.getOutputStream();
         buffer = new byte[bufferSize];
      }

      public void run () {

         int bytesRead;

         try {
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
               outputStream.write(buffer, 0, bytesRead);
            }

            inputSocket.shutdownInput();
            outputSocket.shutdownOutput();
            lock.unlock(condition);
         }
         catch (IOException ioException) {
            synchronized (lockThread) {
               if (threadException == null) {
                  threadException = ioException;
                  lockThread.interrupt();
               }
            }
         }
      }

   }

}
