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

/**
 * Useful functions for dealing with bits
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Bits {

    private Bits() {
        // Does Nothing
    }

    /**
     * Returns the bit shift for a given mask
     *
     * @param mask
     *            The mask to get the shift of
     * @return The number of bits to shift
     */
    public static int getShift(int mask) {
        int shift = 0;
        while ((mask & 0x1) == 0) {
            mask = mask >> 1;
            shift++;
        }
        return shift;
    }

    /**
     * Returns the number of bits for the given mask
     *
     * @param mask
     *            The mask to get the bits for
     * @param shift
     *            The shift to get for the mask
     * @return The number of bits in the mask
     */
    public static int getBitCount(int mask, int shift) {
        int bits = 0;
        mask = mask >> shift;
        while ((mask & 0x1) == 1) {
            mask = mask >> 1;
            bits++;
        }
        return bits;
    }

    /**
     * Converts a number to a string in binary
     * @param c The number to convert
     * @param len The length to display
     * @return The string
     */
    public static String toBinaryString(int c, int len) {
        String s = "";
        for (int i = len - 1; i >= 0; i--) {
            if ((c & (1 << i)) > 0) {
                s += "1";
            } else {
                s += "0";
            }
        }
        return s;
    }
}
