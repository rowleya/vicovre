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

import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItem;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.NetworkLocation;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.Recording;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.RecordingMetadata;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.Recordings;

public class RecordingItemLoader extends AbstractRestCall {

    private FolderPanel folderPanel = null;

    private RecordPanel panel = null;

    private ActionLoader loader = null;

    private String url = null;

    private String baseUrl = null;

    public static void loadRecordingItems(String folder,
            FolderPanel folderPanel,
            RecordPanel panel, ActionLoader loader, String url) {
        RecordingItemLoader riLoader = new RecordingItemLoader(folder,
                folderPanel, panel, loader, url);
        riLoader.go();
    }

    public RecordingItemLoader(String folder,
            FolderPanel folderPanel,
            RecordPanel panel, ActionLoader loader, String url) {
        this.folderPanel = folderPanel;
        this.panel = panel;
        this.loader = loader;
        this.url = url + "record" + folder;
        this.baseUrl = url;
    }

    public void go() {
        GWT.log("URL = " + url);
        go(url);
    }

    protected void onError(String message) {
        GWT.log("Error loading recording items: " + message);
        loader.itemFailed("Error loading recording items: " + message);
    }

    private RecordingItem buildRecordingItem(Recording recording) {

        String id = recording.getId();
        if (id == null) {
            GWT.log("Warning: Recording id is missing");
            return null;
        }

        RecordingMetadata metadata = recording.getMetadata();
        if (metadata == null) {
            GWT.log("Warning: Recording metadata is missing");
            return null;
        }
        String name = metadata.getName();
        if (name == null) {
            GWT.log("Warning: Recording name is missing");
            return null;
        }
        RecordingItem recordingItem = new RecordingItem(folderPanel, id, name,
                baseUrl);
        recordingItem.setDescription(metadata.getDescription());
        recordingItem.setDescriptionIsEditable(
                metadata.isDescriptionEditable());

        if (recording.getStartDate() != null) {
            recordingItem.setStartDate(Recording.DATE_FORMAT.parse(
                recording.getStartDate()));
        }
        if (recording.getStopDate() != null) {
            recordingItem.setStopDate(Recording.DATE_FORMAT.parse(
                recording.getStopDate()));
        }

        String venueServerUrl = recording.getAg3VenueServer();
        GWT.log("Venue server = " + venueServerUrl);
        if (venueServerUrl != null) {
            recordingItem.setVenueServerUrl(venueServerUrl);
            recordingItem.setVenueUrl(recording.getAg3VenueUrl());
        } else {
            JsArray<NetworkLocation> addresses = recording.getAddresses();
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

    protected void onSuccess(Response response) {
        JsonRepresentation representation = response.getEntityAsJson();
        JSONValue object = representation.getValue();
        if (object != null) {
            Recordings recordingsList = Recordings.parse(object.toString());
            JsArray<Recording> recordings = recordingsList.getRecordings();
            if (recordings != null) {
                Vector<RecordingItem> recordingItems =
                    new Vector<RecordingItem>();
                for (int i = 0; i < recordings.length(); i++) {
                    RecordingItem item = buildRecordingItem(recordings.get(i));
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
        loader.itemLoaded();

    }

}
