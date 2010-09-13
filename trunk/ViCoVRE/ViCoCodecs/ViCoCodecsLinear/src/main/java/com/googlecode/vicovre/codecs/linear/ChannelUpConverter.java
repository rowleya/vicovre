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

package com.googlecode.vicovre.codecs.linear;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 * Converts the rate between two values
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ChannelUpConverter implements Codec {

    private AudioFormat inputFormat = null;

    private AudioFormat outputFormat = null;

    /**
     *
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new Format[]{
            new AudioFormat(AudioFormat.LINEAR)
        };
    }

    /**
     *
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format format) {
        if (format == null) {
            return new Format[]{
                new AudioFormat(AudioFormat.LINEAR)
            };
        }

        if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            if (af.getEncoding().equals(AudioFormat.LINEAR)
                    && (af.getChannels() == 1)) {
                double sampleRate = af.getSampleRate();
                int sampleSize = af.getSampleSizeInBits();
                int signed = af.getSigned();
                int endian = af.getEndian();

                return new Format[] {
                    new AudioFormat(AudioFormat.LINEAR, sampleRate,
                            sampleSize, 2, endian, signed)

                };
            }
        }

        return null;
    }

    /**
     *
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer input, Buffer output) {
        int bytesPerSample = inputFormat.getSampleSizeInBits() / Byte.SIZE;
        int inLength = input.getLength();
        int inOffset = input.getOffset();
        byte[] inData = (byte[]) input.getData();
        int outLength = inLength * 2;
        byte[] outData = (byte[]) output.getData();
        if ((outData == null)
                || (outData.length != outLength)) {
            outData = new byte[outLength];
        }

        int outPos = 0;
        for (int i = inOffset; i < (inOffset + inLength); i += bytesPerSample) {
            for (int j = 0; j < bytesPerSample; j++) {
                outData[outPos++] = inData[i + j];
            }
            for (int j = 0; j < bytesPerSample; j++) {
                outData[outPos++] = inData[i + j];
            }
        }

        output.setData(outData);
        output.setLength(outData.length);
        output.setOffset(0);
        output.setFormat(outputFormat);
        output.setTimeStamp(input.getTimeStamp());
        output.setSequenceNumber(input.getSequenceNumber());

        return BUFFER_PROCESSED_OK;
    }

    /**
     *
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format format) {
        if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            if (af.getEncoding().equals(AudioFormat.LINEAR)) {
                inputFormat = af;
                return format;
            }
        }

        return null;
    }

    /**
     *
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format format) {
        if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            if (af.getEncoding().equals(AudioFormat.LINEAR)) {
                outputFormat = af;
                return format;
            }
        }

        return null;
    }

    /**
     *
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        // Does Nothing
    }

    /**
     *
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "RateConverter";
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
