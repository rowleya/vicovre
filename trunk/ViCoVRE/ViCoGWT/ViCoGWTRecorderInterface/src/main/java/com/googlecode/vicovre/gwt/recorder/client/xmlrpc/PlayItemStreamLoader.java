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

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.Application;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;
import com.googlecode.vicovre.gwt.recorder.client.Stream;

public class PlayItemStreamLoader implements AsyncCallback<List<Object>> {

    private PlayItem playItem = null;

    private ActionLoader loader = null;

    public static void loadStreams(PlayItem playItem,
            ActionLoader loader) {
        if (playItem.getStreams() != null) {
            loader.itemLoaded();
            return;
        }
        XmlRpcClient client = Application.getXmlRpcClient();
        XmlRpcRequest<List<Object>> request = new XmlRpcRequest<List<Object>>(
                client, "recording.getStreams",
                new Object[]{playItem.getFolder(), playItem.getId()},
                new PlayItemStreamLoader(playItem, loader));
        request.execute();
    }

    private PlayItemStreamLoader(PlayItem playItem, ActionLoader loader) {
        this.playItem = playItem;
        this.loader = loader;
    }

    public void onFailure(Throwable error) {
        loader.itemFailed("Error loading layout: " + error.getMessage());
    }

    public void onSuccess(List<Object> items) {
        List<Stream> streams = new Vector<Stream>();
        for (Object item : items) {
            Map<String, Object> streamMap = (Map<String, Object>) item;
            String ssrc = (String) streamMap.get("ssrc");
            String cname = (String) streamMap.get("cname");
            String streamName = (String) streamMap.get("name");
            String note = (String) streamMap.get("note");
            String mediaType = (String) streamMap.get("mediaType");
            Stream stream = new Stream(ssrc, cname, streamName, note,
                    mediaType);
            streams.add(stream);
        }
        playItem.setStreams(streams);
        if (loader != null) {
            loader.itemLoaded();
        }
    }
}
