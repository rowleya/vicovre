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

package com.googlecode.vicovre.media.multiplexer;

import java.io.IOException;
import java.util.LinkedList;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.ResourceUnavailableException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

public abstract class BasicMultiplexer implements Multiplexer,
        MultiplexerStream {

    private ContentDescriptor[] contentTypes = null;

    private MultiplexerDataSource dataSource = new MultiplexerDataSource(this);

    private Format[] supportedFormats = null;

    private Buffer buffer = null;

    private int bufferTrack = 0;

    private Integer bufferSync = new Integer(0);

    private boolean done = false;

    private int result = -1;

    private int noTracks = 0;

    private Format[] trackFormats = null;

    private ContentDescriptor contentType = null;

    private int maxTracks = 0;

    private LinkedList<Buffer> bufferedBuffers = new LinkedList<Buffer>();

    private LinkedList<Integer> bufferedTracks = new LinkedList<Integer>();

    private boolean[] trackSeen = null;

    private int tracksToSee = 0;

    private Integer tracksSync = new Integer(0);

    protected BasicMultiplexer(ContentDescriptor[] contentTypes,
            Format[] supportedFormats, int maxTracks) {
        this.contentTypes = contentTypes;
        this.supportedFormats = supportedFormats;
        this.maxTracks = maxTracks;
    }

    public DataSource getDataOutput() {
        return dataSource;
    }

    public Format[] getSupportedInputFormats() {
        return supportedFormats;
    }

    public ContentDescriptor[] getSupportedOutputContentDescriptors(
            Format[] inputs) {
        if (inputs == null) {
            return contentTypes;
        }
        boolean error = false;
        for (int i = 0; i < inputs.length; i++) {
            boolean formatFound = false;
            for (int j = 0; j < supportedFormats.length; j++) {
                if (inputs[i].matches(supportedFormats[j])) {
                    formatFound = true;
                }
            }
            if (!formatFound) {
                error = true;
            }
        }
        if (error) {
            return new ContentDescriptor[0];
        }
        return contentTypes;
    }

    private int doProcess(Buffer buf, int track) {
        synchronized (bufferSync) {
            while ((buffer != null) && !done) {
                try {
                    bufferSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }

            if (!done) {
                result = -1;
                buffer = buf;
                bufferTrack = track;
                bufferSync.notifyAll();
            }

            while (((result == -1) || (buffer != null)) && !done) {
                try {
                    bufferSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }

            if (done) {
                return BUFFER_PROCESSED_OK;
            }

            return result;
        }
    }

    public int process(Buffer buf, int trk) {
        synchronized (tracksSync) {
            if (tracksToSee > 0) {
                bufferedBuffers.addLast((Buffer) buf.clone());
                bufferedTracks.addLast(trk);
                if (!trackSeen[trk]) {
                    trackSeen[trk] = true;
                    tracksToSee--;
                    System.err.println("Track " + trk + " input format = " + buf.getFormat());
                    setInputFormat(buf.getFormat(), trk);
                    if (tracksToSee > 0) {
                        return BUFFER_PROCESSED_OK;
                    }
                }
            }
        }
        if (!bufferedBuffers.isEmpty()) {
            while (!bufferedBuffers.isEmpty()) {
                Buffer buffer = bufferedBuffers.removeFirst();
                int track = bufferedTracks.removeFirst();
                if (doProcess(buffer, track) == BUFFER_PROCESSED_FAILED) {
                    return BUFFER_PROCESSED_FAILED;
                }
            }
            return BUFFER_PROCESSED_OK;
        }
        return doProcess(buf, trk);
    }

    public ContentDescriptor setContentDescriptor(
            ContentDescriptor contentDescriptor) {
        for (ContentDescriptor type : contentTypes) {
            if (type.getContentType().equals(
                    contentDescriptor.getContentType())) {
                contentType = type;
                return contentDescriptor;
            }
        }
        return null;
    }

    public Format setInputFormat(Format format, int track) {
        for (int i = 0; i < supportedFormats.length; i++) {
            if (format.matches(supportedFormats[i])) {
                trackFormats[track] = format;
                return format;
            }
        }
        return null;
    }

    public int setNumTracks(int numtracks) {
        noTracks = numtracks;
        if (numtracks > maxTracks) {
            noTracks = maxTracks;
        }
        trackFormats = new Format[noTracks];
        tracksToSee = noTracks;
        trackSeen = new boolean[noTracks];
        return noTracks;
    }

    public void close() {
        synchronized (bufferSync) {
            done = true;
            bufferSync.notifyAll();
        }
    }

    public void open() throws ResourceUnavailableException {
        // Does Nothing
    }

    public void reset() {
        done = false;
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    protected void setResult(int result, boolean notify) {
        synchronized (bufferSync) {
            if (result == BUFFER_PROCESSED_OK) {
                buffer = null;
            }
            this.result = result;
            if (notify) {
                bufferSync.notifyAll();
            }
        }
    }

    protected abstract int read(byte[] buf, int off, int len, Buffer buffer,
            int track) throws IOException;

    protected abstract int readLast(byte[] buf, int off, int len)
        throws IOException;

    public int read(byte[] buf, int off, int len) throws IOException {
        synchronized (bufferSync) {
            while ((buffer == null) && !done) {
                try {
                    bufferSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }

            if (buffer != null) {
                return read(buf, off, len, buffer, bufferTrack);
            } else if (done) {
                return readLast(buf, off, len);
            }
            return 0;
        }
    }

    public boolean willReadBlock() {
        return (buffer == null) && !done;
    }

    public boolean endOfStream() {
        return done && (buffer == null);
    }

    public ContentDescriptor getContentDescriptor() {
        return contentType;
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    protected int getNoTracks() {
        return noTracks;
    }

    protected Format getTrackFormat(int track) {
        return trackFormats[track];
    }

    protected boolean isDone() {
        return done;
    }
}
