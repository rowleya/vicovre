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
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.json.JSONMetadata;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.MetadataPopup;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItem;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONNetworkLocation;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONUnfinishedRecording;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONUnfinishedRecordings;

public class RecordingItemLoader extends AbstractJSONRestCall {

    private FolderPanel folderPanel = null;

    private PlayPanel playPanel = null;

    private RecordPanel panel = null;

    private ActionLoader loader = null;

    private String url = null;

    private String baseUrl = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void loadRecordingItems(String folder,
            FolderPanel folderPanel, PlayPanel playPanel,
            RecordPanel panel, ActionLoader loader, String url,
            Layout[] layouts, Layout[] customLayouts) {
        RecordingItemLoader riLoader = new RecordingItemLoader(folder,
               folderPanel, playPanel, panel, loader, url, layouts,
               customLayouts);
        riLoader.go();
    }

    public RecordingItemLoader(String folder,
            FolderPanel folderPanel, PlayPanel playPanel,
            RecordPanel panel, ActionLoader loader, String url,
            Layout[] layouts, Layout[] customLayouts) {
        super(false);
        this.folderPanel = folderPanel;
        this.playPanel = playPanel;
        this.panel = panel;
        this.loader = loader;
        this.url = url + "record" + folder;
        this.baseUrl = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void go() {
        GWT.log("URL = " + url);
        go(url);
    }

    protected void onError(String message) {
        GWT.log("Error loading recording items: " + message);
        loader.itemFailed("Error loading recording items: " + message);
    }

    public static RecordingItem buildRecordingItem(
            JSONUnfinishedRecording recording, FolderPanel folderPanel,
            PlayPanel playPanel, String baseUrl, Layout[] layouts,
            Layout[] customLayouts) {

        String id = recording.getId();
        if (id == null) {
            GWT.log("Warning: Recording id is missing");
            return null;
        }

        JSONMetadata metadata = recording.getMetadata();
        if (metadata == null) {
            GWT.log("Warning: Recording metadata is missing");
            return null;
        }
        MetadataPopup metadataPopup = new MetadataPopup(baseUrl,
                metadata.getPrimaryKey());
        metadataPopup.setMetadata(metadata);
        RecordingItem recordingItem = new RecordingItem(folderPanel, playPanel,
                id, baseUrl, metadataPopup, null, layouts, customLayouts);

        if (recording.getStartDate() != null) {
            recordingItem.setStartDate(
                    JSONUnfinishedRecording.DATE_FORMAT.parse(
                recording.getStartDate()));
        }
        if (recording.getStopDate() != null) {
            recordingItem.setStopDate(
                    JSONUnfinishedRecording.DATE_FORMAT.parse(
                recording.getStopDate()));
        }

        String venueServerUrl = recording.getAg3VenueServer();
        if (venueServerUrl != null) {
            recordingItem.setVenueServerUrl(venueServerUrl);
            recordingItem.setVenueUrl(recording.getAg3VenueUrl());
        } else {
            JsArray<JSONNetworkLocation> addresses = recording.getAddresses();
            String[] addrs = new String[addresses.length()];
            for (int i = 0; i < addresses.length(); i++) {
                String host = addresses.get(i).getHost();
                int port = addresses.get(i).getPort();
                int ttl = addresses.get(i).getTtl();
                addrs[i] = host + "/" + port + "/" + ttl;
            }
            recordingItem.setAddresses(addrs);
        }

        recordingItem.setStatus(recording.getStatus());
        return recordingItem;
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
                    RecordingItem item = buildRecordingItem(recordings.get(i),
                            folderPanel, playPanel, baseUrl, layouts,
                            customLayouts);
                    if (item != null) {
                        recordingItems.add(item);
                    }
                }
                Collections.sort(recordingItems);
                for (RecordingItem item : recordingItems) {
                    panel.addItem(item);
                }
            }
        }
        GWT.log("Finished loading recording items");
        loader.itemLoaded();

    }

}
