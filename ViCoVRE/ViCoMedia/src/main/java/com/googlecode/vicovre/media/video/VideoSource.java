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

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.media.MemeticFileReader;
import com.googlecode.vicovre.media.processor.SimpleProcessor;

public class VideoSource {

    private Buffer buffer = null;

    private MemeticFileReader source = null;

    private long startTime = 0;

    private long offsetShift = 0;

    private double msPerRead = 0;

    private long sourceOffset = 0;

    private double currentOffset = 0;

    private double bufferStartOffset = 0;

    private VideoFormat format = null;

    private SimpleProcessor processor = null;

    private boolean isFinished = false;

    public VideoSource(MemeticFileReader source, VideoFormat convertFormat,
            long minStartTime, float frameRate)
            throws UnsupportedFormatException {
        this.source = source;
        this.startTime = source.getStartTime();
        this.offsetShift = startTime - minStartTime;
        this.format = (VideoFormat) source.getFormat();
        if (!format.getEncoding().equals(convertFormat)) {
            processor = new SimpleProcessor(format, convertFormat);
            format = (VideoFormat) processor.getOutputFormat();
        }
        msPerRead = 1000.0 / frameRate;
    }

    public void seek(long offset) throws IOException {
        source.streamSeek(offset - offsetShift);
        currentOffset = offset - msPerRead;
        sourceOffset = source.getOffset() + offsetShift;
    }

    public long getOffset() {
        return source.getOffset() + offsetShift;
    }

    public void setTimestampOffset(long timestampOffset) {
        source.setTimestampOffset(timestampOffset);
    }

    public void readNextBuffer(Buffer bufferToFill, int x, int y)
            throws IOException {
        currentOffset += msPerRead;
        if (!isFinished && (buffer == null)) {
            isFinished = !source.readNextPacket();
            if (!isFinished) {
                buffer = source.getBuffer();
                if (processor != null) {
                    processor.process(buffer);
                    buffer = processor.getOutputBuffer();
                    format = (VideoFormat) buffer.getFormat();
                }
                bufferStartOffset = sourceOffset
                    + (source.getTimestamp() / 1000000);
            }
        }
        if (isFinished || (currentOffset < bufferStartOffset)) {
            return;
        }

        VideoFormat bufferFormat = (VideoFormat) bufferToFill.getFormat();
        if (bufferFormat instanceof RGBFormat) {
            RGBFormat rgb = (RGBFormat) format;
            RGBFormat targetRgb = (RGBFormat) bufferFormat;
            Dimension size = format.getSize();
            Dimension targetSize = bufferFormat.getSize();
            Object data = buffer.getData();
            Object targetData = bufferToFill.getData();

            for (int i = 0; i < size.height; i++) {
                int srcPos = buffer.getOffset()
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
            Dimension targetSize = bufferFormat.getSize();
            Object data = buffer.getData();
            Object targetData = bufferToFill.getData();
            int type = yuv.getYuvType();
            if ((type == YUVFormat.YUV_111)
                    || (type == YUVFormat.YUV_411)
                    || (type == YUVFormat.YUV_420)
                    || (type == YUVFormat.YUV_422)
                    || (type == YUVFormat.YUV_YVU9)) {
                int[] offsets = new int[]{yuv.getOffsetY(), yuv.getOffsetU(),
                                        yuv.getOffsetV()};
                int[] targetOffsets = new int[]{targetYuv.getOffsetY(),
                        targetYuv.getOffsetU(), targetYuv.getOffsetV()};
                int[] strides = new int[]{yuv.getStrideY(), yuv.getStrideUV(),
                        yuv.getStrideUV()};
                int[] targetStrides = new int[]{targetYuv.getStrideY(),
                        targetYuv.getStrideUV(), targetYuv.getStrideUV()};
                for (int j = 0; j < offsets.length; j++) {
                    int offset = buffer.getOffset() + offsets[j];
                    int targetOffset = bufferToFill.getOffset()
                        + targetOffsets[j];
                    int stride = strides[j];
                    int targetStride = targetStrides[j];
                    for (int i = 0; i < size.height; i++) {
                        int srcPos = offset + (size.width * i * stride);
                        int destPos = targetOffset
                            + (targetSize.width * (x + i) * targetStride)
                            + (y * targetStride);
                        int length = size.width * stride;
                        System.arraycopy(data, srcPos,
                                targetData, destPos, length);
                    }
                }
            } else if (type == YUVFormat.YUV_YUYV) {
                int stride = 4;
                if (yuv.getDataType() == Format.intArray) {
                    stride = 1;
                } else if (yuv.getDataType() == Format.shortArray) {
                    stride = 2;
                }
                for (int i = 0; i < size.height; i++) {
                    int srcPos = buffer.getOffset()
                        + (size.width * i * stride);
                    int destPos = bufferToFill.getOffset()
                        + (targetSize.width * (x + i) * stride)
                        + (y * stride);
                    int length = size.width * stride;
                    System.arraycopy(data, srcPos,
                            targetData, destPos, length);
                }
            }
        }
        buffer = null;
    }

}
