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

import org.restlet.gwt.data.MediaType;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Reference;
import org.restlet.gwt.data.Response;

import com.google.gwt.core.client.GWT;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.MetadataPopup;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItem;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItemPopup;

public class RecordingItemCreator extends AbstractRestCall
        implements MessageResponseHandler {

    private FolderPanel folderPanel = null;

    private PlayPanel playPanel = null;

    private RecordPanel panel = null;

    private RecordingItem item = null;

    private String url = null;

    private String baseUrl = null;

    private RecordingItemPopup popup = null;

    private MetadataPopup metadataPopup = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void createRecordingItem(FolderPanel folderPanel,
            PlayPanel playPanel, RecordPanel panel, String url,
            Layout[] layouts, Layout[] customLayouts) {
        RecordingItemCreator creator =
            new RecordingItemCreator(folderPanel, playPanel, panel, url,
                    layouts, customLayouts);
        creator.go();
    }

    private RecordingItemCreator(FolderPanel folderPanel, PlayPanel playPanel,
            RecordPanel panel, String url, Layout[] layouts,
            Layout[] customLayouts) {
        this.folderPanel = folderPanel;
        this.playPanel = playPanel;
        this.panel = panel;
        this.url = url + "record" + folderPanel.getCurrentFolder();
        this.baseUrl = url;
        this.metadataPopup = new MetadataPopup(baseUrl, "Name");
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void go() {
        popup = new RecordingItemPopup(this, baseUrl, metadataPopup);
        popup.center();
    }

    protected void onError(String message) {
        item.setFailedToCreate();
        item.setStatus("Error: Creation Failed: " + message);
        GWT.log("Error creating recording item: " + message);
    }

    protected void onSuccess(Response response) {
        String id = new Reference(
                response.getEntity().getText()).getLastSegment();
        item.setId(id);
        item.setCreated(true);
        item.setStatus("Stopped");
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            item = new RecordingItem(folderPanel, playPanel, null,
                    baseUrl, metadataPopup, popup, layouts,
                    customLayouts);
            popup.setRecordingItem(item);
            item.handleResponse(response);
            item.setCreated(false);
            item.setStatus("Creating...");
            panel.addItem(item);

            String itemUrl = url + "?";
            itemUrl += item.getDetailsAsUrl();

            GWT.log("Item url = " + itemUrl);
            go(itemUrl, Method.POST, MediaType.TEXT_PLAIN);
        }
    }

}