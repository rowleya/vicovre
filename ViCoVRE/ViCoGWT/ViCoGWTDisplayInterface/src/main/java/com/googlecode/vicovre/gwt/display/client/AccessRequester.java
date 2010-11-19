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

package com.googlecode.vicovre.gwt.display.client;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.WaitPopup;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;

public class AccessRequester extends AbstractVoidRestCall {

    private String url = null;

    private String baseUrl = null;

    private WaitPopup waitPopup = new WaitPopup("Requesting", true);

    public static void requestAccess(String baseUrl, String url,
            String folder, String recordingId, String emailAddress) {
        AccessRequester requester = new AccessRequester(baseUrl, url, folder,
                recordingId, emailAddress);
        requester.go();
    }

    public AccessRequester(String baseUrl, String url,
            String folder, String recordingId, String emailAddress) {
        waitPopup.setBaseUrl(baseUrl);
        this.baseUrl = baseUrl;
        this.url = url + "recording" + folder + "/" + recordingId
            + "/requestAccess?emailAddress="
            + URL.encodeComponent(emailAddress);
    }

    public void go() {
        waitPopup.center();
        go(url);
    }

    protected void onError(String error) {
        waitPopup.hide();
        MessagePopup popup = new MessagePopup("Error sending request: " + error,
                null, baseUrl + MessagePopup.ERROR, MessageResponse.OK);
        popup.center();
    }

    protected void onSuccess() {
        waitPopup.hide();
        MessagePopup popup = new MessagePopup("Your request has been sent.\n"
                + " There is no guarentee that you will receive a reply to"
                + " this request.",
                null, baseUrl + MessagePopup.INFO);
        popup.center();
    }

}
