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

import java.awt.Component;

import javax.media.control.BitRateControl;
import javax.media.format.AudioFormat;

import com.googlecode.vicovre.codecs.ffmpeg.Utils;

public class AACEncoder extends FFMPEGAudioCodec implements BitRateControl {

    int bitRate = 128000;

    public AACEncoder() {
        super(Utils.CODEC_ID_AAC,
            new AudioFormat[]{new AudioFormat("aac")}, true);
    }

    public AudioCodecContext getContext() {
        AudioCodecContext context = super.getContext();
        context.setFlags(context.getFlags() | Utils.CODEC_FLAG_GLOBAL_HEADER);
        context.setBitRate(bitRate);
        return context;
    }

    public int getBitRate() {
        return bitRate;
    }

    public int getMaxSupportedBitRate() {
        return 320000;
    }

    public int getMinSupportedBitRate() {
        return 32000;
    }

    public int setBitRate(int bitRate) {
        this.bitRate = bitRate;
        return bitRate;
    }

    public Component getControlComponent() {
        return null;
    }

}