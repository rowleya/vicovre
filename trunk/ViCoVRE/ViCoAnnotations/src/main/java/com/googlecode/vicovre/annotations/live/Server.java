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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.googlecode.vicovre.repositories.liveAnnotation.LiveAnnotationTypeRepository;

public class Server extends Thread {

    private LinkedList<Message> queue = new LinkedList<Message>();

    private LinkedList<Message> history = new LinkedList<Message>();

    private HashMap<String, Client> clients = new HashMap<String, Client>();

    private HashSet<String> usernames = new HashSet<String>();

    private File storeDirectory = null;

    private LiveAnnotationTypeRepository liveAnnotationTypeRepository = null;

    private boolean done = false;

    public Server(LiveAnnotationTypeRepository liveAnnotationTypes) {
        this.liveAnnotationTypeRepository = liveAnnotationTypes;
        Runtime.getRuntime().addShutdownHook(new DoShutdown());
        start();
    }

    public LiveAnnotationTypeRepository getLiveAnnotationTypeRepository() {
        return liveAnnotationTypeRepository;
    }

    public void setStoreDirectory(File storeDirectory) {
        this.storeDirectory = storeDirectory;
    }

    public void run() {
        done = false;
        while (!done) {
            processMessages();
        }
    }

    public Client createClient(String name, String email)
            throws NameInUseException {
        Client client = new Client(this, storeDirectory, name, email);
        synchronized (clients) {
            if (clients.containsKey(email)) {
                throw new NameInUseException();
            }
            if (usernames.contains(name)) {
                throw new NameInUseException();
            }
            clients.put(email, client);
            usernames.add(name);
            addMessage(new AddUserMessage(client));
            synchronized (history) {
                for (Message annotation : history) {
                    client.addMessage(annotation);
                }
            }
        }
        return client;
    }

    public void deleteClient(Client client) {
        synchronized (clients) {
            clients.remove(client.getEmail());
            usernames.remove(client.getName());
        }
        addMessage(new DeleteUserMessage(client));
    }

    public void closeAll() {
        synchronized (queue) {
            done = true;
            queue.notifyAll();
            synchronized (clients) {
                for (Client client : clients.values()) {
                    client.close();
                }
            }
        }
    }

    public void addMessage(Message message) {
        synchronized (queue) {
            if (!done) {
                queue.addLast(message);
                queue.notifyAll();
            }
        }
    }

    private void processMessages() {
        Message message = null;
        synchronized (queue) {
           if (!done && queue.isEmpty()) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            if (!queue.isEmpty()) {
                message = queue.removeFirst();
            }
            queue.notifyAll();
        }
        if (message != null) {
            if (message.isPrivate()) {
                synchronized (clients) {
                    message.getClient().addMessage(message);
                }
            } else {
                synchronized (history) {
                    history.add(message);
                }
                synchronized (clients) {
                    for (Client client : clients.values()) {
                        client.addMessage(message);
                    }
                }
            }
        }
    }

    public LinkedList<Message> getHistory() {
        return history;
    }

    private class DoShutdown extends Thread {
        public void run() {
            closeAll();
        }
    }

}
