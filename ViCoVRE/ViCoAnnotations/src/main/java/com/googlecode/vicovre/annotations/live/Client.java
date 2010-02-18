/*
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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
 *
 */

package com.googlecode.vicovre.annotations.live;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.Timer;


import com.googlecode.vicovre.annotations.LiveAnnotation;

public class Client implements ActionListener {

    private static final int WAIT_TIME = 10000;

    private String name = null;

    private String email = null;

    private LinkedList<Message> queue = new LinkedList<Message>();

    private boolean done = false;

    private Server server = null;

    private Integer storeSync = new Integer(0);

    private PrintWriter storeWriter = null;

    private Timer timer = null;

    /**
     * Creates a new Client
     *
     */
    protected Client(Server server, File storeDirectory, String name,
            String email) {
        this.server = server;
        this.name = name;
        this.email = email;
        if (storeDirectory != null) {
            try {
                this.storeWriter = new PrintWriter(
                        getStoreFile(storeDirectory));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        timer = new Timer(WAIT_TIME * 2, this);
        timer.start();
    }

    private File getStoreFile(File storeDirectory) {
        File file = new File(storeDirectory,
                email.replace("@", "_at_") + "_annotations.xml");
        file.getParentFile().mkdirs();
        return file;
    }

    protected void setStoreDirectory(File storeDirectory) {
        synchronized (storeSync) {
            if (storeDirectory != null) {
                try {
                    this.storeWriter = new PrintWriter(
                            getStoreFile(storeDirectory));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                this.storeWriter = null;
            }
        }
    }

    // Gets all the messages in the queue
    public Message getNextMessage() {
        timer.restart();
        Message annotation = new NoMessage();
        synchronized (queue) {
            if (!done && queue.isEmpty()) {
                try {
                    queue.wait(WAIT_TIME);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            if (!queue.isEmpty()) {
                annotation = queue.removeFirst();
            }
        }
        return annotation;
    }

    // Adds a message to the queue
    public void addMessage(Message message) {
        synchronized (queue) {
            queue.addLast(message);
            queue.notifyAll();
        }
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Vector<String> getSessions() {
        Vector<String> sessions = new Vector<String>();
        return sessions;
    }

    /**
     * Closes the connection to the server
     */
    public void close() {
        timer.stop();
        done = true;
        synchronized (storeSync) {
            if (storeWriter != null) {
                storeWriter.close();
            }
        }
        server.deleteClient(this);
    }

    public void setMessage(LiveAnnotation annotation) {
        annotation.setAuthor(email);
        synchronized (storeSync) {
            if (storeWriter != null) {
                storeWriter.println(annotation.toXml());
            }
        }
        server.addMessage(new AddAnnotationMessage(this, annotation));
    }

    public void actionPerformed(ActionEvent e) {
        close();
    }

}
