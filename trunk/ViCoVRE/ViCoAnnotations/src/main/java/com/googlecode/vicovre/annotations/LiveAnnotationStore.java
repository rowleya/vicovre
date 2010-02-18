/*
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

package com.googlecode.vicovre.annotations;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class LiveAnnotationStore {
    private PrintWriter outFile = null;
    private static LiveAnnotationStore ref;
    private static int refCount = 0;

    private LiveAnnotationStore(String filename) {
        File dumpFile = new File(filename);
        try {
            if (dumpFile.getParentFile() != null) {
                dumpFile.getParentFile().mkdirs();
            }
            dumpFile.createNewFile();
            outFile = new PrintWriter(new FileWriter(filename, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LiveAnnotationStore getAnnotationStore(String filename) {
        if (ref == null) {
            // it's ok, we can call this constructor
            ref = new LiveAnnotationStore(filename);
        }
        refCount++;
        return ref;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
        // that'll teach 'em
    }

    public void close() {
        refCount--;
        if (refCount == 0) {
            outFile.close();
            ref = null;
        }
    }

    public void write(LiveAnnotation annotation) {
        outFile.println(annotation.toXml());
        outFile.flush();
    }
}