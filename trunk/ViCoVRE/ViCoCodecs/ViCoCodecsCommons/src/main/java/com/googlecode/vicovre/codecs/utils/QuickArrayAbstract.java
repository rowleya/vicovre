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

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Abstract class for quick but unsafe (using sun.misc.Unsafe) arrays
 * @author Andrew G D Rowley
 * @version 1.0
 */
@SuppressWarnings("restriction")
public abstract class QuickArrayAbstract {

    protected static final boolean DEBUG = false;

    private Unsafe unsafe = null;

    private long byteSize = 0;

    private long intSize = 0;

    private long shortSize = 0;

    private long longSize = 0;

    /**
     * Creates a new QuickArrayAbstract
     * @throws QuickArrayException
     */
    public QuickArrayAbstract() throws QuickArrayException {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);

            byteSize = unsafe.arrayIndexScale(byte[].class);
            shortSize = unsafe.arrayIndexScale(short[].class);
            intSize = unsafe.arrayIndexScale(int[].class);
            longSize = unsafe.arrayIndexScale(long[].class);
        } catch (Exception e) {
            throw new QuickArrayException(e);
        }
    }

    protected Unsafe getUnsafe() {
        return unsafe;
    }

    protected abstract long getDataAddress();

    protected abstract boolean freed();

    protected abstract long getLength();

    protected synchronized void check(long pos, long length) {
        String error = null;
        if (freed()) {
            error = "Array has already been freed!";
        } else if ((pos + length) > getLength()) {
            error = "Access will go out of bounds: starting at " + pos + " for "
                    + length + " bytes of " + getLength();
        }
        if (error != null) {
            Throwable t = new Throwable(error);
            t.fillInStackTrace();
            t.printStackTrace();
        }

    }

    /**
     * Gets a byte from the array
     * @param pos The position in the array as if it were a byte array
     * @return The byte at the given position
     */
    public byte getByte(int pos) {
        if (DEBUG) {
            check(pos * byteSize, 1);
        }
        return unsafe.getByte(getDataAddress() + (pos * byteSize));
    }

    /**
     * Gets a short from the array
     * @param pos The position in the array as if it were a short array
     * @return The short at the given position
     */
    public short getShort(int pos) {
        if (DEBUG) {
            check(pos * shortSize, 2);
        }
        return unsafe.getShort(getDataAddress() + (pos * shortSize));
    }

    /**
     * Gets an int from the array
     * @param pos The position in the array as if it were an int array
     * @return The int at the given position
     */
    public int getInt(int pos) {
        if (DEBUG) {
            check(pos * intSize, 4);
        }
        return unsafe.getInt(getDataAddress() + (pos * intSize));
    }

    /**
     * Gets a long from the array
     * @param pos The position in the array as if it were a long array
     * @return The long at the given position
     */
    public long getLong(int pos) {
        if (DEBUG) {
            check(pos * longSize, 8);
        }
        return unsafe.getLong(getDataAddress() + (pos * longSize));
    }

    /**
     * Sets a byte in the array
     * @param pos The position in the array as if it were a byte array
     * @param b The byte to set
     */
    public void setByte(int pos, byte b) {
        if (DEBUG) {
            check(pos * byteSize, 1);
        }
        unsafe.putByte(getDataAddress() + (pos * byteSize), b);
    }

    /**
     * Sets a short in the array
     * @param pos The position in the array as if it were a short array
     * @param s The short to set
     */
    public void setShort(int pos, short s) {
        if (DEBUG) {
            check(pos * shortSize, 2);
        }
        unsafe.putShort(getDataAddress() + (pos * shortSize), s);
    }

    /**
     * Sets an int in the array
     * @param pos The position in the array as if it were an int array
     * @param i The int to set
     */
    public void setInt(int pos, int i) {
        if (DEBUG) {
            check(pos * intSize, 4);
        }
        unsafe.putInt(getDataAddress() + (pos * intSize), i);
    }

    /**
     * Sets a long in the array
     * @param pos The position in the array as if it were a long array
     * @param l The long to set
     */
    public void setLong(int pos, long l) {
        if (DEBUG) {
            check(pos * longSize, 8);
        }
        unsafe.putLong(getDataAddress() + (pos * longSize), l);
    }

    /**
     * Copies another array into this one
     * @param in The array to copy
     * @param inOffset The offset to start copying from
     * @param offset The offset to start copying to
     * @param length The length of data to copy in bytes
     */
    public void copy(QuickArrayAbstract in, int inOffset, int offset,
            int length) {
        if (DEBUG) {
            check(offset, length);
        }
        unsafe.copyMemory(in.getDataAddress() + inOffset,
                getDataAddress() + offset, length);
    }
}
