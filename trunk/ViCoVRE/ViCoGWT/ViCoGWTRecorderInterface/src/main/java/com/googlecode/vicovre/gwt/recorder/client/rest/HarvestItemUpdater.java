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

import java.util.Collections;
import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.HarvestItem;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItem;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONUnfinishedRecording;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONUnfinishedRecordings;

public class HarvestItemUpdater extends AbstractJSONRestCall {

    private HarvestItem item = null;

    private RecordPanel recordPanel = null;

    private PlayPanel playPanel = null;

    private FolderPanel folderPanel = null;

    private String url = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void harvest(HarvestItem item, FolderPanel folderPanel,
            RecordPanel recordPanel, PlayPanel playPanel, String url,
            Layout[] layouts, Layout[] customLayouts) {
        HarvestItemUpdater updater = new HarvestItemUpdater(item, folderPanel,
                recordPanel, playPanel, url, layouts, customLayouts);
        updater.go();
    }

    public HarvestItemUpdater(HarvestItem item, FolderPanel folderPanel,
            RecordPanel recordPanel, PlayPanel playPanel, String url,
            Layout[] layouts, Layout[] customLayouts) {
        super(false);
        this.item = item;
        this.folderPanel = folderPanel;
        this.recordPanel = recordPanel;
        this.playPanel = playPanel;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
        this.url = url + "harvest" + item.getFolder();
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += item.getId() + "/harvest";
    }

    public void go() {
        item.setStatus("Harvesting...");
        GWT.log("Harvesting from url " + url);
        go(url);
    }

    protected void onError(String message) {
        item.setStatus("Error harvesting: " + message);
    }

    protected void onSuccess(JSONObject object) {
        if (object != null) {
            JSONUnfinishedRecordings recordingsList =
                JSONUnfinishedRecordings.parse(object.toString());
            JsArray<JSONUnfinishedRecording> recordings =
                recordingsList.getRecordings();
            if (recordings != null) {
                Vector<RecordingItem> recordingItems =
                    new Vector<RecordingItem>();
                for (int i = 0; i < recordings.length(); i++) {
                    String folder = recordings.get(i).getFolder();
                    if (folder.equals(folderPanel.getCurrentFolder())) {
                        RecordingItem item =
                            RecordingItemLoader.buildRecordingItem(
                                recordings.get(i), folderPanel, playPanel, url,
                                layouts, customLayouts);
                        if (item != null) {
                            recordingItems.add(item);
                        }
                    } else {
                        folderPanel.addFolder(folder);
                    }
                }
                Collections.sort(recordingItems);
                for (RecordingItem item : recordingItems) {
                    recordPanel.addItem(item);
                }
            }
        }
        item.setStatus("OK");

    }



}
