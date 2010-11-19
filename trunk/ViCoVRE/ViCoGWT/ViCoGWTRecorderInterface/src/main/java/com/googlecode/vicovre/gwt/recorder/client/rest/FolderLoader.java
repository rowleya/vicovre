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

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.FolderPanel;

public class FolderLoader extends AbstractJSONRestCall {

    private FolderPanel panel = null;

    private ActionLoader loader = null;

    private String url = null;

    public static void load(FolderPanel panel, ActionLoader loader,
            String url) {
        FolderLoader fLoader = new FolderLoader(panel, loader, url);
        fLoader.go();
    }

    public FolderLoader(FolderPanel panel, ActionLoader loader, String url) {
        super(true);
        this.panel = panel;
        this.loader = loader;
        this.url = url + "folders/list";
    }

    public void go() {
        go(url);
    }

    protected void onError(String message) {
        loader.itemFailed("Error loading folders: " + message);
    }

    protected void onSuccess(JSONObject object) {
        JSONValue folderValue = object.get("folder");
        if (folderValue != null) {
            JSONArray folders = folderValue.isArray();
            if (folders != null) {
                for (int i = 0; i < folders.size(); i++) {
                    JSONString folder = folders.get(i).isString();
                    panel.addFolder(folder.stringValue());
                }
            } else {
                onError("Folder is not an array");
                return;
            }
        }

        loader.itemLoaded();
        panel.setFolder("");
    }

}
