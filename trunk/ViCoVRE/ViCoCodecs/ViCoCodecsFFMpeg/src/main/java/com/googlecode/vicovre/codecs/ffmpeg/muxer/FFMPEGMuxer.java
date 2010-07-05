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

package com.googlecode.vicovre.codecs.ffmpeg.muxer;

import java.io.IOException;
import java.nio.ByteOrder;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;

import com.googlecode.vicovre.codecs.ffmpeg.Utils;
import com.googlecode.vicovre.media.multiplexer.BasicMultiplexer;

public class FFMPEGMuxer extends BasicMultiplexer {

    private static final Format[] getFormats() {
        Format[] videoFormats = Utils.getVideoFormats(null, -1);
        AudioFormat audioFormat = new AudioFormat(AudioFormat.LINEAR,
                -1, 16, -1,
                ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
                    ? AudioFormat.BIG_ENDIAN
                    : AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED);
        Format[] formats = new Format[videoFormats.length + 1];
        for (int i = 0; i < videoFormats.length; i++) {
            formats[i] = videoFormats[i];
        }
        formats[formats.length - 1] = audioFormat;
        return formats;
    }

    protected FFMPEGMuxer(ContentDescriptor contentType) {
        super(new ContentDescriptor[]{contentType}, getFormats(), 2);
    }

    protected int read(byte[] buf, int off, int len, Buffer buffer, int track)
            throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected int readLast(byte[] buf, int off, int len) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getName() {
        return "FFMPEGMuxer";
    }



}