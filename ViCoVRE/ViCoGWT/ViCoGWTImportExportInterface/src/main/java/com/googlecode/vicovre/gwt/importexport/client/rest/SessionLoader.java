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

package com.googlecode.vicovre.gwt.importexport.client.rest;

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
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.importexport.client.ExportPanel;
import com.googlecode.vicovre.gwt.importexport.client.Stream;

public class SessionLoader extends Callback {

    private ExportPanel panel = null;

    private String url = null;

    private String id = null;

    public static void loadSession(ExportPanel panel, String url, String id) {
        SessionLoader loader = new SessionLoader(panel, url, id);
        loader.go();
    }

    public SessionLoader(ExportPanel panel, String url, String id) {
        this.panel = panel;
        this.url = url;
        this.id = id;
    }

    public void go() {
        Client client = new Client(Protocol.HTTP);
        url += "export/" + id + "/list";
        GWT.log("URL = " + url);
        Request request = new Request(Method.GET, url);
        request.getClientInfo().getAcceptedMediaTypes().add(
                new Preference<MediaType>(MediaType.APPLICATION_JSON));
        client.handle(request, this);
    }

    public void onEvent(Request request, Response response) {
        if (response.getStatus().equals(Status.SUCCESS_OK)) {
            JsonRepresentation representation = response.getEntityAsJson();
            GWT.log(representation.getText());
            if (representation.getValue().isNull() != null) {
                panel.setStreams(null);
            } else {
                Vector<Stream> streams = new Vector<Stream>();
                JSONObject object = representation.getValue().isObject();
                if (object != null) {
                    JSONValue value = object.get("stream");
                    JSONArray streamsArray = value.isArray();
                    JSONObject streamObject = value.isObject();
                    if (streamsArray != null) {
                        for (int i = 0; i < streamsArray.size(); i++) {
                            JSONObject stream = streamsArray.get(i).isObject();
                            if (stream != null) {
                                streams.add(new Stream(stream));
                            }
                        }
                    } else if (streamObject != null) {
                        streams.add(new Stream(streamObject));
                    }
                }
                panel.setStreams(streams.toArray(new Stream[0]));
            }
        } else {
            String errorMessage = "Error loading session "
                + response.getStatus().getCode() + ": "
                + response.getStatus().getDescription();
            MessagePopup error = new MessagePopup(errorMessage,
                    null, MessagePopup.ERROR, MessageResponse.OK);
            error.center();
        }
    }

}
