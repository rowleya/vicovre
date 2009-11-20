/*
 * @(#)AudioMixer.java
 * Created: 18 Feb 2008
 * Version: 1.0
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
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
 */

package com.googlecode.vicovre.media.audio;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;

import com.googlecode.vicovre.media.MemeticFileReader;

/**
 * A stream from which mixed audio is read
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class AudioMixer {

    private static final double GAIN = 0.0015;

    private static final int SAMPLES_PER_BUFFER = 882;

    private static final double SAMPLE_RATE = 44100.0;

    private static final double NANOS_PER_SAMPLE = 1000000000.0 / SAMPLE_RATE;

    private static final long BUFFER_DURATION =
        (long) (SAMPLES_PER_BUFFER * NANOS_PER_SAMPLE);

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.LINEAR, SAMPLE_RATE, 16, 1, AudioFormat.LITTLE_ENDIAN,
            AudioFormat.SIGNED);

    private static final int MAX_SAMPLE =
        (1 << (FORMAT.getSampleSizeInBits()));

    private AudioSource[] sources = null;

    private long minStartTime = 0;

    private long currentTimestamp = 0;

    private double[] samples = new double[SAMPLES_PER_BUFFER];

    private byte[] data = new byte[SAMPLES_PER_BUFFER * 2];

    /**
     * Creates a new AudioMixer
     * @param sources The linear audio sources to mix
     * @throws UnsupportedFormatException
     */
    public AudioMixer(MemeticFileReader[] sources)
            throws UnsupportedFormatException {
        this.sources = new AudioSource[sources.length];
        minStartTime = Long.MAX_VALUE;
        for (MemeticFileReader source : sources) {
            if (source.getStartTime() < minStartTime) {
                minStartTime = source.getStartTime();
            }
        }
        for (int i = 0; i < sources.length; i++) {
            this.sources[i] = new AudioSource(sources[i], FORMAT, minStartTime);
        }
    }

    public void streamSeek(long offset) throws IOException {
        for (int i = 0; i < sources.length; i++) {
            sources[i].seek(offset);
        }
    }

    public long getStartTime() {
        return minStartTime;
    }

    public void setStartTime(long startTime) {
        minStartTime = startTime;
    }

    public long getTimestamp() {
        return currentTimestamp;
    }

    public long getOffset() {
        long minOffset = Long.MAX_VALUE;
        for (int i = 0; i < sources.length; i++) {
            minOffset = Math.min(minOffset, sources[i].getOffset());
        }
        return minOffset;
    }

    public void setTimestampOffset(long timestampOffset) {
        for (int i = 0; i < sources.length; i++) {
            sources[i].setTimestampOffset(timestampOffset);
        }
    }

    public Format getFormat() {
        return FORMAT;
    }

    public boolean readNextBuffer() throws IOException {
        for (int i = 0; i < samples.length; i++) {
            samples[i] = 0;
        }

        for (int i = 0; i < sources.length; i++) {
            for (int j = 0; j < SAMPLES_PER_BUFFER; j++) {
                if (!sources[i].isFinished()) {
                    double sample = sources[i].readNextSample();
                    samples[j] = (samples[j] + sample);
                }
            }
        }

        double energy = 0;
        for (int i = 0; i < SAMPLES_PER_BUFFER; i++) {
            energy += samples[i] * samples[i];
        }
        double k = Math.sqrt((GAIN * SAMPLES_PER_BUFFER) / energy);

        for (int i = 0; i < SAMPLES_PER_BUFFER; i++) {
            samples[i] *= k;
            if (samples[i] > 1) {
                samples[i] = 1;
            }
            if (samples[i] < -1.0) {
                samples[i] = -1.0;
            }
            int sample = (int) (samples[i] * MAX_SAMPLE);
            for (int j = i * 2; j <= ((i * 2) + 1); j++) {
                data[j] = (byte) (sample & 0xFF);
                sample >>= Byte.SIZE;
            }
        }

        return true;
    }

    /**
     * Reads the next buffer
     * @return The next buffer
     */
    public Buffer getBuffer() {
        Buffer buffer = new Buffer();
        buffer.setData(data);
        buffer.setOffset(0);
        buffer.setLength(data.length);
        buffer.setFormat(FORMAT);
        buffer.setTimeStamp(getTimestamp());
        buffer.setDuration(BUFFER_DURATION);
        currentTimestamp += BUFFER_DURATION;
        return buffer;
    }

    /**
     * Closes the mixer
     *
     */
    public void close() {
        for (int i = 0; i < sources.length; i++) {
            sources[i].close();
            sources[i] = null;
        }
        sources = null;
    }


}
