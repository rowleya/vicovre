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
import javax.media.Format;
import javax.media.format.VideoFormat;

/**
 * A depacketizer for the IOCom H.264 RTP format
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class JavaIoComDepacketizer implements Codec {

    private static final String INPUT = "H264/RTP/IOCOM";

    private static final String OUTPUT = "H264/RTP";

    private static final VideoFormat OUTPUT_FORMAT = new VideoFormat(OUTPUT);

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
        byte[] out = null;

        if (aggregatePacket) {
            out = new byte[(input.getLength() - 20) + 2];
            out[0] = 28 & 0x1f;
            out[1] = 0;
            System.arraycopy(in, input.getOffset() + 20, out, 2,
                    input.getLength() - 20);
        } else {
            out = new byte[(input.getLength() - 24) + 2];
            out[0] = (byte) ((in[input.getOffset() + 24] & 0xe0) | (28 & 0x1f));
            out[1] = (byte) (0x80 | (in[input.getOffset() + 24] & 0x1f));
            System.arraycopy(in, input.getOffset() + 24, out, 2,
                    input.getLength() - 24);
        }
        aggregatePacket = true;

        if ((input.getFlags() & Buffer.FLAG_RTP_MARKER) > 0) {
            aggregatePacket = false;
        }

        output.setFormat(OUTPUT_FORMAT);
        output.setData(out);
        output.setOffset(0);
        output.setLength(out.length);
        output.setFlags(input.getFlags());
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
