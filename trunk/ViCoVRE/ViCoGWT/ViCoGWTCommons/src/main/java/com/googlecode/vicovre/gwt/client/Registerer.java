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

package com.googlecode.vicovre.gwt.client;

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class Registerer extends AbstractVoidRestCall {

    private String url = null;

    private WaitPopup waitPopup = new WaitPopup("Registering", true);

    public static void register(String url, String username,
            String password, String successUrl) {
        Registerer registerer = new Registerer(url, username, password,
                successUrl);
        registerer.go();
    }

    public Registerer(String url, String username,
            String password, String successUrl) {
        this.url = url + "user/" + URL.encodeComponent(username)
            + "?password=" + URL.encodeComponent(password)
            + "&successUrl=" + URL.encodeComponent(successUrl);
    }

    public void go() {
        waitPopup.center();
        go(url, Method.PUT);
    }

    protected void onError(String message) {
        if (!waitPopup.wasCancelled()) {
            waitPopup.hide();
            MessagePopup popup = new MessagePopup(
                    "Error registering: " + message,
                    null, MessagePopup.ERROR, MessageResponse.OK);
            popup.center();
        }
    }

    protected void onSuccess() {
        if (!waitPopup.wasCancelled()) {
            waitPopup.hide();
            MessagePopup popup = new MessagePopup(
                    "Please check your e-mail to complete the registration",
                    null, MessagePopup.INFO, MessageResponse.OK);
            popup.center();
        }
    }

}
