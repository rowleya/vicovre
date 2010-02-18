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
import java.util.Vector;

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.MediaType;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Preference;
import org.restlet.gwt.data.Protocol;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.data.Status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.WaitPopup;

public class LiveAnnotationTypeReceiver extends Callback {

    private Application application = null;

    private WaitPopup waitPopup = new WaitPopup("Loading Live Annotation Types",
            false);

    public static void getLiveAnnotations(Application application) {
        LiveAnnotationTypeReceiver receiver = new LiveAnnotationTypeReceiver(
                application);
        receiver.go();
    }

    public LiveAnnotationTypeReceiver(Application application) {
        this.application = application;
    }

    public void go() {
        waitPopup.show();
        Client client = new Client(Protocol.HTTP);
        String url = application.getUrl();
        url += "annotations/types";
        GWT.log("URL = " + url, null);
        Request request = new Request(Method.GET, url);
        request.getClientInfo().getAcceptedMediaTypes().add(
                new Preference<MediaType>(MediaType.TEXT_XML));
        client.handle(request, this);
    }


    public void onEvent(Request request, Response response) {
        waitPopup.hide();
        if (!response.getStatus().equals(Status.SUCCESS_OK)) {
            String errorMessage = "Error setting up client "
                + response.getStatus().getCode() + ": "
                + response.getStatus().getDescription();
            MessagePopup error = new MessagePopup(errorMessage,
                    null, MessagePopup.ERROR);
            error.center();
        } else {
            Vector<LiveAnnotationType> laTypes =
                new Vector<LiveAnnotationType>();
            Document doc = response.getEntityAsXml().getDocument();
            NodeList types = doc.getElementsByTagName("type");
            for (int i = 0; i < types.getLength(); i++) {
                Node type = types.item(i);

                String name =
                    type.getAttributes().getNamedItem("name").getNodeValue();
                String image = null;
                String visible = null;
                HashMap<String, HashMap<String, String>> fields =
                    new HashMap<String, HashMap<String,String>>();

                NodeList children = type.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeName().equals("image")) {
                        image = child.getChildNodes().item(0).getNodeValue();
                    } else if (child.getNodeName().equals("visible")) {
                        visible = child.getChildNodes().item(0).getNodeValue();
                    } else if (child.getNodeName().equals("field")) {
                        HashMap<String, String> values =
                            new HashMap<String, String>();
                        NamedNodeMap attributes = child.getAttributes();
                        for (int k = 0; k < attributes.getLength(); k++) {
                            Node attribute = attributes.item(k);
                            values.put(attribute.getNodeName(),
                                    attribute.getNodeValue());
                        }
                        String fieldName = values.get("name");
                        fields.put(fieldName, values);
                    }
                }

                LiveAnnotationType laType = new LiveAnnotationType(application,
                        name, image, visible, fields);
                laTypes.add(laType);
            }
            application.setupInterface(laTypes);
        }
    }

}
