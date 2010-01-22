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
import com.googlecode.vicovre.gwtinterface.client.HarvestItem;
import com.googlecode.vicovre.gwtinterface.client.HarvestItemPopup;
import com.googlecode.vicovre.gwtinterface.client.HarvestPanel;
import com.googlecode.vicovre.gwtinterface.client.MessageResponse;
import com.googlecode.vicovre.gwtinterface.client.MessageResponseHandler;

public class HarvestItemCreator implements AsyncCallback<Integer>,
        MessageResponseHandler {

    private FolderPanel folderPanel = null;

    private String folder = null;

    private HarvestPanel panel = null;

    private HarvestItem harvestItem = null;

    private HarvestItemPopup popup = null;

    public static void createHarvestItem(FolderPanel folderPanel,
            HarvestPanel panel) {
        new HarvestItemCreator(folderPanel, panel);
    }

    private HarvestItemCreator(FolderPanel folderPanel, HarvestPanel panel) {
        this.folderPanel = folderPanel;
        this.panel = panel;
        this.folder = folderPanel.getCurrentFolder();
        popup = new HarvestItemPopup(this);
        popup.show();
    }

    public void onFailure(Throwable error) {
        harvestItem.setFailedToCreate();
        harvestItem.setStatus("Creation Failed: " + error.getMessage());
        GWT.log("Error creating harvest item", error);
    }

    public void onSuccess(Integer id) {
        harvestItem.setId(id);
        harvestItem.setCreated(true);
        harvestItem.setStatus("OK");
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            harvestItem = new HarvestItem(folderPanel, 0, popup.getName());
            harvestItem.handleResponse(response);
            harvestItem.setCreated(false);
            harvestItem.setStatus("Creating...");
            panel.addItem(harvestItem);
            Map<String, Object> details = harvestItem.getDetails();
            XmlRpcClient xmlRpcClient = Application.getXmlRpcClient();
            XmlRpcRequest<Integer> request = new XmlRpcRequest<Integer>(
                    xmlRpcClient, "harvest.addHarvestSource",
                    new Object[]{folder, details}, this);
            request.execute();
        }
    }

}
