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

package com.googlecode.vicovre.gwtinterface.client.xmlrpc;


import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwtinterface.client.Application;
import com.googlecode.vicovre.gwtinterface.client.FolderCreatePopup;
import com.googlecode.vicovre.gwtinterface.client.FolderPanel;
import com.googlecode.vicovre.gwtinterface.client.MessagePopup;
import com.googlecode.vicovre.gwtinterface.client.MessageResponse;
import com.googlecode.vicovre.gwtinterface.client.MessageResponseHandler;

public class FolderCreator implements AsyncCallback<Boolean>,
        MessageResponseHandler {

    private FolderPanel folderPanel = null;

    private FolderCreatePopup popup = null;

    private String folderPath = null;

    public static void createFolder(FolderPanel folderPanel) {
        new FolderCreator(folderPanel);
    }

    private FolderCreator(FolderPanel folderPanel) {
        this.folderPanel = folderPanel;
        popup = new FolderCreatePopup(this);
        popup.show();
    }

    public void onFailure(Throwable error) {
        GWT.log("Error creating folder", error);
        MessagePopup errorPopup = new MessagePopup("Error creating folder: "
                + error.getMessage(), null, MessagePopup.ERROR,
                MessageResponse.OK);
        errorPopup.center();
    }

    public void onSuccess(Boolean result) {
        if (result) {
            folderPanel.addFolder(folderPath);
        } else {
            onFailure(new Exception("Folder could not be created"));
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            folderPath = folderPanel.getCurrentFolder();
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }
            folderPath += popup.getName();
            XmlRpcClient xmlRpcClient = Application.getXmlRpcClient();
            XmlRpcRequest<Boolean> request = new XmlRpcRequest<Boolean>(
                    xmlRpcClient, "folder.createFolder",
                    new Object[]{folderPath}, this);
            request.execute();
        }
    }

}
