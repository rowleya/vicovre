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

import java.util.LinkedList;
import java.util.List;

import org.restlet.client.data.Method;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.client.WaitPopup;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;

public class PermissionSetter extends AbstractVoidRestCall {

    private String url = null;

    private String baseUrl = null;

    private ModalPopup<? extends Widget> popup = null;

    private WaitPopup waitPopup = new WaitPopup("Setting Permissions", true);

    private LinkedList<String> queries = new LinkedList<String>();

    public static void setPermissions(String baseUrl, String url, String folder,
            String recordingId, String[] aclTypes,
            ModalPopup<? extends Widget> popup, boolean[] allow,
            List<String>[] exceptionTypes, List<String>[] exceptions) {
        PermissionSetter setter = new PermissionSetter(baseUrl, url, folder,
            recordingId, aclTypes, popup, allow, exceptionTypes, exceptions);
        setter.go();
    }

    public PermissionSetter(String baseUrl, String url, String folder,
            String recordingId, String[] aclTypes,
            ModalPopup<? extends Widget> popup,
            boolean[] allow, List<String>[] exceptionTypes,
            List<String>[] exceptions) {
        waitPopup.setBaseUrl(baseUrl);
        this.baseUrl = baseUrl;
        this.url = url + "recording" + folder + "/" + recordingId + "/acl/";
        for (int i = 0; i < aclTypes.length; i++) {
             String query = aclTypes[i] + "?public=" + allow[i];
             if (exceptionTypes[i] != null && exceptions[i] != null) {
                 for (int j = 0; j < exceptionTypes[i].size(); j++) {
                     query += "&exceptionType=" + URL.encodeComponent(
                             exceptionTypes[i].get(j));
                     query += "&exceptionName=" + URL.encodeComponent(
                             exceptions[i].get(j));
                 }
             }
             queries.addLast(query);
        }

        this.popup = popup;
    }

    public void go() {
        if (!waitPopup.isShowing()) {
            waitPopup.center();
        }
        String requestUrl = url + queries.removeFirst();
        GWT.log("URL = " + requestUrl);
        go(requestUrl, Method.PUT);
    }

    protected void onError(String message) {
        waitPopup.hide();
        MessagePopup popup = new MessagePopup(
                "Error setting permissions: " + message, null,
                baseUrl + MessagePopup.ERROR, MessageResponse.OK);
        popup.center();
    }

    protected void onSuccess() {
        if (queries.isEmpty()) {
            waitPopup.hide();
            if (popup != null) {
                popup.hide();
            }
        } else {
            go();
        }
    }

}
