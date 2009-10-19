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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.VideoFormat;

/**
 * A depacketizer for the IOCom H.264 RTP format
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class JavaIoComDepacketizer implements Codec {

    private static final String INPUT = "H264/RTP/IOCOM";

    private static final String OUTPUT = "H264";

    private static final VideoFormat OUTPUT_FORMAT = new VideoFormat(OUTPUT);

    private byte[][] packets = new byte[1000][];

    private long firstSequence = -1;

    private long lastSequence = 0;

    private boolean aggregatePacket = false;

    /**
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new Format[]{new VideoFormat(INPUT)};
    }

    /**
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return new Format[]{OUTPUT_FORMAT};
        }
        if (!(input instanceof VideoFormat)) {
            return new Format[0];
        }
        if (!input.getEncoding().equalsIgnoreCase(INPUT)) {
            return new Format[0];
        }
        return new Format[]{OUTPUT_FORMAT};
    }

    /**
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format input) {
        if ((input instanceof VideoFormat)
                && input.getEncoding().equals(INPUT)) {
            return input;
        }
        return null;
    }

    /**
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format output) {
        if ((output instanceof VideoFormat)
                && output.getEncoding().equals(OUTPUT)) {
            return output;
        }
        return null;
    }

    /**
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "H264 IOCom Depacketizer";
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
            aggregatePacket = false;
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

        if (aggregatePacket) {
            packets[index] = new byte[input.getLength() - 20];
            System.arraycopy(in, input.getOffset() + 20, packets[index], 0,
                    input.getLength() - 20);
        } else {
            packets[index] = new byte[input.getLength() - 21];
            System.arraycopy(in, input.getOffset() + 21, packets[index], 0,
                    input.getLength() - 21);
        }
        aggregatePacket = true;

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
