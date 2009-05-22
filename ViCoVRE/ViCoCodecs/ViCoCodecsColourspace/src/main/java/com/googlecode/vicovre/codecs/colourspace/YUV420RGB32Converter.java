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
import java.lang.reflect.Field;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;

import sun.misc.Unsafe;

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

    private Unsafe unsafe = null;

    private long intSize = 0;

    private long inOffset = 0;

    private long outOffset = 0;

    private long byteArrayOffset = 0;

    private long intArrayOffset = 0;

    private Object inObject = null;

    private Object outObject = null;

    private long uvtab = 0;

    private long lumtab = 0;

    private long satR = 0;

    private long satG = 0;

    private long satB = 0;

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
            if ((yuv.getYuvType() == YUVFormat.YUV_420) &&
                    yuv.getDataType().equals(Format.byteArray)) {
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

    private static final double LIMIT(double x) {
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
                r = (int) (LIMIT(vfa * 1.596) + 128);
                b = (int) (LIMIT(ufa * 2.016) + 128);
                g = (int) (LIMIT(ufa * -0.392 - vfa * 0.813) + 128);

                // Store XBGR in uvtab_ table
                unsafe.putInt(uvtab + (((u << 8) | v) * intSize),
                        ((r & 0xFF) <<  0) |
                        ((g & 0xFF) <<  8) |
                        ((b & 0xFF) << 16));
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
            if (val > 383)
                val = 383;
            if (val < 0)
                val = 0;

            unsafe.putInt(lumtab + (s * intSize), val);
        }

        for (s = 0; s < 256; s++) {
            val = s;
            unsafe.putInt(satR + ((s + 256) * intSize),
                    ((val & 0xFF) >> rlose) << rshift);
            unsafe.putInt(satG + ((s + 256) * intSize),
                    ((val & 0xFF) >> glose) << gshift);
            unsafe.putInt(satB + ((s + 256) * intSize),
                    ((val & 0xFF) >> blose) << bshift);
        }

        for (s = 0; s < 256; s++) {
            unsafe.putInt(satR + (s * intSize),
                    unsafe.getInt(satR + (256 * intSize)));
            unsafe.putInt(satG + (s * intSize),
                    unsafe.getInt(satG + (256 * intSize)));
            unsafe.putInt(satB + (s * intSize),
                    unsafe.getInt(satB + (256 * intSize)));

            unsafe.putInt(satR + ((s + 512) * intSize),
                    unsafe.getInt(satR + (511 * intSize)));
            unsafe.putInt(satG + ((s + 512) * intSize),
                    unsafe.getInt(satG + (511 * intSize)));
            unsafe.putInt(satB + ((s + 512) * intSize),
                    unsafe.getInt(satB + (511 * intSize)));
        }

    }

    private final int ONEPIX(int r, int g, int b, int y) {
        int t = unsafe.getShort(lumtab + (y * intSize)) & 0xFFFF;
        int sr = unsafe.getInt(satR + ((r + t) * intSize));
        int sg = unsafe.getInt(satG + ((g + t) * intSize));
        int sb = unsafe.getInt(satB + ((b + t) * intSize));
        return sr | sg | sb;
    }

    /**
     *
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {
        inObject = input.getData();
        outObject = output.getData();
        if (inObject == null) {
            return BUFFER_PROCESSED_FAILED;
        }

        Dimension outSize = ((YUVFormat) input.getFormat()).getSize();

        if ((outObject == null)
                || ((((int[]) outObject).length - output.getOffset())
                    < (outSize.width * outSize.height))) {
            outObject = new int[outSize.width * outSize.height];
        }

        int w = outSize.width;
        int iw = inputFormat.getSize().width;

        int outAdd = 0;
        int yAdd = 0;
        int uAdd = 0;
        int vAdd = 0;
        for (int len = outSize.width * outSize.height; len > 0; len -= 4) {
            final long out = unsafe.getLong(this, outOffset)
                + (output.getOffset() * intSize) + intArrayOffset + outAdd;
            final long in = unsafe.getLong(this, inOffset) + input.getOffset()
                + byteArrayOffset;
            final long y = in + inputFormat.getOffsetY() + yAdd;
            final long u = in + inputFormat.getOffsetU() + uAdd;
            final long v = in + inputFormat.getOffsetV() + vAdd;

            int uVal = unsafe.getByte(u) & 0xFF;
            int vVal = unsafe.getByte(v) & 0xFF;
            int yVal = unsafe.getByte(y) & 0xFF;
            int yPlus1Val = unsafe.getByte(y + 1) & 0xFF;

            int sum = unsafe.getInt(uvtab + (((uVal << 8) | vVal) * intSize));
            int r = sum & 0xFF;
            int g = (sum >> 8) & 0xFF;
            int b = (sum >> 16) & 0xFF;

            unsafe.putInt(out, ONEPIX(r, g, b, yVal));
            unsafe.putInt(out + intSize, ONEPIX(r, g, b, yPlus1Val));

            outAdd += 2 * intSize;
            yAdd += 2;
            uAdd += 1;
            vAdd += 1;

            w -= 2;
            if (w <= 0) {
                w = outSize.width;
                yAdd += 2 * iw - w;
                uAdd += (iw - w) >> 1;
                vAdd += (iw - w) >> 1;
                outAdd += (2 * outSize.width - w) * intSize;
            }
        }

        outAdd = 0;
        yAdd = 0;
        uAdd = 0;
        vAdd = 0;
        for (int len = outSize.width * outSize.height; len > 0; len -= 4) {
            final long out = unsafe.getLong(this, outOffset)
                + (output.getOffset() * intSize) + intArrayOffset + outAdd;
            final long out2 = out + (outSize.width * intSize);

            final long in = unsafe.getLong(this, inOffset) + input.getOffset()
                + byteArrayOffset;
            final long y = in + inputFormat.getOffsetY() + yAdd;
            final long u = in + inputFormat.getOffsetU() + uAdd;
            final long v = in + inputFormat.getOffsetV() + vAdd;
            final long y2 = y + iw;

            int uVal = unsafe.getByte(u) & 0xFF;
            int vVal = unsafe.getByte(v) & 0xFF;
            int y2Val = unsafe.getByte(y2) & 0xFF;
            int y2Plus1Val = unsafe.getByte(y2 + 1) & 0xFF;

            int sum = unsafe.getInt(uvtab + (((uVal << 8) | vVal) * intSize));
            int r = sum & 0xFF;
            int g = (sum >> 8) & 0xFF;
            int b = (sum >> 16) & 0xFF;

            unsafe.putInt(out2, ONEPIX(r, g, b, y2Val));
            unsafe.putInt(out2 + intSize, ONEPIX(r, g, b, y2Plus1Val));

            outAdd += 2 * intSize;
            yAdd += 2;
            uAdd += 1;
            vAdd += 1;

            w -= 2;
            if (w <= 0) {
                w = outSize.width;
                yAdd += 2 * iw - w;
                uAdd += (iw - w) >> 1;
                vAdd += (iw - w) >> 1;
                outAdd += (2 * outSize.width - w) * intSize;
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
            if ((yuv.getYuvType() == YUVFormat.YUV_420) &&
                    yuv.getDataType().equals(Format.byteArray)) {
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
            if ((rgb.getBitsPerPixel() == 32) &&
                    rgb.getDataType().equals(Format.intArray)) {
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
        unsafe.freeMemory(uvtab);
        unsafe.freeMemory(lumtab);
        unsafe.freeMemory(satR);
        unsafe.freeMemory(satG);
        unsafe.freeMemory(satB);
    }

    /**
     *
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return getClass().getName();
    }

    private static final int mtos(int mask) {
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
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            intSize = unsafe.arrayIndexScale(int[].class);
            inOffset = unsafe.objectFieldOffset(
                    getClass().getDeclaredField("inObject"));
            outOffset = unsafe.objectFieldOffset(
                    getClass().getDeclaredField("outObject"));
            byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
            intArrayOffset = unsafe.arrayBaseOffset(int[].class);

            uvtab = unsafe.allocateMemory(65536 * intSize);
            updateUVTable();

            lumtab = unsafe.allocateMemory(256 * intSize);
            satR = unsafe.allocateMemory(768 * intSize);
            satG = unsafe.allocateMemory(768 * intSize);
            satB = unsafe.allocateMemory(768 * intSize);
            updateSaturationTable();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceUnavailableException("Error getting unsafe");
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
