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

package com.googlecode.vicovre.codecs.multiplexers.mp3;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;

import com.googlecode.vicovre.media.multiplexer.BasicMultiplexer;

public class JavaMultiplexer extends BasicMultiplexer {

    public JavaMultiplexer() {
        super(new ContentDescriptor[]{new ContentDescriptor("audio/mpeg")},
            new Format[]{new VideoFormat(null),
                         new AudioFormat(AudioFormat.MPEGLAYER3)},
            1);
    }

    public int process(Buffer buf, int track) {
        if (buf.getFormat() instanceof VideoFormat) {
            return BUFFER_PROCESSED_OK;
        }
        return super.process(buf, track);
    }

    public String getName() {
        return "MP3 Multiplexer";
    }

    protected int read(byte[] buf, int off, int len, Buffer buffer, int track) {
        Format format = buffer.getFormat();
        int length = buffer.getLength();
        int offset = buffer.getOffset();

        if (format instanceof AudioFormat) {
            int toCopy = length;
            if (length > len) {
                toCopy = len;
            }
            System.arraycopy(buffer.getData(), offset, buf, off,
                    toCopy);
            if (toCopy < length) {
                buffer.setOffset(offset + toCopy);
                buffer.setLength(length - toCopy);
            } else {
                setResult(BUFFER_PROCESSED_OK, true);
            }
            return toCopy;
        }
        setResult(BUFFER_PROCESSED_OK, true);
        return 0;
    }

    protected int readLast(byte[] buf, int off, int len) {
        return 0;
    }
}
