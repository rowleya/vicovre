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

import java.util.Map;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwtinterface.client.Application;
import com.googlecode.vicovre.gwtinterface.client.FolderPanel;
import com.googlecode.vicovre.gwtinterface.client.MessageResponse;
import com.googlecode.vicovre.gwtinterface.client.MessageResponseHandler;
import com.googlecode.vicovre.gwtinterface.client.RecordPanel;
import com.googlecode.vicovre.gwtinterface.client.RecordingItem;
import com.googlecode.vicovre.gwtinterface.client.RecordingItemPopup;

public class RecordingItemCreator implements AsyncCallback<String>,
        MessageResponseHandler {

    private FolderPanel folderPanel = null;

    private String folder = null;

    private RecordPanel panel = null;

    private RecordingItem item = null;

    private RecordingItemPopup popup = null;

    public static void createRecordingItem(FolderPanel folderPanel,
            RecordPanel panel) {
        new RecordingItemCreator(folderPanel, panel);
    }

    private RecordingItemCreator(FolderPanel folderPanel, RecordPanel panel) {
        this.folderPanel = folderPanel;
        this.panel = panel;
        this.folder = folderPanel.getCurrentFolder();
        popup = new RecordingItemPopup(this);
        popup.center();
    }

    public void onFailure(Throwable error) {
        item.setFailedToCreate();
        item.setStatus("Error: Creation Failed: " + error.getMessage());
        GWT.log("Error creating recording item", error);
    }

    public void onSuccess(String id) {
        item.setId(id);
        item.setCreated(true);
        item.setStatus("Stopped");
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            item = new RecordingItem(folderPanel, null, popup.getName());
            item.handleResponse(response);
            item.setCreated(false);
            item.setStatus("Creating...");
            panel.addItem(item);
            Map<String, Object> details = item.getDetails();
            XmlRpcClient xmlRpcClient = Application.getXmlRpcClient();
            XmlRpcRequest<String> request = new XmlRpcRequest<String>(
                    xmlRpcClient, "unfinishedRecording.addUnfinishedRecording",
                    new Object[]{folder, details}, this);
            request.execute();
        }
    }

}
