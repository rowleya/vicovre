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

import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Response;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.client.WaitPopup;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;

public class PasswordSetter extends AbstractRestCall {

    private String baseUrl = null;

    private String url = null;

    private ModalPopup<? extends Widget> popup = null;

    private WaitPopup waitPopup = new WaitPopup("Setting Password", true);

    public static void setPassword(String baseUrl, String url,
            ModalPopup<? extends Widget> popup, String oldPassword,
            String newPassword) {
        PasswordSetter setter = new PasswordSetter(baseUrl, url, popup,
                oldPassword, newPassword);
        setter.go();
    }

    public PasswordSetter(String baseUrl, String url,
            ModalPopup<? extends Widget> popup, String oldPassword,
            String newPassword) {
        waitPopup.setBaseUrl(baseUrl);
        this.baseUrl = baseUrl;
        this.popup = popup;
        this.url = url + "user/password?oldPassword="
            + URL.encodeComponent(oldPassword)
            + "&password=" + URL.encodeComponent(newPassword);
    }

    public void go() {
        waitPopup.center();
        go(url, Method.PUT);
    }

    protected void onError(String message) {
        waitPopup.hide();
        MessagePopup popup = new MessagePopup(
                "Error setting password: " + message, null,
                baseUrl + MessagePopup.ERROR, MessageResponse.OK);
        popup.center();
    }

    protected void onSuccess(Response response) {
        waitPopup.hide();
        popup.hide();
    }

}