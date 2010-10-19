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

import java.util.List;
import java.util.Map;

import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONStreamMetadata;

public class DefaultLayout {

    private String name = null;

    private long time = 0;

    private long endTime = 0;

    private Map<String, JSONStreamMetadata> positions = null;

    private List<JSONStreamMetadata> audioStreams = null;

    public DefaultLayout(String name, long time, long endTime,
            Map<String, JSONStreamMetadata> positions,
            List<JSONStreamMetadata> audioStreams) {
        this.name = name;
        this.time = time;
        this.endTime = endTime;
        this.positions = positions;
        this.audioStreams = audioStreams;
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

    public JSONStreamMetadata getStream(String position) {
        return positions.get(position);
    }

    public List<JSONStreamMetadata> getAudioStreams() {
        return audioStreams;
    }

}
