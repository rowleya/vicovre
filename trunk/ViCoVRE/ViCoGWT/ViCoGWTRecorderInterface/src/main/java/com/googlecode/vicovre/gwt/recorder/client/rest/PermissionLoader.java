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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONObject;
import com.googlecode.vicovre.gwt.client.json.JSONACL;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.PermissionPopup;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class PermissionLoader extends AbstractJSONRestCall {

    private String baseUrl = null;

    private String url = null;

    private String folder = null;

    private String recordingId = null;

    private JsArrayString users = null;

    private JsArrayString groups = null;

    private JSONACL readAcl = null;

    private JSONACL playAcl = null;

    private WaitPopup waitPopup = new WaitPopup("Loading Existing Permissions",
            true);

    public static void load(String url, String folder,
            String recordingId, JsArrayString users, JsArrayString groups) {
        PermissionLoader loader = new PermissionLoader(url, folder, recordingId,
                users, groups);
        loader.go();
    }

    public PermissionLoader(String url, String folder,
            String recordingId, JsArrayString users, JsArrayString groups) {
        super(true);
        this.baseUrl = url;
        this.url = url + "recording" + folder;
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += recordingId + "/acl/";
        this.folder = folder;
        this.recordingId = recordingId;
        this.users = users;
        this.groups = groups;
    }

    public void go() {
        if (readAcl == null) {
            waitPopup.center();
            String url = this.url + "read";
            GWT.log("Permission load url = " + url);
            go(url);
        } else if (playAcl == null) {
            String url = this.url + "play";
            GWT.log("Permission load url = " + url);
            go(url);
        }
    }

    protected void onSuccess(JSONObject object) {
        if (!waitPopup.wasCancelled()) {
            if (readAcl == null) {
                readAcl = JSONACL.parse(object.toString());
                go();
            } else {
                waitPopup.hide();
                playAcl = JSONACL.parse(object.toString());
                PermissionPopup popup = new PermissionPopup(baseUrl, folder,
                        recordingId, users, groups, playAcl, readAcl);
                popup.center();
            }
        }
    }

    protected void onError(String message) {
        waitPopup.hide();
        displayError("Error getting current permissions: " + message);
    }

}