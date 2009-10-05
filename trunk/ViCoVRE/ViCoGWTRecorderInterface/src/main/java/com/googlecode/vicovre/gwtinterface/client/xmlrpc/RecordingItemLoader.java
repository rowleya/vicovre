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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwtinterface.client.Application;
import com.googlecode.vicovre.gwtinterface.client.HarvestItem;
import com.googlecode.vicovre.gwtinterface.client.HarvestItemPopup;
import com.googlecode.vicovre.gwtinterface.client.HarvestPanel;
import com.googlecode.vicovre.gwtinterface.client.RecordPanel;
import com.googlecode.vicovre.gwtinterface.client.RecordingItem;

public class RecordingItemLoader implements AsyncCallback<List<Object>> {

    private RecordPanel panel = null;

    public static void loadRecordingItems(String folder,
            RecordPanel panel) {
        XmlRpcClient xmlRpcClient = Application.getXmlRpcClient();
        XmlRpcRequest<List<Object>> request = new XmlRpcRequest<List<Object>>(
                xmlRpcClient, "unfinishedRecording.getUnfinishedRecordings",
                new Object[]{folder},
                new RecordingItemLoader(panel));
        request.execute();
    }

    private RecordingItemLoader(RecordPanel panel) {
        this.panel = panel;
    }

    public void onFailure(Throwable error) {
        Application.showErrorLoading();
        GWT.log("Error loading recording items", error);
    }

    public static RecordingItem buildRecordingItem(Map<String, Object> item) {
        Integer id = (Integer) item.get("id");

        Map<String, Object> metadata = (Map<String, Object>)
            item.get("metadata");
        String name = (String) metadata.get("name");
        RecordingItem recordingItem = new RecordingItem(id, name);
        recordingItem.setDescription((String) metadata.get("description"));
        recordingItem.setDescriptionIsEditable((Boolean)
                metadata.get("descriptionIsEditable"));

        recordingItem.setStartDate((Date) item.get("startDate"));
        recordingItem.setStopDate((Date) item.get("stopDate"));

        String venueServerUrl = (String) item.get("ag3VenueServer");
        if (venueServerUrl != null) {
            recordingItem.setVenueServerUrl(venueServerUrl);
            recordingItem.setVenueUrl(
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
            recordingItem.setAddresses(addrs);
        }

        recordingItem.setStatus((String) item.get("status"));
        return recordingItem;
    }

    public void onSuccess(List<Object> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Map) {
                Map<String, Object> item = (Map) items.get(i);
                RecordingItem recordingItem = buildRecordingItem(item);
                panel.addItem(recordingItem);
            } else {
            }
        }
    }



}
