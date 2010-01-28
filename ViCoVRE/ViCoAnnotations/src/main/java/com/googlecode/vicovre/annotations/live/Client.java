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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import com.googlecode.vicovre.annotations.LiveAnnotation;
import com.googlecode.vicovre.annotations.LiveAnnotationClient;
import com.googlecode.vicovre.annotations.LiveAnnotationEvent;

public class Client {

    private LinkedList<LiveAnnotationEvent> queue =
        new LinkedList<LiveAnnotationEvent>();

    private boolean done = false;

    private LiveAnnotationClient clientDetails = null;

    private static final int WAIT_TIME = 10000;

    private Server annotationServer = null;

    /**
     * Creates a new Client
     *
     */
    public Client(Server server, String name, String email) {
        annotationServer = server;
        this.clientDetails = new LiveAnnotationClient(name, email);
        if (annotationServer != null) {
            annotationServer.addClient(this);
        }
    }

    // Gets all the messages in the queue
    private LiveAnnotationEvent getAnnotation() {
        LiveAnnotationEvent annotation = null;
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
    public void addAnnotation(LiveAnnotationEvent annotation) {
        synchronized (queue) {
            queue.addLast(annotation);
            queue.notifyAll();
        }
    }

    public String getClientUserName() {
        return clientDetails.getName();
    }

    public LiveAnnotationClient getClientDetails() {
        return clientDetails;
    }

    public Vector<String> getSessions() {
        Vector<String> sessions = new Vector<String>();
        return sessions;
    }

    /**
     * Waits until the messages are available and then gets them
     *
     * @return The messages in the queue
     */
    public String getMessage() {
        Client client = null;
        String out = "<type>None</type>";
        LiveAnnotationEvent ann = getAnnotation();
        if (ann != null) {
            out = "<type>" + ann.getClass().getSimpleName() + "</type>";
            out += "<displayName>" + ann.getIdent() + "</displayName>";
            if (ann.getClass() == LiveAnnotation.class) {
                client = (Client) ((LiveAnnotation) ann).getClient();
                out += "<messageId>"
                        + ((LiveAnnotation) ann).getAnnotationId()
                        + "</messageId>";
                if (client == this) {
                    out += "<submittingClient>true</submittingClient>";
                }
                try {
                    out += "<crew_annotation>" + StringEscapeUtils.escapeXml(
                                ((LiveAnnotation) ann).getToolText()) + "</crew_annotation>";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            out += "<colour>" + annotationServer.getColour(ann.getIdent()) + "</colour>";
            out += ann.toXml();
        }
        return out;
    }

    /**
     * Closes the connection to the server
     */
    public void close() {
        done = true;
        clientDetails.setEvent("remove");
        annotationServer.addAnnotation(clientDetails);
        annotationServer.close(this);
    }

    public void setMessage(LiveAnnotation annotation) {
        annotation.setAnnotator(clientDetails.getName());
        annotation.setAuthor(clientDetails.getEmail());
        annotation.setClient(this);
        annotationServer.addAnnotation(annotation);
    }

}
