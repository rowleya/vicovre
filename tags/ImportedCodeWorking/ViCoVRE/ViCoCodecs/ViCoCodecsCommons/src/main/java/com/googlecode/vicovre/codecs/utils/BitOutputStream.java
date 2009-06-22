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
 * Outputs bits to an output stream
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class BitOutputStream {

    private static final int FIRST_BYTE_SHIFT = 24;

    private static final int SECOND_BYTE_SHIFT = 16;

    private static final int THIRD_BYTE_SHIFT = 8;

    private static final int FOURTH_BYTE_SHIFT = 0;

    private static final int BYTE_MASK = 0xFF;

    private static final int INT_SIZE = 32;

    private static final int BYTE_SIZE = 8;

    private static final int TIMES_8_SHIFT = 3;

    // The bit buffer
    private int bb = 0;

    // The number of bits in the buffer
    private int nbb = 0;

    // The output array to write to
    private QuickArrayWrapper output = null;

    private int startOutput = 0;

    private int currentOffset = 0;

    /**
     * Creates a new bit output stream
     * @param output The byte array to output to
     * @param offset The offset to start writing at
     * @throws QuickArrayException
     */
    public BitOutputStream(byte[] output, int offset)
            throws QuickArrayException {

        this.output = new QuickArrayWrapper(output);
        startOutput = offset;
        currentOffset = startOutput;
    }

    private void storeBits() {
        output.setByte(currentOffset++,
                (byte) ((bb >> FIRST_BYTE_SHIFT) & BYTE_MASK));
        output.setByte(currentOffset++,
                (byte) ((bb >> SECOND_BYTE_SHIFT) & BYTE_MASK));
        output.setByte(currentOffset++,
                (byte) ((bb >> THIRD_BYTE_SHIFT) & BYTE_MASK));
        output.setByte(currentOffset++,
                (byte) ((bb >> FOURTH_BYTE_SHIFT) & BYTE_MASK));
    }

    /**
     * Writes the least significant count bits from bits.
     * The most significant bit is written first
     * @param bits The bits to write
     * @param count The number of bits to write from bits
     */
    public void add(int bits, int count) {
        nbb += count;
        if (nbb > INT_SIZE) {
            int extra = nbb - INT_SIZE;
            bb |= (bits >> extra);
            storeBits();
            bb = bits << (INT_SIZE - extra);
            nbb = extra;
        } else {
            bb |= bits << (INT_SIZE - nbb);
        }
    }

    /**
     * Returns the number of bits written
     * @return the number of bits written
     */
    public int noBits() {
        return ((currentOffset - startOutput) << TIMES_8_SHIFT) + nbb;
    }

    /**
     * Gets the number of bytes written to the stream
     * @return The number of bytes written to the stream
     */
    public int getLength() {
        return (currentOffset - startOutput);
    }

    /**
     * Forces any bits in the buffer to be written to the stream
     * Note that the last byte may not be filled
     *
     * @return The number of extra bits unused in the last byte
     */
    public int flush() {
        if (nbb > 0) {
            output.setByte(currentOffset++,
                    (byte) ((bb >> FIRST_BYTE_SHIFT) & BYTE_MASK));
            nbb -= BYTE_SIZE;
        }
        if (nbb > 0) {
            output.setByte(currentOffset++,
                    (byte) ((bb >> SECOND_BYTE_SHIFT) & BYTE_MASK));
            nbb -= BYTE_SIZE;
        }
        if (nbb > 0) {
            output.setByte(currentOffset++,
                    (byte) ((bb >> THIRD_BYTE_SHIFT) & BYTE_MASK));
            nbb -= BYTE_SIZE;
        }
        if (nbb > 0) {
            output.setByte(currentOffset++,
                    (byte) ((bb >> FOURTH_BYTE_SHIFT) & BYTE_MASK));
            nbb -= BYTE_SIZE;
        }
        int extra = -nbb;
        nbb = 0;
        return extra;
    }
}
