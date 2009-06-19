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
 * Decides which blocks should be replenished
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ConditionalReplenishment {

    /**
     * Indicates that there is motion in the block
     */
    public static final int CR_MOTION = 0;

    /**
     * Indicates that the block was updated as background
     */
    public static final int CR_BG = 0x41;

    private static final int CR_STATE_MASK = 0x7f;

    private static final int CR_AGETHRESH = 31;

    private static final int CR_IDLE = 0x40;

    private static final int CR_SEND = 0x80;

    private static final int DIV_16_SHIFT = 4;

    private static final int TIMES_16_SHIFT = 4;

    private static final int TIMES_8_SHIFT = 3;

    private static final int BYTE_MASK = 0xFF;

    private static final int BLOCK_SIZE = 16;

    private static final int INIT_THRESHOLD = 48;

    private static final int SCAN_LINE_SKIP = 3;

    private static final int SCAN_MASK = 7;

    private static int crState(int s) {
        return ((s) & CR_STATE_MASK);
    }

    private int[] crvec = null;

    private QuickArray refbuf = null;

    private int scan = 0;

    private int rover = 0;

    private int blkw = 0;

    private int blkh = 0;

    private int nblk = 0;

    private int threshold = INIT_THRESHOLD;

    private int width = 0;

    /**
     * Creates a new ConditionalReplenishment
     *
     * @param width
     *            The width to replenish
     * @param height
     *            The height to replenish
     * @throws QuickArrayException
     */
    public ConditionalReplenishment(int width, int height)
            throws QuickArrayException {

        blkw = width >> DIV_16_SHIFT;
        blkh = height >> DIV_16_SHIFT;
        nblk = blkw * blkh;
        crvec = new int[nblk];
        refbuf = new QuickArray(byte[].class, width * height);
        reset();
        this.width = width;
    }

    /**
     * Resets the replenishment so all blocks appear new
     */
    public void reset() {
        for (int i = 0; i < nblk; i++) {
            crvec[i] = CR_MOTION | CR_SEND;
        }
        refbuf.clear();
    }

    /**
     * Updates the replenishment
     *
     * @param devbuf
     *            The buffer to update with
     * @throws QuickArrayException
     */
    public void replenish(byte[] devbuf) throws QuickArrayException {

        /*
         * First age the blocks from the previous frame.
         */
        ageBlocks();
        QuickArrayWrapper in = new QuickArrayWrapper(devbuf);

        int ds = width;
        int rs = width;
        int db = scan * ds;
        int rb = scan * rs;
        int w = blkw;
        int crv = 0;

        for (int y = 0; y < blkh; ++y) {
            int ndb = db;
            int nrb = rb;
            int ncrv = crv;
            for (int x = 0; x < blkw; x++) {
                int left = 0;
                int right = 0;
                int top = 0;
                int bottom = 0;
                left += (in.getByte(db + 0) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0) & BYTE_MASK);
                left += (in.getByte(db + 1) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1) & BYTE_MASK);
                left += (in.getByte(db + 2) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2) & BYTE_MASK);
                left += (in.getByte(db + 3) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3) & BYTE_MASK);
                top += (in.getByte(db + 0 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0 + 1 * 4) & BYTE_MASK);
                top += (in.getByte(db + 1 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1 + 1 * 4) & BYTE_MASK);
                top += (in.getByte(db + 2 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2 + 1 * 4) & BYTE_MASK);
                top += (in.getByte(db + 3 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3 + 1 * 4) & BYTE_MASK);
                top += (in.getByte(db + 0 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0 + 2 * 4) & BYTE_MASK);
                top += (in.getByte(db + 1 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1 + 2 * 4) & BYTE_MASK);
                top += (in.getByte(db + 2 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2 + 2 * 4) & BYTE_MASK);
                top += (in.getByte(db + 3 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3 + 2 * 4) & BYTE_MASK);
                right += (in.getByte(db + 0 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0 + 3 * 4) & BYTE_MASK);
                right += (in.getByte(db + 1 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1 + 3 * 4) & BYTE_MASK);
                right += (in.getByte(db + 2 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2 + 3 * 4) & BYTE_MASK);
                right += (in.getByte(db + 3 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3 + 3 * 4) & BYTE_MASK);
                right = Math.abs(right);
                left = Math.abs(left);
                top = Math.abs(top);
                db += ds << TIMES_8_SHIFT;
                rb += rs << TIMES_8_SHIFT;
                left += (in.getByte(db + 0) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0) & BYTE_MASK);
                left += (in.getByte(db + 1) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1) & BYTE_MASK);
                left += (in.getByte(db + 2) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2) & BYTE_MASK);
                left += (in.getByte(db + 3) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3) & BYTE_MASK);
                bottom += (in.getByte(db + 0 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0 + 1 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 1 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1 + 1 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 2 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2 + 1 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 3 + 1 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3 + 1 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 0 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0 + 2 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 1 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1 + 2 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 2 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2 + 2 * 4) & BYTE_MASK);
                bottom += (in.getByte(db + 3 + 2 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3 + 2 * 4) & BYTE_MASK);
                right += (in.getByte(db + 0 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 0 + 3 * 4) & BYTE_MASK);
                right += (in.getByte(db + 1 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 1 + 3 * 4) & BYTE_MASK);
                right += (in.getByte(db + 2 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 2 + 3 * 4) & BYTE_MASK);
                right += (in.getByte(db + 3 + 3 * 4) & BYTE_MASK)
                        - (refbuf.getByte(rb + 3 + 3 * 4) & BYTE_MASK);
                right = Math.abs(right);
                left = Math.abs(left);
                bottom = Math.abs(bottom);
                db -= ds << TIMES_8_SHIFT;
                rb -= rs << TIMES_8_SHIFT;

                int center = 0;
                if (left >= threshold && x > 0) {
                    crvec[crv - 1] = CR_MOTION | CR_SEND;
                    center = 1;
                }
                if (right >= threshold && x < w - 1) {
                    crvec[crv + 1] = CR_MOTION | CR_SEND;
                    center = 1;
                }
                if (bottom >= threshold && y < blkh - 1) {
                    crvec[crv + w] = CR_MOTION | CR_SEND;
                    center = 1;
                }
                if (top >= threshold && y > 0) {
                    crvec[crv - w] = CR_MOTION | CR_SEND;
                    center = 1;
                }
                if (center > 0) {
                    crvec[crv + 0] = CR_MOTION | CR_SEND;
                }

                db += BLOCK_SIZE;
                rb += BLOCK_SIZE;
                ++crv;
            }
            db = ndb + (ds << TIMES_16_SHIFT);
            rb = nrb + (rs << TIMES_16_SHIFT);
            crv = ncrv + w;
        }
        saveblks(in);

        /*
         * Bump the CR scan pointer. This variable controls which scan line of a
         * block we use to make the replenishment decision. We skip 3 lines at a
         * time to quickly precess over the block. Since 3 and 8 are coprime, we
         * will sweep out every line.
         */
        scan = (scan + SCAN_LINE_SKIP) & SCAN_MASK;
    }

    private void ageBlocks() {
        for (int i = 0; i < nblk; ++i) {
            int s = crState(crvec[i]);
            /*
             * Age this block. Once we hit the age threshold, we set CR_SEND as
             * a hint to send a higher-quality version of the block. After this
             * the block will stop aging, until there is motion. In the
             * meantime, we might send it as background fill using the highest
             * quality.
             */
            if (s <= CR_AGETHRESH) {
                if (s == CR_AGETHRESH) {
                    s = CR_IDLE;
                } else {
                    if (++s == CR_AGETHRESH) {
                        s |= CR_SEND;
                    }
                }
                crvec[i] = s;
            } else if (s == CR_BG) {
                /*
                 * reset the block to IDLE if it was sent as a BG block in the
                 * last frame.
                 */
                crvec[i] = CR_IDLE;
            }
        }
        /*
         * Now go through and look for some idle blocks to send as background
         * fill.
         */
        int blkno = rover;
        int n = 2;
        while (n > 0) {
            int s = crState(crvec[blkno]);
            if (s == CR_IDLE) {
                crvec[blkno] = CR_SEND | CR_BG;
                --n;
            }
            if (++blkno >= nblk) {
                blkno = 0;
                /* one way to guarantee loop termination */
                break;
            }
        }
        rover = blkno;
    }

    private void save(QuickArrayAbstract lum, int pos, int stride) {
        for (int i = BLOCK_SIZE; --i >= 0;) {
            refbuf.copy(lum, pos, pos, BLOCK_SIZE);
            pos += stride;
        }
    }

    /*
     * Default save routine -- stuff new luma blocks into cache.
     */
    private void saveblks(QuickArrayAbstract lum) {
        int crv = 0;
        int pos = 0;
        int stride = width;
        stride = (stride << TIMES_16_SHIFT) - stride;
        for (int y = 0; y < blkh; y++) {
            for (int x = 0; x < blkw; x++) {
                if ((crvec[crv++] & CR_SEND) != 0) {
                    save(lum, pos, width);
                }
                pos += BLOCK_SIZE;
            }
            pos += stride;
        }
    }

    /**
     * Gets the conditional replenishment for a block
     *
     * @param block
     *            The block
     * @return The state
     */
    public int getCrState(int block) {
        return crState(crvec[block]);
    }

    /**
     * Determines if a block should be sent
     *
     * @param block
     *            The block to decide
     * @return True if the block should be sent
     */
    public boolean send(int block) {
        return (crvec[block] & CR_SEND) > 0;
    }

    /**
     * Frees any data structures in use
     */
    public void close() {
        refbuf.free();
    }
}
