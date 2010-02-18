/**
 * Copyright (c) 2009, University of Manchester
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
 * 3) Neither the name of the and the University of Manchester nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
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

package com.googlecode.vicovre.gwt.annotations.client;

import java.util.HashMap;

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.MediaType;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Preference;
import org.restlet.gwt.data.Protocol;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.data.Status;
import org.restlet.gwt.resource.XmlRepresentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;

public class MessageReceiver extends Callback
        implements MessageResponseHandler {

    private Application application = null;

    private boolean done = false;

    private Client client = new Client(Protocol.HTTP);

    public MessageReceiver(Application application) {
        this.application = application;
    }

    public void start() {
        done = false;
        getNextMessage();
    }

    public void stop() {
        done = true;
    }

    private void getNextMessage() {
        String url = application.getUrl();
        url += "annotations/get";
        Request request = new Request(Method.GET, url);
        request.getClientInfo().getAcceptedMediaTypes().add(
                new Preference<MediaType>(MediaType.TEXT_XML));
        client.setConnectTimeout(20000);
        client.handle(request, this);
        GWT.log("Getting messages...", null);
    }

    private String getNodeText(Node node) {
        return node.getChildNodes().item(0).getNodeValue();
    }

    public void onEvent(Request request, Response response) {
        if (!done) {
            GWT.log("Message received " + response.getStatus(), null);
            if (response.getStatus().equals(Status.SUCCESS_OK)) {
                XmlRepresentation xml = response.getEntityAsXml();
                Document doc = xml.getDocument();
                String type = getNodeText(
                        doc.getElementsByTagName("type").item(0));
                GWT.log("Message, type = " + type, null);
                if (type.equals("AddUser")) {
                    String name = getNodeText(
                            doc.getElementsByTagName("name").item(0));
                    String email = getNodeText(
                            doc.getElementsByTagName("email").item(0));
                    User user = new User(name, email);
                    application.addUser(user);
                } else if (type.equals("DeleteUser")) {
                    String email = getNodeText(
                            doc.getElementsByTagName("email").item(0));
                    application.removeUser(email);
                } else if (type.equals("AddAnnotation")) {
                    NodeList annotations =
                        doc.getElementsByTagName("annotation");
                    String html = getNodeText(
                            doc.getElementsByTagName("html").item(0));
                    for (int i = 0; i < annotations.getLength(); i++) {
                        Node annotation = annotations.item(i);
                        NodeList children = annotation.getChildNodes();
                        String annotationType = null;
                        String id = null;
                        String author = null;
                        String timestamp = null;
                        HashMap<String, String> body =
                            new HashMap<String, String>();
                        for (int j = 0; j < children.getLength(); j++) {
                            Node child = children.item(j);
                            String name = child.getNodeName();
                            String value = getNodeText(child);
                            if (name.equals("type")) {
                                annotationType = value;
                            } else if (name.equals("id")) {
                                id = value;
                            } else if (name.equals("author")) {
                                author = value;
                            } else if (name.equals("timestamp")) {
                                timestamp = value;
                            } else {
                                body.put(name, value);
                            }
                        }
                        Annotation ann = new Annotation(application, id,
                                annotationType, author, timestamp, body, html);
                        application.addAnnotation(ann);
                    }
                }

                if (!type.equals("Done")) {
                    getNextMessage();
                } else {
                    application.close();
                }
            } else {
                String errorMessage = "Error receiving messages "
                    + response.getStatus().getCode() + ": "
                    + response.getStatus().getDescription() + "\n"
                    + "Do you want to retry?  If not, the client will stop!";
                MessagePopup error = new MessagePopup(errorMessage,
                        this, MessagePopup.ERROR, MessageResponse.YES,
                        MessageResponse.NO);
                error.center();
            }
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.YES) {
            getNextMessage();
        } else {
            application.close();
        }
    }

}
