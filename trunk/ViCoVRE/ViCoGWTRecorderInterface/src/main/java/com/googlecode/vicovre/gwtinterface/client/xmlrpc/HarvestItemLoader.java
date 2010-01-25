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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwtinterface.client.ActionLoader;
import com.googlecode.vicovre.gwtinterface.client.Application;
import com.googlecode.vicovre.gwtinterface.client.FolderPanel;
import com.googlecode.vicovre.gwtinterface.client.HarvestItem;
import com.googlecode.vicovre.gwtinterface.client.HarvestItemPopup;
import com.googlecode.vicovre.gwtinterface.client.HarvestPanel;

public class HarvestItemLoader implements AsyncCallback<List<Object>> {

    private FolderPanel folderPanel = null;

    private HarvestPanel panel = null;

    private ActionLoader loader = null;

    public static void loadHarvestItems(String folder, FolderPanel folderPanel,
            HarvestPanel panel, ActionLoader loader) {
        XmlRpcClient xmlRpcClient = Application.getXmlRpcClient();
        XmlRpcRequest<List<Object>> request = new XmlRpcRequest<List<Object>>(
                xmlRpcClient, "harvest.getSources", new Object[]{folder},
                new HarvestItemLoader(folderPanel, panel, loader));
        request.execute();
    }

    private HarvestItemLoader(FolderPanel folderPanel, HarvestPanel panel,
            ActionLoader loader) {
        this.folderPanel = folderPanel;
        this.panel = panel;
        this.loader = loader;
    }

    public void onFailure(Throwable error) {
        GWT.log("Error loading harvest items", error);
        loader.itemFailed("Error loading harvest items");
    }

    private HarvestItem buildHarvestItem(Map<String, Object> item) {
        String id = (String) item.get("id");
        String name = (String) item.get("name");
        HarvestItem harvestItem = new HarvestItem(folderPanel, id, name);
        harvestItem.setUrl((String) item.get("url"));
        harvestItem.setFormat((String) item.get("format"));
        String frequency = (String) item.get("updateFrequency");
        harvestItem.setUpdateFrequency(frequency);
        if (frequency.equals(HarvestItemPopup.UPDATE_ANUALLY)) {
            harvestItem.setMonth((Integer) item.get("month"));
            harvestItem.setDayOfMonth(
                    (Integer) item.get("dayOfMonth"));
        } else if (frequency.equals(HarvestItemPopup.UPDATE_MONTHLY)) {
            harvestItem.setDayOfMonth(
                    (Integer) item.get("dayOfMonth"));
        } else if (frequency.equals(HarvestItemPopup.UPDATE_WEEKLY)) {
            harvestItem.setDayOfWeek(
                    (Integer) item.get("dayOfWeek"));
        }
        Integer hour = (Integer) item.get("hour");
        Integer minute = (Integer) item.get("minute");
        harvestItem.setHour(hour);
        harvestItem.setMinute(minute);

        String venueServerUrl = (String) item.get("ag3VenueServer");
        if (venueServerUrl != null) {
            harvestItem.setVenueServerUrl(venueServerUrl);
            harvestItem.setVenueUrl(
                    (String) item.get("ag3VenueUrl"));
        } else {
            List<Map<String, Object>> addresses =
                (List<Map<String, Object>>) item.get("addresses");
            String[] addrs = new String[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                Map<String, Object> address = addresses.get(i);
                String host = (String) address.get("host");
                Integer port = (Integer) address.get("port");
                Integer ttl = (Integer) address.get("ttl");
                addrs[i] = host + "/" + port + "/" + ttl;
            }
            harvestItem.setAddresses(addrs);
        }
        return harvestItem;
    }

    public void onSuccess(List<Object> items) {
        Vector<HarvestItem> harvestItems = new Vector<HarvestItem>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Map) {
                Map<String, Object> item = (Map) items.get(i);
                HarvestItem harvestItem = buildHarvestItem(item);
                harvestItems.add(harvestItem);
            }
        }
        Collections.sort(harvestItems);
        for (HarvestItem item : harvestItems) {
            panel.addItem(item);
        }
        loader.itemLoaded();
    }



}
