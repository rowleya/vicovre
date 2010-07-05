/*
 * @(#)VideoWebServer.java
 * Created: 2005-04-21
 * Version: 1-1-alpha3
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.googlecode.vicovre.streamer.web;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * A Web Server to display video
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class VideoWebServer extends Thread {

    // The time to wait between stopping one web server and starting another
    private static final int WEB_SERVER_PORT_CHANGE_DELAY = 100;

    // The amount to increase the buffer by if it is too small
    private static final int BUFFER_INCREASE_MULTIPLIER = 2;

    // The initial size of the buffer
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    // The amount to multiply the number of threads by when there isn't enough
    private static final int THREAD_MULTIPLIER = 2;

    // The initial number of threads
    private static final int INITIAL_THREADS = 10;

    // True if the server should stop
    private boolean done = false;

    // True if the thread is paused
    private boolean paused = false;

    // Object to synchronize pause listening on
    private Integer pauseSync = new Integer(0);

    // The HTTP socket
    private ServerSocket serverSocket = null;

    // The number of threads in the thread pool
    private int threads = INITIAL_THREADS;

    // The Thread Pool to do the Web Serving
    private VideoWebHandler[] threadPool = new VideoWebHandler[threads];

    // The offline data
    private byte[] offlineData = new byte[0];

    // A map of streamid to stream
    private HashMap<String, StreamUpdateListener> streams =
        new HashMap<String, StreamUpdateListener>();

    private HashMap<InetAddress, Boolean> isNewStream =
        new HashMap<InetAddress, Boolean>();

    // A list of clients that are to be serviced by a pool of threads
    private LinkedList<Socket> clients = new LinkedList<Socket>();

    /**
     * Creates a new VideoWebServer
     * @param port The port to serve images on
     *
     * @throws IOException
     */
    public VideoWebServer(int port) throws IOException {

        // Load the offline image
        BufferedInputStream input = new BufferedInputStream(getClass()
                .getResourceAsStream("offline.JPG"));
        int bufSize = DEFAULT_BUFFER_SIZE;
        int bufPos = 0;
        byte[] tmpData = new byte[bufSize];
        int nextByte;
        try {
            while ((nextByte = input.read()) != -1) {
                if (bufPos == bufSize) {
                    bufSize *= BUFFER_INCREASE_MULTIPLIER;
                    byte[] newData = new byte[bufSize];
                    System.arraycopy(tmpData, 0, newData, 0, bufPos);
                    tmpData = newData;
                }
                tmpData[bufPos] = (byte) nextByte;
                bufPos++;
            }
            offlineData = new byte[bufPos];
            System.arraycopy(tmpData, 0, offlineData, 0, bufPos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        serverSocket = new ServerSocket(port);
        for (int i = 0; i < threads; i++) {
            threadPool[i] = new VideoWebHandler(this);
            threadPool[i].start();
        }
    }

    protected Socket nextClient() {
        synchronized (clients) {
            while (clients.isEmpty()) {
                try {
                    clients.wait();
                } catch (InterruptedException ignore) {
                    // Do Nothing
                }
            }
            return clients.removeFirst();
        }
    }

    private int addClient(Socket client) {
        synchronized (clients) {
            clients.addLast(client);
            clients.notify();
            synchronized (isNewStream) {
                if (!isNewStream.containsKey(client.getInetAddress())) {
                    isNewStream.put(client.getInetAddress(), true);
                    isNewStream.notifyAll();
                }
            }
            return clients.size();
        }
    }

    private void pause(boolean paused) {
        synchronized (pauseSync) {
            this.paused = paused;
            pauseSync.notifyAll();
        }
    }

    /**
     * Runs a web server that serves the image and pages
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {

        System.err.println("Starting Web Server on Port "
                + serverSocket.getLocalPort());

        // Continue serving clients until stopped
        while (!done) {
            try {
                synchronized (pauseSync) {
                    while (paused) {
                        try {
                            pauseSync.wait(1000);
                        } catch (InterruptedException e) {
                            // Do Nothing
                        }
                    }
                }
                Socket client = serverSocket.accept();

                int noClients = addClient(client);
                if (noClients >= (THREAD_MULTIPLIER * threads)) {
                    VideoWebHandler[] oldThreadPool = threadPool;
                    threadPool =
                        new VideoWebHandler[THREAD_MULTIPLIER * threads];
                    for (int i = 0; i < threads; i++) {
                        threadPool[i] = oldThreadPool[i];
                    }
                    for (int i = threads; i < (threads * THREAD_MULTIPLIER);
                            i++) {
                        threadPool[i] = new VideoWebHandler(this);
                        threadPool[i].start();
                    }
                    threads *= THREAD_MULTIPLIER;
                }
            } catch (SocketException e) {
                if (done) {
                    System.err.println("Web Server Stopped");
                } else if (paused) {
                    System.err.println("Web Server Paused");
                } else {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the web server
     */
    public void end() {
        done = true;
        try {
            serverSocket.close();
            for (int i = 0; i < threads; i++) {
                threadPool[i].end();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the port that the server is listening on
     * @param port The new port
     * @throws IOException
     */
    public void setPort(int port) throws IOException {
        if (port == serverSocket.getLocalPort()) {
            return;
        }
        pause(true);
        ServerSocket oldServerSocket = serverSocket;
        try {
            serverSocket = new ServerSocket(port);
            try {
                Thread.sleep(WEB_SERVER_PORT_CHANGE_DELAY);
            } catch (InterruptedException e1) {
                // Do Nothing
            }
        } catch (IOException e) {
            pause(false);
            throw e;
        }

        try {
            oldServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        pause(false);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void indicateNewStream() {
        synchronized (isNewStream) {
            for (InetAddress client : isNewStream.keySet()) {
                isNewStream.put(client, true);
            }
            isNewStream.notifyAll();
        }
    }

    public StreamUpdateListener getStream(String id, String name) {
        synchronized (streams) {
            if ((id != null) && !streams.containsKey(id) && !id.equals("")) {
                StreamUpdateListener listener = new StreamUpdateListener(id,
                        offlineData);
                listener.setName(name);
                streams.put(id, listener);
                indicateNewStream();
            }
            return streams.get(id);
        }
    }

    public List<StreamUpdateListener> getStreams() {
        return new Vector<StreamUpdateListener>(streams.values());
    }

    public boolean isNewStream(InetAddress client) {
        synchronized (isNewStream) {
            if (!isNewStream.get(client)) {
                try {
                    isNewStream.wait(10000);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            boolean result = isNewStream.get(client);
            isNewStream.put(client, false);
            return result;
        }
    }
}
