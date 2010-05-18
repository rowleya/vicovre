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

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Protocol;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.data.Status;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.VenuePanel;

public class TransmitStreamSender extends Callback {

    private String url = null;

    private String sessionId = null;

    private String streamId = null;

    private String substreamId = null;

    private VenuePanel venuePanel = null;

    public static void send(String url, String sessionId, String streamId,
            String substreamId, VenuePanel venuePanel) {
        TransmitStreamSender sender = new TransmitStreamSender(url, sessionId,
                streamId, substreamId, venuePanel);
        sender.go();
    }

    public TransmitStreamSender(String url, String sessionId,  String streamId,
            String substreamId,	VenuePanel venuePanel) {
        this.url = url;
        this.sessionId = sessionId;
        this.streamId = streamId;
        this.substreamId = substreamId;
        this.venuePanel = venuePanel;
    }

    public void go() {
        Client client = new Client(Protocol.HTTP);
        url += "export/" + URL.encodeComponent(sessionId) + "/"
            + URL.encodeComponent(streamId);
        if (substreamId != null) {
            url += "?substreamid=" + substreamId;
        }
        String venue = venuePanel.getVenue();
        String[] addresses = venuePanel.getAddresses();
        if ((venue != null) && !venue.equals("")) {
            url += "&venue=" + URL.encodeComponent(venue);
        } else if ((addresses != null) && (addresses.length > 0)) {
            String[] addressParts = addresses[0].split("/");
            url += "&address=" + URL.encodeComponent(addressParts[0]);
            url += "&port=" + URL.encodeComponent(addressParts[1]);
            url += "&ttl=" + URL.encodeComponent(addressParts[2]);
        } else {
            MessagePopup error = new MessagePopup("You must choose a venue",
                    null, MessagePopup.ERROR, MessageResponse.OK);
            error.center();
            return;
        }
        Request request = new Request(Method.POST, url);
        client.handle(request, this);
    }

    public void onEvent(Request request, Response response) {
        if (response.getStatus().equals(Status.SUCCESS_OK)) {
            // Do Nothing
        } else {
            String errorMessage = "Error transmitting: "
                + response.getStatus().getCode() + ": "
                + response.getStatus().getDescription();
            MessagePopup error = new MessagePopup(errorMessage,
                    null, MessagePopup.ERROR, MessageResponse.OK);
            error.center();
        }
    }

}
