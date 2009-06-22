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
 * An input stream for reading things a bit at a time
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class BitInputStream {

    private static final int SHORT_SHIFT = 16;

    private static final int BYTE_MASK = 0xFF;

    private static final int BYTE_SHIFT = 8;

    private static final int HUFF_SIZE_MASK = 0x1F;

    private static final int HUFF_SIZE_SHIFT = 5;

    private static final int TIMES_8_SHIFT = 3;

    private int bb = 0;

    private int nbb = 0;

    private QuickArrayWrapper input = null;

    private int currentOffset = 0;

    private int endInput = 0;

    private int lastLen = 0;

    private int lastCode = 0;

    /**
     * Creates a new BitInputStream
     * @param input The data array to read bits from
     * @param offset The offset to start reading from
     * @param length The length of the data in input
     * @throws QuickArrayException
     */
    public BitInputStream(byte[] input, int offset, int length)
            throws QuickArrayException {

        this.input = new QuickArrayWrapper(input);
        currentOffset = offset;
        endInput = length;
    }

    private void huffRQ() {
        bb <<= SHORT_SHIFT;
        bb |= (input.getByte(currentOffset++) & BYTE_MASK) << BYTE_SHIFT;
        bb |=  input.getByte(currentOffset++) & BYTE_MASK;
    }

    /**
     * Looks at the next bit that will be read
     * @return The next bit
     */
    public int peekNextBit() {
        return bb >> (nbb - 1);
    }

    /**
     * Reads a set of bits from the input
     * @param n The number of bits to read
     * @return An integer containing the bits
     */
    public int readBits(int n) {
        nbb -= n;
        if (nbb < 0) {
            huffRQ();
            nbb += SHORT_SHIFT;
        }
        int val = (bb >> nbb) & ((1 << n) - 1);
        /*System.err.print("Reading " + n + " bits: ");
        for (int i = n - 1; i >= 0; i--) {
            if ((val & (1 << i)) > 0) {
                System.err.print("1");
            } else {
                System.err.print("0");
            }
        }
        System.err.println(); */
        return val;
    }

    /**
     * Decodes a huffman code
     * @param ht The address of a huffman table (shorts)
     * @param maxLen The maximum length of each code
     * @return The code found
     */
    public int huffDecode(QuickArray ht, int maxLen) {
        if (nbb < SHORT_SHIFT) {
            huffRQ();
            nbb += SHORT_SHIFT;
        }
        int s = maxLen;
        int v = (bb >> (nbb - s)) & ((1 << s) - 1);
        s = ht.getShort(v);
        nbb -= (s & HUFF_SIZE_MASK);

        lastLen = (s & HUFF_SIZE_MASK);
        lastCode = v >> (maxLen - lastLen);

        /*int len = (s & 0x1f);
        int code = v >> (maxLen - len);
        int val = (s >> 5);
        System.err.print("Huffman decoded " + len + " bits from "
            + H261ASDecoder.toBinaryString(v, maxLen) + ": ");
        for (int i = len - 1; i >= 0; i--) {
            if ((code & (1 << i)) > 0) {
                System.err.print("1");
            } else {
                System.err.print("0");
            }
        }
        System.err.println(" : " + val); */

        return (s >> HUFF_SIZE_SHIFT);
    }

    /**
     * Gets the number of bits left to read
     * @return The number of bits left
     */
    public int bitsRemaining() {
        return ((endInput - currentOffset) << TIMES_8_SHIFT) + nbb;
    }

    /**
     * Returns a string representation of the last code read
     * @return The last code read
     */
    public String getLastCode() {
        String code = "";
        for (int i = lastLen - 1; i >= 0; i--) {
            if ((lastCode & (1 << i)) > 0) {
                code += 1;
            } else {
                code += 0;
            }
        }
        return code;
    }
}
