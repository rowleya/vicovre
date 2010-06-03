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

package com.googlecode.vicovre.codecs.ffmpeg.audio;

import java.nio.ByteOrder;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

import com.googlecode.vicovre.codecs.ffmpeg.Log;
import com.googlecode.vicovre.utils.nativeloader.NativeLoader;

public abstract class FFMPEGAudioCodec implements Codec {

    private final int codecId;

    private static final AudioFormat DECODED_FORMAT =
            new AudioFormat(AudioFormat.LINEAR, -1, 16, -1,
                    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
                            ? AudioFormat.BIG_ENDIAN
                            : AudioFormat.LITTLE_ENDIAN,
                    AudioFormat.SIGNED);

    private final AudioFormat[] inputFormats;

    private final AudioFormat[] outputFormats;

    private final boolean encode;

    private byte[] data = null;

    private int logLevel = Log.AV_LOG_QUIET;

    private long ref = -1;

    private AudioFormat inputFormat = null;

    private AudioFormat outputFormat = null;

    private int encoderInputSize = 0;

    private byte[] encoderInputData = null;

    private long encoderInputDataTimestamp = 0;

    private int encoderInputDataSize = 0;

    private double nsPerSample = 0;

    private int sampleSize = 0;

    private int sequenceNumber = 0;

    protected FFMPEGAudioCodec(int codecId,
            AudioFormat[] encodedFormats, boolean encode) {
        this.codecId = codecId;
        if (encode) {
             this.outputFormats = encodedFormats;
             this.inputFormats = new AudioFormat[]{DECODED_FORMAT};
        } else {
            this.inputFormats = encodedFormats;
            this.outputFormats = new AudioFormat[]{DECODED_FORMAT};
        }
        this.encode = encode;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public AudioCodecContext getContext() {
        AudioCodecContext context = new AudioCodecContext();
        fillInCodecContext(ref, context);
        context.setChannels(inputFormat.getChannels());
        context.setSampleRate((int) inputFormat.getSampleRate());
        return context;
    }

    public Format[] getSupportedInputFormats() {
        return inputFormats;
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return outputFormats;
        }
        for (AudioFormat format : inputFormats) {
            if (input.matches(format)) {
                return outputFormats;
            }
        }
        return new Format[0];
    }

    public int process(Buffer input, Buffer output) {
        if (data == null) {
            inputFormat = (AudioFormat) input.getFormat();
            outputFormat = new AudioFormat(outputFormat.getEncoding(),
                inputFormat.getSampleRate(), 16, inputFormat.getChannels());
            AudioCodecContext context = getContext();
            int dataSize = init(ref, context);
            if (dataSize < 0) {
                System.err.println("FFMPEGAudioCodec failed to initialize: "
                        + dataSize);
                return BUFFER_PROCESSED_FAILED;
            }
            data = new byte[dataSize];
            if (encode) {
                encoderInputSize =
                    context.getFrameSize() * context.getChannels() * 2;
                encoderInputData = new byte[encoderInputSize];
                nsPerSample = 1000000000.0 / inputFormat.getSampleRate();
                sampleSize = 2 * context.getChannels();
            }
        }
        output.setData(data);
        output.setOffset(0);
        output.setLength(data.length);
        output.setFormat(outputFormat);
        int result = 0;
        if (encode) {
            if (encoderInputDataSize == 0) {
                encoderInputDataTimestamp = input.getTimeStamp();
            }
            int encodeSpace = encoderInputSize - encoderInputDataSize;
            int toCopy = input.getLength();
            if (toCopy > encodeSpace) {
                toCopy = encodeSpace;
            }
            System.arraycopy(input.getData(), input.getOffset(),
                    encoderInputData, encoderInputDataSize, toCopy);
            encoderInputDataSize += toCopy;
            if (encoderInputDataSize >= encoderInputSize) {
                Buffer buffer = new Buffer();
                buffer.setData(encoderInputData);
                buffer.setOffset(0);
                buffer.setLength(encodeSpace);
                result = encode(ref, buffer, output);
                if (result == BUFFER_PROCESSED_OK) {
                    encoderInputDataSize = 0;
                    output.setTimeStamp(encoderInputDataTimestamp);

                    if (toCopy < input.getLength()) {
                        input.setOffset(input.getOffset() + toCopy);
                        input.setLength(input.getLength() - toCopy);
                        long samplesTime = (long) ((nsPerSample * toCopy)
                                / sampleSize);
                        input.setTimeStamp(input.getTimeStamp() + samplesTime);
                        result = INPUT_BUFFER_NOT_CONSUMED;
                    }
                }
            } else {
                result = OUTPUT_BUFFER_NOT_FILLED;
            }
        } else {
            result = decode(ref, input, output);
        }
        if ((result != OUTPUT_BUFFER_NOT_FILLED)
                && (result != BUFFER_PROCESSED_FAILED)) {
            if (output.getLength() == 0) {
                output.setDiscard(true);
            } else {
                output.setSequenceNumber(sequenceNumber++);
            }
        }
        return result;
    }

    public Format setInputFormat(Format input) {
        for (AudioFormat format : inputFormats) {
            if (input.matches(format)) {
                return input;
            }
        }
        return null;
    }

    public Format setOutputFormat(Format output) {
        for (AudioFormat format : outputFormats) {
            if (output.matches(format)) {
                outputFormat = (AudioFormat) output;
                return output;
            }
        }
        return null;
    }

    public void close() {
        close(ref);
    }

    public String getName() {
        return "FFMPEGAudioCodec";
    }

    public void open() throws ResourceUnavailableException {
        NativeLoader.loadLibrary(getClass(), "ffmpegj");
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
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    private native long open(boolean encode, int codecId, int logLevel);

    private native void fillInCodecContext(long ref,
            AudioCodecContext codecContext);

    private native int init(long ref, AudioCodecContext context);

    private native int encode(long ref, Buffer input, Buffer output);

    private native int decode(long ref, Buffer input, Buffer output);

    private native void close(long ref);

}
