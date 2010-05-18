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

package com.googlecode.vicovre.gwt.importexport.client;

import java.util.List;
import java.util.Vector;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.TreeItem;

public class Stream {

    private String id = null;

    private List<SubStream> substreams = new Vector<SubStream>();

    public Stream(JSONObject stream) {
        JSONValue idValue = stream.get("id");
        if (idValue != null) {
            JSONString idString = idValue.isString();
            if (idString != null) {
                id = idString.stringValue();
            }
        }
        JSONValue audioValue = stream.get("audio");
        if (audioValue != null) {
            JSONArray audioArray = audioValue.isArray();
            JSONObject audioObject = audioValue.isObject();
            if (audioArray != null) {
                for (int i = 0; i < audioArray.size(); i++) {
                    JSONObject object = audioArray.get(i).isObject();
                    if (object != null) {
                        substreams.add(new AudioSubStream(object, this));
                    }
                }
            } else if (audioObject != null) {
                substreams.add(new AudioSubStream(audioObject, this));
            }
        }
        JSONValue videoValue = stream.get("video");
        if (videoValue != null) {
            JSONArray videoArray = videoValue.isArray();
            JSONObject videoObject = videoValue.isObject();
            if (videoArray != null) {
                for (int i = 0; i < videoArray.size(); i++) {
                    JSONObject object = videoArray.get(i).isObject();
                    if (object != null) {
                        substreams.add(new VideoSubStream(object, this));
                    }
                }
            } else if (videoObject != null) {
                substreams.add(new VideoSubStream(videoObject, this));
            }
        }
    }

    public String getId() {
        return id;
    }

    public List<SubStream> getSubStreams() {
        return substreams;
    }

}
