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
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.HarvestItem;
import com.googlecode.vicovre.gwt.recorder.client.HarvestItemPopup;
import com.googlecode.vicovre.gwt.recorder.client.HarvestPanel;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;
import com.googlecode.vicovre.gwt.recorder.client.RecordPanel;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONHarvestSource;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONHarvestSources;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONNetworkLocation;

public class HarvestItemLoader extends AbstractRestCall {

    private FolderPanel folderPanel = null;

    private RecordPanel recordPanel = null;

    private PlayPanel playPanel = null;

    private HarvestPanel panel = null;

    private ActionLoader loader = null;

    private String url = null;

    private String baseUrl = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public static void loadHarvestItems(String folder, FolderPanel folderPanel,
            RecordPanel recordPanel, PlayPanel playPanel, HarvestPanel panel,
            ActionLoader loader, String url, Layout[] layouts,
            Layout[] customLayouts) {
        HarvestItemLoader itemLoader = new HarvestItemLoader(folder,
                folderPanel, recordPanel, playPanel, panel, loader, url,
                layouts, customLayouts);
        itemLoader.go();
    }

    public HarvestItemLoader(String folder, FolderPanel folderPanel,
            RecordPanel recordPanel, PlayPanel playPanel, HarvestPanel panel,
            ActionLoader loader, String url, Layout[] layouts,
            Layout[] customLayouts) {
        this.folderPanel = folderPanel;
        this.recordPanel = recordPanel;
        this.playPanel = playPanel;
        this.panel = panel;
        this.loader = loader;
        this.url = url + "harvest" + folder;
        this.baseUrl = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void go() {
        GWT.log("URL = " + url);
        go(url);
    }

    protected void onError(String message) {
        GWT.log("Error loading harvest items: " + message);
        loader.itemFailed("Error loading harvest items: " + message);
    }

    private HarvestItem buildHarvestItem(JSONHarvestSource harvestSource) {
        String id = harvestSource.getId();
        String name = harvestSource.getName();
        HarvestItem harvestItem = new HarvestItem(baseUrl, folderPanel,
                recordPanel, playPanel, id, name, null, layouts, customLayouts);
        harvestItem.setUrl(harvestSource.getUrl());
        harvestItem.setFormat(harvestSource.getFormat());
        String frequency = harvestSource.getUpdateFrequency();
        harvestItem.setUpdateFrequency(frequency);
        if (frequency.equals(HarvestItemPopup.UPDATE_ANUALLY)) {
            harvestItem.setMonth(harvestSource.getMonth());
            harvestItem.setDayOfMonth(harvestSource.getDayOfMonth());
        } else if (frequency.equals(HarvestItemPopup.UPDATE_MONTHLY)) {
            harvestItem.setDayOfMonth(harvestSource.getDayOfMonth());
        } else if (frequency.equals(HarvestItemPopup.UPDATE_WEEKLY)) {
            harvestItem.setDayOfWeek(harvestSource.getDayOfWeek());
        }
        Integer hour = harvestSource.getHour();
        Integer minute = harvestSource.getMinute();
        harvestItem.setHour(hour);
        harvestItem.setMinute(minute);

        String venueServerUrl = harvestSource.getAg3VenueServer();
        if (venueServerUrl != null) {
            harvestItem.setVenueServerUrl(venueServerUrl);
            harvestItem.setVenueUrl(harvestSource.getAg3VenueUrl());
        } else {
            JsArray<JSONNetworkLocation> addresses =
                harvestSource.getAddresses();
            String[] addrs = new String[addresses.length()];
            for (int i = 0; i < addresses.length(); i++) {
                String host = addresses.get(i).getHost();
                int port = addresses.get(i).getPort();
                int ttl = addresses.get(i).getTtl();
                addrs[i] = host + "/" + port + "/" + ttl;
            }
            harvestItem.setAddresses(addrs);
        }
        return harvestItem;
    }

    protected void onSuccess(Response response) {
        JsonRepresentation representation = response.getEntityAsJson();
        JSONValue object = representation.getValue();
        if ((object != null) && (object.isNull() == null)) {
            JSONHarvestSources harvestSourceList =
                JSONHarvestSources.parse(object.toString());
            JsArray<JSONHarvestSource> harvestSources =
                harvestSourceList.getHarvestSources();
            if (harvestSources != null) {
                Vector<HarvestItem> harvestItems = new Vector<HarvestItem>();
                for (int i = 0; i < harvestSources.length(); i++) {
                    HarvestItem item = buildHarvestItem(harvestSources.get(i));
                    if (item != null) {
                        harvestItems.add(item);
                    }
                }
                Collections.sort(harvestItems);
                for (HarvestItem item : harvestItems) {
                    panel.addItem(item);
                }
            }
        }
        loader.itemLoaded();
    }

}
