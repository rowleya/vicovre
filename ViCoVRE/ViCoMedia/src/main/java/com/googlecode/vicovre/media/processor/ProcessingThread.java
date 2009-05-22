/**
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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


package com.googlecode.vicovre.media.processor;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferStream;

/**
 * A thread to process a datasource
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ProcessingThread extends DataSink implements PushBufferStream {

    private SimpleProcessor processor = null;

    private BufferTransferHandler handler = null;

    /**
     * A thread for processing a track from a data source
     *
     * @param dataSource The data source
     * @param track The track to process
     * @param processor The processor to use
     */
    public ProcessingThread(DataSource dataSource, int track,
            SimpleProcessor processor) {
        super(dataSource, track);
        this.processor = processor;
    }

    /**
     *
     * @see com.googlecode.vicovre.media.processor.DataSink#handleBuffer(
     *     javax.media.Buffer)
     */
    public void handleBuffer(Buffer buffer) {
        int status = processor.process(buffer);
        if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
            System.err.println("Plug in Error!");
            close();
        }
    }

    /**
     * Indicates that the processor has finished a frame
     */
    public void finishedProcessing() {
        if (handler != null) {
            handler.transferData(this);
        }
    }

    /**
     *
     * @see javax.media.protocol.PushBufferStream#getFormat()
     */
    public Format getFormat() {
        return processor.getOutputFormat();
    }

    /**
     *
     * @see javax.media.protocol.PushBufferStream#read(javax.media.Buffer)
     */
    public void read(Buffer buffer) {
        buffer.copy(processor.getOutputBuffer(), true);
    }

    /**
     *
     * @see javax.media.protocol.PushBufferStream#setTransferHandler(
     *     javax.media.protocol.BufferTransferHandler)
     */
    public void setTransferHandler(BufferTransferHandler handler) {
        System.err.println("Setting transfer handler");
        this.handler = handler;
    }

    /**
     *
     * @see javax.media.protocol.SourceStream#endOfStream()
     */
    public boolean endOfStream() {
        return false;
    }

    /**
     *
     * @see javax.media.protocol.SourceStream#getContentDescriptor()
     */
    public ContentDescriptor getContentDescriptor() {
        if (getFormat().getEncoding().toLowerCase().endsWith("rtp")) {
            System.err.println("Output is RTP");
            return new ContentDescriptor(ContentDescriptor.RAW_RTP);
        }
        return new ContentDescriptor(ContentDescriptor.RAW);
    }

    /**
     *
     * @see javax.media.protocol.SourceStream#getContentLength()
     */
    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    /**
     *
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String clss) {
        return null;
    }

    /**
     *
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return new Object[0];
    }

}
