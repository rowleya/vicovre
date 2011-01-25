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
import com.googlecode.vicovre.gwt.client.json.JSONGroups;
import com.googlecode.vicovre.gwt.client.json.JSONUsers;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.FolderPermissionPopup;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class FolderPermissionLoader extends AbstractJSONRestCall {

    private String baseUrl = null;

    private String url = null;

    private String folder = null;

    private JsArrayString users = null;

    private JsArrayString groups = null;

    private boolean usersLoaded = false;

    private boolean groupsLoaded = false;

    private JSONACL readAcl = null;

    private WaitPopup waitPopup = new WaitPopup("Loading Existing Permissions",
            true);

    public static void load(String url, String folder) {
        FolderPermissionLoader loader = new FolderPermissionLoader(url, folder);
        loader.go();
    }

    public FolderPermissionLoader(String url, String folder) {
        super(false);
        this.baseUrl = url;
        this.url = url + "folders" + folder;
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += "acl";
        this.folder = folder;
    }

    public void go() {
        if (!usersLoaded) {
            waitPopup.center();
            go(baseUrl + "user");
        } else if (!groupsLoaded) {
            go(baseUrl + "group");
        } else if (readAcl == null) {
            GWT.log("Permission load url = " + url);
            go(url);
        } else {
            onSuccess((JSONObject) null);
        }
    }

    protected void onSuccess(JSONObject object) {
        if (!waitPopup.wasCancelled()) {
            if (!usersLoaded) {
                if (object != null) {
                    JSONUsers usersResponse = JSONUsers.parse(
                            object.toString());
                    users = usersResponse.getUsers();
                }
                usersLoaded = true;
                go();
            } else if (!groupsLoaded) {
                if (object != null) {
                    JSONGroups groupsResponse = JSONGroups.parse(
                            object.toString());
                    groups = groupsResponse.getGroups();
                }
                groupsLoaded = true;
                go();
            } else if (readAcl == null) {
                if (object == null) {
                    onError("Error loading acl!");
                    return;
                }
                readAcl = JSONACL.parse(object.toString());
                waitPopup.hide();
                FolderPermissionPopup popup = new FolderPermissionPopup(
                        baseUrl, folder, users, groups, readAcl);
                popup.center();
            }
        }
    }


    protected void onError(String message) {
        waitPopup.hide();
        displayError("Error getting current permissions: " + message);
    }

}
