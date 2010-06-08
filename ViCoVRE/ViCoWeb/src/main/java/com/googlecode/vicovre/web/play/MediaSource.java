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

package com.googlecode.vicovre.web.play;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.PlugIn;
import javax.media.ResourceUnavailableException;
import javax.media.format.UnsupportedFormatException;

import com.googlecode.vicovre.media.processor.ProcessorListener;
import com.googlecode.vicovre.media.processor.SimpleProcessor;

public abstract class MediaSource extends Thread implements ProcessorListener {

    private SimpleProcessor processor = null;

    private Multiplexer multiplexer = null;

    private int track = -1;

    private Integer processSync = new Integer(0);

    private Buffer outputBuffer = null;

    private boolean sourceFinished = false;

    private IOException sourceReadException = null;

    private boolean started = false;

    private long timestampOffset = 0;

    public MediaSource(Format inputFormat, Multiplexer multiplexer, int track)
            throws UnsupportedFormatException, ResourceUnavailableException {
        processor = new SimpleProcessor(inputFormat, multiplexer, track);
        this.multiplexer = multiplexer;
        this.track = track;
        processor.addListener(this);
    }

    protected abstract boolean readNextBuffer() throws IOException;

    protected abstract Buffer getBuffer();

    public void setTimestampOffset(long timestampOffset) {
        this.timestampOffset = timestampOffset;
    }

    public void run() {
        while (!sourceFinished) {
            try {
                sourceFinished = !readNextBuffer();
            } catch (IOException e) {
                sourceFinished = true;
                sourceReadException = e;
            }
            if (!sourceFinished) {
                Buffer buffer = getBuffer();
                processor.process(buffer, false);
            }
        }
        synchronized (processSync) {
            processSync.notifyAll();
        }
    }

    public void close() {
        synchronized (processSync) {
            sourceFinished = true;
            outputBuffer = null;
            processSync.notifyAll();
        }
    }

    public void finishedProcessing(Buffer buffer) {
        synchronized (processSync) {
            outputBuffer = buffer;
            processSync.notifyAll();

            while (outputBuffer != null) {
                try {
                    processSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }
    }

    public boolean readNext() throws IOException {
        synchronized (processSync) {
            outputBuffer = null;
            processSync.notifyAll();
            if (!started) {
                start();
                started = true;
            }
            while (!sourceFinished && (outputBuffer == null)) {
                try {
                    processSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            if (sourceReadException != null) {
                throw sourceReadException;
            }
            return !sourceFinished;
        }
    }

    public long getTimestamp() {
        return outputBuffer.getTimeStamp();
    }

    public void setTimestamp(long timestamp) {
        outputBuffer.setTimeStamp(timestamp);
    }

    public void process() {
        synchronized (processSync) {
            while (!sourceFinished && (outputBuffer == null)) {
                try {
                    processSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }

            if (!sourceFinished) {
                int result = PlugIn.INPUT_BUFFER_NOT_CONSUMED;
                while (result == PlugIn.INPUT_BUFFER_NOT_CONSUMED) {
                    outputBuffer.setTimeStamp(outputBuffer.getTimeStamp()
                            + timestampOffset);
                    result = multiplexer.process(outputBuffer, track);
                }
            }
        }
    }

    public Object getControl(String className) {
        return processor.getControl(className);
    }

}
