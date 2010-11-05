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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.media.MemeticFileReader;

public class VideoMixer {

    private static final float FRAME_RATE = 25.0f;

    private static final Class<?> DATA_TYPE = Format.byteArray;

    private static final int YUV_TYPE = YUVFormat.YUV_420;

    private static final double BUFFER_DURATION = 1000000000.0 / FRAME_RATE;

    private VideoFormat format = null;

    private VideoSource[] sources = null;

    private boolean[] sourceFinished = null;

    private long minStartTime = 0;

    private long currentTimestamp = 0;

    private byte[] data = null;

    private Buffer buffer = null;

    private boolean firstFrameRead = false;

    public VideoMixer(MemeticFileReader[] sources, Rectangle[] positions,
            int backgroundColour, boolean forceFillFirstFrame,
            Dimension outputSize)
            throws UnsupportedFormatException {
        this.sources = new VideoSource[sources.length];
        sourceFinished = new boolean[sources.length];
        minStartTime = Long.MAX_VALUE;
        for (MemeticFileReader source : sources) {
            if (source.getStartTime() < minStartTime) {
                minStartTime = source.getStartTime();
            }
        }

        int minx = Integer.MAX_VALUE;
        int maxx = 0;
        int miny = Integer.MAX_VALUE;
        int maxy = 0;
        for (int i = 0; i < sources.length; i++) {
            int posWidth = positions[i].width;
            int posHeight = positions[i].height;

            minx = Math.min(minx, positions[i].x);
            maxx = Math.max(maxx, positions[i].x + posWidth);
            miny = Math.min(miny, positions[i].y);
            maxy = Math.max(maxy, positions[i].y + posHeight);
        }

        Dimension outSize = null;
        if (outputSize != null) {
            outSize = new Dimension(outputSize);
        } else {
            outSize = new Dimension(minx + maxx, miny + maxy);
        }
        if ((outSize.width % 16) != 0) {
            outSize.width += 16 - (outSize.width % 16);
        }
        if ((outSize.height % 16) != 0) {
            outSize.height += 16 - (outSize.height % 16);
        }
        System.err.println("Output size = " + outSize);
        double scaleWidth = (double) outSize.width / (minx + maxx);
        double scaleHeight = (double) outSize.height / (miny + maxy);

        for (int i = 0; i < sources.length; i++) {
            int posWidth = (int) (positions[i].width * scaleWidth);
            int posHeight = (int) (positions[i].height * scaleHeight);
            if ((posWidth % 2) != 0) {
                posWidth += 1;
            }
            if ((posHeight % 2) != 0) {
                posHeight += 1;
            }

            int ysize = posWidth * posHeight;
            int csize = ysize / 4;
            int maxdatalength = ysize + (csize * 2);
            YUVFormat convertFormat = new YUVFormat(
                    new Dimension(posWidth, posHeight), maxdatalength,
                    DATA_TYPE, FRAME_RATE, YUV_TYPE, posWidth, posWidth / 2,
                    0, ysize, ysize + csize);
            this.sources[i] = new VideoSource(sources[i], convertFormat,
                    minStartTime, (int) (positions[i].x * scaleWidth),
                    (int) (positions[i].y * scaleHeight));
            sourceFinished[i] = false;
        }


        int ysize = outSize.width * outSize.height;
        int csize = ysize / 4;
        int maxdatalength = ysize + (csize * 2);
        format = new YUVFormat(outSize, maxdatalength,
                DATA_TYPE, FRAME_RATE, YUV_TYPE, outSize.width,
                outSize.width / 2, 0, ysize, ysize + csize);

        data = new byte[maxdatalength];
        Color bg = new Color(backgroundColour);
        int r = bg.getRed();
        int g = bg.getGreen();
        int b = bg.getBlue();
        byte y  = (byte) (((int)  ( 0.29900 * r + 0.58700 * g + 0.11400 * b))
                & 0xFF);
        byte cr = (byte) (((int) ((-0.16874 * r - 0.33126 * g + 0.50000 * b)))
                & 0xFF);
        byte cb = (byte) (((int) (( 0.50000 * r - 0.41869 * g - 0.08131 * b)))
                & 0xFF);
        Arrays.fill(data, 0, ysize - 1, y);
        Arrays.fill(data, ysize, ysize + csize - 1, (byte) (cr - 128));
        Arrays.fill(data, ysize + csize, maxdatalength - 1, (byte) (cb - 128));

        buffer = new Buffer();
        buffer.setData(data);
        buffer.setOffset(0);
        buffer.setLength(data.length);
        buffer.setFormat(format);

        firstFrameRead = !forceFillFirstFrame;
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
        return format;
    }

    public boolean readNextBuffer() throws IOException {
        boolean force = false;
        if (!firstFrameRead) {
            force = true;
            firstFrameRead = true;
        }
        for (int i = 0; i < sources.length; i++) {
            if (!sourceFinished[i]) {
                boolean finished = !sources[i].readNextBuffer(buffer, force);
                if (finished && !sourceFinished[i]) {
                    sourceFinished[i] = true;
                    sources[i].readNextBuffer(buffer, true);
                }
            }
        }

        return true;
    }

    /**
     * Reads the next buffer
     * @return The next buffer
     */
    public Buffer getBuffer() {
        buffer.setTimeStamp(getTimestamp());
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
