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

package com.googlecode.vicovre.codecs.multiplexers.mp3;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import com.googlecode.vicovre.codecs.multiplexers.MultiplexerDataSource;
import com.googlecode.vicovre.codecs.multiplexers.MultiplexerStream;

public class JavaMultiplexer implements Multiplexer, MultiplexerStream {

    public static final String CONTENT_TYPE = "audio/mpeg";

    private MultiplexerDataSource dataSource = new MultiplexerDataSource(this);

    private final Format[] supportedFormats = new Format[]{
        new VideoFormat(null),
        new AudioFormat(AudioFormat.MPEGLAYER3)
    };

    private Buffer buffer = null;

    private Integer bufferSync = new Integer(0);

    private boolean done = false;

    private int result = -1;

    private boolean sameBuffer = false;

    public DataSource getDataOutput() {
        return dataSource;
    }

    public Format[] getSupportedInputFormats() {
        return supportedFormats;
    }

    public ContentDescriptor[] getSupportedOutputContentDescriptors(
            Format[] inputs) {
        if (inputs == null) {
            return new ContentDescriptor[]{
                new ContentDescriptor(CONTENT_TYPE)
            };
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
        return new ContentDescriptor[]{
            new ContentDescriptor(CONTENT_TYPE)
        };
    }

    public int process(Buffer buf, int track) {
        if (buf.getFormat() instanceof VideoFormat) {
            return BUFFER_PROCESSED_OK;
        }
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
                sameBuffer = false;
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

    public ContentDescriptor setContentDescriptor(
            ContentDescriptor contentDescriptor) {
        if (contentDescriptor.getContentType().equals(CONTENT_TYPE)) {
            return contentDescriptor;
        }
        return null;
    }

    public Format setInputFormat(Format format, int track) {
        for (int i = 0; i < supportedFormats.length; i++) {
            if (format.matches(supportedFormats[i])) {
                return format;
            }
        }
        return null;
    }

    public int setNumTracks(int numtracks) {
        return numtracks;
    }

    public void close() {
        synchronized (bufferSync) {
            done = true;
            bufferSync.notifyAll();
        }
    }

    public String getName() {
        return "MP3 Multiplexer";
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

    public int read(byte[] buf, int off, int len) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean willReadBlock() {
        return (buffer == null) && !done;
    }

    public boolean endOfStream() {
        return done;
    }

    public ContentDescriptor getContentDescriptor() {
        return new ContentDescriptor(CONTENT_TYPE);
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }



}
