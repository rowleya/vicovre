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
import com.googlecode.vicovre.gwt.client.json.JSONRecording;
import com.googlecode.vicovre.gwt.client.json.JSONMetadata;
import com.googlecode.vicovre.gwt.client.json.JSONRecordings;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.MetadataPopup;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;

public class PlayItemLoader extends AbstractJSONRestCall {

    private FolderPanel folderPanel = null;

    private PlayPanel panel = null;

    private ActionLoader loader = null;

    private String url = null;

    private String baseUrl = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void loadPlayItems(String folder, FolderPanel folderPanel,
            PlayPanel panel, ActionLoader loader, String url, Layout[] layouts,
            Layout[] customLayouts) {
        PlayItemLoader itemLoader =
            new PlayItemLoader(folder, folderPanel, panel, loader, url,
                    layouts, customLayouts);
        itemLoader.go();
    }

    public PlayItemLoader(String folder, FolderPanel folderPanel,
            PlayPanel panel, ActionLoader loader, String url, Layout[] layouts,
            Layout[] customLayouts) {
        super(false);
        this.folderPanel = folderPanel;
        this.panel = panel;
        this.loader = loader;
        this.url = url + "recording" + folder;
        this.baseUrl = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void go() {
        GWT.log("URL = " + url);
        go(url);
    }

    protected void onError(String message) {
        GWT.log("Error loading play items: " + message);
        loader.itemFailed("Error loading play items: " + message);
    }

    public static PlayItem buildPlayItem(JSONRecording recording,
            FolderPanel folderPanel, String baseUrl, Layout[] layouts,
            Layout[] customLayouts) {
        String id = recording.getId();
        JSONMetadata metadata = recording.getMetadata();
        if (metadata == null) {
            GWT.log("Warning: Recording metadata is missing");
            return null;
        }
        String primaryKey = metadata.getPrimaryKey();
        MetadataPopup popup = new MetadataPopup(baseUrl,
                primaryKey);
        popup.setMetadata(metadata);
        PlayItem playItem = new PlayItem(baseUrl, folderPanel, id, popup,
                layouts, customLayouts);
        playItem.setStartDate(JSONRecording.DATE_FORMAT.parse(
                recording.getStartTime()));
        playItem.setDuration((long) recording.getDuration());
        playItem.setPlayable(recording.isPlayable());
        playItem.setEditable(recording.isEditable());
        return playItem;
    }

    protected void onSuccess(JSONObject object) {
        if (object != null) {
            JSONRecordings recordingsList =
                JSONRecordings.parse(object.toString());
            JsArray<JSONRecording> recordings = recordingsList.getRecordings();
            if (recordings != null) {
                Vector<PlayItem> playItems = new Vector<PlayItem>();
                for (int i = 0; i < recordings.length(); i++) {
                    PlayItem item = buildPlayItem(recordings.get(i),
                            folderPanel, baseUrl, layouts, customLayouts);
                    if (item != null) {
                        playItems.add(item);
                    }
                }
                Collections.sort(playItems);
                for (PlayItem item : playItems) {
                    panel.addItem(item);
                }
            }
        }
        GWT.log("Finished loading play items");
        loader.itemLoaded();
    }

}
