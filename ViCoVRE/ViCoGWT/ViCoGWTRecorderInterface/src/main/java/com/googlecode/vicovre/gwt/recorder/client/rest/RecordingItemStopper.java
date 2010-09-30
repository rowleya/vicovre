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

import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.json.JSONRecording;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItem;

public class RecordingItemStopper extends AbstractRestCall {

    private FolderPanel folderPanel = null;

    private PlayPanel playPanel = null;

    private RecordingItem item = null;

    private String url = null;

    private String baseUrl = null;

    private String oldStatus = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void stop(FolderPanel folderPanel, PlayPanel playPanel,
            RecordingItem item, String url, Layout[] layouts,
            Layout[] customLayouts) {
        RecordingItemStopper stopper = new RecordingItemStopper(folderPanel,
                playPanel, item, url, layouts, customLayouts);
        stopper.go();
    }

    public RecordingItemStopper(FolderPanel folderPanel, PlayPanel playPanel,
            RecordingItem item, String url, Layout[] layouts,
            Layout[] customLayouts) {
        this.folderPanel = folderPanel;
        this.playPanel = playPanel;
        this.item = item;
        this.url = url + "record" + item.getFolder();
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += item.getId() + "/stop";
        this.oldStatus = item.getStatus();
        this.baseUrl = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void go() {
        item.setStatus("Stopping...");
        item.setCreated(false);
        go(url);
    }

    protected void onError(String message) {
        item.setCreated(true);
        item.setStatus(oldStatus);
        item.setStatus("Error: " + message);
    }

    protected void onSuccess(Response response) {
        JsonRepresentation representation = response.getEntityAsJson();
        JSONValue object = representation.getValue();
        if ((object != null) && (object.isNull() == null)) {
            JSONRecording recording = JSONRecording.parse(object.toString());
            if (recording != null) {

                PlayItem playItem = PlayItemLoader.buildPlayItem(recording,
                        folderPanel, baseUrl, layouts, customLayouts);
                if (item != null) {
                    playPanel.addItem(playItem);
                }
            }
        }
        item.setCreated(true);
        item.setStatus("Completed");
    }

}
