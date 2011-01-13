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

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class FolderPermissionSetter extends AbstractVoidRestCall {

    private String url = null;

    private ModalPopup<? extends Widget> popup = null;

    private WaitPopup waitPopup = new WaitPopup("Setting Permissions", true);

    public static void setPermissions(String url, String folder,
            ModalPopup<? extends Widget> popup, boolean allow,
            String[] exceptionTypes, String[] exceptions) {
        FolderPermissionSetter setter = new FolderPermissionSetter(url, folder,
            popup, allow, exceptionTypes, exceptions);
        setter.go();
    }

    public FolderPermissionSetter(String url, String folder,
            ModalPopup<? extends Widget> popup,
            boolean allow, String[] exceptionTypes, String[] exceptions) {

        this.url = url + "folders" + folder + "/acl/";
        this.url += "?public=" + allow;
        if (exceptionTypes != null && exceptions != null) {
            for (int j = 0; j < exceptionTypes.length; j++) {
                this.url += "&exceptionType=" + URL.encodeComponent(
                        exceptionTypes[j]);
                this.url += "&exceptionName=" + URL.encodeComponent(
                        exceptions[j]);
            }
        }

        this.popup = popup;
    }

    public void go() {
        if (!waitPopup.isShowing()) {
            waitPopup.center();
        }
        go(url, Method.PUT);
    }

    protected void onError(String message) {
        waitPopup.hide();
        MessagePopup popup = new MessagePopup(
                "Error setting permissions: " + message, null,
                MessagePopup.ERROR, MessageResponse.OK);
        popup.center();
    }

    protected void onSuccess() {
        waitPopup.hide();
        if (popup != null) {
            popup.hide();
        }
    }

}
