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

package com.googlecode.vicovre.codecs.ulaw;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;

public class JavaEncoder implements Codec {

    private static final Format[] INPUT_FORMATS = new Format[]{
            new AudioFormat(AudioFormat.LINEAR, -1, 8, -1, -1, -1),
            new AudioFormat(AudioFormat.LINEAR, -1, 16, -1, -1, -1)
        };

    private AudioFormat outputFormat = null;

    private byte[] outData = null;


    public Format[] getSupportedInputFormats() {
        return INPUT_FORMATS;
    }

    public Format[] getSupportedOutputFormats(Format format) {
        if (format == null) {
            return new Format[]{
                new AudioFormat(AudioFormat.ULAW, -1, 8, -1,
                        AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED),
            };
        }

        if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            if (af.getEncoding().equals(AudioFormat.LINEAR)
                    && ((af.getSampleSizeInBits() == 8)
                            || (af.getSampleSizeInBits() == 16))) {
                return new Format[]{
                    new AudioFormat(AudioFormat.ULAW, af.getSampleRate(),
                        8, af.getChannels(), AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED)
                };
            }
        }
        return null;
    }

    public int process(Buffer input, Buffer output) {
        AudioFormat format = (AudioFormat) input.getFormat();
        if (outputFormat == null) {
            outputFormat = new AudioFormat(AudioFormat.ULAW,
                    format.getSampleRate(), 8, format.getChannels(),
                    AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
        }

        byte[] inData = (byte[]) input.getData();
        int inOffset = input.getOffset();
        int inLength = input.getLength();
        int outLength = inLength;
        int increment = format.getSampleSizeInBits() / Byte.SIZE;
        outLength /= increment;
        if ((outData == null) || (outData.length < outLength)) {
            outData = new byte[outLength];
        }

        int outPos = 0;
        for (int i = inOffset; i < inOffset + inLength; i += increment) {
            int sample = 0;
            if (format.getEndian() == AudioFormat.BIG_ENDIAN) {
                for (int j = 0; j < increment; j++) {
                    sample |= (inData[i + j] & 0xFF)
                        << (j * Byte.SIZE);
                }
            } else {
                for (int j = 0; j < increment; j++) {
                    sample |= (inData[i + j] & 0xFF)
                        << ((increment - j - 1) * Byte.SIZE);
                }
            }

            if (format.getSigned() == AudioFormat.UNSIGNED) {
                sample = (short) (sample + (Short.MAX_VALUE + 1));
            }

            int signBit = 0x80;
            if (sample < 0) {
                signBit = 0x00;
                sample = -sample;
            }

            sample = (132 + sample) >> 3;
            if (sample >= 0x1000) {
                sample = 0xFFF;
            }

            boolean haveSample = false;
            int level = 0x0020;
            for (int j = 7; (j >= 0) && !haveSample; j++) {
                if (sample < level) {
                    outData[outPos++] =
                        (byte) (signBit | (j << 4)
                                | (31 - (sample >> ((8 - j) - 1))));
                    haveSample = true;
                }
                level *= 2;
            }
        }

        output.setData(outData);
        output.setLength(outLength);
        output.setOffset(0);
        output.setFormat(format);
        output.setTimeStamp(input.getTimeStamp());
        output.setDuration(input.getDuration());
        output.setSequenceNumber(input.getSequenceNumber());

        return BUFFER_PROCESSED_OK;
    }

    public Format setInputFormat(Format input) {
        if (input instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) input;
            if (af.getEncoding().equals(AudioFormat.LINEAR)
                    && ((af.getSampleSizeInBits() == 8)
                            || (af.getSampleSizeInBits() == 16))) {
                return input;
            }
        }
        return null;
    }

    public Format setOutputFormat(Format output) {
        if (output instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) output;
            if (af.getEncoding().equals(AudioFormat.ULAW)
                    && (af.getSampleSizeInBits() == 8)) {
                return output;
            }
        }
        return null;
    }

    public void close() {
        // Does Nothing
    }

    public String getName() {
        return "ULAW Encoder";
    }

    public void open() {
        // Does Nothing
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

}
