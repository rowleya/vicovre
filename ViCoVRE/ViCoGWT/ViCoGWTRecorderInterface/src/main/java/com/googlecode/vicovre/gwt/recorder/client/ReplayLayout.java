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

package com.googlecode.vicovre.gwt.recorder.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONReplayLayout;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONReplayLayoutPosition;

public class ReplayLayout {

    private String name = null;

    private long time = 0;

    private long endTime = 0;

    private Map<String, String> positions = null;

    private List<String> audioStreams = null;

    public ReplayLayout(String name, long time, long endTime,
            Map<String, String> positions, List<String> audioStreams) {
        this.name = name;
        this.time = time;
        this.endTime = endTime;
        this.positions = positions;
        this.audioStreams = audioStreams;
    }

    public ReplayLayout(JSONReplayLayout layout) {
        this.name = layout.getName();
        this.time = layout.getTime();
        this.endTime = layout.getEndTime();
        positions = new HashMap<String, String>();
        JsArray<JSONReplayLayoutPosition> layoutPositions =
            layout.getPositions();
        for (int i = 0; i < layoutPositions.length(); i++) {
            JSONReplayLayoutPosition position = layoutPositions.get(i);
            positions.put(position.getName(), position.getStream());
        }
        audioStreams = new Vector<String>();
        JsArrayString layoutAudioStreams = layout.getAudioStreams();
        for (int i = 0; i < layoutAudioStreams.length(); i++) {
            audioStreams.add(layoutAudioStreams.get(i));
        }
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getStream(String position) {
        return positions.get(position);
    }

    public List<String> getAudioStreams() {
        return audioStreams;
    }

}
