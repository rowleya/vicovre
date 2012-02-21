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

package com.googlecode.vicovre.codecs.ffmpeg.video;

import javax.media.format.VideoFormat;

import com.googlecode.vicovre.codecs.ffmpeg.PixelFormat;
import com.googlecode.vicovre.codecs.ffmpeg.Utils;

/**
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class MPEG4Encoder extends FFMPEGVideoCodec {

    /**
     * Creates a new Encoder.
     *
     */
    public MPEG4Encoder() {
        super(Utils.CODEC_ID_MPEG4,
                new VideoFormat[]{new VideoFormat("mpeg4")},
                true, PixelFormat.PIX_FMT_YUV420P);
    }

    public VideoCodecContext getContext() {
        VideoCodecContext context = super.getContext();
        context.setFlags(context.getFlags() | Utils.CODEC_FLAG_GLOBAL_HEADER);
        double ratio = (double)(context.getOutputWidth() * context.getOutputHeight())
            / (320.0 * 240.0);
        int bitRate = (int) (200.0 * 1000.0 * ratio);
        context.setBitrate(bitRate);
        context.setBitrateTolerance(bitRate / context.getFrameRate());
        return context;
    }
}
