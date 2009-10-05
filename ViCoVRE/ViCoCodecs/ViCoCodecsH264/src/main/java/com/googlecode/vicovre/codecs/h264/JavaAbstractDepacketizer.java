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

package com.googlecode.vicovre.codecs.h264;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.format.VideoFormat;

/**
 * Abstract H.264 Depacketizer
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public abstract class JavaAbstractDepacketizer implements Codec {

    protected static final String OUTPUT = "H264";

    protected static final VideoFormat OUTPUT_FORMAT = new VideoFormat(OUTPUT);

    private byte[][] packets = new byte[1000][];

    private long firstSequence = -1;

    private long lastSequence = 0;

    protected abstract void newPacketSet();

    protected abstract boolean handleData(int index, byte[] in, int offset,
            int length);

    protected void copy(int index, byte[] in, int offset,
            int length) {
        define(index, length);
        offsetCopy(index, 0, in, offset, length);
    }

    protected void define(int index, int length) {
        packets[index] = new byte[length];
    }

    protected void offsetCopy(int index, int start, byte[] in, int offset,
            int length) {
        System.arraycopy(in, offset, packets[index], start, length);
    }

    /**
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {
        byte[] in = (byte[]) input.getData();
        long sequence = input.getSequenceNumber();

        if (Math.abs(lastSequence - sequence) > 5) {
            firstSequence = -1;
        }
        lastSequence = sequence;

        if (firstSequence == -1) {
            newPacketSet();
            firstSequence = sequence;
            lastSequence = firstSequence - 1;
            for (int i = 0; i < packets.length; i++) {
                packets[i] = null;
            }
        }
        int index = (int) (sequence - firstSequence);
        if (index < 0) {
            index = (int) ((0xFFFF - firstSequence) + sequence);
        }

        if (!handleData(index, in, input.getOffset(), input.getLength())) {
            return BUFFER_PROCESSED_FAILED;
        }

        if ((input.getFlags() & Buffer.FLAG_RTP_MARKER) == 0) {
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        firstSequence = -1;

        int noPackets = index + 1;
        int totalBytes = 0;
        for (int i = 0; i < noPackets; i++) {
            if (packets[i] == null) {
                return OUTPUT_BUFFER_NOT_FILLED;
            }
            totalBytes += packets[i].length;
        }

        byte[] bytes = new byte[totalBytes];
        int length = 0;
        for (int i = 0; i < noPackets; i++) {
            System.arraycopy(packets[i], 0, bytes, length,
                    packets[i].length);
            length += packets[i].length;
        }

        output.setFormat(OUTPUT_FORMAT);
        output.setData(bytes);
        output.setOffset(0);
        output.setLength(totalBytes);
        return BUFFER_PROCESSED_OK;
    }


    /**
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        // Does Nothing
    }

    /**
     * @see javax.media.PlugIn#open()
     */
    public void open() {
        // Does Nothing
    }

    /**
     * @see javax.media.PlugIn#reset()
     */
    public void reset() {
        // Does Nothing
    }

    /**
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String className) {
        return null;
    }

    /**
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return new Object[0];
    }

}
