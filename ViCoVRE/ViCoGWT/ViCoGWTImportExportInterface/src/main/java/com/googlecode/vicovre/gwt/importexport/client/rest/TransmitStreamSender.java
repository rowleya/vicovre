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

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.rest.AbstractPlainRestCall;
import com.googlecode.vicovre.gwt.client.venue.VenuePanel;
import com.googlecode.vicovre.gwt.importexport.client.StreamPanel;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;

public class TransmitStreamSender extends AbstractPlainRestCall {

    private String url = null;

    private String sessionId = null;

    private String streamId = null;

    private String substreamId = null;

    private VenuePanel venuePanel = null;

    private StreamPanel streamPanel = null;

    public static void send(String url, String sessionId, String streamId,
            String substreamId, VenuePanel venuePanel,
            StreamPanel streamPanel) {
        TransmitStreamSender sender = new TransmitStreamSender(url, sessionId,
                streamId, substreamId, venuePanel, streamPanel);
        sender.go();
    }

    public TransmitStreamSender(String url, String sessionId,  String streamId,
            String substreamId,	VenuePanel venuePanel,
            StreamPanel streamPanel) {
        this.url = url;
        this.sessionId = sessionId;
        this.streamId = streamId;
        this.substreamId = substreamId;
        this.venuePanel = venuePanel;
        this.streamPanel = streamPanel;
    }

    public void go() {
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
        go(url, Method.POST);
    }

    protected void onSuccess(String text) {
         streamPanel.transmitStarted(text);
    }

    protected void onError(String message) {
        streamPanel.transmitFailed();
        MessagePopup error = new MessagePopup(message,
                null, MessagePopup.ERROR, MessageResponse.OK);
        error.center();
    }

}
