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

import javax.media.Format;
import javax.media.format.VideoFormat;

/**
 * A depacketizer for the RFC H.264 RTP format
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class JavaDepacketizer extends JavaAbstractDepacketizer {

    private static final String INPUT = "H264/RTP";

    private static final byte[] START_SEQUENCE = new byte[]{0, 0, 1};

    /*private static final byte[] SPS = new byte[]{
        0x67, 0x42, (byte) 0xc0, 0x0c, (byte) 0x92, 0x54, 0x0a, 0x0f,
        (byte) 0xd0, (byte) 0x80, 0x00, 0x00, 0x03, 0x00, (byte) 0x80, 0x00,
        0x00, 0x08, 0x47, (byte) 0x8a, 0x15, 0x50};

    private static final byte[] PPS = new byte[]{
        0x68, (byte) 0xce, 0x3c, (byte) 0x80};

    private boolean firstPacketDecoded = false; */

    private int indexOffset = 0;

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
        return "H264 Depacketizer";
    }

    /**
     *
     * @see com.googlecode.vicovre.codecs.h264.JavaAbstractDepacketizer#
     *     handleData(byte[], int, int)
     */
    protected boolean handleData(int index, byte[] in, int offset, int length) {
        byte nal = in[offset];
        byte type = (byte) (nal & 0x1f);
        if (type >= 1 && type <= 23) {
            type = 1;
        }
        /*if (!firstPacketDecoded) {
            firstPacketDecoded = true;
            define(index, SPS.length + START_SEQUENCE.length);
            offsetCopy(index, 0, START_SEQUENCE, 0, START_SEQUENCE.length);
            offsetCopy(index, START_SEQUENCE.length, SPS, 0, SPS.length);
            define(index + 1, PPS.length + START_SEQUENCE.length);
            offsetCopy(index + 1, 0, START_SEQUENCE, 0, START_SEQUENCE.length);
            offsetCopy(index + 1, START_SEQUENCE.length, PPS, 0, PPS.length);
            indexOffset = 2;
        } */
        int ind = index + indexOffset;

        switch (type) {

        case 1:
            define(ind, length + START_SEQUENCE.length);
            offsetCopy(ind, 0, START_SEQUENCE, 0, START_SEQUENCE.length);
            offsetCopy(ind, START_SEQUENCE.length, in, offset, length);
            break;

        case 24:
            int end = offset + length;
            int pos = offset + 1;
            int totalBytes = 0;
            while ((end - pos) > 2) {
                int size = (in[pos] << 8) | in[pos + 1];
                totalBytes += size + START_SEQUENCE.length;
                pos += 2 + size;
                if (pos > end) {
                    System.err.println("Consumed too many bytes!");
                    return false;
                }
            }

            define(ind, totalBytes);
            pos = offset + 1;
            int packetPos = 0;
            while ((end - pos) > 2) {
                int size = (in[pos] << 8) | in[pos + 1];
                offsetCopy(ind, packetPos, START_SEQUENCE, 0,
                        START_SEQUENCE.length);
                pos += 2;
                packetPos += START_SEQUENCE.length;

                offsetCopy(ind, packetPos, in, pos, size);
                pos += size;
                packetPos += size;
            }
            break;

        case 28:
            byte fuIndicator = nal;
            byte fuHeader = in[offset + 1];
            boolean startBit = ((fuHeader & 0x80) >> 7) > 0;
            byte nalType = (byte) (fuHeader & 0x1f);

            byte reconstructedNal = (byte) (fuIndicator & 0xe0);
            reconstructedNal |= nalType;

            if (startBit) {
                define(ind, START_SEQUENCE.length + (length - 2) + 1);
                offsetCopy(ind, 0, START_SEQUENCE, 0, START_SEQUENCE.length);
                offsetCopy(ind, START_SEQUENCE.length,
                        new byte[]{reconstructedNal}, 0, 1);
                offsetCopy(ind, START_SEQUENCE.length + 1, in, offset + 2,
                        length - 2);
            } else {
                copy(ind, in, offset + 2, length - 2);
            }
            break;

        case 25:
        case 26:
        case 27:
        case 29:
            System.err.println("Unhandled H.264 NAL Type " + type);
            return false;


        default:
            System.err.println("Undefined H.264 NAL Type 0");
            return false;
        }

        return true;
    }

    /**
     *
     * @see com.googlecode.vicovre.codecs.h264.JavaAbstractDepacketizer#
     *     newPacketSet()
     */
    protected void newPacketSet() {
        indexOffset = 0;
    }

}
