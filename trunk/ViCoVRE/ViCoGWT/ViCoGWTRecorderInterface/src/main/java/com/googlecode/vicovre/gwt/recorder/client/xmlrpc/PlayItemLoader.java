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

package com.googlecode.vicovre.gwt.recorder.client.xmlrpc;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.Application;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;
import com.googlecode.vicovre.gwt.recorder.client.PlayPanel;

public class PlayItemLoader implements AsyncCallback<List<Object>> {

    private FolderPanel folderPanel = null;

    private PlayPanel panel = null;

    private ActionLoader loader = null;

    public static void loadPlayItems(String folder, FolderPanel folderPanel,
            PlayPanel panel, ActionLoader loader) {
        XmlRpcClient client = Application.getXmlRpcClient();
        XmlRpcRequest<List<Object>> request = new XmlRpcRequest<List<Object>>(
                client, "recording.getRecordings", new Object[]{folder},
                new PlayItemLoader(folderPanel, panel, loader));
        request.execute();
    }

    private PlayItemLoader(FolderPanel folderPanel, PlayPanel panel,
            ActionLoader loader) {
        this.folderPanel = folderPanel;
        this.panel = panel;
        this.loader = loader;
    }

    public void onFailure(Throwable error) {
        GWT.log("Error loading play items", error);
        loader.itemFailed("Error loading play items");
    }

    private PlayItem buildPlayItem(Map<String, Object> item) {
        String id = (String) item.get("id");
        Map<String, Object> metadata = (Map<String, Object>)
            item.get("metadata");
        String name = (String) metadata.get("name");
        PlayItem playItem = new PlayItem(folderPanel, id, name);
        playItem.setDescription((String) metadata.get("description"));
        playItem.setDescriptionIsEditable((Boolean)
                metadata.get("descriptionIsEditable"));
        playItem.setStartDate((Date) item.get("startTime"));
        playItem.setDuration(((Integer) item.get("duration")).longValue());
        return playItem;
    }

    public void onSuccess(List<Object> items) {
        Vector<PlayItem> playItems = new Vector<PlayItem>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Map) {
                Map<String, Object> item = (Map<String, Object>) items.get(i);
                PlayItem playItem = buildPlayItem(item);
                playItems.add(playItem);
            }
        }
        Collections.sort(playItems);
        for (PlayItem item : playItems) {
            panel.addItem(item);
        }
        loader.itemLoaded();
    }




}
