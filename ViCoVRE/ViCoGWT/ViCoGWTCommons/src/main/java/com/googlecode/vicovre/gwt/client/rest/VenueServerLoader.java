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

package com.googlecode.vicovre.gwt.client.rest;

import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class VenueServerLoader extends AbstractRestCall {

    private VenueServerReceiver receiver = null;

    private String url = null;

    public static void load(VenueServerReceiver receiver, String url) {
        VenueServerLoader loader = new VenueServerLoader(receiver, url);
        loader.go();
    }

    public VenueServerLoader(VenueServerReceiver receiver, String url) {
        this.receiver = receiver;
        this.url = url + "venue/servers";
    }

    public void go() {
        go(url);
    }

    protected void onError(String message) {
        receiver.failedToGetVenueServers(message);
    }

    protected void onSuccess(Response response) {
        JsonRepresentation representation = response.getEntityAsJson();
        JSONObject object = representation.getValue().isObject();
        if ((object != null) && (object.isNull() == null)) {
            JSONValue serverValue = object.get("server");
            if (serverValue != null) {
                JSONArray servers = serverValue.isArray();
                if (servers != null) {
                    for (int i = 0; i < servers.size(); i++) {
                        JSONString server = servers.get(i).isString();
                        receiver.addVenueServer(server.stringValue());
                    }
                } else {
                    onError("Server is not an array");
                    return;
                }
            }
        }
        receiver.finishedAddingVenueServers();
    }
}
