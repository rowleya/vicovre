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

package com.googlecode.vicovre.codecs.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class HeaderOutputStream implements DataOutput {

    private int sizePosition = 0;

    private int sizeLength = 0;

    private ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    private DataOutputStream output = new DataOutputStream(bytes);

    public HeaderOutputStream(int sizePosition, int sizeLength) {
        this.sizePosition = sizePosition;
        this.sizeLength = sizeLength;
    }

    public void write(int b) throws IOException {
        output.write(b);
    }

    public void write(byte[] b) throws IOException {
        output.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    public void writeBoolean(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        output.writeByte(v);
    }

    public void writeBytes(String s) throws IOException {
        output.writeBytes(s);
    }

    public void writeChar(int v) throws IOException {
        output.writeChar(v);
    }

    public void writeChars(String s) throws IOException {
        output.writeChars(s);
    }

    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    public void writeShort(int v) throws IOException {
        output.writeShort(v);
    }

    public void writeUTF(String s) throws IOException {
        output.writeUTF(s);
    }

    public int getSize() throws IOException {
        output.flush();
        bytes.flush();
        return bytes.size();
    }

    public byte[] getBytes() throws IOException {
        output.flush();
        bytes.flush();
        byte[] data = bytes.toByteArray();
        int size = data.length;
        for (int i = (sizePosition + sizeLength - 1); i >= sizePosition ; i--) {
            data[i] = (byte) (size & 0xFF);
            size >>= Byte.SIZE;
        }
        return data;
    }
}
