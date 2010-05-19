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

package com.googlecode.vicovre.utils.nativeloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Loads a library from the class resources.
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ResourceLoader implements Loader {

    private static final int BUFFER_SIZE = 1024;

    /**
     * {@inheritDoc}
     * @see com.googlecode.vicovre.utils.nativeloader.Loader#load(
     *     java.lang.Class, java.lang.String)
     */
    public void load(final Class<?> loadingClass, final String name) {
        InputStream input = loadingClass.getResourceAsStream("/" + name);
        if (input != null) {
            File file = new File(NativeLoader.USER_LIB_DIR, name);
            if (!file.exists()) {
                System.err.println("    Extracting to " + file);
                file.getParentFile().mkdirs();
                try {
                    FileOutputStream output = new FileOutputStream(file);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead = input.read(buffer);
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead);
                        bytesRead = input.read(buffer);
                    }
                    output.close();
                    input.close();
                } catch (Exception e) {
                    throw new UnsatisfiedLinkError(e.getMessage());
                }
            }
            System.load(file.getAbsolutePath());
            return;
        }
        throw new UnsatisfiedLinkError("Could not find " + name
                + " in classpath");
    }
}