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

package com.googlecode.vicovre.codecs.h261;

import java.awt.Component;
import java.awt.Dimension;
import java.text.DecimalFormat;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;


import com.googlecode.vicovre.codecs.controls.FrameFillControl;
import com.googlecode.vicovre.codecs.utils.BitInputStream;
import com.googlecode.vicovre.codecs.utils.QuickArray;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;
import com.googlecode.vicovre.codecs.utils.QuickArrayWrapper;

/**
 * A decoder for H261AS
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class H261ASDecoder extends H261AbstractDecoder
        implements FrameFillControl {

    private YUVFormat outputFormat = null;

    private long sequence = 0;

    private QuickArray blockPos = null;

    private byte[] frameData = null;

    private byte[] outputObject = null;

    private QuickArrayWrapper out = null;

    private int y1Offset = 0;

    private int y2Offset = 0;

    private int y3Offset = 0;

    private int y4Offset = 0;

    private int crOffset = 0;

    private int cbOffset = 0;

    /**
     * Creates a new H261ASDecoder
     * @throws QuickArrayException
     *
     */
    public H261ASDecoder() throws QuickArrayException {
        super();
    }

    /**
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new Format[]{new VideoFormat("h261as/rtp", null,
                Format.NOT_SPECIFIED, Format.byteArray, Format.NOT_SPECIFIED)};
    }

    /**
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return new Format[]{new YUVFormat(YUVFormat.YUV_420)};
        }
        if (input.getEncoding().equals("h261as/rtp")) {
            VideoFormat format = (VideoFormat) input;
            Dimension size = format.getSize();
            if ((size == null) || (size.width < 1) || (size.height < 1)) {
                size = new Dimension(320, 240);
            }
            int ysize = size.width * size.height;
            int csize = (size.width / 2) * (size.height / 2);
            return new Format[]{new YUVFormat(size, Format.NOT_SPECIFIED,
                    Format.byteArray, format.getFrameRate(), YUVFormat.YUV_420,
                    0, ysize, ysize + csize, size.width, size.width / 2)};
        }
        return null;
    }

    /**
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {

        BitInputStream in = null;
        try {
            in = new BitInputStream((byte[]) input.getData(),
                input.getOffset(), input.getLength());
        } catch (QuickArrayException e) {
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
        }

        // Read the H261AS header
        int ebit = in.readBits(3);
        int quant = in.readBits(5);
        int width = (in.readBits(12) + 1) << 4;
        int height = (in.readBits(12) + 1) << 4;
        int ysize = width * height;
        int csize = (width / 2) * (height / 2);
        int dataSize = ysize + (2 * csize);

        int qt = quant << 8;

        int offset = output.getOffset();
        if (outputObject == null) {
            outputObject = new byte[dataSize];
            try {
                out = new QuickArrayWrapper(outputObject);
            } catch (QuickArrayException e) {
                e.printStackTrace();
                return BUFFER_PROCESSED_FAILED;
            }
            offset = 0;
            output.setOffset(offset);
            if (frameData != null) {
                System.arraycopy(frameData, 0, outputObject, 0,
                        Math.max(frameData.length, dataSize));
                frameData = null;
            }
        }
        output.setLength(dataSize);

        if (outputFormat == null) {
            VideoFormat inputFormat = (VideoFormat) input.getFormat();
            outputFormat = new YUVFormat(new Dimension(width, height),
                    dataSize, Format.byteArray,
                    inputFormat.getFrameRate(), YUVFormat.YUV_420,
                    width, width / 2, 0, ysize, ysize + csize);
            int nBlocksWidth = width / 16;
            int nBlocksHeight = height / 16;
            int nBlocks = nBlocksWidth * nBlocksHeight;
            int nGobs = nBlocks / 33;
            if ((nGobs * 33) < nBlocks) {
                nGobs += 1;
            }
            try {
                blockPos = new QuickArray(int[].class, nBlocks);
            } catch (QuickArrayException e) {
                e.printStackTrace();
                return BUFFER_PROCESSED_FAILED;
            }
            for (int i = 0; i < nBlocks; i++) {
                int blocky = i / nBlocksWidth;
                int blockx = i % nBlocksWidth;
                int x = blockx * 16;
                int y = blocky * 16;
                blockPos.setInt(i,
                        ((x & 0xffff) << 16) | (y & 0xffff));
            }
            y1Offset = 0;
            y2Offset = 8;
            y3Offset = 8 * width;
            y4Offset = (8 * width) + 8;
            crOffset = ysize;
            cbOffset = ysize + csize;
        }

        int gob = 0;
        int mba = 0;
        while (in.bitsRemaining() > ebit) {
            int mbadiff = readMba(in);
            if (mbadiff == -1) {

                // Read the rest of the GOB Header
                gob = ((in.readBits(10) & 0x3FF)) << 10
                    | (in.readBits(10) & 0x3FF);
                quant = in.readBits(5);
                qt = quant << 8;
                mba = 0;

            } else if (mbadiff >= 0) {

                // Read the rest of the Macroblock
                mba += mbadiff + 1;
                int mtype = readMtype(in);
                System.err.println("Mtype = " + mtype);
                if ((mtype & MT_MQUANT) > 0) {
                    quant = in.readBits(5);
                    qt = quant << 8;
                }
                if ((mtype & MT_CBP) > 0) {
                    System.err.println("CBP unsupported!");
                    return BUFFER_PROCESSED_FAILED;
                }

                int blockNo = (gob * 33) + (mba - 1);
                int x = blockPos.getInt(blockNo);
                int y = x & 0xffff;
                x = (x >> 16) & 0xffff;
                int yOffset = y * width + x;
                int cOffset = ((y / 2) * (width / 2)) + (x / 2);

                if ((mtype & MT_TCOEFF) > 0) {
                    try {
                        readBlock(in, out,
                                yOffset + y1Offset + output.getOffset(), width,
                                true, qt, ebit);
                        readBlock(in, out,
                                yOffset + y2Offset + output.getOffset(), width,
                                true, qt, ebit);
                        readBlock(in, out,
                                yOffset + y3Offset + output.getOffset(), width,
                                true, qt, ebit);
                        readBlock(in, out,
                                yOffset + y4Offset + output.getOffset(), width,
                                true, qt, ebit);
                        readBlock(in, out,
                                cOffset + crOffset + output.getOffset(),
                                width / 2, true, qt, ebit);
                        readBlock(in, out,
                                cOffset + cbOffset + output.getOffset(),
                                width / 2, true, qt, ebit);
                    } catch (QuickArrayException e) {
                        e.printStackTrace();
                        return BUFFER_PROCESSED_FAILED;
                    }
                }
            } else if (mbadiff != MBA_STUFF) {

                // Only stuffing is recognised
                System.err.println("Unknown code");
                return BUFFER_PROCESSED_FAILED;
            }
        }
        if ((input.getFlags() & Buffer.FLAG_RTP_MARKER) > 0) {
            output.setData(outputObject);
            output.setFormat(outputFormat);
            output.setDiscard(false);
            output.setTimeStamp(input.getTimeStamp());
            output.setSequenceNumber(sequence++);
            return BUFFER_PROCESSED_OK;
        }
        return OUTPUT_BUFFER_NOT_FILLED;
    }

    /**
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format format) {
        if (format.getEncoding().equals("h261as/rtp")) {
            return format;
        }
        return null;
    }

    /**
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format format) {
        if (format instanceof YUVFormat) {
            YUVFormat yuv = (YUVFormat) format;
            YUVFormat testFormat = new YUVFormat(yuv.getSize(),
                    Format.NOT_SPECIFIED, Format.byteArray,
                    Format.NOT_SPECIFIED, YUVFormat.YUV_420,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED);
            if (yuv.matches(testFormat)) {
                return yuv;
            }
        }
        return null;
    }

    /**
     *
     * @see com.googlecode.vicovre.codecs.h261.H261AbstractDecoder#close()
     */
    public void close() {
        super.close();
        blockPos.free();
    }

    /**
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "H261ASDecoder";
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
        if (className.equals("controls.FrameFillControl")) {
            return this;
        }
        return null;
    }

    /**
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return new Object[]{this};
    }

    /**
     * Test method
     * @param args Ignored
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        H261ASEncoder encoder = new H261ASEncoder();
        H261ASDecoder decoder = new H261ASDecoder();

        int ysize = 16 * 16;
        int csize = 8 * 8;
        int dataSize = ysize + (2 * csize);
        YUVFormat yuv = new YUVFormat(new Dimension(16, 16), dataSize,
                Format.byteArray, Format.NOT_SPECIFIED, YUVFormat.YUV_420,
                16, 8, 0, ysize, ysize + csize);
        VideoFormat h261as = new VideoFormat("h261as/rtp");
        encoder.setInputFormat(yuv);
        encoder.setOutputFormat(h261as);
        decoder.setInputFormat(h261as);
        decoder.setOutputFormat(yuv);

        byte[] data = new byte[dataSize];
        for (int i = 0; i < dataSize; i++) {
            data[i] = (byte) ((int) (Math.random() * 255) & 0xFF);
        }

        Buffer inputBuffer = new Buffer();
        inputBuffer.setData(data);
        inputBuffer.setLength(dataSize);
        inputBuffer.setOffset(0);
        inputBuffer.setFormat(yuv);
        Buffer outputBuffer = new Buffer();
        outputBuffer.setData(null);
        outputBuffer.setLength(0);

        encoder.process(inputBuffer, outputBuffer);

        Buffer out = new Buffer();
        decoder.process(outputBuffer, out);

        byte[] outData = (byte[]) out.getData();

        DecimalFormat format = new DecimalFormat(" 000;-000");

        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 16; j++) {
                System.err.print(format.format(data[(i * 16) + j]) + " ");
            }
            System.err.print("    ");
            for (int j = 0; j < 16; j++) {
                System.err.print(format.format(outData[(i * 16) + j]) + " ");
            }
            System.err.println();
        }
        System.err.println();
    }

    /**
     *
     * @see com.googlecode.vicovre.codecs.controls.FrameFillControl#fillFrame(
     *     byte[])
     */
    public void fillFrame(byte[] frameData) {
        this.frameData = frameData;
    }

    /**
     *
     * @see javax.media.Control#getControlComponent()
     */
    public Component getControlComponent() {
        return null;
    }
}
