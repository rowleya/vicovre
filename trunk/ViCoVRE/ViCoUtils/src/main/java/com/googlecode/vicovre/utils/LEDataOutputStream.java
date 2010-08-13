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

package com.googlecode.vicovre.utils;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LEDataOutputStream implements DataOutput {

    private DataOutputStream output = null;

    private long count = 0;

    public LEDataOutputStream(OutputStream output) {
        this.output = new DataOutputStream(output);
    }

    public void write(int b) throws IOException {
        output.write(b);
        count += 1;
    }

    public void write(byte[] b) throws IOException {
        output.write(b);
        count += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
        count += len;
    }

    public void writeBoolean(boolean v) throws IOException {
        output.writeBoolean(v);
        count += 1;
    }

    public void writeByte(int v) throws IOException {
        output.writeByte(v);
        count += 1;
    }

    public void writeBytes(String s) throws IOException {
        char[] chars = s.toCharArray();
        for (char c : chars) {
            write(c);
        }
    }

    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    public void writeChars(String s) throws IOException {
        char[] chars = s.toCharArray();
        for (char c : chars) {
            writeChar(c);
        }
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeInt(int v) throws IOException {
        output.write((byte) (0xFF & v));
        output.write((byte) (0xFF & (v >> 8)));
        output.write((byte) (0xFF & (v >> 16)));
        output.write((byte) (0xFF & (v >> 24)));
        count += 4;
    }

    public void writeLong(long v) throws IOException {
        output.write((byte) (0xFF & v));
        output.write((byte) (0xFF & (v >> 8)));
        output.write((byte) (0xFF & (v >> 16)));
        output.write((byte) (0xFF & (v >> 24)));
        output.write((byte) (0xFF & (v >> 32)));
        output.write((byte) (0xFF & (v >> 40)));
        output.write((byte) (0xFF & (v >> 48)));
        output.write((byte) (0xFF & (v >> 56)));
        count += 8;
    }

    public void writeShort(int v) throws IOException {
        output.write((byte) (0xFF & v));
        output.write((byte) (0xFF & (v >> 8)));
        count += 2;
    }

    public void writeUTF(String s) throws IOException {
        int len = 0;
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if ((c >= '\u0001') && (c <= '\u007f')) {
                len += 1;
            } else if ((c == '\u0000')
                    || ((c >= '\u0080') && (c <= '\u07ff'))) {
                len += 2;
            } else {
                len += 3;
            }
        }
        writeShort(len);
        for (char c : chars) {
            if ((c >= '\u0001') && (c <= '\u007f')) {
                output.write((byte) c);
                count += 1;
            } else if ((c == '\u0000')
                    || ((c >= '\u0080') && (c <= '\u07ff'))) {
                output.write((byte) (0x80 | (0x3f & c)));
                output.write((byte) (0xc0 | (0x1f & (c >> 6))));
                count += 2;
            } else {
                output.write((byte)(0x80 | (0x3f & c)));
                output.write((byte)(0x80 | (0x3f & (c >>  6))));
                output.write((byte)(0xe0 | (0x0f & (c >> 12))));
                count += 3;
            }
        }
    }

    public long getCount() {
        return count;
    }
}
