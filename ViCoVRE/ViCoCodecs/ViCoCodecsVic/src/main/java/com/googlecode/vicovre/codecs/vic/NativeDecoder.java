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

package com.googlecode.vicovre.codecs.vic;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.codecs.controls.FrameFillControl;
import com.googlecode.vicovre.codecs.nativeloader.NativeLoader;

public class NativeDecoder implements Codec, FrameFillControl {

    private static final VicCodec[] CODECS = new VicCodec[]{
        new VicCodec("JpegDecoder", new VideoFormat("JPEG/RTP"),
                new YUVFormat(YUVFormat.YUV_422), 1),
        new VicCodec("H261Decoder", new VideoFormat("H261/RTP"),
                new YUVFormat(new Dimension(352, 288), -1, Format.byteArray, -1,
                        YUVFormat.YUV_420, -1, -1, -1, -1, -1), 2),
        new VicCodec("H261Decoder", new VideoFormat("H261AS/RTP"),
                new YUVFormat(YUVFormat.YUV_420), 1),
    };

    private long ref = 0;

    private int codec = -1;

    private YUVFormat outputFormat = null;

    byte[] buffer = null;

    byte[] frame = null;

    public Format[] getSupportedInputFormats() {
        Format[] formats = new Format[CODECS.length];
        for (int i = 0; i < CODECS.length; i++) {
            formats[i] = CODECS[i].getInputFormat();
        }
        return formats;
    }

    private YUVFormat getOutputFormat(Format input, int codec, int decimation) {
        VideoFormat format = (VideoFormat) input;
        YUVFormat yuv = CODECS[codec].getOutputFormat();
        Dimension size = yuv.getSize();
        if (size == null) {
            size = format.getSize();
        }
        int dataLength = -1;
        int strideY = -1;
        int strideUV = -1;
        int offsetY = -1;
        int offsetU = -1;
        int offsetV = -1;
        int yuvType = yuv.getYuvType();
        if (decimation == 422) {
            yuvType = YUVFormat.YUV_422;
        } else if (decimation == 420) {
            yuvType = YUVFormat.YUV_420;
        }
        if (size != null) {
            int pixels = size.width * size.height;
            if (yuvType == YUVFormat.YUV_420) {
                dataLength = pixels + (pixels >> 1);
                strideY = size.width;
                strideUV = size.width >> 1;
                offsetY = 0;
                offsetU = pixels;
                offsetV = pixels + (pixels >> 2);
            } else if (yuvType == YUVFormat.YUV_422) {
                dataLength = pixels * 2;
                strideY = size.width;
                strideUV = size.width >> 1;
                offsetY = 0;
                offsetU = pixels;
                offsetV = pixels + (pixels >> 1);
            }
        }
        return new YUVFormat(size,
                dataLength, YUVFormat.byteArray,
                format.getFrameRate(), yuvType,
                strideY, strideUV, offsetY, offsetU, offsetV);
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            Format[] formats = new Format[CODECS.length];
            for (int i = 0; i < CODECS.length; i++) {
                formats[i] = CODECS[i].getOutputFormat();
            }
            return formats;
        }
        System.err.println("Getting output formats for input " + input);
        Vector<Format> outputFormats = new Vector<Format>();
        for (int i = 0; i < CODECS.length; i++) {
            if (CODECS[i].getInputFormat().matches(input)) {
                YUVFormat format = getOutputFormat(input, i, -1);
                System.err.println("    " + format);
                outputFormats.add(format);
            }
        }
        return outputFormats.toArray(new Format[0]);
    }

    public int process(Buffer input, Buffer output) {
        if (outputFormat == null) {
            decodeHeader(ref, input);
        }
        if (frame != null) {
            System.arraycopy(frame, 0, buffer, 0,
                    Math.min(frame.length, buffer.length));
            frame = null;
        }
        output.setData(buffer);
        output.setFormat(outputFormat);
        output.setOffset(0);
        output.setLength(buffer.length);
        return decode(ref, input, output);
    }

    public Format setInputFormat(Format format) {
        if (codec != -1) {
            if (!CODECS[codec].getInputFormat().matches(format)) {
                codec = -1;
            } else {
                return format;
            }
        }
        if (codec == -1) {
            for (int i = 0; i < CODECS.length; i++) {
                if (CODECS[i].getInputFormat().matches(format)) {
                    codec = i;
                    return format;
                }
            }
        }
        return null;
    }

    public Format setOutputFormat(Format format) {
        if (codec != -1) {
            if (!CODECS[codec].getOutputFormat().matches(format)) {
                codec = -1;
            } else {
                return format;
            }
        }
        if (codec == -1) {
            for (int i = 0; i < CODECS.length; i++) {
                if (CODECS[i].getOutputFormat().matches(format)) {
                    codec = i;
                    return format;
                }
            }
        }
        return null;
    }

    public void close() {
        if (ref != 0) {
            closeCodec(ref);
        }
    }

    public String getName() {
        return "NativeDecoder";
    }

    public void open() throws ResourceUnavailableException {
        NativeLoader.loadLibrary(getClass(), "vic");
        if (codec == -1) {
            throw new ResourceUnavailableException("Codec not set");
        }
        ref = openCodec(codec);
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

    public void resize(int width, int height, int decimation) {
        VideoFormat vf = CODECS[codec].getInputFormat();
        VideoFormat input = new VideoFormat(vf.getEncoding(),
                new Dimension(width, height), -1, null, -1);
        outputFormat = getOutputFormat(input, codec, decimation);
        buffer = new byte[outputFormat.getMaxDataLength()
                          * CODECS[codec].getDataLengthMultiplier()];
        Arrays.fill(buffer, (byte) 0x80);
    }

    public void fillFrame(byte[] frame) {
        this.frame = frame;
    }

    public Component getControlComponent() {
        return null;
    }

    private native long openCodec(int id);

    private native void decodeHeader(long ref, Buffer input);

    private native int decode(long ref, Buffer input, Buffer output);

    private native void closeCodec(long ref);


}
