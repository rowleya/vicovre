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

package com.googlecode.vicovre.media.video;

import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import javax.media.Buffer;
import javax.media.PlugIn;
import javax.media.format.RGBFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.media.MemeticFileReader;
import com.googlecode.vicovre.media.processor.SimpleProcessor;

public class VideoSource {

    private Buffer buffer = null;

    private Buffer nextBuffer = null;

    private MemeticFileReader source = null;

    private long startTime = 0;

    private long offsetShift = 0;

    private double msPerRead = 0;

    private long sourceOffset = 0;

    private double currentOffset = 0;

    private double bufferStartOffset = 0;

    private double bufferEndOffset = 0;

    private VideoFormat format = null;

    private VideoFormat convertFormat = null;

    private SimpleProcessor inputProcessor = null;

    private SimpleProcessor outputProcessor = null;

    private boolean isFinished = false;

    private boolean frameCopied = false;

    private int x = 0;

    private int y = 0;

    public VideoSource(MemeticFileReader source, VideoFormat convertFormat,
            long minStartTime, int x, int y)
    throws UnsupportedFormatException {
        this.source = source;
        this.startTime = source.getStartTime();
        this.offsetShift = startTime - minStartTime;
        this.x = x;
        this.y = y;
        this.convertFormat = convertFormat;
        inputProcessor = new SimpleProcessor(source.getFormat(),
                (VideoFormat) null);
        format = (VideoFormat) inputProcessor.getOutputFormat();
        msPerRead = 1000.0 / convertFormat.getFrameRate();
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

    private void readNextBuffer() throws IOException {
        nextBuffer = null;
        isFinished = !source.readNextPacket();
        if (!isFinished) {
            int result = PlugIn.OUTPUT_BUFFER_NOT_FILLED;
            while (!isFinished
                    && (result != PlugIn.BUFFER_PROCESSED_OK)
                    && (result != PlugIn.INPUT_BUFFER_NOT_CONSUMED)) {
                nextBuffer = source.getBuffer();
                result = inputProcessor.process(nextBuffer);
                if ((result != PlugIn.BUFFER_PROCESSED_OK)
                        && (result != PlugIn.INPUT_BUFFER_NOT_CONSUMED)) {
                    isFinished = !source.readNextPacket();
                }

            }
            if ((result == PlugIn.BUFFER_PROCESSED_OK)
                    || (result == PlugIn.INPUT_BUFFER_NOT_CONSUMED)) {
                Buffer inputBuffer = inputProcessor.getOutputBuffer();
                if (outputProcessor == null) {
                    try {
                        outputProcessor = new SimpleProcessor(
                                inputBuffer.getFormat(), convertFormat);
                        format = (VideoFormat)
                            outputProcessor.getOutputFormat();
                    } catch (UnsupportedFormatException e) {
                        e.printStackTrace();
                    }
                }
                outputProcessor.process(inputBuffer);
                nextBuffer = outputProcessor.getOutputBuffer();
            }
        }
    }

    private void readBuffer() throws IOException {
        if ((nextBuffer == null) && !isFinished) {
            readNextBuffer();
        }
        do {
            if (nextBuffer != null) {
                buffer = nextBuffer;
                bufferStartOffset = sourceOffset
                    + (source.getTimestamp() / 1000000);

                readNextBuffer();
                if (!isFinished) {
                    bufferEndOffset = sourceOffset
                        + (source.getTimestamp() / 1000000);
                } else {
                    bufferEndOffset = bufferStartOffset + msPerRead;
                }
            } else {
                buffer = null;
            }
        } while ((buffer != null) && (bufferEndOffset < currentOffset));
    }

    public boolean readNextBuffer(Buffer bufferToFill, boolean force)
            throws IOException {
        if (!force) {
            currentOffset += msPerRead;
        }

        if (!isFinished && ((buffer == null)
                || (currentOffset > bufferEndOffset))) {
            readBuffer();
            frameCopied = false;
        }

        if (!force && ((buffer == null)
                || (currentOffset < bufferStartOffset)
                || frameCopied)) {
            return !isFinished;
        }
        frameCopied = true;

        VideoFormat bufferFormat = (VideoFormat) bufferToFill.getFormat();
        Dimension targetSize = bufferFormat.getSize();
        Object targetData = bufferToFill.getData();
        Object data = null;
        int offset = 0;
        boolean created = false;
        if (buffer != null) {
            data = buffer.getData();
            offset = buffer.getOffset();
            if (data == null) {
                created = true;
                data = Array.newInstance(targetData.getClass(),
                        format.getMaxDataLength());
            }
        } else {
            created = true;
            data = Array.newInstance(targetData.getClass(),
                    format.getMaxDataLength());
        }

        if (bufferFormat instanceof RGBFormat) {
            RGBFormat rgb = (RGBFormat) format;
            RGBFormat targetRgb = (RGBFormat) bufferFormat;
            Dimension size = format.getSize();

            for (int i = 0; i < size.height; i++) {
                int srcPos = offset
                    + (size.width * i * rgb.getPixelStride());
                int destPos = bufferToFill.getOffset()
                    + (targetSize.width * (x + i) * targetRgb.getPixelStride())
                    + (y * targetRgb.getPixelStride());
                int length = size.width * rgb.getPixelStride();
                System.arraycopy(data, srcPos,
                    targetData, destPos, length);
            }
        } else if (bufferFormat instanceof YUVFormat) {
            YUVFormat yuv = (YUVFormat) format;
            YUVFormat targetYuv = (YUVFormat) bufferFormat;
            Dimension size = format.getSize();
            int type = yuv.getYuvType();
            if ((type == YUVFormat.YUV_111)
                    || (type == YUVFormat.YUV_411)
                    || (type == YUVFormat.YUV_420)
                    || (type == YUVFormat.YUV_422)
                    || (type == YUVFormat.YUV_YVU9)) {
                int ratio = size.width / yuv.getStrideUV();
                int crheight = size.height / ratio;

                if (created) {
                    Arrays.fill((byte[]) data, yuv.getOffsetY(),
                            (size.width * size.height) - 1, (byte) 0);
                    Arrays.fill((byte[]) data, yuv.getOffsetU(),
                            (yuv.getStrideUV() * crheight - 1), (byte) 0x80);
                    Arrays.fill((byte[]) data, yuv.getOffsetV(),
                            (yuv.getStrideUV() * crheight - 1), (byte) 0x80);
                }
                int[] heights = new int[]{size.height, crheight, crheight};
                int[] offsets = new int[]{yuv.getOffsetY(), yuv.getOffsetU(),
                                        yuv.getOffsetV()};
                int[] targetOffsets = new int[]{targetYuv.getOffsetY(),
                        targetYuv.getOffsetU(), targetYuv.getOffsetV()};
                int[] strides = new int[]{yuv.getStrideY(), yuv.getStrideUV(),
                        yuv.getStrideUV()};
                int[] targetStrides = new int[]{targetYuv.getStrideY(),
                        targetYuv.getStrideUV(), targetYuv.getStrideUV()};
                int[] ratios = new int[]{1, ratio, ratio};

                for (int j = 0; j < offsets.length; j++) {
                    int off = offset + offsets[j];
                    int targetOff = bufferToFill.getOffset()
                        + targetOffsets[j];
                    int stride = strides[j];
                    int targetStride = targetStrides[j];
                    for (int i = 0; i < heights[j]; i++) {
                        int srcPos = off + (i * stride);
                        int destPos = targetOff
                            + (((y / ratios[j]) + i) * targetStride)
                            + (x / ratios[j]);
                        System.arraycopy(data, srcPos,
                            targetData, destPos, stride);
                    }
                }
            }
        }
        return true;
    }

    public void close() {
        if (inputProcessor != null) {
            inputProcessor.close();
        }
        if (outputProcessor != null) {
            outputProcessor.close();
        }
    }

}
