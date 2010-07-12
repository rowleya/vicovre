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

package com.googlecode.vicovre.codecs.multiplexers.mp4;

import java.io.IOException;

import com.googlecode.vicovre.codecs.utils.HeaderOutputStream;

public class Atom extends HeaderOutputStream {

    public Atom(String tag) throws IOException {
        super(0, 4);
        writeInt(0); // Size placeholder
        writeBytes(tag);
    }

    public static Atom getFree() throws IOException {
        Atom atom = new Atom("free");
        return atom;
    }

    public Atom(String tag, long size) throws IOException {
        super(0, 0);
        if (size > Math.pow(2, 32)) {
            writeInt(1);
            writeBytes(tag);
            writeLong(size);
        } else {
            write(getFree());
            writeInt((int) (size & 0x00000000FFFFFFFFL));
            writeBytes(tag);
        }
    }

    public void write(Atom atom) throws IOException {
        write(atom.getBytes());
    }

    public void writeInt24(int i) throws IOException {
        write((i >> 16) & 0xFF);
        write((i >> 8) & 0xFF);
        write(i & 0xFF);
    }
}
