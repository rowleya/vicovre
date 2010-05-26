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

import java.io.IOException;

import javax.media.Buffer;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;

import com.googlecode.vicovre.media.MemeticFileReader;
import com.googlecode.vicovre.media.processor.SimpleProcessor;

public class AudioSource {

    private Buffer buffer = null;

    private MemeticFileReader source = null;

    private boolean isFinished = false;

    private long startTime = 0;

    private long offsetShift = 0;

    private double msPerRead = 0;

    private long sourceOffset = 0;

    private double currentOffset = 0;

    private double bufferStartOffset = 0;

    private double bufferEndOffset = 0;

    private AudioFormat format = null;

    private SimpleProcessor processor = null;

    private int sampleSizeInBytes = 0;

    private int maxSample = 0;

    private int samplePosition = 0;

    public AudioSource(MemeticFileReader source, long minStartTime)
            throws UnsupportedFormatException {
        this.source = source;
        this.startTime = source.getStartTime();
        this.offsetShift = startTime - minStartTime;
        this.format = (AudioFormat) source.getFormat();
        if (!format.getEncoding().equals(AudioFormat.LINEAR)) {
            processor = new SimpleProcessor(format,
                    new AudioFormat(AudioFormat.LINEAR));
            format = (AudioFormat) processor.getOutputFormat();
        }
        this.msPerRead = 1000.0 / format.getSampleRate();
        this.sampleSizeInBytes = format.getSampleSizeInBits() / Byte.SIZE;
        this.maxSample = (1 << format.getSampleSizeInBits() - 1) - 1;
    }

    public void seek(long offset) throws IOException {
        source.streamSeek(offset - offsetShift);
        currentOffset = offset - msPerRead;
        sourceOffset = source.getOffset() + offsetShift;
    }

    public long getOffset() {
        return (long) (currentOffset + msPerRead);
    }

    public void setTimestampOffset(long timestampOffset) {
        source.setTimestampOffset(timestampOffset);
    }

    private void readBuffer() throws IOException {
        isFinished = !source.readNextPacket();
        if (!isFinished) {
            buffer = source.getBuffer();
            if (processor != null) {
                processor.process(buffer);
                buffer = processor.getOutputBuffer();
                format = (AudioFormat) buffer.getFormat();
            }
            bufferStartOffset = sourceOffset
                + (source.getTimestamp() / 1000000);
            long samplesInBuffer = buffer.getLength()
                / sampleSizeInBytes;
            double durationInMs = (1000.0 * samplesInBuffer)
                / format.getSampleRate();
            bufferEndOffset = bufferStartOffset + durationInMs;
            samplePosition = buffer.getOffset();
        }
    }

    private double readSample(int position) {
        int sample = 0;
        byte[] data = (byte[]) buffer.getData();
        for (int i = 0; i < sampleSizeInBytes; i++) {
            int shift = (sampleSizeInBytes - i - 1) * Byte.SIZE;
            if (format.getEndian() == AudioFormat.LITTLE_ENDIAN) {
                shift = i * Byte.SIZE;
            }
            sample |= (data[position + i] & 0xFF) << shift;
        }

        if (format.getSigned() == AudioFormat.SIGNED) {
            int shift = Integer.SIZE - format.getSampleSizeInBits();
            sample <<= shift;
            sample >>= shift;
        } else {
            sample -= maxSample;
        }
        return (double) sample / maxSample;
    }

    public double readNextSample() throws IOException {
        currentOffset += msPerRead;
        if (!isFinished && ((buffer == null)
                || (currentOffset > bufferEndOffset))) {
            readBuffer();
        }
        if (isFinished || (currentOffset < bufferStartOffset)) {
            return 0;
        }
        double sample = readSample(samplePosition);
        samplePosition += sampleSizeInBytes;
        return sample;
    }

    public void close() {
        if (processor != null) {
            processor.close();
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    public double getSampleRate() {
        return format.getSampleRate();
    }

}
