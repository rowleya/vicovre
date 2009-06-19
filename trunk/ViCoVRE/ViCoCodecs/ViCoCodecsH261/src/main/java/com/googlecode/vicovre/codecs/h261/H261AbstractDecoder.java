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

package com.googlecode.vicovre.codecs.h261;

import javax.media.Codec;

import com.googlecode.vicovre.codecs.controls.FrameFillControl;
import com.googlecode.vicovre.codecs.utils.BitInputStream;
import com.googlecode.vicovre.codecs.utils.DCT;
import com.googlecode.vicovre.codecs.utils.QuickArray;
import com.googlecode.vicovre.codecs.utils.QuickArrayAbstract;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;

/**
 * Common H261 Decoding functions
 * @author Andrew G D Rowley
 * @version 1.0
 */
public abstract class H261AbstractDecoder implements Codec,
        FrameFillControl {

    protected static final int MT_TCOEFF = 0x01;

    protected static final int MT_CBP    = 0x02;

    protected static final int MT_MVD    = 0x04;

    protected static final int MT_MQUANT = 0x08;

    protected static final int MT_FILTER = 0x10;

    protected static final int MT_INTRA  = 0x20;

    protected static final int MBA_STUFF = -2;

    protected static final int CBP_Y1 = 32;

    protected static final int CBP_Y2 = 16;

    protected static final int CBP_Y3 = 8;

    protected static final int CBP_Y4 = 4;

    protected static final int CBP_CB = 2;

    protected static final int CBP_CR = 1;

    private static final int GOB_HEADER = -1;

    private static final int SYM_ILLEGAL = -2;

    private static final int EOB = -1;

    private static final int ESCAPE = 0;

    private QuickArray qtable = null;

    private QuickArray mbaHuff = null;

    private int mbaHuffMaxLen = 0;

    private QuickArray runLevelHuff = null;

    private int runLevelHuffMaxLen = 0;

    private QuickArray mtypeHuff = null;

    private int mtypeHuffMaxLen = 10;

    private QuickArray cbpHuff = null;

    private int cbpHuffMaxLen = 0;

    private QuickArray block = null;

    private DCT dct = null;

    /**
     * Creates a new H261AbstractDecoder
     * @throws QuickArrayException
     */
    public H261AbstractDecoder() throws QuickArrayException {
        qtable = new QuickArray(short[].class, 32 * 256);

        for (int mq = 0; mq < 32; mq++) {
            int qtp = (mq << 8);
            for (int v = 0; v < 256; v++) {
                int s = (v << 24) >> 24;
                int val = 0;
                if (s > 0) {
                    val = (((s << 1) + 1) * mq) - (~mq & 1);
                } else {
                    val = (((s << 1) - 1) * mq) + (~mq & 1);
                }
                qtable.setShort(qtp + v, (short) val);
            }
        }

        mbaHuffMaxLen = 16;
        mbaHuff = makeHuff(H261Constants.MBAHUFF, mbaHuffMaxLen);
        addCode(0xf, 11,  MBA_STUFF, mbaHuff, mbaHuffMaxLen);
        addCode(0x1, 16, GOB_HEADER, mbaHuff, mbaHuffMaxLen);
        runLevelHuffMaxLen = getMaxLen(H261Constants.RUNLEVELHUFF);
        runLevelHuff = makeHuff(H261Constants.RUNLEVELHUFF,
                runLevelHuffMaxLen);
        addCode(0x2, 2,    EOB, runLevelHuff, runLevelHuffMaxLen);
        addCode(0x1, 6, ESCAPE, runLevelHuff, runLevelHuffMaxLen);

        mtypeHuff = new QuickArray(short[].class, 1 << mtypeHuffMaxLen);
        addCode(0x1,  1, MT_CBP | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  2, MT_FILTER | MT_MVD | MT_CBP | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  3, MT_FILTER | MT_MVD,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  4, MT_INTRA | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  5, MT_MQUANT | MT_CBP | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  6, MT_MQUANT | MT_FILTER | MT_MVD | MT_CBP | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  7, MT_INTRA | MT_MQUANT | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  8, MT_MVD | MT_CBP | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1,  9, MT_MVD,
                mtypeHuff, mtypeHuffMaxLen);
        addCode(0x1, 10, MT_MQUANT | MT_CBP | MT_MVD | MT_TCOEFF,
                mtypeHuff, mtypeHuffMaxLen);

        cbpHuffMaxLen = 9;
        cbpHuff = makeHuff(H261Constants.CPBHUFF, cbpHuffMaxLen);

        block = new QuickArray(short[].class, 64);
        dct = new DCT();
    }



    private void addCode(int code, int length, int val, QuickArray huff,
            int maxLen) {
        int nbit = maxLen - length;
        int map = (val << 5) | length;
        code = (code & ((1 << maxLen) - 1)) << nbit;
        for (int n = (1 << nbit) - 1; n >= 0; --n) {
            int c = (code | n);
            huff.setShort(c, (short) map);
        }
    }

    private QuickArray makeHuff(int[] ht, int maxLen)
            throws QuickArrayException {
        int huffSize = 1 << maxLen;
        QuickArray huff = new QuickArray(short[].class, huffSize);
        for (int i = 0; i < huffSize; ++i) {
            huff.setShort(i, (short) ((SYM_ILLEGAL << 5) | maxLen));
        }
        for (int i = 0; i < ht.length; i += 2) {
            int length = ht[i + 1];
            int code = ht[i];
            if (length != 0) {
                addCode(code, length, i / 2, huff, maxLen);
            }
        }
        return huff;
    }

    private int getMaxLen(int[] ht) {
        int maxLen = 0;
        for (int i = 0; i < ht.length; i += 2) {
            int length = ht[i + 1];

            if (length > maxLen) {
                maxLen = length;
            }
        }
        return maxLen;
    }

    protected boolean readBlock(BitInputStream in, QuickArrayAbstract out,
            int offset, int stride,  boolean intra, int qt, int ebit)
            throws QuickArrayException {

        block.clear();

        if (in.bitsRemaining() <= ebit) {
            return true;
        }

        int nc = 0;
        int k = 0;
        int m0 = 0;
        int dc = 0;
        if (intra) {
            dc = in.readBits(8);
            if (dc == 255) {
                dc = 128;
            }
            dc = dc << 3;
            block.setShort(0, (short) (dc & 0xffff));
            k = 1;
            m0 = 1;
        } else if (in.peekNextBit() == 1) {
            int code = in.readBits(2);
            int level = (code & 1) > 0 ? 0xFF : 1;
            short realLevel = qtable.getShort(qt + level);
            block.setShort(0, realLevel);
            k = 1;
            m0 = 1;
        } else {
            k = 0;
        }
        boolean eob = false;
        while (!eob && (in.bitsRemaining() > ebit)) {
            int runLevel = in.huffDecode(runLevelHuff, runLevelHuffMaxLen);
            int run = 0;
            int level = 0;
            if (runLevel == EOB) {
                eob = true;
            } else if (runLevel == SYM_ILLEGAL) {
                if (in.bitsRemaining() > ebit) {
                    System.err.println("Illegal code "
                            + in.getLastCode() + " bits remaining = "
                            + in.bitsRemaining() + " k = " + k);
                    return false;
                }
                eob = true;
            } else if (runLevel == ESCAPE) {
                run = in.readBits(6);
                level = in.readBits(8);
            } else {
                level = (runLevel >> 6) & 0xFF;
                run = runLevel & 0x1f;
            }

            if (!eob) {
                k += run;
                if (k >= 64) {
                    System.err.println("Run overflow");
                    return false;
                }
                int pos = H261Constants.COLZAG[k++];
                short realLevel = qtable.getShort(qt + level);
                block.setShort(pos, realLevel);
                nc++;
                m0 |= 1 << pos;
            }
        }

        if (nc == 0) {
            int dcFillVal = (dc + 4) >> 3;
            for (int i = 0; i < 8; i++) {
                int outPos = offset + (i * stride);
                out.setByte(outPos + 0, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 1, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 2, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 3, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 4, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 5, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 6, (byte) (dcFillVal & 0xFF));
                out.setByte(outPos + 7, (byte) (dcFillVal & 0xFF));
            }
        } else {
            dct.rdct(block, m0, out, offset, stride);
        }

        return true;
    }

    protected int readMba(BitInputStream in) {
        return in.huffDecode(mbaHuff, mbaHuffMaxLen);
    }

    protected int readMtype(BitInputStream in) {
        return in.huffDecode(mtypeHuff, mtypeHuffMaxLen);
    }

    protected int readCpb(BitInputStream in) {
        return in.huffDecode(cbpHuff, cbpHuffMaxLen);
    }

    /**
     *
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        qtable.free();
        mtypeHuff.free();
        block.free();
        mbaHuff.free();
        runLevelHuff.free();
        dct.close();
    }
}
