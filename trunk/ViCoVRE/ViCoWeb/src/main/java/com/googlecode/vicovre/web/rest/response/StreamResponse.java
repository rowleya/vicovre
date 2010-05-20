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

package com.googlecode.vicovre.web.rest.response;

import java.util.List;
import java.util.Vector;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.googlecode.vicovre.web.convert.ConvertSession;
import com.googlecode.vicovre.web.convert.ImportStream;

@XmlRootElement(name="stream")
public class StreamResponse {

    private String id = null;

    private List<AVStreamResponse> substreams = new Vector<AVStreamResponse>();

    public StreamResponse() {
        // Does Nothing
    }

    public StreamResponse(String id, ImportStream stream,
            ConvertSession session) {
        this.id = id;
        for (int i = 0; i < stream.getNoStreams(); i++) {
            String transmitStreamId = session.getTransmitStreamId(id, i);
            Format format = stream.getFormat(i);
            if (format instanceof AudioFormat) {
                substreams.add(new AudioStreamResponse(String.valueOf(i),
                        (AudioFormat) format, transmitStreamId));
            } else if (format instanceof VideoFormat) {
                substreams.add(new VideoStreamResponse(String.valueOf(i),
                        (VideoFormat) format, transmitStreamId));
            }
        }
    }

    @XmlElement(name="id")
    public String getId() {
        return id;
    }

    @XmlElements({
        @XmlElement(name="audio", type=AudioStreamResponse.class),
        @XmlElement(name="video", type=VideoStreamResponse.class)
    })
    public List<AVStreamResponse> getSubstreams() {
        return substreams;
    }
}
