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
import org.restlet.client.data.Reference;

import com.google.gwt.core.client.GWT;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.rest.AbstractPlainRestCall;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.HarvestItem;
import com.googlecode.vicovre.gwt.recorder.client.HarvestItemPopup;
import com.googlecode.vicovre.gwt.recorder.client.HarvestPanel;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordPanel;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class HarvestItemCreator extends AbstractPlainRestCall
        implements MessageResponseHandler {

    private FolderPanel folderPanel = null;

    private RecordPanel recordPanel = null;

    private PlayPanel playPanel = null;

    private HarvestPanel panel = null;

    private HarvestItem item = null;

    private String url = null;

    private String baseUrl = null;

    private HarvestItemPopup popup = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void createHarvestItem(FolderPanel folderPanel,
            RecordPanel recordPanel, PlayPanel playPanel, HarvestPanel panel,
            String url, Layout[] layouts, Layout[] customLayouts) {
        HarvestItemCreator creator = new HarvestItemCreator(folderPanel,
                recordPanel, playPanel, panel, url, layouts, customLayouts);
        creator.go();
    }

    public HarvestItemCreator(FolderPanel folderPanel, RecordPanel recordPanel,
            PlayPanel playPanel, HarvestPanel panel, String url,
            Layout[] layouts, Layout[] customLayouts) {
        this.folderPanel = folderPanel;
        this.recordPanel = recordPanel;
        this.playPanel = playPanel;
        this.panel = panel;
        this.url = url + "harvest" + folderPanel.getCurrentFolder();
        this.baseUrl = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void go() {
        popup = new HarvestItemPopup(baseUrl, this);
        popup.center();
    }


    protected void onError(String message) {
        item.setFailedToCreate();
        item.setStatus("Error: Creation Failed: " + message);
        GWT.log("Error creating recording item: " + message);
    }

    protected void onSuccess(String text) {
        String id = new Reference(text).getLastSegment();
        item.setId(id);
        item.setCreated(true);
        item.setStatus("OK");
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            item = new HarvestItem(baseUrl, folderPanel, recordPanel,
                    playPanel, null, popup.getName(), popup,
                    layouts, customLayouts);
            item.handleResponse(response);
            item.setCreated(false);
            item.setStatus("Creating...");
            panel.addItem(item);

            String itemUrl = url + "?";
            itemUrl += item.getDetailsAsUrl();

            GWT.log("Item url = " + itemUrl);
            go(itemUrl, Method.POST);
        }
    }

}
