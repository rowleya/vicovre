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

package com.googlecode.vicovre.media.processor;

import java.util.Comparator;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

public class ClosestFormatComparator implements Comparator<Format> {

    private Format format = null;

    public ClosestFormatComparator(Format format) {
        this.format = format;
    }

    private int test(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return -1;
        }
        if (((value1 == null) && (value2 != null))
                || ((value1 != null) && (value2 == null))) {
            return 0;
        }
        if (value1.equals(value2)) {
            return -1;
        }
        return 0;
    }

    private int test(int value1, int value2) {
        if (value1 == -1 && value2 == -1) {
            return -1;
        }
        if (((value1 == -1) && (value2 != -1))
                || ((value1 != -1) && (value2 == -1))) {
            return 0;
        }
        if (value1 == value2) {
            return -1;
        }
        return 0;
    }

    private int getMatchCount(Format testFormat) {
        if (format.equals(testFormat)) {
            return -100;
        }

        int matchCount = 0;
        matchCount += test(format.getEncoding(), testFormat.getEncoding());
        matchCount += test(format.getDataType(), testFormat.getDataType());

        if (testFormat instanceof AudioFormat) {
            return getMatchCount((AudioFormat) testFormat, matchCount);
        }
        if (testFormat instanceof VideoFormat) {
            return getMatchCount((VideoFormat) testFormat, matchCount);
        }

        return matchCount;
    }

    private int getMatchCount(AudioFormat testFormat, int matchCount) {
        if (!(format instanceof AudioFormat)) {
            return 100;
        }
        AudioFormat af = (AudioFormat) format;
        matchCount += test(af.getChannels(), testFormat.getChannels());
        matchCount += test(af.getEndian(), testFormat.getEndian());
        matchCount += test(af.getFrameRate(), testFormat.getFrameRate());
        matchCount += test(af.getFrameSizeInBits(),
                testFormat.getFrameSizeInBits());
        matchCount += test(af.getSampleRate(), testFormat.getSampleRate());
        matchCount += test(af.getSigned(), testFormat.getSigned());
        return matchCount;
    }

    private int getMatchCount(VideoFormat testFormat, int matchCount) {
        if (!(format instanceof VideoFormat)) {
            return 100;
        }
        VideoFormat vf = (VideoFormat) format;
        matchCount += test(vf.getFrameRate(), testFormat.getFrameRate());
        matchCount += test(vf.getMaxDataLength(),
                testFormat.getMaxDataLength());
        matchCount += test(vf.getSize(), testFormat.getSize());

        if (testFormat instanceof RGBFormat) {
            return getMatchCount((RGBFormat) testFormat, matchCount);
        }
        if (testFormat instanceof YUVFormat) {
            return getMatchCount((YUVFormat) testFormat, matchCount);
        }
        return matchCount;
    }

    private int getMatchCount(RGBFormat testFormat, int matchCount) {
        if (!(format instanceof RGBFormat)) {
            return 100;
        }
        RGBFormat rgb = (RGBFormat) format;
        matchCount += test(rgb.getBitsPerPixel(), testFormat.getBitsPerPixel());
        matchCount += test(rgb.getBlueMask(), testFormat.getBlueMask());
        matchCount += test(rgb.getEndian(), testFormat.getEndian());
        matchCount += test(rgb.getFlipped(), testFormat.getFlipped());
        matchCount += test(rgb.getGreenMask(), testFormat.getGreenMask());
        matchCount += test(rgb.getLineStride(), testFormat.getLineStride());
        matchCount += test(rgb.getPixelStride(), testFormat.getPixelStride());
        matchCount += test(rgb.getRedMask(), testFormat.getRedMask());

        return matchCount;
    }

    private int getMatchCount(YUVFormat testFormat, int matchCount) {
        if (!(format instanceof YUVFormat)) {
            return 100;
        }
        YUVFormat yuv = (YUVFormat) format;
        matchCount += test(yuv.getOffsetU(), testFormat.getOffsetU());
        matchCount += test(yuv.getOffsetV(), testFormat.getOffsetV());
        matchCount += test(yuv.getOffsetY(), testFormat.getOffsetY());
        matchCount += test(yuv.getStrideUV(), testFormat.getStrideUV());
        matchCount += test(yuv.getStrideY(), testFormat.getStrideY());
        int yuvCount = test(yuv.getYuvType(), testFormat.getYuvType());
        matchCount += yuvCount;
        return matchCount;
    }

    public static boolean isRaw(Format testFormat) {
        if (testFormat instanceof YUVFormat) {
            return true;
        }
        if (testFormat instanceof RGBFormat) {
            return true;
        }
        if ((testFormat instanceof AudioFormat)
                && testFormat.getEncoding().equals(AudioFormat.LINEAR)) {
            return true;
        }
        return false;
    }

    public int compare(Format f1, Format f2) {
        if (format == null) {
            boolean f1isRaw = isRaw(f1);
            boolean f2isRaw = isRaw(f2);
            if (f1isRaw && f2isRaw) {
                return 0;
            }
            if (f1isRaw) {
                return -1;
            }
            return 1;
        }
        int f1Count = getMatchCount(f1);
        int f2Count = getMatchCount(f2);
        return f1Count - f2Count;
    }

}
