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

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

import com.googlecode.vicovre.codecs.nativeloader.NativeLoader;

public class ResampleCodec implements Codec {

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.LINEAR, -1,	16, -1,
            AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);

    private AudioFormat outputFormat = null;

    private byte[] outputData = null;

    private long ref = -1;

    private double ratio = 0;

    public Format[] getSupportedInputFormats() {
        return new Format[]{new AudioFormat(AudioFormat.LINEAR, -1, 16, -1,
                AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)};
    }

    public Format[] getSupportedOutputFormats(Format format) {
        if (format == null) {
            return new Format[]{new AudioFormat(AudioFormat.LINEAR, -1, 16, -1,
                    AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)};
        }
        if (format.matches(FORMAT)) {
            return new Format[]{new AudioFormat(AudioFormat.LINEAR, -1, 16, -1,
                    AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED)};
        }
        return null;
    }

    public int process(Buffer input, Buffer output) {
        if (ref == -1) {
            AudioFormat inputFormat = (AudioFormat) input.getFormat();
            int outputRate = (int) outputFormat.getSampleRate();
            if (outputRate == -1) {
                outputRate = (int) inputFormat.getSampleRate();
            }
            int outputChannels = outputFormat.getChannels();
            if (outputChannels == -1) {
                outputChannels = inputFormat.getChannels();
            }
            ref = open((int) inputFormat.getSampleRate(), outputRate,
                    inputFormat.getChannels(), outputChannels);
            if (ref <= 0) {
                System.err.println("Error opening codec: " + ref);
                return BUFFER_PROCESSED_FAILED;
            }

            ratio = (outputRate * outputChannels)
                / (inputFormat.getSampleRate() * inputFormat.getChannels());
        }

        int outputLength = (int) ((input.getLength() + 0.5) * ratio);
        if ((outputData == null) || (outputData.length < outputLength)) {
            outputData = new byte[outputLength];
        }
        output.setData(outputData);
        output.setLength(outputLength);
        output.setOffset(0);
        output.setFormat(outputFormat);
        output.setSequenceNumber(input.getSequenceNumber());
        output.setTimeStamp(input.getTimeStamp());

        return process(ref, input, output);
    }

    public Format setInputFormat(Format format) {
        if (format.matches(FORMAT)) {
            return format;
        }
        return null;
    }

    public Format setOutputFormat(Format format) {
        if (format.matches(FORMAT)) {
            outputFormat = (AudioFormat) format;
            return format;
        }
        return null;
    }

    public void close() {
        if (ref != -1) {
            close(ref);
            ref = -1;
        }
    }

    public String getName() {
        return "ResampleCodec";
    }

    public void open() throws ResourceUnavailableException {
        NativeLoader.loadLibrary(getClass(), "ffmpegj");
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

    private native long open(int inputRate, int outputRate, int inputChannels,
            int outputChannels);

    private native int process(long ref, Buffer input, Buffer output);

    private native void close(long ref);

}
