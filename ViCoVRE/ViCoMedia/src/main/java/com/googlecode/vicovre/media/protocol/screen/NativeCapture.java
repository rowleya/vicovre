/*
 * @(#)NativeCapture.java
 * Created: 23 Jan 2008
 * Version: 1.0
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
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
 */

package com.googlecode.vicovre.media.protocol.screen;

import com.googlecode.vicovre.utils.nativeloader.NativeLoader;

/**
 * Captures a screen from a native source
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class NativeCapture {

    private long peer = 0;

    /**
     * Creates a new NativeCapture
     *
     * @param x The start position on the x axis
     * @param y The start position on the y axis
     * @param width The width of the capture
     * @param height The height of the capture
     *
     */
    public NativeCapture(int x, int y, int width, int height) {
        NativeLoader.loadLibrary(getClass(), "nativecapture");
        init(x, y, width, height);
    }

    /**
     * Sets the address of the native peer
     * @param peer The native peer address
     */
    public void setPeer(long peer) {
        this.peer = peer;
    }

    /**
     * Gets the address of the native peer
     * @return The address of the native peer
     */
    public long getPeer() {
        return peer;
    }

    private native void init(int x, int y, int width, int height);

    /**
     * Captures a portion of the screen
     * @param data The array into which the data is to be stored
     */
    public native void captureScreen(byte[] data);

    /**
     * Closes the screen capture
     */
    public native void close();
}
