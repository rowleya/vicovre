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

package com.googlecode.vicovre.codecs.ffmpeg.demuxer;

import javax.media.Buffer;

public class CircularBuffer {

    private byte[] data = null;

    private int first = 0;

    private int last = 0;

    private int readSize = 0;

    public CircularBuffer(int capacity) {
        data = new byte[capacity];
        first = 0;
        last = 0;
    }

    public void setReadSize(int readSize) {
        this.readSize = readSize;
    }

    public boolean canRead() {
        if (last > first) {
            return (last - first) > readSize;
        }
        if ((data.length - first) > readSize) {
            return true;
        }
        return last > readSize;
    }

    public Buffer getNext() {
        if (!canRead()) {
            return null;
        }
        if (first > last) {
            if ((data.length - first) < readSize) {
                first = 0;
            }
        }
        Buffer buffer = new Buffer();
        buffer.setData(data);
        buffer.setOffset(first);
        buffer.setLength(readSize);

        first = (first + readSize) % data.length;
        return buffer;
    }

    public void shrinkLastBuffer(Buffer buffer) {
        if (((buffer.getOffset() + readSize) % data.length) != first) {
            throw new RuntimeException(
                    "Shrinking buffer that was not the last allocated");
        }
        int diff = readSize - buffer.getLength();
        if (first >= diff) {
            first -= diff;
        } else {
            first = data.length - (first - diff);
        }

    }

    public void freeFront(Buffer buffer) {
        if (buffer.getOffset() != last) {
            throw new RuntimeException("Freeing buffer that is not at front");
        }
        last = (last + buffer.getLength()) % data.length;
    }

}
