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
import javax.media.Codec;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.control.KeyFrameControl;
import javax.media.control.QualityControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.codecs.controls.KeyFrameForceControl;
import com.googlecode.vicovre.codecs.utils.BitOutputStream;
import com.googlecode.vicovre.codecs.utils.ConditionalReplenishment;
import com.googlecode.vicovre.codecs.utils.DCT;
import com.googlecode.vicovre.codecs.utils.QuickArray;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;
import com.googlecode.vicovre.codecs.utils.QuickArrayWrapper;

/**
 * @author Andrew G D Rowley
 * @version 1.0
 */
public abstract class H261AbstractEncoder implements Codec,
        QualityControl, KeyFrameControl, KeyFrameForceControl {

    // The number of bits in the header
    private static final int HEADER_BITS = 32;

    // The maximum sending size of an RTP packet (in bits)
    private static final int MAX_SEND_SIZE = 960 * 8;

    // The size of the level map
    private static final int LEVEL_MAP_SIZE = 8196;

    // The amount to shift each level by in the level map (to take account
    // of the unsigned nature of the levels)
    private static final int LEVEL_MAP_SHIFT = 1024;

    // The threshold position in the zig zag array after which filtered
    // values will be used
    private static final int FILTER_THRESHOLD = 20;

    // The start position of filtered values in the level map
    private static final int LEVEL_MAP_FILTER_SHIFT = 4096;

    // The DC quantization shift
    private static final int DC_QUANT_SHIFT = 3;

    // The DCT values of the first Y block
    private QuickArray y1DCT = null;

    // The DCT values of the second Y block
    private QuickArray y2DCT = null;

    // The DCT values of the third Y block
    private QuickArray y3DCT = null;

    // The DCT values of the fourth Y block
    private QuickArray y4DCT = null;

    // The DCT values of the Cb block
    private QuickArray cbDCT = null;

    // The DCT values of the Cr block
    private QuickArray crDCT = null;

    // The level map
    private QuickArray[] levelmap = new QuickArray[31];

    // The level map for color
    private QuickArray[] levelmapc = new QuickArray[31];

    private QuickArray mbaHuffEncOffset = null;

    private QuickArray runLevelEncOffset = null;

    private DCT dct = new DCT();

    private ConditionalReplenishment cr = null;

    // The quantizer of the frames
    private int hq = 1;
    private int mq = 2;
    private int lq = 4;

    // The number of frames sent
    private int count = 0;

    // The width
    private int width = 0;

    // The height
    private int height = 0;

    private int yStart = 0;

    private int crStart = 0;

    private int cbStart = 0;

    private int yStride = 0;

    private int crcbStride = 0;

    private int nGobs = 0;

    private int nBlocksWidth = 0;

    private int nBlocksHeight = 0;

    private int nBlocks = 0;

    private int framesBetweenKey = 250;

    private int framesSinceLastKey = 0;

    private Format outputFormat = null;

    private Format[] inputFormats = null;

    private Format[] outputFormats = null;

    private String codecName = null;

    /**
     * Creates a new H261AbstractEncoder
     * @param codecName the name of the codec
     * @param inputFormat the format of the input (must be YUV_420)
     * @throws QuickArrayException
     */
    public H261AbstractEncoder(String codecName, YUVFormat inputFormat)
            throws QuickArrayException {

        // Set up the input and output formats
        inputFormats = new Format[]{inputFormat};
        outputFormats = new VideoFormat[1];
        outputFormats[0] = new VideoFormat(codecName);
        this.codecName = codecName;

        y1DCT = new QuickArray(int[].class, 64);
        y2DCT = new QuickArray(int[].class, 64);
        y3DCT = new QuickArray(int[].class, 64);
        y4DCT = new QuickArray(int[].class, 64);
        crDCT = new QuickArray(int[].class, 64);
        cbDCT = new QuickArray(int[].class, 64);
        y1DCT.clear();
        y2DCT.clear();
        y3DCT.clear();
        y4DCT.clear();
        crDCT.clear();
        cbDCT.clear();

        mbaHuffEncOffset = new QuickArray(int[].class,
                H261Constants.MBAHUFF.length);
        for (int i = 0; i < H261Constants.MBAHUFF.length; i++) {
            mbaHuffEncOffset.setInt(i, H261Constants.MBAHUFF[i]);
        }
        runLevelEncOffset = new QuickArray(int[].class,
                H261Constants.RUNLEVELHUFF.length);
        for (int i = 0; i < H261Constants.RUNLEVELHUFF.length; i++) {
            runLevelEncOffset.setInt(i, H261Constants.RUNLEVELHUFF[i]);
        }
    }

    private QuickArray makeLevelMap(int quant, int fthresh)
            throws QuickArrayException {

        QuickArray map = new QuickArray(byte[].class, LEVEL_MAP_SIZE);

        // Set up the level map
        int i;
        map.setByte(LEVEL_MAP_SHIFT, (byte) 0);
        map.setByte(LEVEL_MAP_FILTER_SHIFT + LEVEL_MAP_SHIFT, (byte) 0);
        int q = quant * H261Constants.QUANT_SCALE;
        for (i = 1; i < LEVEL_MAP_SHIFT; ++i) {
            byte l = (byte) (i / q);
            map.setByte(i + LEVEL_MAP_SHIFT, l);
            map.setByte(-i + LEVEL_MAP_SHIFT, (byte) -l);

            if (l <= fthresh) {
                l = 0;
            }
            map.setByte(LEVEL_MAP_FILTER_SHIFT + i + LEVEL_MAP_SHIFT, l);
            map.setByte(LEVEL_MAP_FILTER_SHIFT - i + LEVEL_MAP_SHIFT,
                    (byte) -l);
        }
        return map;
    }

    // Adds a block to the BitVector
    protected void addBlock(BitOutputStream outputdata, QuickArray dctVals,
            QuickArray levelmap) {

         // Add the DC of the block
         int dc = dctVals.getInt(0) >> DC_QUANT_SHIFT;

         // The DC cannot be less than 0 or more than 254
         if (dc <= 0) {
             dc = 1;
         } else if (dc > H261Constants.DC_MAX) {
             dc = H261Constants.DC_MAX;
         }
         outputdata.add(dc, H261Constants.DC_BITS);

         // Add the remaining AC values
         int n = 1;
         int filter = 0;
         while (n < (H261Constants.BLOCK_SIZE * H261Constants.BLOCK_SIZE)) {

             // Add up the number of times a value appears
             int run = 0;
             while ((n < (H261Constants.BLOCK_SIZE * H261Constants.BLOCK_SIZE))
                     && (levelmap.getByte(dctVals.getInt(
                             (H261Constants.ZZY[n] * H261Constants.BLOCK_SIZE)
                                 + H261Constants.ZZX[n])
                             + LEVEL_MAP_SHIFT + filter) == 0)) {
                 n++;
                 run++;
                 if (n == FILTER_THRESHOLD) {
                     filter = LEVEL_MAP_FILTER_SHIFT;
                 }
             }

             // Encode the run-level value
             if (n < (H261Constants.BLOCK_SIZE * H261Constants.BLOCK_SIZE)) {
                 byte level = levelmap.getByte(dctVals.getInt(
                             (H261Constants.ZZY[n] * H261Constants.BLOCK_SIZE)
                                 + H261Constants.ZZX[n])
                             + LEVEL_MAP_SHIFT);
                 int runLevel =
                     (((level + H261Constants.MAX_LEVEL) & 0xFF) << 6) | run;
                 int code = 0;
                 int len = 0;
                 if (level >= -15 && level <= 15) {
                     len = runLevelEncOffset.getInt((runLevel * 2) + 1);
                     if (len != 0) {
                         code = runLevelEncOffset.getInt(runLevel * 2);
                     }
                 }

                 if (len == 0) {
                     len = 20;
                     code = (0x4000) | ((run & 0x3f) << 8) | (level & 0xff);
                 }
                 outputdata.add(code, len);
                 n++;
             }
         }

         // Add the end of the block marker
         outputdata.add(H261Constants.EOB, H261Constants.EOB_BITS);
    }

    protected abstract void finishBuffers(Buffer output,
            BitOutputStream outputdata, int startMquant, long timestamp)
            throws QuickArrayException;


    /**
     * Processes an RGB frame to convert it to H.261AS
     *
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {

        try {
            VideoFormat ivf = (VideoFormat) input.getFormat();

            if (ivf instanceof YUVFormat) {
                if (cr == null) {
                    cr = new ConditionalReplenishment(width, height);
                }
                if (framesSinceLastKey >= framesBetweenKey) {
                    cr.reset();
                    framesSinceLastKey = 0;
                } else {
                    framesSinceLastKey += 1;
                }

                byte[] yuv = (byte[]) input.getData();
                int offset = input.getOffset();
                int currentX = offset % width;
                int currentY = offset / width;

                // Find the current gob number and macroblock
                int blockN = ((currentY / 16) * nBlocksWidth)
                    + (currentX / 16);
                int gob = blockN / 33;
                int mba = blockN % 33;

                // Create the packet
                byte[] bytes = (byte[]) output.getData();
                if (output.getLength() < (MAX_SEND_SIZE >> 2)) {
                    bytes = new byte[MAX_SEND_SIZE >> 2];
                    output.setData(bytes);
                    output.setOffset(0);
                }
                BitOutputStream outputdata = new BitOutputStream(bytes,
                        output.getOffset());

                // Add 32 bits to be used for the header later
                outputdata.add(0, HEADER_BITS);

                // Store the initial values
                int startMquant = lq;
                int mquant = lq;

                for (int gobn = gob; gobn < nGobs; gobn++) {

                    // Write the GOB header
                    int lastMba = -1;
                    outputdata.add(H261Constants.GOB_START, 16); // GBSC
                    outputdata.add(gobn, 20); // GN
                    outputdata.add(mquant, 5); // GQUANT

                    for (int mb = mba; (mb < 33) && (blockN < nBlocks); mb++) {

                        // Decide whether to send this macroblock
                        boolean send = cr.send(blockN);

                        if (send) {

                            int quant = 0;
                            int how = cr.getCrState(blockN);
                            if (how == ConditionalReplenishment.CR_MOTION) {
                                quant = lq;
                            } else if (how == ConditionalReplenishment.CR_BG) {
                                quant = hq;
                            } else {
                                quant = mq;
                            }

                            // DCT the blocks
                            QuickArrayWrapper yuvw =
                                new QuickArrayWrapper(yuv);
                            dct.fdct(yuvw, y1DCT, yStart, currentX,
                                    currentY, yStride);
                            dct.fdct(yuvw, y2DCT, yStart, currentX + 8,
                                    currentY, yStride);
                            dct.fdct(yuvw, y3DCT, yStart, currentX,
                                    currentY + 8, yStride);
                            dct.fdct(yuvw, y4DCT, yStart, currentX + 8,
                                    currentY + 8,  yStride);
                            dct.fdct(yuvw, cbDCT, cbStart, currentX / 2,
                                    currentY / 2, crcbStride);
                            dct.fdct(yuvw, crDCT, crStart, currentX / 2,
                                    currentY / 2, crcbStride);

                            // Check the quantizer is enough for the
                            // macroblock
                            int max = 0;
                            int min = 0;
                            for (int z = 1; z < 64; z++) {
                                max = Math.max(max, y1DCT.getInt(z));
                                min = Math.min(min, y1DCT.getInt(z));
                                max = Math.max(max, y2DCT.getInt(z));
                                min = Math.min(min, y2DCT.getInt(z));
                                max = Math.max(max, y3DCT.getInt(z));
                                min = Math.min(min, y3DCT.getInt(z));
                                max = Math.max(max, y4DCT.getInt(z));
                                min = Math.min(min, y4DCT.getInt(z));
                                max = Math.max(max, crDCT.getInt(z));
                                min = Math.min(min, crDCT.getInt(z));
                                max = Math.max(max, cbDCT.getInt(z));
                                min = Math.min(min, cbDCT.getInt(z));
                            }

                            // Need to requantize
                            if (-min > max) {
                                max = -min;
                            }
                            if (max / quant >= H261Constants.MAX_LEVEL) {
                                while (max / quant
                                        >= H261Constants.MAX_LEVEL) {
                                    quant += 1;
                                }
                            }

                            QuickArray map = levelmap[quant];

                            if (map == null) {
                                levelmap[quant] = makeLevelMap(quant, 1);
                                levelmapc[quant] = makeLevelMap(quant, 2);
                                map = levelmap[quant];
                            }

                            // MB Header
                            int mdiff = mb - lastMba - 1;
                            lastMba = mb;

                            int code = mbaHuffEncOffset.getInt(mdiff * 2);
                            int len = mbaHuffEncOffset.getInt((mdiff * 2) + 1);
                            outputdata.add(code, len);

                            if (mquant != quant) {
                                outputdata.add(1, H261Constants.
                                        MTYPE_INTRA_MQUANT_TCOEFF_BITS); // MTYP
                                outputdata.add(quant,
                                        H261Constants.QUANT_BITS); // MQ
                                 mquant = quant;
                            } else {
                                outputdata.add(1,
                                        H261Constants.MTYPE_INTRA_TCOEFF_BITS);
                            }

                            // MB Data - Y
                            addBlock(outputdata, y1DCT, map);
                            addBlock(outputdata, y2DCT, map);
                            addBlock(outputdata, y3DCT, map);
                            addBlock(outputdata, y4DCT, map);

                            // MB Data - Cb
                            map = levelmapc[quant];
                            addBlock(outputdata, cbDCT, map);

                            // MB Data - Cr
                            addBlock(outputdata, crDCT, map);

                        }

                        // Work out the x and y of the next block
                        currentX += 16;
                        if (currentX >= width) {
                            currentX = 0;
                            currentY += 16;
                        }
                        blockN += 1;

                        // Determine if enough bits have been added to
                        // send this packet
                        if (outputdata.noBits() > MAX_SEND_SIZE) {
                            finishBuffers(output, outputdata,
                                    startMquant, input.getTimeStamp());
                            output.setFlags(output.getFlags()
                                    & ~Buffer.FLAG_RTP_MARKER);
                            input.setOffset((currentY * width) + currentX);
                            return INPUT_BUFFER_NOT_CONSUMED;
                        }
                    }
                    mba = 0;
                }

                finishBuffers(output, outputdata,
                        startMquant, input.getTimeStamp());
                cr.replenish(yuv);
                output.setFlags(output.getFlags()
                        | Buffer.FLAG_RTP_MARKER);
                count++;
                return PlugIn.BUFFER_PROCESSED_OK;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PlugIn.BUFFER_PROCESSED_FAILED;
    }



    /**
     *
     * @see javax.media.control.QualityControl#getPreferredQuality()
     */
    public float getPreferredQuality() {
        return 0.75f;
    }

    /**
     *
     * @see javax.media.control.QualityControl#getQuality()
     */
    public float getQuality() {
        return lq / 31;
    }

    /**
     *
     * @see javax.media.control.QualityControl
     *     #isTemporalSpatialTradeoffSupported()
     */
    public boolean isTemporalSpatialTradeoffSupported() {
        return false;
    }

    /**
     *
     * @see javax.media.control.QualityControl#setQuality(float)
     */
    public float setQuality(float f) {
        lq = (int) ((1 - f) * 31.0f);
        mq = lq / 2;
        hq = 1;
        return f;
    }

    /**
     *
     * @see javax.media.Control#getControlComponent()
     */
    public Component getControlComponent() {
        return null;
    }

    /**
     *
     * @see com.sun.media.BasicPlugIn#getControl(java.lang.String)
     */
    public Object getControl(String className) {
        if (className.equals(QualityControl.class.getCanonicalName())) {
            System.err.println("Quality");
            return this;
        }
        return null;
    }

    /**
     *
     * @see com.sun.media.BasicPlugIn#getControls()
     */
    public Object[] getControls() {
        return new Object[]{this};
    }

    private boolean checkInputFormat(Format input) {
        if (input instanceof RGBFormat) {
            return true;
        } else if (input instanceof YUVFormat) {
            if (!input.matches(inputFormats[0])) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     *
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format in) {
        Format format = in;
        if (format instanceof YUVFormat) {
            YUVFormat vfIn = (YUVFormat) format;
            Dimension size = vfIn.getSize();
            width = size.width;
            height = size.height;
            yStart = vfIn.getOffsetY();
            cbStart = vfIn.getOffsetU();
            crStart = vfIn.getOffsetV();
            yStride = vfIn.getStrideY();
            crcbStride = vfIn.getStrideUV();
            nBlocksWidth = width / 16;
            nBlocksHeight = height / 16;
            nBlocks = nBlocksWidth * nBlocksHeight;
            nGobs = nBlocks / 33;
            if ((nGobs * 33) < nBlocks) {
                nGobs += 1;
            }
        }
        return format;
    }

    /**
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format in) {
        if (in == null) {
            return outputFormats;
        }
        if (!checkInputFormat(in)) {
            return new Format[0];
        }
        VideoFormat vf = (VideoFormat) in;

        return new Format[]{new VideoFormat(codecName, vf.getSize(),
                Format.NOT_SPECIFIED, Format.byteArray, vf.getFrameRate())};
    }

    /**
     *
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return inputFormats;
    }

    /**
     *
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format format) {
        outputFormat = format;
        return format;
    }

    /**
     *
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        y1DCT.free();
        y2DCT.free();
        y3DCT.free();
        y4DCT.free();
        crDCT.free();
        cbDCT.free();
        mbaHuffEncOffset.free();
        runLevelEncOffset.free();
        for (int quant = 0; quant < levelmap.length; quant++) {
            if (levelmap[quant] != null) {
                levelmap[quant].free();
                levelmapc[quant].free();
            }
        }
        cr.close();
        dct.close();
    }

    /**
     *
     * @see javax.media.PlugIn#open()
     */
    public void open() {
        // Does Nothing
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
     * @see javax.media.control.KeyFrameControl#getKeyFrameInterval()
     */
    public int getKeyFrameInterval() {
        return framesBetweenKey;
    }

    /**
     *
     * @see javax.media.control.KeyFrameControl#getPreferredKeyFrameInterval()
     */
    public int getPreferredKeyFrameInterval() {
        return 250;
    }

    /**
     *
     * @see javax.media.control.KeyFrameControl#setKeyFrameInterval(int)
     */
    public int setKeyFrameInterval(int frames) {
        framesBetweenKey = frames;
        return frames;
    }

    /**
     *
     * @see com.googlecode.vicovre.codecs.controls.KeyFrameForceControl#
     *     nextFrameKey()
     */
    public void nextFrameKey() {
        framesSinceLastKey = framesBetweenKey;
    }

    protected Format getOutputFormat() {
        return outputFormat;
    }

    protected int getWidth() {
        return width;
    }

    protected int getHeight() {
        return height;
    }

}
