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

package com.googlecode.vicovre.codecs.ffmpeg.video;

import java.awt.Dimension;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;

import com.googlecode.vicovre.codecs.ffmpeg.Log;
import com.googlecode.vicovre.codecs.ffmpeg.PixelFormat;
import com.googlecode.vicovre.codecs.ffmpeg.Utils;
import com.googlecode.vicovre.media.controls.KeyFrameForceControl;
import com.googlecode.vicovre.media.format.BitRateVideoFormat;
import com.googlecode.vicovre.utils.nativeloader.NativeLoader;

public abstract class FFMPEGVideoCodec implements Codec, KeyFrameForceControl {

    private static final int DEF_WIDTH = 352;

    private static final int DEF_HEIGHT = 288;

    private final int codecId;

    private final VideoFormat[] inputFormats;

    private final VideoFormat[] outputFormats;

    private final boolean encode;

    private Object data = null;

    private int logLevel = Log.AV_LOG_QUIET;

    private long ref = -1;

    private VideoFormat inputFormat = null;

    private VideoFormat outputFormat = null;

    private int sequenceNumber = 0;

    private boolean inited = false;

    private VideoCodecContext context = null;

    private boolean nextKey = true;

    protected FFMPEGVideoCodec(int codecId,
            VideoFormat[] encodedFormats, boolean encode) {
        this.codecId = codecId;
        if (encode) {
            this.inputFormats = Utils.getVideoFormats(null, -1);
            this.outputFormats = encodedFormats;
        } else {
            this.outputFormats = Utils.getVideoFormats(null, -1);
            this.inputFormats = encodedFormats;
        }
        this.encode = encode;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public VideoCodecContext getContext() {
        VideoCodecContext context = new VideoCodecContext();
        fillInCodecContext(ref, context);
        Dimension outputSize = outputFormat.getSize();
        Dimension inputSize = inputFormat.getSize();
        if (outputSize == null) {
            outputSize = inputSize;
        }
        if (inputSize == null) {
            inputSize = outputSize;
        }
        if ((inputSize == null) && (outputSize == null) && encode) {
            inputSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);
            outputSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);
        }
        if (inputSize != null) {
            context.setInputWidth(inputSize.width);
            context.setInputHeight(inputSize.height);
        }
        if (outputSize != null) {
            context.setOutputWidth(outputSize.width);
            context.setOutputHeight(outputSize.height);
        }
        if (inputFormat.getFrameRate() != -1) {
            context.setFrameRate((int) inputFormat.getFrameRate());
        }
        if (encode) {
            context.setPixelFmt(Utils.getPixFormat(inputFormat).getId());
        } else {
            context.setPixelFmt(Utils.getPixFormat(outputFormat).getId());
        }

        return context;
    }

    public Format[] getSupportedInputFormats() {
        return inputFormats;
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return outputFormats;
        }
        for (VideoFormat format : inputFormats) {
            if (input.matches(format)) {
                return outputFormats;
            }
        }
        return new Format[0];
    }

    private void allocateData(int size) {
        Class<?> type = outputFormat.getDataType();
        if ((type == null) || type.equals(Format.byteArray)) {
            data = new byte[size];
        } else if (type.equals(Format.intArray)) {
            data = new int[size / 4];
        } else if (type.equals(Format.shortArray)) {
            data = new int[size / 2];
        }
    }

    public int process(Buffer input, Buffer output) {
        if (!inited) {
            inputFormat = (VideoFormat) input.getFormat();
            context = getContext();
            int dataSize = init(ref, context);
            if (dataSize < 0) {
                System.err.println(getClass().getName()
                        + " failed to initialize: " + dataSize);
                return BUFFER_PROCESSED_FAILED;
            }
            if (encode) {
                outputFormat = new BitRateVideoFormat(
                        outputFormat.getEncoding(),
                        new Dimension(context.getOutputWidth(),
                                context.getOutputHeight()),
                        -1, Format.byteArray, -1,
                        context.getBitrate(),
                        context.getBitrateTolerance());
                allocateData(dataSize);
                byte[] extradata = context.getExtraData();
                if (extradata != null) {
                    output.setHeader(extradata);
                }
            }
            inited = true;
        }

        if (!encode && (data == null)) {
            int result = decodeFirst(ref, input, context);
            if ((result == INPUT_BUFFER_NOT_CONSUMED)
                    || (result == BUFFER_PROCESSED_OK)) {
                int dataSize = context.getOutputDataSize();
                Dimension outputSize = new Dimension(context.getOutputWidth(),
                        context.getOutputHeight());
                allocateData(dataSize);
                PixelFormat pixelFormat = Utils.getPixFormat(outputFormat);
                context.setPixelFmt(pixelFormat.getId());
                outputFormat = Utils.getVideoFormat(pixelFormat, outputSize,
                        outputFormat.getFrameRate());
            } else {
                return result;
            }
        }

        output.setData(data);
        output.setOffset(0);
        output.setFormat(outputFormat);
        int result = 0;
        if (encode) {
            if (nextKey) {
                nextKey = false;
                input.setFlags(input.getFlags() | Buffer.FLAG_KEY_FRAME);
            }
            result = encode(ref, input, output);
        } else {
            result = decode(ref, input, output);
        }
        if ((result != OUTPUT_BUFFER_NOT_FILLED)
                && (result != BUFFER_PROCESSED_FAILED)) {
            output.setSequenceNumber(sequenceNumber++);
            output.setTimeStamp(input.getTimeStamp());
            Class<?> type = outputFormat.getDataType();
            if (type.equals(Format.intArray)) {
                output.setLength(output.getLength() / 4);
            } else if (type.equals(Format.shortArray)) {
                output.setLength(output.getLength() / 2);
            }
        }
        return result;
    }

    public Format setInputFormat(Format input) {
        for (VideoFormat format : inputFormats) {
            if (input.matches(format)) {
                inputFormat = (VideoFormat) input;
                return input;
            }
        }
        return null;
    }

    public Format setOutputFormat(Format output) {
        for (VideoFormat format : outputFormats) {
            if (output.matches(format)) {
                outputFormat = (VideoFormat) output;
                return output;
            }
        }
        return null;
    }

    public void close() {
        close(ref);
    }

    public String getName() {
        return "FFMPEGVideoCodec";
    }

    public void open() throws ResourceUnavailableException {
        NativeLoader.loadLibrary(getClass(), "ffmpegj");
        System.err.println("Log level = " + logLevel);
        ref = open(encode, codecId, logLevel);
        if (ref <= 0) {
            throw new ResourceUnavailableException("Could not open codec: "
                    + ref);
        }
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        if (className.equals(KeyFrameForceControl.class.getName())) {
            return this;
        }
        return null;
    }

    public Object[] getControls() {
        return new Object[]{this};
    }

    public void nextFrameKey() {
        nextKey = true;
    }

    private native long open(boolean encoding, int codecId, int logLevel);

    private native void fillInCodecContext(long ref, VideoCodecContext context);

    private native int init(long ref, VideoCodecContext context);

    private native int decode(long ref, Buffer input, Buffer output);

    private native int decodeFirst(long ref, Buffer input,
            VideoCodecContext context);

    private native int encode(long ref, Buffer input, Buffer output);

    private native void close(long ref);

}
