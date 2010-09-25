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

import java.util.List;
import java.util.Vector;

import org.restlet.gwt.data.Response;
import org.restlet.gwt.resource.JsonRepresentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;
import com.googlecode.vicovre.gwt.recorder.client.ReplayLayout;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONReplayLayout;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONReplayLayouts;

public class PlayItemLayoutLoader extends AbstractRestCall {

    private PlayItem playItem = null;

    private ActionLoader loader = null;

    private String url = null;

    public static void loadLayouts(PlayItem playItem,
            ActionLoader loader, String url) {
        if (playItem.getReplayLayouts() != null) {
            loader.itemLoaded();
            return;
        }
        PlayItemLayoutLoader layoutLoader = new PlayItemLayoutLoader(playItem,
                loader, url);
        layoutLoader.go();
    }

    public PlayItemLayoutLoader(PlayItem playItem, ActionLoader loader,
            String url) {
        this.playItem = playItem;
        this.loader = loader;
        this.url = url + "recording" + playItem.getFolder();
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += playItem.getId() + "/layouts";
    }

    public void go() {
        go(url);
    }

    protected void onError(String message) {
        loader.itemFailed("Error loading layout: " + message);
    }

    protected void onSuccess(Response response) {
        JsonRepresentation representation = response.getEntityAsJson();
        JSONValue object = representation.getValue();
        List<ReplayLayout> replayLayouts = new Vector<ReplayLayout>();
        if (object != null) {
            JSONReplayLayouts replayLayoutList = JSONReplayLayouts.parse(
                    object.toString());
            JsArray<JSONReplayLayout> layouts =
                replayLayoutList.getReplayLayouts();
            for (int i = 0; i < layouts.length(); i++) {
                replayLayouts.add(new ReplayLayout(layouts.get(i)));
            }
        }

        playItem.setReplayLayouts(replayLayouts);
        if (loader != null) {
            loader.itemLoaded();
        }
    }
}
