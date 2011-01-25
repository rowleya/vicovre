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

import java.io.IOException;

import org.restlet.client.Client;
import org.restlet.client.Request;
import org.restlet.client.Response;
import org.restlet.client.Uniform;
import org.restlet.client.data.Cookie;
import org.restlet.client.data.MediaType;
import org.restlet.client.data.Method;
import org.restlet.client.data.Preference;
import org.restlet.client.data.Protocol;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public abstract class AbstractRestCall implements Uniform {

    private WaitPopup waitPopup = null;

    protected void go(String url, Method method, MediaType mediaType) {
        Client client = new Client(Protocol.HTTP);
        Request request = new Request(method, url);
        String sessionid = Cookies.getCookie("JSESSIONID");
        if (sessionid != null) {
            request.getCookies().add(new Cookie("JSESSIONID", sessionid));
        }
        request.getClientInfo().getAcceptedMediaTypes().add(
                new Preference<MediaType>(mediaType));
        client.handle(request, this);
    }

    public void handle(Request request, Response response) {
        try {
            if (waitPopup != null) {
                waitPopup.hide();
            }
            if (response.getStatus().isSuccess()) {
                onSuccess(response);
            } else {
                String message = null;
                try {
                    if ((response.getEntity() != null)
                            && (response.getEntity().getMediaType() != null)
                            && response.getEntity().getMediaType().equals(
                                MediaType.TEXT_PLAIN)) {
                        message = response.getEntity().getText();
                    } else {
                        message = response.getStatus().getDescription();
                    }
                } catch (IOException e) {
                    message = response.getStatus().getDescription();
                }
                onError(response.getStatus().getCode() + ": " + message);
            }
        } catch (Throwable e) {
            GWT.log("Error", e);
            onError("Error: " + e.getMessage());
        }
    }

    protected abstract void onError(String message);

    protected abstract void onSuccess(Response response);

    protected void displayError(String error) {
        MessagePopup popup = new MessagePopup(error, null, MessagePopup.ERROR,
                MessageResponse.OK);
        popup.center();
    }

    protected void displayWaitMessage(String message, boolean cancellable) {
        waitPopup = new WaitPopup(message, cancellable);
        waitPopup.center();
    }

    protected boolean wasCancelled() {
        if (waitPopup != null) {
            return waitPopup.wasCancelled();
        }
        return false;
    }
}
