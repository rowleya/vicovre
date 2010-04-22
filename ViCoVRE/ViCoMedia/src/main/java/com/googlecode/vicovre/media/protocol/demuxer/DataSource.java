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

package com.googlecode.vicovre.media.protocol.demuxer;

import java.io.IOException;

import javax.media.BadHeaderException;
import javax.media.Demultiplexer;
import javax.media.ResourceUnavailableException;
import javax.media.Time;
import javax.media.Track;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;

public class DataSource extends PullBufferDataSource {

    private Demultiplexer demuxer = null;

    private TrackStream[] streams = null;

    private boolean started = false;

    public DataSource(Demultiplexer demuxer) {
        this.demuxer = demuxer;
    }

    public PullBufferStream[] getStreams() {
        return streams;
    }

    public void connect() throws IOException {
        try {
            demuxer.open();
        } catch (ResourceUnavailableException e) {
            throw new IOException(e);
        }
    }

    public void disconnect() {
        // Does Nothing
    }

    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    public Object getControl(String className) {
        return demuxer.getControl(className);
    }

    public Object[] getControls() {
        return demuxer.getControls();
    }

    public Time getDuration() {
        return demuxer.getDuration();
    }

    public void start() throws IOException {
        if (!started) {
            demuxer.start();
            if (streams == null) {
                try {
                    Track[] tracks = demuxer.getTracks();
                    streams = new TrackStream[tracks.length];
                    for (int i = 0; i < tracks.length; i++) {
                        streams[i] = new TrackStream(tracks[i]);
                    }
                } catch (BadHeaderException e) {
                    throw new IOException(e);
                }
            }
            for (int i = 0; i < streams.length; i++) {
                streams[i].start();
            }
            started = true;
        }
    }

    public void stop() throws IOException {
        if (started) {
            for (int i = 0; i < streams.length; i++) {
                streams[i].stop();
            }
            started = false;
            demuxer.stop();
        }
    }

}
