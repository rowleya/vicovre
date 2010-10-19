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

package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.WaitPopup;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.DefaultLayoutPopup;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONStreamsMetadata;

public class DefaultLayoutLoader extends AbstractRestCall {

    private DefaultLayoutPopup popup = null;

    private String url = null;

    private WaitPopup waitPopup = new WaitPopup("Loading streams...", true);

    public static void loadLayouts(String folder, DefaultLayoutPopup popup,
            String url) {
        DefaultLayoutLoader layoutLoader = new DefaultLayoutLoader(folder,
                popup, url);
        layoutLoader.go();
    }

    public DefaultLayoutLoader(String folder, DefaultLayoutPopup popup,
            String url) {
        this.popup = popup;
        this.url = url + "folders" + folder;
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += "streams";
    }

    public void go() {
        waitPopup.center();
        GWT.log("Loading default layout from " + url);
        go(url);
    }

    protected void onError(String message) {
        waitPopup.hide();
        if (!waitPopup.wasCancelled()) {
            MessagePopup popup = new MessagePopup(
                    "Error loading streams: " + message,
                    null, MessagePopup.ERROR, MessageResponse.OK);
            popup.center();
        }
    }

    protected void onSuccess(Response response) {
        waitPopup.hide();
        if (!waitPopup.wasCancelled()) {
            JsonRepresentation representation = response.getEntityAsJson();
            JSONValue object = representation.getValue();
            if ((object != null) && (object.isNull() == null)) {
                JSONStreamsMetadata streamsObject =
                    JSONStreamsMetadata.parse(object.toString());
                popup.setStreams(streamsObject.getStreams());
                popup.center();
            } else {
                MessagePopup popup = new MessagePopup(
                        "No streams found!",
                        null, MessagePopup.ERROR, MessageResponse.OK);
                popup.center();
            }
        }
    }
}
