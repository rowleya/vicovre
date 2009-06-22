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

import java.text.DecimalFormat;

/**
 * This class is used to perform the forward and inverse discrete cosine
 * transform (DCT) as specified by the CCITT TI.81 recommendation
 * (www.w3.org/Graphics/JPEG/itu-t81.pdf). The implementation of the IDCT and
 * FDCT algorithms are based on the jfdctflt.c and jidctflt.c implementations
 * written by Thomas G. Lane.
 */
public class DCT {

    private static final int BLOCK_SIZE = 8;

    private static final double R2 = Math.sqrt(2);

    // these values are used in the IDCT
    private static final double[] SCALE_FACTOR = {1.0, // 1.0
            Math.cos(1 * Math.PI / 16) * R2, // 1.3870398453221475
            Math.cos(2 * Math.PI / 16) * R2, // 1.3065629648763766
            Math.cos(3 * Math.PI / 16) * R2, // 1.1758756024193588
            Math.cos(4 * Math.PI / 16) * R2, // 1.0
            Math.cos(5 * Math.PI / 16) * R2, // 0.7856949583871023
            Math.cos(6 * Math.PI / 16) * R2, // 0.5411961001461971
            Math.cos(7 * Math.PI / 16) * R2 }; // 0.2758993792829431

    private static final float[] SCALE_FACTOR_F = {
            (float) SCALE_FACTOR[0],
            (float) SCALE_FACTOR[1], (float) SCALE_FACTOR[2],
            (float) SCALE_FACTOR[3], (float) SCALE_FACTOR[4],
            (float) SCALE_FACTOR[5], (float) SCALE_FACTOR[6],
            (float) SCALE_FACTOR[7] };

    // these values are used in the FDCT
    private static final double F0 = 1.0 / R2;

    private static final double F1 = Math.cos(1 * Math.PI / 16) / 2;

    private static final double F2 = Math.cos(2 * Math.PI / 16) / 2;

    private static final double F3 = Math.cos(3 * Math.PI / 16) / 2;

    private static final double F4 = Math.cos(4 * Math.PI / 16) / 2;

    private static final double F5 = Math.cos(5 * Math.PI / 16) / 2;

    private static final double F6 = Math.cos(6 * Math.PI / 16) / 2;

    private static final double F7 = Math.cos(7 * Math.PI / 16) / 2;

    private static final double D71 = F7 - F1; // -0.39284747919355106

    private static final double D35 = F3 - F5; // 0.13794968964147147

    private static final double D62 = F6 - F2; // -0.27059805007309845

    private static final double S71 = F7 + F1; // 0.5879378012096794

    private static final double S35 = F3 + F5; // 0.6935199226610738

    private static final double S62 = F6 + F2; // 0.6532814824381883

    private static final float F0_F = (float) F0;

    private static final float F3_F = (float) F3;

    private static final float F4_F = (float) F4;

    private static final float F6_F = (float) F6;

    private static final float F7_F = (float) F7;

    private static final float D71_F = (float) D71;

    private static final float D35_F = (float) D35;

    private static final float D62_F = (float) D62;

    private static final float S71_F = (float) S71;

    private static final float S35_F = (float) S35;

    private static final float S62_F = (float) S62;

    private static final float B0 = 0.35355339059327376220f;

    private static final float B1 = 0.25489778955207958447f;

    private static final float B2 = 0.27059805007309849220f;

    private static final float B3 = 0.30067244346752264027f;

    private static final float B4 = 0.35355339059327376220f;

    private static final float B5 = 0.44998811156820785231f;

    private static final float B6 = 0.65328148243818826392f;

    private static final float B7 = 1.28145772387075308943f;

    private static int fpScale(float v) {
        return (int) ((double) v * (double) (1 << 15) + 0.5);
    }

    private int[] crossStage = new int[] {
            fpScale(B0 * B0), fpScale(B0 * B1), fpScale(B0 * B2),
            fpScale(B0 * B3), fpScale(B0 * B4), fpScale(B0 * B5),
            fpScale(B0 * B6), fpScale(B0 * B7),

            fpScale(B1 * B0), fpScale(B1 * B1), fpScale(B1 * B2),
            fpScale(B1 * B3), fpScale(B1 * B4), fpScale(B1 * B5),
            fpScale(B1 * B6), fpScale(B1 * B7),

            fpScale(B2 * B0), fpScale(B2 * B1), fpScale(B2 * B2),
            fpScale(B2 * B3), fpScale(B2 * B4), fpScale(B2 * B5),
            fpScale(B2 * B6), fpScale(B2 * B7),

            fpScale(B3 * B0), fpScale(B3 * B1), fpScale(B3 * B2),
            fpScale(B3 * B3), fpScale(B3 * B4), fpScale(B3 * B5),
            fpScale(B3 * B6), fpScale(B3 * B7),

            fpScale(B4 * B0), fpScale(B4 * B1), fpScale(B4 * B2),
            fpScale(B4 * B3), fpScale(B4 * B4), fpScale(B4 * B5),
            fpScale(B4 * B6), fpScale(B4 * B7),

            fpScale(B5 * B0), fpScale(B5 * B1), fpScale(B5 * B2),
            fpScale(B5 * B3), fpScale(B5 * B4), fpScale(B5 * B5),
            fpScale(B5 * B6), fpScale(B5 * B7),

            fpScale(B6 * B0), fpScale(B6 * B1), fpScale(B6 * B2),
            fpScale(B6 * B3), fpScale(B6 * B4), fpScale(B6 * B5),
            fpScale(B6 * B6), fpScale(B6 * B7),

            fpScale(B7 * B0), fpScale(B7 * B1), fpScale(B7 * B2),
            fpScale(B7 * B3), fpScale(B7 * B4), fpScale(B7 * B5),
            fpScale(B7 * B6), fpScale(B7 * B7), };

    private static final int A1 = fpScale(0.7071068f);

    private static final int A2 = fpScale(0.5411961f);

    private static final int A3 = A1;

    private static final int A4 = fpScale(1.3065630f);

    private static final int A5 = fpScale(0.3826834f);

    private static int fpMultiply(int a, int b) {
        return ((((a) >> 5) * ((b) >> 5)) >> 5);
    }

    private QuickArray tmp = null;

    private QuickArray quickCrossStage = null;

    /**
     * Creates a new DCT object
     * @throws QuickArrayException
     *
     */
    public DCT() throws QuickArrayException {
        quickCrossStage = new QuickArray(int[].class, crossStage.length);
        for (int i = 0; i < crossStage.length; i++) {
            quickCrossStage.setInt(i, crossStage[i]);
        }
        tmp = new QuickArray(int[].class, 64);
    }

    /**
     * This method performs the forward discrete cosine transform (FDCT). The in
     * array is linearized.
     * @param in The input array
     * @param out The output array
     * @param startoff The start offset in the input array
     * @param xoff The x offset in the input array
     * @param yoff The y offset in the input array
     * @param stride The stride of the input array
     * @throws QuickArrayException
     */
    public void fdct(byte[] in, int[] out, int startoff, int xoff, int yoff,
            int stride) throws QuickArrayException {
        QuickArrayWrapper outWrapper = new QuickArrayWrapper(out);
        QuickArrayWrapper inWrapper = new QuickArrayWrapper(in);
        fdct(inWrapper, outWrapper, startoff, xoff, yoff, stride);
    }

    /**
     * This method performs the forward discrete cosine transform (FDCT). The in
     * array is linearized.
     * @param in The input array
     * @param out The output array
     * @param startoff The start offset in the input array
     * @param xoff The x offset in the input array
     * @param yoff The y offset in the input array
     * @param stride The stride of the input array
     */
    public void fdct(QuickArrayAbstract in, QuickArrayAbstract out,
            int startoff, int xoff, int yoff, int stride) {
        float temp;
        float a0, a1, a2, a3, a4, a5, a6, a7;
        float b0, b1, b2, b3, b4, b5, b6, b7;

        int pin = startoff + (yoff * stride) + xoff;
        int pout = 0;

        // Horizontal transform
        for (int i = 0; i < BLOCK_SIZE; i++) {
            b0 = (in.getByte(pin + 0) & 0xFF)
                    + (in.getByte(pin + 7) & 0xFF);
            b7 = (in.getByte(pin + 0) & 0xFF)
                    - (in.getByte(pin + 7) & 0xFF);
            b1 = (in.getByte(pin + 1) & 0xFF)
                    + (in.getByte(pin + 6) & 0xFF);
            b6 = (in.getByte(pin + 1) & 0xFF)
                    - (in.getByte(pin + 6) & 0xFF);
            b2 = (in.getByte(pin + 2) & 0xFF)
                    + (in.getByte(pin + 5) & 0xFF);
            b5 = (in.getByte(pin + 2) & 0xFF)
                    - (in.getByte(pin + 5) & 0xFF);
            b3 = (in.getByte(pin + 3) & 0xFF)
                    + (in.getByte(pin + 4) & 0xFF);
            b4 = (in.getByte(pin + 3) & 0xFF)
                    - (in.getByte(pin + 4) & 0xFF);

            a0 = b0 + b3;
            a1 = b1 + b2;
            a2 = b1 - b2;
            a3 = b0 - b3;
            a4 = b4;
            a5 = (b6 - b5) * F0_F;
            a6 = (b6 + b5) * F0_F;
            a7 = b7;
            out.setInt(pout + 0, (int) ((a0 + a1) * F4_F));
            out.setInt(pout + 4, (int) ((a0 - a1) * F4_F));

            temp = (a3 + a2) * F6_F;
            out.setInt(pout + 2, (int) (temp - a3 * D62_F));
            out.setInt(pout + 6, (int) (temp - a2 * S62_F));

            b4 = a4 + a5;
            b7 = a7 + a6;
            b5 = a4 - a5;
            b6 = a7 - a6;

            temp = (b7 + b4) * F7_F;
            out.setInt(pout + 1, (int) (temp - b7 * D71_F));
            out.setInt(pout + 7, (int) (temp - b4 * S71_F));

            temp = (b6 + b5) * F3_F;
            out.setInt(pout + 5, (int) (temp - b6 * D35_F));
            out.setInt(pout + 3, (int) (temp - b5 * S35_F));

            pin += stride;
            pout += BLOCK_SIZE;
        }

        // Vertical transform
        pout = 0;
        for (int i = 0; i < BLOCK_SIZE; i++) {
            b0 = out.getInt(pout + (0 * BLOCK_SIZE))
                    + out.getInt(pout + (7 * BLOCK_SIZE));
            b7 = out.getInt(pout + (0 * BLOCK_SIZE))
                    - out.getInt(pout + (7 * BLOCK_SIZE));
            b1 = out.getInt(pout + (1 * BLOCK_SIZE))
                    + out.getInt(pout + (6 * BLOCK_SIZE));
            b6 = out.getInt(pout + (1 * BLOCK_SIZE))
                    - out.getInt(pout + (6 * BLOCK_SIZE));
            b2 = out.getInt(pout + (2 * BLOCK_SIZE))
                    + out.getInt(pout + (5 * BLOCK_SIZE));
            b5 = out.getInt(pout + (2 * BLOCK_SIZE))
                    - out.getInt(pout + (5 * BLOCK_SIZE));
            b3 = out.getInt(pout + (3 * BLOCK_SIZE))
                    + out.getInt(pout + (4 * BLOCK_SIZE));
            b4 = out.getInt(pout + (3 * BLOCK_SIZE))
                    - out.getInt(pout + (4 * BLOCK_SIZE));

            a0 = b0 + b3;
            a1 = b1 + b2;
            a2 = b1 - b2;
            a3 = b0 - b3;
            a4 = b4;
            a5 = (b6 - b5) * F0_F;
            a6 = (b6 + b5) * F0_F;
            a7 = b7;
            out.setInt(pout + (0 * BLOCK_SIZE),
                    (int) ((a0 + a1) * F4_F));
            out.setInt(pout + (4 * BLOCK_SIZE),
                    (int) ((a0 - a1) * F4_F));

            temp = (a3 + a2) * F6_F;
            out.setInt(pout + (2 * BLOCK_SIZE), (int) (temp - a3
                    * D62_F));
            out.setInt(pout + (6 * BLOCK_SIZE), (int) (temp - a2
                    * S62_F));

            b4 = a4 + a5;
            b7 = a7 + a6;
            b5 = a4 - a5;
            b6 = a7 - a6;

            temp = (b7 + b4) * F7_F;
            out.setInt(pout + (1 * BLOCK_SIZE), (int) (temp - b7
                    * D71_F));
            out.setInt(pout + (7 * BLOCK_SIZE), (int) (temp - b4
                    * S71_F));

            temp = (b6 + b5) * F3_F;
            out.setInt(pout + (5 * BLOCK_SIZE), (int) (temp - b6
                    * D35_F));
            out.setInt(pout + (3 * BLOCK_SIZE), (int) (temp - b5
                    * S35_F));
            pout += 1;
        }
    }

    private static int fpNormalize(int v) {
        return (((v) + (1 << (15 - 1))) >> 15);
    }

    private static int limit(int x) {
        int t = x;
        t &= ~(t >> 31);
        return (t | ~((t - 256) >> 31)) & 0xFF;
    }

    /**
     * Reverse DCT algorithm
     * @param block The short array block to apply algorithm to
     * @param m0 The mapping indicating which positions in the block contain
     *           coefficients
     * @param out The output array
     * @param offset The offset in the output array to write to
     * @param stride The stride of the output array
     * @throws QuickArrayException
     */
    public void rdct(QuickArrayAbstract block, long m0,
            byte[] out, int offset, int stride) throws QuickArrayException {
        QuickArrayWrapper output = new QuickArrayWrapper(out);
        rdct(block, m0, output, offset, stride);
    }

    /**
     * Reverse DCT algorithm
     * @param block The short array block to apply algorithm to
     * @param m0 The mapping indicating which positions in the block contain
     *           coefficients
     * @param output The output array
     * @param offset The offset in the output array to write to
     * @param stride The stride of the output array
     * @throws QuickArrayException
     */
    public void rdct(QuickArrayAbstract block, long m0,
            QuickArrayAbstract output,
            int offset, int stride) throws QuickArrayException {
        tmp.clear();
        int tp = 0;
        int qt = 0;
        int bp = 0;
        /*
         * First pass is 1D transform over the rows of the input array.
         */
        int i;
        for (i = 8; --i >= 0;) {
            if ((m0 & 0xfe) == 0) {
                /*
                 * All ac terms are zero.
                 */
                int v = 0;
                if (((m0 >> 0) & 0x1) > 0) {
                    v = quickCrossStage.getInt(qt) * block.getShort(bp);
                }
                tmp.setInt(tp + 0, v);
                tmp.setInt(tp + 1, v);
                tmp.setInt(tp + 2, v);
                tmp.setInt(tp + 3, v);
                tmp.setInt(tp + 4, v);
                tmp.setInt(tp + 5, v);
                tmp.setInt(tp + 6, v);
                tmp.setInt(tp + 7, v);
            } else {
                int t4 = 0, t5 = 0, t6 = 0, t7 = 0;
                if ((m0 & 0xaa) != 0) {
                    /* odd part */
                    if (((m0 >> 1) & 0x1) > 0) {
                        t4 = quickCrossStage.getInt(qt + 1)
                                * block.getShort(bp + 1);
                    }
                    if (((m0 >> 3) & 0x1) > 0) {
                        t5 = quickCrossStage.getInt(qt + 3)
                                * block.getShort(bp + 3);
                    }
                    if (((m0 >> 5) & 0x1) > 0) {
                        t6 = quickCrossStage.getInt(qt + 5)
                                * block.getShort(bp + 5);
                    }
                    if (((m0 >> 7) & 0x1) > 0) {
                        t7 = quickCrossStage.getInt(qt + 7)
                                * block.getShort(bp + 7);
                    }
                    int x0 = t6 - t5;
                    t6 += t5;
                    int x1 = t4 - t7;
                    t7 += t4;

                    t5 = fpMultiply(t7 - t6, A3);
                    t7 += t6;

                    t4 = fpMultiply(x1 + x0, A5);
                    t6 = fpMultiply(x1, A4) - t4;
                    t4 += fpMultiply(x0, A2);

                    t7 += t6;
                    t6 += t5;
                    t5 += t4;
                }
                int t0 = 0, t1 = 0, t2 = 0, t3 = 0;
                if ((m0 & 0x55) != 0) {
                    /* even part */
                    if (((m0 >> 0) & 0x1) > 0) {
                        t0 = quickCrossStage.getInt(qt + 0)
                                * block.getShort(bp + 0);
                    }
                    if (((m0 >> 2) & 0x1) > 0) {
                        t1 = quickCrossStage.getInt(qt + 2)
                                * block.getShort(bp + 2);
                    }
                    if (((m0 >> 4) & 0x1) > 0) {
                        t2 = quickCrossStage.getInt(qt + 4)
                                * block.getShort(bp + 4);
                    }
                    if (((m0 >> 6) & 0x1) > 0) {
                        t3 = quickCrossStage.getInt(qt + 6)
                                * block.getShort(bp + 6);
                    }

                    int x0 = fpMultiply(t1 - t3, A1);
                    t3 += t1;
                    t1 = t0 - t2;
                    t0 += t2;
                    t2 = t3 + x0;
                    t3 = t0 - t2;
                    t0 += t2;
                    t2 = t1 - x0;
                    t1 += x0;
                }
                tmp.setInt(tp + 0, t0 + t7);
                tmp.setInt(tp + 1, t1 + t6);
                tmp.setInt(tp + 2, t2 + t5);
                tmp.setInt(tp + 3, t3 + t4);
                tmp.setInt(tp + 4, t3 - t4);
                tmp.setInt(tp + 5, t2 - t5);
                tmp.setInt(tp + 6, t1 - t6);
                tmp.setInt(tp + 7, t0 - t7);
            }
            qt += 8;
            tp += 8;
            bp += 8;
            m0 >>= 8;
        }
        tp -= 64;
        /*
         * Second pass is 1D transform over the rows of the temp array.
         */
        for (i = 0; i < 8; i++) {
            int t4 = tmp.getInt(tp + (8 * 1));
            int t5 = tmp.getInt(tp + (8 * 3));
            int t6 = tmp.getInt(tp + (8 * 5));
            int t7 = tmp.getInt(tp + (8 * 7));
            if ((t4 | t5 | t6 | t7) != 0) {
                /* odd part */
                int x0 = t6 - t5;
                t6 += t5;
                int x1 = t4 - t7;
                t7 += t4;

                t5 = fpMultiply(t7 - t6, A3);
                t7 += t6;

                t4 = fpMultiply(x1 + x0, A5);
                t6 = fpMultiply(x1, A4) - t4;
                t4 += fpMultiply(x0, A2);

                t7 += t6;
                t6 += t5;
                t5 += t4;
            }
            int t0 = tmp.getInt(tp + (8 * 0));
            int t1 = tmp.getInt(tp + (8 * 2));
            int t2 = tmp.getInt(tp + (8 * 4));
            int t3 = tmp.getInt(tp + (8 * 6));
            if ((t0 | t1 | t2 | t3) != 0) {
                /* even part */
                int x0 = fpMultiply(t1 - t3, A1);
                t3 += t1;
                t1 = t0 - t2;
                t0 += t2;
                t2 = t3 + x0;
                t3 = t0 - t2;
                t0 += t2;
                t2 = t1 - x0;
                t1 += x0;
            }

            int p = offset + (i * stride);
            output.setByte(p + 0, (byte) (limit(fpNormalize(t0 + t7))));
            output.setByte(p + 1, (byte) (limit(fpNormalize(t1 + t6))));
            output.setByte(p + 2, (byte) (limit(fpNormalize(t2 + t5))));
            output.setByte(p + 3, (byte) (limit(fpNormalize(t3 + t4))));
            output.setByte(p + 4, (byte) (limit(fpNormalize(t3 - t4))));
            output.setByte(p + 5, (byte) (limit(fpNormalize(t2 - t5))));
            output.setByte(p + 6, (byte) (limit(fpNormalize(t1 - t6))));
            output.setByte(p + 7, (byte) (limit(fpNormalize(t0 - t7))));

            tp += 1;
        }
    }

    /**
     * This method applies the pre-scaling that the IDCT(float[][], float[][],
     * float[][]) method needs to work correctly. The table parameter should be
     * 8x8, non-zigzag order.
     * @param table The table to scale
     */
    public static void scaleQuantizationTable(float[][] table) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
            for (int j = 0; j < BLOCK_SIZE; j++) {
                table[i][j] = table[i][j] * SCALE_FACTOR_F[i]
                                          * SCALE_FACTOR_F[j] / 8;
            }
        }
    }

    private void test() throws Exception {
        byte[] testArray = new byte[64];
        for (int i = 0; i < 64; i++) {
            testArray[i] = (byte) ((int) (Math.random() * 255) & 0xFF);
        }
        int[] out = new int[64];
        fdct(testArray, out, 0, 0, 0, 8);

        QuickArray block = new QuickArray(short[].class, 64);
        for (int i = 0; i < 64; i++) {
            block.setShort(i, (short) out[i]);
        }
        DecimalFormat format = new DecimalFormat(" 0000;-0000");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.err.print(format.format(out[(i * 8) + j])
                        + " ");
            }
            System.err.print("    ");
            for (int j = 0; j < 8; j++) {
                System.err.print(format.format(block.getShort((i * 8) + j))
                        + " ");
            }
            System.err.println();
        }
        System.err.println();

        byte[] testOutArray = new byte[64];
        rdct(block, 0xFFFFFFFFFFFFFFFFL, testOutArray, 0, 8);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.err.print(format.format(testArray[i * 8 + j]) + " ");
            }
            System.err.print("    ");
            for (int j = 0; j < 8; j++) {
                System.err.print(format.format(testOutArray[i * 8 + j]) + " ");
            }
            System.err.println();
        }
    }

    /**
     * Frees any resources used
     */
    public void close() {
        quickCrossStage.free();
        tmp.free();
    }

    /**
     * Test method
     * @param args Ignored
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        DCT dct = new DCT();
        dct.test();
    }

}
