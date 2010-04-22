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

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Format;
import javax.media.Track;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;

public class TrackStream implements PullBufferStream {

    private boolean started = false;

    private Integer startSync = new Integer(0);

    private Track track = null;

    public TrackStream(Track track) {
        this.track = track;
    }

    public Format getFormat() {
        return track.getFormat();
    }

    public void read(Buffer buffer) throws IOException {
        synchronized (startSync) {
            while (!started) {
                try {
                    startSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
        }
        track.readFrame(buffer);
    }

    public boolean willReadBlock() {
        synchronized (startSync) {
            return !started;
        }
    }

    public boolean endOfStream() {
        return false;
    }

    public ContentDescriptor getContentDescriptor() {
        return new ContentDescriptor(ContentDescriptor.RAW);
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public Object getControl(String className) {
        try {
            Class<?> cls = Class.forName(className);
            if (cls.isInstance(className)) {
                return track;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object[] getControls() {
        if (track instanceof Control) {
            return new Object[]{track};
        }
        return new Object[0];
    }

    protected void start() {
        synchronized (startSync) {
            started = true;
            startSync.notifyAll();
        }
    }

    protected void stop() {
        synchronized (startSync) {
            started = false;
            startSync.notifyAll();
        }
    }

}
