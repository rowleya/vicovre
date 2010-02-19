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

package com.googlecode.vicovre.codecs.ffmpeg;

import java.awt.Dimension;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;

import com.googlecode.vicovre.codecs.utils.BitInputStream;
import com.googlecode.vicovre.codecs.utils.BitOutputStream;
import com.googlecode.vicovre.codecs.utils.QuickArray;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;

public class H261RTPDecoder implements Codec {

    /**
     * MBA huffman table for encoding
     */
    private static final int[][] MBAHUFF = new int[][]{
        {1, 1},
        {3, 3},
        {2, 3},
        {3, 4},
        {2, 4},
        {3, 5},
        {2, 5},
        {7, 7},
        {6, 7},
        {11, 8},
        {10, 8},
        {9, 8},
        {8, 8},
        {7, 8},
        {6, 8},
        {23, 10},
        {22, 10},
        {21, 10},
        {20, 10},
        {19, 10},
        {18, 10},
        {35, 11},
        {34, 11},
        {33, 11},
        {32, 11},
        {31, 11},
        {30, 11},
        {29, 11},
        {28, 11},
        {27, 11},
        {26, 11},
        {25, 11},
        {24, 11},
    };

    private static final VideoFormat INPUT_FORMAT =
        new VideoFormat(VideoFormat.H261_RTP, null, Format.NOT_SPECIFIED,
                Format.byteArray, Format.NOT_SPECIFIED);

    private static final VideoFormat OUTPUT_FORMAT =
        new VideoFormat(VideoFormat.H261, new Dimension(352, 288),
                Format.NOT_SPECIFIED,
                Format.byteArray, Format.NOT_SPECIFIED);

    private static final int MBA_STUFF = -2;

    private static final int GOB_HEADER = -1;

    private H261Decoder decoder = new H261Decoder();

    private byte[] outData = new byte[352 * 288 * 4];

    private Buffer buffer = new Buffer();

    private QuickArray mbaHuff = null;

    private int mbaHuffMaxLen = 0;

    public H261RTPDecoder() {
        try {
            mbaHuffMaxLen = 16;
            mbaHuff = makeHuff(MBAHUFF, mbaHuffMaxLen);
            addCode(0xf, 11,  MBA_STUFF, mbaHuff, mbaHuffMaxLen);
            addCode(0x1, 16, GOB_HEADER, mbaHuff, mbaHuffMaxLen);
        } catch (QuickArrayException e) {
            e.printStackTrace();
        }
        addCode(0xf, 11, -2, mbaHuff, mbaHuffMaxLen);
        buffer.setData(outData);
        buffer.setFormat(OUTPUT_FORMAT);
        buffer.setOffset(0);
        decoder.setLogLevel(Log.AV_LOG_DEBUG);
    }

    /**
     *
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new VideoFormat[]{INPUT_FORMAT};
    }

    /**
     *
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return decoder.getSupportedOutputFormats(null);
        }
        if (input.matches(INPUT_FORMAT)) {
            return decoder.getSupportedOutputFormats(OUTPUT_FORMAT);
        }
        return new Format[0];
    }

    /**
     *
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {
        byte[] inData = (byte[]) input.getData();
        int inOffset = input.getOffset();
        int inLength = input.getLength();

        try {
            BitOutputStream out = new BitOutputStream(outData, 0);
            BitInputStream in = new BitInputStream(inData, inOffset, inLength);
            int sbit = in.readBits(3);
            int ebit = in.readBits(3);
            in.readBits(1);
            in.readBits(1);
            int gob = in.readBits(4);
            int mbap = in.readBits(5) + 1;
            int quant = in.readBits(5);
            in.readBits(5);
            in.readBits(5);
            in.readBits(sbit);

            out.add(1, 16);
            out.add(0, 4);
            out.add(0, 5);
            out.add(4, 6);
            out.add(0, 1);
            if (gob != 0) {
                out.add(1, 16);
                out.add(gob, 4);
                out.add(quant, 5);
                out.add(0, 1);

                // read the mba from the last
                int mbadiff = in.huffDecode(mbaHuff, mbaHuffMaxLen);
                int mba = mbadiff + mbap;
                out.add(MBAHUFF[mba][0], MBAHUFF[mba][1]);
            } else {
                int mbadiff = in.huffDecode(mbaHuff, mbaHuffMaxLen);
                if (mbadiff == GOB_HEADER) {
                    gob = in.readBits(4);
                    if (gob == 0) {
                        in.readBits(5);
                        in.readBits(6);
                        while (in.readBits(1) == 1) {
                            in.readBits(8);
                        }
                    } else {
                        out.add(gob, 4);
                    }
                }
            }
            while (in.bitsRemaining() > 16) {
                out.add(in.readBits(16), 16);
            }
            int bitsToAdd = in.bitsRemaining() - ebit;
            if (bitsToAdd > 0) {
                out.add(in.readBits(bitsToAdd), bitsToAdd);
            }
            out.flush();
            buffer.setOffset(0);
            buffer.setLength(out.getLength());
            buffer.setTimeStamp(input.getTimeStamp());
            buffer.setSequenceNumber(input.getSequenceNumber());
            int result = INPUT_BUFFER_NOT_CONSUMED;
            while (result == INPUT_BUFFER_NOT_CONSUMED) {
                result = decoder.process(buffer, output);
            }
            if ((input.getFlags() & Buffer.FLAG_RTP_MARKER) != 0) {
                return result;
            }
            return OUTPUT_BUFFER_NOT_FILLED;
        } catch (QuickArrayException e) {
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
        }
    }

    private void addCode(int code, int length, int val, QuickArray huff,
            int maxLen) {
        int nbit = maxLen - length;
        int map = (val << 5) | length;
        code = (code & ((1 << maxLen) - 1)) << nbit;
        for (int n = (1 << nbit) - 1; n >= 0; --n) {
            int c = (code | n);
            huff.setShort(c, (short) map);
        }
    }

    private QuickArray makeHuff(int[][] ht, int maxLen)
            throws QuickArrayException {
        int huffSize = 1 << maxLen;
        QuickArray huff = new QuickArray(short[].class, huffSize);
        for (int i = 0; i < huffSize; ++i) {
            huff.setShort(i, (short) ((-2 << 5) | maxLen));
        }
        for (int i = 0; i < ht.length; i++) {
            int length = ht[i][1];
            int code = ht[i][0];
            if (length != 0) {
                addCode(code, length, i / 2, huff, maxLen);
            }
        }
        return huff;
    }

    /**
     *
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format format) {
        if (getSupportedOutputFormats(format).length > 0) {
            decoder.setInputFormat(OUTPUT_FORMAT);
            return format;
        }
        return null;
    }

    /**
     *
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format format) {
        return decoder.setOutputFormat(format);
    }

    /**
     *
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        decoder.close();
    }

    /**
     *
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "H261Depacketizer";
    }

    /**
     *
     * @see javax.media.PlugIn#open()
     */
    public void open() throws ResourceUnavailableException {
        decoder.open();
    }

    /**
     *
     * @see javax.media.PlugIn#reset()
     */
    public void reset() {
        // Does Nothing
    }

    /**
     *
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlType) {
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
