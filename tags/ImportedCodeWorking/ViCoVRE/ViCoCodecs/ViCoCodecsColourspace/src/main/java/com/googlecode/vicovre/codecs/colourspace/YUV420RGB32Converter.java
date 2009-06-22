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

package com.googlecode.vicovre.codecs.colourspace;

import java.awt.Dimension;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.codecs.utils.QuickArray;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;
import com.googlecode.vicovre.codecs.utils.QuickArrayWrapper;

/**
 * A convertor for YUV420 to RGB32
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class YUV420RGB32Converter implements Codec {

    private static final int DEFAULT_RED_MASK = 0xFF0000;

    private static final int DEFAULT_GREEN_MASK = 0x00FF00;

    private static final int DEFAULT_BLUE_MASK = 0x0000FF;

    private YUVFormat inputFormat = null;

    private RGBFormat outputFormat = null;

    private int redMask = 0;

    private int greenMask = 0;

    private int blueMask = 0;

    private QuickArray uvtab = null;

    private QuickArray lumtab = null;

    private QuickArray satR = null;

    private QuickArray satG = null;

    private QuickArray satB = null;

    private int rlose = 0;

    private int rshift = 0;

    private int glose = 0;

    private int gshift = 0;

    private int blose = 0;

    private int bshift = 0;

    /**
     *
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new Format[]{
                new YUVFormat(null, Format.NOT_SPECIFIED, Format.byteArray,
                Format.NOT_SPECIFIED, YUVFormat.YUV_420, Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED, Format.NOT_SPECIFIED)};
    }

    /**
     *
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format inputFormat) {
        if (inputFormat == null) {
            return new Format[]{
                new RGBFormat(null, Format.NOT_SPECIFIED,
                        Format.intArray,
                        Format.NOT_SPECIFIED, 32, DEFAULT_RED_MASK,
                        DEFAULT_GREEN_MASK, DEFAULT_BLUE_MASK,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED)
            };
        }
        if (inputFormat instanceof YUVFormat) {
            YUVFormat yuv = (YUVFormat) inputFormat;
            if ((yuv.getYuvType() == YUVFormat.YUV_420)
                    && yuv.getDataType().equals(Format.byteArray)) {
                return new Format[] {
                    new RGBFormat(yuv.getSize(), Format.NOT_SPECIFIED,
                            Format.intArray,
                            yuv.getFrameRate(), 32, DEFAULT_RED_MASK,
                            DEFAULT_GREEN_MASK, DEFAULT_BLUE_MASK,
                            Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED, Format.NOT_SPECIFIED)
                };
            }
        }
        return null;
    }

    private static double limit(double x) {
        return (((x) < -128) ? -128 : (((x) > 127) ? 127 : (x)));
    }

    // Fill up the UV lookup table
    private void updateUVTable() {
        int r, g, b;
        double uf, vf;
        double theta = (0.5 - 0.5) * 3.14159;
        double ufa, vfa;
        double costheta = Math.cos(theta);
        double sintheta = Math.sin(theta);
        for (int u = 0; u < 256; ++u) {

            uf = u - 128;
            for (int v = 0; v < 256; ++v) {
                vf = v - 128;
                ufa = (uf * costheta + vf * sintheta);
                vfa = (vf * costheta - uf * sintheta);
                r = (int) (limit(vfa * 1.596) + 128);
                b = (int) (limit(ufa * 2.016) + 128);
                g = (int) (limit(ufa * -0.392 - vfa * 0.813) + 128);

                // Store XBGR in uvtab_ table
                uvtab.setInt((u << 8) | v,
                        ((r & 0xFF) <<  0)
                        | ((g & 0xFF) <<  8)
                        | ((b & 0xFF) << 16));
            }
        }
    }

    // Fill up the saturation table. Multiply the color by the brightness
    // and add the tint component for each color
    private void updateSaturationTable() {
        int s, val;
        int ycor = -16;
        double gamma = 1.164;

        for (s = 0; s < 256; s++) {
            val = s;
            val = (int) ((val + ycor) * gamma + 128);
            if (val > 383) {
                val = 383;
            }
            if (val < 0) {
                val = 0;
            }

            lumtab.setInt(s, val);
        }

        for (s = 0; s < 256; s++) {
            val = s;
            satR.setInt(s + 256, ((val & 0xFF) >> rlose) << rshift);
            satG.setInt(s + 256, ((val & 0xFF) >> glose) << gshift);
            satB.setInt(s + 256, ((val & 0xFF) >> blose) << bshift);
        }

        for (s = 0; s < 256; s++) {
            satR.setInt(s, satR.getInt(256));
            satG.setInt(s, satG.getInt(256));
            satB.setInt(s, satB.getInt(256));

            satR.setInt(s + 512, satR.getInt(511));
            satG.setInt(s + 512, satG.getInt(511));
            satB.setInt(s + 512, satB.getInt(511));
        }

    }

    private int onePix(int r, int g, int b, int y) {
        int t = (short) (lumtab.getInt(y) & 0xFFFF);
        int sr = satR.getInt(r + t);
        int sg = satG.getInt(g + t);
        int sb = satB.getInt(b + t);
        return sr | sg | sb;
    }

    /**
     *
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {
        Object inObject = input.getData();
        Object outObject = output.getData();
        if (inObject == null) {
            return BUFFER_PROCESSED_FAILED;
        }

        Dimension outSize = ((YUVFormat) input.getFormat()).getSize();

        if ((outObject == null)
                || ((((int[]) outObject).length - output.getOffset())
                    < (outSize.width * outSize.height))) {
            outObject = new int[outSize.width * outSize.height];
        }

        QuickArrayWrapper in = null;
        QuickArrayWrapper out = null;
        try {
            in = new QuickArrayWrapper(inObject);
            out = new QuickArrayWrapper(outObject);
        } catch (QuickArrayException e) {
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
        }

        int w = outSize.width;
        int iw = inputFormat.getSize().width;

        int outPos = output.getOffset();
        int y = input.getOffset() + inputFormat.getOffsetY();
        int u = input.getOffset() + inputFormat.getOffsetU();
        int v = input.getOffset() + inputFormat.getOffsetV();
        for (int len = outSize.width * outSize.height; len > 0; len -= 4) {

            int uVal = in.getByte(u) & 0xFF;
            int vVal = in.getByte(v) & 0xFF;
            int yVal = in.getByte(y) & 0xFF;
            int yPlus1Val = in.getByte(y + 1) & 0xFF;

            int sum = uvtab.getInt((uVal << 8) | vVal);
            int r = sum & 0xFF;
            int g = (sum >> 8) & 0xFF;
            int b = (sum >> 16) & 0xFF;

            out.setInt(outPos, onePix(r, g, b, yVal));
            out.setInt(outPos + 1, onePix(r, g, b, yPlus1Val));

            outPos += 2;
            y += 2;
            u += 1;
            v += 1;

            w -= 2;
            if (w <= 0) {
                w = outSize.width;
                y += 2 * iw - w;
                u += (iw - w) >> 1;
                v += (iw - w) >> 1;
                outPos += (2 * outSize.width - w);
            }
        }

        outPos = output.getOffset();
        y = input.getOffset() + inputFormat.getOffsetY();
        u = input.getOffset() + inputFormat.getOffsetU();
        v = input.getOffset() + inputFormat.getOffsetV();
        for (int len = outSize.width * outSize.height; len > 0; len -= 4) {

            int uVal = in.getByte(u) & 0xFF;
            int vVal = in.getByte(v) & 0xFF;
            int yVal = in.getByte(y + iw) & 0xFF;
            int yPlus1Val = in.getByte(y + iw + 1) & 0xFF;

            int sum = uvtab.getInt((uVal << 8) | vVal);
            int r = sum & 0xFF;
            int g = (sum >> 8) & 0xFF;
            int b = (sum >> 16) & 0xFF;

            out.setInt(outPos + outSize.width, onePix(r, g, b, yVal));
            out.setInt(outPos + outSize.width + 1, onePix(r, g, b, yPlus1Val));

            outPos += 2;
            y += 2;
            u += 1;
            v += 1;

            w -= 2;
            if (w <= 0) {
                w = outSize.width;
                y += 2 * iw - w;
                u += (iw - w) >> 1;
                v += (iw - w) >> 1;
                outPos += (2 * outSize.width - w);
            }
        }

        output.setData(outObject);
        output.setOffset(0);
        output.setLength(outSize.width * outSize.height);
        output.setFormat(new RGBFormat(outSize,
                outSize.width * outSize.height, Format.intArray,
                Format.NOT_SPECIFIED, 32, redMask, greenMask, blueMask,
                1, outSize.width, Format.FALSE, Format.NOT_SPECIFIED));
        output.setDiscard(false);
        output.setEOM(false);
        return BUFFER_PROCESSED_OK;
    }

    /**
     *
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format inputFormat) {
        if (inputFormat instanceof YUVFormat) {
            YUVFormat yuv = (YUVFormat) inputFormat;
            if ((yuv.getYuvType() == YUVFormat.YUV_420)
                    && yuv.getDataType().equals(Format.byteArray)) {
                this.inputFormat = yuv;
                return yuv;
            }
        }
        return null;
    }

    /**
     *
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format outputFormat) {
        if (outputFormat instanceof RGBFormat) {
            RGBFormat rgb = (RGBFormat) outputFormat;
            if ((rgb.getBitsPerPixel() == 32)
                    && rgb.getDataType().equals(Format.intArray)) {
                this.outputFormat = rgb;
                return rgb;
            }
        }
        return null;
    }

    /**
     *
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        uvtab.free();
        lumtab.free();
        satR.free();
        satG.free();
        satB.free();
    }

    /**
     *
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return getClass().getName();
    }

    private static int mtos(int mask) {
        int shift = 0;
        if (mask != 0) {
            while ((mask & 1) == 0) {
                mask >>= 1;
                ++shift;
            }
        }
        return (shift);
    }

    /**
     *
     * @see javax.media.PlugIn#open()
     */
    public void open() throws ResourceUnavailableException {
        if ((inputFormat == null) || (outputFormat == null)) {
            throw new ResourceUnavailableException("Formats not set");
        }

        redMask = outputFormat.getRedMask();
        if (redMask == Format.NOT_SPECIFIED) {
            redMask = DEFAULT_RED_MASK;
        }
        greenMask = outputFormat.getGreenMask();
        if (greenMask == Format.NOT_SPECIFIED) {
            greenMask = DEFAULT_GREEN_MASK;
        }
        blueMask = outputFormat.getBlueMask();
        if (blueMask == Format.NOT_SPECIFIED) {
            blueMask = DEFAULT_BLUE_MASK;
        }

        rshift = mtos(redMask);
        rlose = 8 - mtos(~(redMask >> rshift));
        gshift = mtos(greenMask);
        glose = 8 - mtos(~(greenMask >> gshift));
        bshift = mtos(blueMask);
        blose = 8 - mtos(~(blueMask >> bshift));

        try {
            uvtab = new QuickArray(int[].class, 65536);
            updateUVTable();

            lumtab = new QuickArray(int[].class, 256);
            satR = new QuickArray(int[].class, 768);
            satG = new QuickArray(int[].class, 768);
            satB = new QuickArray(int[].class, 768);
            updateSaturationTable();
        } catch (QuickArrayException e) {
            e.printStackTrace();
            throw new ResourceUnavailableException("Error creating arrays");
        }
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
    public Object getControl(String className) {
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
