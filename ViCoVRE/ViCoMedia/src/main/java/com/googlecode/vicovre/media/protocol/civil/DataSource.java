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

package com.googlecode.vicovre.media.protocol.civil;

import java.io.IOException;

import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import com.lti.civil.CaptureException;

public class DataSource extends PushBufferDataSource {

    private LiveStream stream = null;

    private boolean connected = false;

    private boolean started = false;

    public PushBufferStream[] getStreams() {
        if (stream != null) {
            return new PushBufferStream[]{stream};
        }
        return new PushBufferStream[0];
    }

    public void connect() throws IOException {
        if (stream == null) {
            try {
                stream = new LiveStream();
            } catch (CaptureException e) {
                throw new IOException(e);
            }
        }
        if (!connected) {
            try {
                stream.setLocator(getLocator());
                connected = true;
            } catch (CaptureException e) {
                throw new IOException(e);
            }
        }
    }

    public void disconnect() {
        if (stream != null) {
            try {
                stream.close();
                connected = false;
            } catch (CaptureException e) {
                e.printStackTrace();
            }
        }
    }

    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    public Object getControl(String className) {
        if (stream != null) {
            return stream.getControl(className);
        }
        return null;
    }

    public Object[] getControls() {
        if (stream != null) {
            return stream.getControls();
        }
        return new Object[0];
    }

    public Time getDuration() {
        return DURATION_UNBOUNDED;
    }

    public void start() throws IOException {
        if ((stream != null) && !started) {
            try {
                stream.start();
                started = true;
            } catch (CaptureException e) {
                throw new IOException(e);
            }
        }
    }

    public void stop() throws IOException {
        if ((stream != null) && started) {
            try {
                stream.stop();
                started = false;
            } catch (CaptureException e) {
                throw new IOException(e);
            }
        }
    }

}
