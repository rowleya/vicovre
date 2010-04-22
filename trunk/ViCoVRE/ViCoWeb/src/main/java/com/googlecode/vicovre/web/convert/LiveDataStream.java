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

package com.googlecode.vicovre.web.convert;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferStream;

import com.googlecode.vicovre.media.processor.DataSink;
import com.googlecode.vicovre.media.processor.SimpleProcessor;
import com.googlecode.vicovre.media.screencapture.CaptureChangeListener;
import com.googlecode.vicovre.media.screencapture.ChangeDetection;

public class LiveDataStream extends DataSink implements PullBufferStream {

    private Integer bufferSync = new Integer(0);

    private Buffer currentBuffer = null;

    private SimpleProcessor processor = null;

    private Format outputFormat = null;

    private ChangeDetection changeDetection = null;

    private Vector<CaptureChangeListener> listeners =
        new Vector<CaptureChangeListener>();

    public LiveDataStream(DataSource dataSource, int track) {
        super(dataSource, track);
    }

    public Format getFormat() {
        return outputFormat;
    }

    public void read(Buffer buffer) throws IOException {
        synchronized (bufferSync) {
            while (currentBuffer == null) {
                try {
                    bufferSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }

            buffer.copy(currentBuffer);
            Object data = currentBuffer.getData();
            if (data instanceof byte[]) {
                buffer.setData(((byte[]) data).clone());
            } else if (data instanceof int[]) {
                buffer.setData(((int[]) data).clone());
            } else if (data instanceof short[]) {
                buffer.setData(((short[]) data).clone());
            }
            currentBuffer = null;
        }
    }

    public boolean willReadBlock() {
        synchronized (bufferSync) {
            return currentBuffer == null;
        }
    }

    public boolean endOfStream() {
        return false;
    }

    public ContentDescriptor getContentDescriptor() {
        return null;
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public void handleBuffer(Buffer buffer) throws IOException {
        if (processor == null) {
            try {
                Format inputFormat = buffer.getFormat();
                processor = new SimpleProcessor(inputFormat, (Format) null);
                outputFormat = processor.getOutputFormat();
                if (outputFormat instanceof VideoFormat) {
                    changeDetection = new ChangeDetection();
                    changeDetection.setInputFormat(outputFormat);
                    changeDetection.setThreshold(48);
                    double changeThreshold = 1.0 / changeDetection.getNBlocks();
                    changeDetection.setFirstSceneChangeThreshold(
                            changeThreshold);
                    changeDetection.setSceneChangeThreshold(changeThreshold);
                    changeDetection.setImmediatlyNotifyChange(true);
                    for (CaptureChangeListener listener : listeners) {
                        changeDetection.addScreenListener(listener);
                    }
                }
            } catch (UnsupportedFormatException e) {
                throw new IOException(e);
            }
        }

        synchronized (bufferSync) {
            int result = 0;
            do {
                result = processor.process(buffer);
                if ((result == PlugIn.BUFFER_PROCESSED_OK)
                        || (result == PlugIn.INPUT_BUFFER_NOT_CONSUMED)) {
                    currentBuffer = processor.getOutputBuffer();
                    if (changeDetection != null) {
                        changeDetection.process(currentBuffer);
                    }
                    bufferSync.notifyAll();
                    try {
                        bufferSync.wait(1);
                    } catch (InterruptedException e) {
                        // Does Nothing
                    }
                }
            } while (result == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        }
    }

    public void addCaptureChangeListener(CaptureChangeListener listener) {
        if (changeDetection != null) {
            changeDetection.addScreenListener(listener);
        } else {
            listeners.add(listener);
        }
    }

    public BufferedImage getImage() {
        if (changeDetection != null) {
            return changeDetection.getImage();
        }
        return null;
    }

}
