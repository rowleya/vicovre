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

import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.FolderCreatePopup;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;

public class FolderCreator extends AbstractVoidRestCall
        implements MessageResponseHandler {

    private FolderPanel folderPanel = null;

    private FolderCreatePopup popup = null;

    private String folderPath = null;

    private String url = null;

    public static void createFolder(FolderPanel folderPanel, String url) {
        FolderCreator creator = new FolderCreator(folderPanel, url);
        creator.go();
    }

    public FolderCreator(FolderPanel folderPanel, String url) {
        this.folderPanel = folderPanel;
        this.url = url + "folders";
    }

    public void go() {
        popup = new FolderCreatePopup(this);
        popup.show();
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            folderPath = folderPanel.getCurrentFolder();
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }
            folderPath += popup.getName();

            go(url + folderPath, Method.PUT);
        }
    }

    protected void onError(String message) {
        displayError("Error creating folder: " + message);
    }

    protected void onSuccess() {
        folderPanel.addFolder(folderPath);
    }

}
