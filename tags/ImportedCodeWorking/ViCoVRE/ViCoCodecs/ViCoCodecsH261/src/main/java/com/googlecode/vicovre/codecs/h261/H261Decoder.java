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
 * A decoder for H261
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class H261Decoder extends H261AbstractDecoder
        implements FrameFillControl {

    private YUVFormat outputFormat = null;

    private long sequence = 0;

    private QuickArray gobPos = null;

    private QuickArray mbPos = null;

    private byte[] frameData = null;

    private byte[] outputObject = null;

    private QuickArrayWrapper out = null;

    private int y1Offset = 0;

    private int y2Offset = 0;

    private int y3Offset = 0;

    private int y4Offset = 0;

    private int crOffset = 0;

    private int cbOffset = 0;

    private int ebit = 0;

    /**
     * Creates a new H261Decoder
     * @throws QuickArrayException
     *
     */
    public H261Decoder() throws QuickArrayException {
        super();
    }

    /**
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new Format[]{new VideoFormat("h261/rtp", null,
                Format.NOT_SPECIFIED, Format.byteArray, Format.NOT_SPECIFIED)};
    }

    /**
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return new Format[]{new YUVFormat(YUVFormat.YUV_420)};
        }
        if (input.getEncoding().equals("h261/rtp")) {
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

        // Read the H261 header
        int sbit = in.readBits(3);
        ebit = in.readBits(3);
        in.readBits(1);
        in.readBits(1);
        int gob = in.readBits(4);
        int mba = in.readBits(5) + 1;
        int quant = in.readBits(5);
        in.readBits(5);
        in.readBits(5);
        int width = 352;
        int height = 288;
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
            int blockWidth = 16;
            int blockHeight = 16;
            int gobWidth = 11 * blockWidth;
            int gobHeight = 3 * blockHeight;
            try {
                gobPos = new QuickArray(int[].class, nGobs);
                mbPos = new QuickArray(int[].class, 33);
            } catch (QuickArrayException e) {
                e.printStackTrace();
                return BUFFER_PROCESSED_FAILED;
            }
            int x = 0;
            int y = 0;
            for (int i = 0; i < nGobs; i++) {
                gobPos.setInt(i, ((x & 0xffff) << 16) | (y & 0xffff));
                x += gobWidth;
                if (x >= width) {
                    x = 0;
                    y += gobHeight;
                }
            }

            x = 0;
            y = 0;
            for (int i = 0; i < 33; i++) {
                mbPos.setInt(i, ((x & 0xffff) << 16) | (y & 0xffff));
                x += blockWidth;
                if (x >= gobWidth) {
                    x = 0;
                    y += blockHeight;
                }
            }

            y1Offset = 0;
            y2Offset = 8;
            y3Offset = 8 * width;
            y4Offset = (8 * width) + 8;
            crOffset = ysize;
            cbOffset = ysize + csize;
        }

        in.readBits(sbit);
        while ((in.bitsRemaining() > ebit) && (gob <= 12)) {
            int mbadiff = readMba(in);
            if (mbadiff == -1) {

                // Read the rest of the GOB Header
                gob = in.readBits(4);
                if (gob == 0) {
                    in.readBits(5);
                    in.readBits(6);
                } else {
                    quant = in.readBits(5);
                    qt = quant << 8;
                }
                while (in.readBits(1) == 1) {
                    in.readBits(8);
                }
                mba = 0;

            } else if (mbadiff >= 0) {

                // Read the rest of the Macroblock
                mba += mbadiff + 1;
                if (mba > 33) {
                    System.err.println("MBA = " + mba + " not allowed!");
                    return BUFFER_PROCESSED_FAILED;
                }

                if (in.bitsRemaining() <= ebit) {
                    return BUFFER_PROCESSED_OK;
                }
                int mtype = readMtype(in);
                if ((mtype & MT_MQUANT) > 0) {
                    quant = in.readBits(5);
                    qt = quant << 8;
                }
                if ((mtype & MT_MVD) > 0) {
                    return BUFFER_PROCESSED_FAILED;
                }
                int cbp = 63;
                if ((mtype & MT_CBP) > 0) {
                    cbp = readCpb(in);
                }

                int x = gobPos.getInt(gob - 1) + mbPos.getInt(mba - 1);
                int y = x & 0xffff;
                x = (x >> 16) & 0xffff;
                int yOffset = y * width + x;
                int cOffset = ((y / 2) * (width / 2)) + (x / 2);

                boolean intra = (mtype & MT_INTRA) > 0;
                if ((mtype & MT_TCOEFF) > 0) {
                    try {
                        if (((cbp & CBP_Y1) > 0) && !readBlock(in, out,
                                yOffset + y1Offset + output.getOffset(), width,
                                intra, qt, ebit)) {
                            return BUFFER_PROCESSED_FAILED;
                        }
                        if (((cbp & CBP_Y2) > 0) && !readBlock(in, out,
                                yOffset + y2Offset + output.getOffset(), width,
                                intra, qt, ebit)) {
                            return BUFFER_PROCESSED_FAILED;
                        }
                        if (((cbp & CBP_Y3) > 0) && !readBlock(in, out,
                                yOffset + y3Offset + output.getOffset(), width,
                                intra, qt, ebit)) {
                            return BUFFER_PROCESSED_FAILED;
                        }
                        if (((cbp & CBP_Y4) > 0) && !readBlock(in, out,
                                yOffset + y4Offset + output.getOffset(), width,
                                intra, qt, ebit)) {
                            return BUFFER_PROCESSED_FAILED;
                        }
                        if (((cbp & CBP_CB) > 0) && !readBlock(in, out,
                              cOffset + crOffset + output.getOffset(),
                              width / 2, intra, qt, ebit)) {
                            return BUFFER_PROCESSED_FAILED;
                        }
                        if (((cbp & CBP_CR) > 0) && !readBlock(in, out,
                              cOffset + cbOffset + output.getOffset(),
                              width / 2, intra, qt, ebit)) {
                            return BUFFER_PROCESSED_FAILED;
                        }
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
            output.setFormat(outputFormat);
            output.setData(outputObject);
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
        if (format.getEncoding().equals("h261/rtp")) {
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
        gobPos.free();
        mbPos.free();
    }

    /**
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "H261Decoder";
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
     *
     * @see com.googlecode.vicovre.codecs.controls.FrameFillControl#
     *     fillFrame(byte[])
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
