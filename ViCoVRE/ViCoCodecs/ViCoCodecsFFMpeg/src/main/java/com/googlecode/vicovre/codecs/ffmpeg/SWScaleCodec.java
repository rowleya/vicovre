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
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.codecs.nativeloader.NativeLoader;

public class SWScaleCodec implements Codec {

    private PixelFormat pixelFormatIn = null;

    private PixelFormat pixelFormatOut = null;

    private long ref = -1;

    private VideoFormat outputFormat = null;

    private byte[] bytedata = null;

    private int[] intdata = null;

    private short[] shortdata = null;

    private Dimension inSize = null;

    private float inFrameRate = -1;

    private Dimension outSize = null;

    public Format[] getSupportedInputFormats() {
        return Utils.getVideoFormats(null, -1);
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return Utils.getVideoFormats(null, -1);
        }
        VideoFormat vf = (VideoFormat) input;
        return Utils.getVideoFormats(null, vf.getFrameRate());

    }

    public int process(Buffer input, Buffer output) {
        if (ref == -1) {
            VideoFormat inFormat = (VideoFormat) input.getFormat();
            VideoFormat outFormat = (VideoFormat) output.getFormat();
            Dimension inSize = inFormat.getSize();
            if (outSize == null) {
                outSize = outFormat.getSize();
            }
            if (outSize == null) {
                outSize = inSize;
            }
            if (outSize.width == -1) {
                outSize.width = inSize.width;
            }
            if (outSize.height == -1) {
                outSize.height = inSize.height;
            }
            System.err.println("Input = " + input.getFormat());
            System.err.println("Output = " + output.getFormat());
            outputFormat = Utils.getVideoFormat(pixelFormatOut,
                    outSize, inFormat.getFrameRate());
            if (outputFormat.getDataType() == Format.byteArray) {
                bytedata = new byte[outputFormat.getMaxDataLength()];
            } else if (outputFormat.getDataType() == Format.intArray) {
                intdata = new int[outputFormat.getMaxDataLength()];
            } else if (outputFormat.getDataType() == Format.shortArray) {
                shortdata = new short[outputFormat.getMaxDataLength()];
            }
            ref = openCodec(pixelFormatIn.getId(), inSize.width, inSize.height,
                    pixelFormatOut.getId(), outSize.width, outSize.height);
        }
        if (outputFormat.getDataType() == Format.byteArray) {
            output.setData(bytedata);
            output.setLength(bytedata.length);
        } else if (outputFormat.getDataType() == Format.intArray) {
            output.setData(intdata);
            output.setLength(intdata.length);
        } else if (outputFormat.getDataType() == Format.shortArray) {
            output.setData(shortdata);
            output.setLength(shortdata.length);
        }

        output.setOffset(0);
        output.setFormat(outputFormat);
        output.setSequenceNumber(input.getSequenceNumber());
        output.setTimeStamp(input.getTimeStamp());

        return process(ref, input, output);
    }

    public Format setInputFormat(Format input) {
        pixelFormatIn = Utils.getPixFormat((VideoFormat) input);
        if (pixelFormatIn != PixelFormat.PIX_FMT_NONE) {
            inSize = ((VideoFormat) input).getSize();
            inFrameRate = ((VideoFormat) input).getFrameRate();
            return input;
        }
        return null;
    }

    public Format setOutputFormat(Format output) {
        VideoFormat vf = (VideoFormat) output;
        pixelFormatOut = Utils.getPixFormat(vf);
        outSize = vf.getSize();
        if (outSize == null) {
            outSize = inSize;
        }
        if (pixelFormatOut != PixelFormat.PIX_FMT_NONE) {
            return Utils.getVideoFormat(pixelFormatOut, outSize, inFrameRate);
        }
        return null;
    }

    public void close() {
        if (ref != -1) {
            closeCodec(ref);
            ref = -1;
        }
    }

    public String getName() {
        return "SWScaleCodec";
    }

    public void open() throws ResourceUnavailableException {
        NativeLoader.loadLibrary(getClass(), "swscalej");
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    private native long openCodec(int pixFmtIn, int inWidth, int inHeight,
            int pixFmtOut, int outWidth, int outHeight);

    private native int process(long ref, Buffer input, Buffer output);

    private native void closeCodec(long ref);
}
