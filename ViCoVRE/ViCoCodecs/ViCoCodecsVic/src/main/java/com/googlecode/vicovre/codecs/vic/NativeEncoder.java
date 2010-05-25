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
import javax.media.control.KeyFrameControl;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.media.controls.KeyFrameForceControl;
import com.googlecode.vicovre.utils.nativeloader.NativeLoader;

public class NativeEncoder implements Codec,
        KeyFrameControl, KeyFrameForceControl {

    private static final VicCodec[] CODECS = new VicCodec[]{
        new VicCodec("H261Encoder", new VideoFormat("H261/RTP"),
                new YUVFormat(new Dimension(352, 288), -1, Format.byteArray, -1,
                        YUVFormat.YUV_420, -1, -1, -1, -1, -1), 2),
        new VicCodec("H261ASEncoder", new VideoFormat("H261AS/RTP"),
                new YUVFormat(YUVFormat.YUV_420), 1),
        new VicCodec("H264Encoder", new VideoFormat("H264/RTP"),
                new YUVFormat(YUVFormat.YUV_420), 1),
    };

    private long ref = 0;

    private int codec = -1;

    private byte[] buffer = null;

    private int[] frameOffsets = null;

    private int[] frameLengths = null;

    private int sequenceNo = 0;

    private int framesBetweenKey = 250;

    private int framesSinceLastKey = 0;

    public Format[] getSupportedInputFormats() {
        if (codec != -1) {
            return new Format[]{CODECS[codec].getOutputFormat()};
        }
        Format[] formats = new Format[CODECS.length];
        for (int i = 0; i < CODECS.length; i++) {
            formats[i] = CODECS[i].getOutputFormat();
        }
        return formats;
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            Format[] formats = new Format[CODECS.length];
            for (int i = 0; i < CODECS.length; i++) {
                formats[i] = CODECS[i].getInputFormat();
            }
            return formats;
        }
        if (codec != -1) {
            if (CODECS[codec].getOutputFormat().matches(input)) {
                return new Format[]{CODECS[codec].getInputFormat()};
            }
            return new Format[0];
        }
        Vector<Format> outputFormats = new Vector<Format>();
        for (int i = 0; i < CODECS.length; i++) {
            if (CODECS[i].getOutputFormat().matches(input)) {
                outputFormats.add(CODECS[i].getInputFormat());
            }
        }
        System.err.println("For input " + input + " outputs = " + outputFormats);
        return outputFormats.toArray(new Format[0]);
    }

    public int process(Buffer input, Buffer output) {
        if (input.getOffset() == 0) {
            if (buffer == null) {
                YUVFormat inputFormat = (YUVFormat) input.getFormat();
                Dimension size = inputFormat.getSize();
                buffer = new byte[size.width * size.height * 4];
                setSize(ref, size.width, size.height);
                frameOffsets = new int[buffer.length / 1024];
                frameLengths = new int[buffer.length / 1024];
            }
            Arrays.fill(frameOffsets, -1);
            Arrays.fill(frameLengths, -1);
            output.setData(buffer);
            output.setOffset(0);
            output.setLength(buffer.length);
            if (framesSinceLastKey == framesBetweenKey) {
                keyFrame(ref);
                framesSinceLastKey = 0;
            }
            encode(ref, input, output, frameOffsets, frameLengths);
        }
        int frameNo = input.getOffset();
        output.setData(buffer);
        output.setFormat(CODECS[codec].getInputFormat());
        output.setOffset(frameOffsets[frameNo]);
        output.setLength(frameLengths[frameNo]);
        output.setTimeStamp(input.getTimeStamp());
        output.setSequenceNumber(sequenceNo++);
        output.setFlags(0);
        frameNo += 1;
        if ((frameNo == frameOffsets.length) || (frameOffsets[frameNo] == -1)) {
            output.setFlags(Buffer.FLAG_RTP_MARKER);
            input.setOffset(0);
            return BUFFER_PROCESSED_OK;
        }
        input.setOffset(frameNo);
        return INPUT_BUFFER_NOT_CONSUMED;
    }

    public Format setInputFormat(Format format) {
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

    public Format setOutputFormat(Format format) {
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

    public void close() {
        if (ref != 0) {
            closeCodec(ref);
        }
    }

    public String getName() {
        return "NativeEncoder";
    }

    public void open() throws ResourceUnavailableException {
        NativeLoader.loadLibrary(getClass(), "vic");
        if (codec == -1) {
            throw new ResourceUnavailableException("Codec not set");
        }
        ref = openCodec(codec);
        if (ref <= 0) {
            throw new ResourceUnavailableException("Cannot open codec: " + ref);
        }
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        if (className.equals(KeyFrameControl.class.getName())
                || className.equals(KeyFrameForceControl.class.getName())) {
            return this;
        }
        return null;
    }

    public Object[] getControls() {
        return new Object[]{this};
    }


    /**
     *
     * @see javax.media.control.KeyFrameControl#getKeyFrameInterval()
     */
    public int getKeyFrameInterval() {
        return framesBetweenKey;
    }

    /**
     *
     * @see javax.media.control.KeyFrameControl#getPreferredKeyFrameInterval()
     */
    public int getPreferredKeyFrameInterval() {
        return 250;
    }

    /**
     *
     * @see javax.media.control.KeyFrameControl#setKeyFrameInterval(int)
     */
    public int setKeyFrameInterval(int frames) {
        framesBetweenKey = frames;
        return frames;
    }

    /**
     *
     * @see com.googlecode.vicovre.media.controls.KeyFrameForceControl#
     *     nextFrameKey()
     */
    public void nextFrameKey() {
        framesSinceLastKey = framesBetweenKey;
    }

    public Component getControlComponent() {
        return null;
    }

    private native long openCodec(int id);

    private native void setSize(long ref, int width, int height);

    private native int encode(long ref, Buffer input, Buffer output,
            int[] frameOffsets, int[] frameLengths);

    private native void keyFrame(long ref);

    private native void closeCodec(long ref);

}
