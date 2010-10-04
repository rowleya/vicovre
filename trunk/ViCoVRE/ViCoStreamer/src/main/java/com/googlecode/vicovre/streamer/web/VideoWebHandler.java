/*
 * @(#)VideoWebHandler.java
 * Created: 06-May-2005
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A Thread Pool Element to serve web pages
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class VideoWebHandler extends Thread {

    // The response to send to a bad request
    private static final String BAD_REQUEST_RESPONSE =
        "HTTP/1.1 400 Bad Request";

    // The HTML content type header
    private static final String HTML_CONTENT_TYPE = "Content-Type: text/html";

    // The HTTP immediate expiry header
    private static final String IMMEDIATE_EXPIRY_HEADER = "Expires: 0";

    // The HTTP old No Cache header
    private static final String NO_CACHE_PRAGMA_HEADER = "Pragma: no-cache";

    // The HTTP new No Cache header
    private static final String NO_CACHE_HEADER = "Cache-Control: no-cache";

    // The response to send to an OK request
    private static final String OK_RESPONSE = "HTTP/1.1 200 OK";

    // The header to close the connection on finishing
    private static final String CONNECTION_CLOSE_HEADER = "Connection: close";

    // The amount to add to the image size to avoid buffer overflow
    private static final int ADDITIONAL_BUFFER_SIZE = 200;

    // The index file name
    private static final String INDEX_FILENAME = "index.html";

    // The prefix to the HTTP header
    private static final String HTTP_PREFIX = "http://";

    // The end-of-line string
    private static final String EOL = "\r\n";

    // True when the handler has stopped
    private boolean done = false;

    // The Web server
    private VideoWebServer server = null;

    /**
     * Creats a new VideoWebHandler
     * @param server The server serving the images
     */
    public VideoWebHandler(VideoWebServer server) {
        this.server = server;
    }

    /**
     * Runs the thread in the pool
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        done = false;
        while (!done) {

            // Setup the socket
            Socket client = null;
            BufferedReader reader = null;
            PrintWriter writer = null;

            try {

                // Get a client
                client = server.nextClient();
                reader = new BufferedReader(new InputStreamReader(client
                        .getInputStream()));
                writer = new PrintWriter(client.getOutputStream());
                //System.err.println("Client connected from " + client);

                // Get the request
                String request = reader.readLine();
                //System.err.println("    Request: " + request);
                StringTokenizer tokens = new StringTokenizer(request, " ");
                String method = tokens.nextToken();
                String uri = tokens.nextToken();
                tokens.nextToken(); // Read the version

                // Fail if the method is not HTTP GET or POST
                if (!method.equals("GET") && !method.equals("POST")) {
                    writer.print("HTTP/1.1 405 Method Not Allowed" + EOL);
                    writer.print("Allow: GET POST" + EOL);
                    writer.print(CONNECTION_CLOSE_HEADER + EOL);
                    writer.print(EOL);
                    writer.flush();
                } else {

                    // Remote the http:// from the uri if present
                    if (uri.startsWith(HTTP_PREFIX)) {
                        uri = uri.substring(HTTP_PREFIX.length());
                        int slash = uri.indexOf('/');
                        if (slash == -1) {
                            uri = INDEX_FILENAME;
                        } else {
                            uri = uri.substring(slash + 1);
                        }
                    }

                    if (uri.equals("/") || uri.equals("")) {
                        if (server.getStreams().size() == 1) {
                            uri += "index.html";
                        } else {
                            uri += "streams.html";
                        }
                    } else if (uri.endsWith("/")) {
                        uri += INDEX_FILENAME;
                    }

                    // Remove a leading slash
                    if (uri.startsWith("/")) {
                        uri = uri.substring(1);
                    }

                    File uriFile = new File(uri);
                    String parent = uriFile.getParent();
                    String streamId = null;
                    if (parent != null) {
                        streamId = URLDecoder.decode(uriFile.getParent(),
                                "UTF8");
                    }
                    StreamUpdateListener stream = server.getStream(streamId,
                            null);
                    if ((stream == null) && !server.getStreams().isEmpty()) {
                        stream = server.getStreams().get(0);
                    }
                    if (stream != null) {
                        stream.addClient(client.getInetAddress());
                    }
                    String path = uriFile.getName();

                    if (path.startsWith("listStreams.xml")) {
                        writer.print(OK_RESPONSE + EOL);
                        writer.print("Content-Type: text/xml" + EOL);
                        writer.print(EOL);
                        writer.println("<?xml version=\"1.0\" "
                                + "encoding=\"UTF-8\" standalone=\"yes\"?>");
                        writer.println("<result>");
                        server.isNewStream(client.getInetAddress());
                        for (StreamUpdateListener listener
                                : server.getStreams()) {
                            String name = listener.getName();
                            if (name != null) {
                                writer.println("<stream name=\"" + name + "\""
                                    + " id=\""
                                    + URLEncoder.encode(listener.getId(),
                                            "UTF8")
                                    + "\"/>");
                            }
                        }
                        writer.println("</result>");
                        writer.flush();
                    } else if (path.startsWith("image.jpg")) {
                        writer.print(OK_RESPONSE + EOL);
                        writer.print("Content-Type: image/jpeg" + EOL);
                        writer.print(EOL);
                        writer.flush();
                        byte[] imageData = stream.getImageData();
                        synchronized (server) {
                            client.setSendBufferSize(imageData.length
                                    + ADDITIONAL_BUFFER_SIZE);
                            client.getOutputStream().write(imageData);
                        }
                    } else if (path.startsWith("isChange.xml")) {
                        writer.print(OK_RESPONSE + EOL);
                        writer.print("Content-Type: text/xml" + EOL);
                        writer.print(EOL);
                        writer.println("<?xml version=\"1.0\" "
                                + "encoding=\"UTF-8\" standalone=\"yes\"?>");
                        writer.println("<result>");
                        writer.print("<isNewImage>");
                        if (stream.waitForImageUpdate(
                                client.getInetAddress())) {
                            writer.print("true");
                        } else {
                            writer.print("false");
                        }
                        writer.println("</isNewImage>");
                        writer.println("</result>");
                        writer.flush();
                    } else {

                        // Try to find the file requested
                        InputStream input = getClass().getResourceAsStream(path);

                        // If the file was not found, send a not found response
                        if (input == null) {
                            writer.print("HTTP:/1.1 404 File Not Found" + EOL);
                            writer.print(CONNECTION_CLOSE_HEADER + EOL);
                            writer.print(EOL);
                            writer.flush();
                        } else {
                            System.err.println("Found " + path);
                            writer.print(OK_RESPONSE + EOL);
                            writer.print(CONNECTION_CLOSE_HEADER + EOL);
                            writer.print(NO_CACHE_HEADER + EOL);
                            writer.print(NO_CACHE_PRAGMA_HEADER + EOL);
                            writer.print(IMMEDIATE_EXPIRY_HEADER + EOL);
                            writer.print(HTML_CONTENT_TYPE + EOL);
                            writer.print(EOL);

                            BufferedReader fileReader = new BufferedReader(
                                    new InputStreamReader(input));
                            String line = "";
                            while (line != null) {
                                line = fileReader.readLine();
                                if (line != null) {
                                    writer.println(line);
                                }
                            }
                            writer.flush();
                        }
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                if (writer != null) {
                    writer.print(BAD_REQUEST_RESPONSE + EOL);
                    writer.print(CONNECTION_CLOSE_HEADER + EOL);
                    writer.print(EOL);
                    writer.flush();
                }
            } catch (NoSuchElementException e) {
                e.printStackTrace();
                if (writer != null) {
                    writer.print(BAD_REQUEST_RESPONSE + EOL);
                    writer.print(CONNECTION_CLOSE_HEADER + EOL);
                    writer.print(EOL);
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Close the reader, writer and client
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops this thread from executing
     */
    public void end() {
        done = true;
    }
}