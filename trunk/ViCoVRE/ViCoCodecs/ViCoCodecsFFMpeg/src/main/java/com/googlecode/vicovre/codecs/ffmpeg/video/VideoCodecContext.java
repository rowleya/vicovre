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

package com.googlecode.vicovre.codecs.ffmpeg.video;

public class VideoCodecContext {


    /**
     * Automatic DCT Algorithm.
     */
    public static final int FF_DCT_AUTO = 0;

    /**
     * DCT Algorithm.
     */
    public static final int FF_DCT_FASTINT = 1;

    /**
     * DCT Algorithm.
     */
    public static final int FF_DCT_INT = 2;

    /**
     * DCT Algorithm.
     */
    public static final int FF_DCT_MMX = 3;

    /**
     * DCT Algorithm.
     */
    public static final int FF_DCT_MLIB = 4;

    /**
     * DCT Algorithm.
     */
    public static final int FF_DCT_ALTIVEC = 5;

    /**
     * DCT Algorithm.
     */
    public static final int FF_DCT_FAAN = 6;

    private int flags = 0;

    private int flags2 = 0;

    private int qmin = 0;

    private int qmax = 0;

    private int maxQdiff = 0;

    private int lowres = 0;

    private int dctAlgo = 0;

    private int debug = 0;

    private int bitrate = 0;

    private int maxrate = 0;

    private int inputWidth = 0;

    private int inputHeight = 0;

    private int outputWidth = 0;

    private int outputHeight = 0;

    private int pixelFmt = 0;

    private int outputDataSize = 0;

    /**
     * Returns the flags.
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Sets the flags.
     * @param flags the flags to set
     */
    public void setFlags(final int flags) {
        this.flags = flags;
    }


    /**
     * Returns the flags2.
     * @return the flags2
     */
    public int getFlags2() {
        return flags2;
    }

    /**
     * Sets the flags2.
     * @param flags2 the flags to set
     */
    public void setFlags2(final int flags2) {
        this.flags2 = flags2;
    }

    /**
     * Returns the qmin.
     * @return the qmin
     */
    public int getQmin() {
        return qmin;
    }

    /**
     * Sets the qmin.
     * @param qmin the qmin to set
     */
    public void setQmin(final int qmin) {
        this.qmin = qmin;
    }

    /**
     * Returns the qmax.
     * @return the qmax
     */
    public int getQmax() {
        return qmax;
    }

    /**
     * Sets the qmax.
     * @param qmax the qmax to set
     */
    public void setQmax(final int qmax) {
        this.qmax = qmax;
    }

    /**
     * Returns the maxQdiff.
     * @return the maxQdiff
     */
    public int getMaxQdiff() {
        return maxQdiff;
    }

    /**
     * Sets the maxQdiff.
     * @param maxQdiff the maxQdiff to set
     */
    public void setMaxQdiff(final int maxQdiff) {
        this.maxQdiff = maxQdiff;
    }

    /**
     * Returns the lowres.
     * @return the lowres
     */
    public int getLowres() {
        return lowres;
    }

    /**
     * Sets the lowres.
     * @param lowres the lowres to set
     */
    public void setLowres(final int lowres) {
        this.lowres = lowres;
    }

    /**
     * Returns the dctAlgo.
     * @return the dctAlgo
     */
    public int getDctAlgo() {
        return dctAlgo;
    }

    /**
     * Sets the dctAlgo.
     * @param dctAlgo the dctAlgo to set
     */
    public void setDctAlgo(final int dctAlgo) {
        this.dctAlgo = dctAlgo;
    }

    /**
     * Gets the debug.
     * @return the debug
     */
    public int getDebug() {
        return debug;
    }

    /**
     * Sets the debug.
     * @param debug The debug to set
     */
    public void setDebug(final int debug) {
        this.debug = debug;
    }

    /**
     * Sets the bitrate.
     * @return bitrate The bitrate
     */
    public int getBitrate() {
        return bitrate;
    }

    /**
     * Gets the bitrate.
     * @param bitrate The bitrate to set
     */
    public void setBitrate(final int bitrate) {
        this.bitrate = bitrate;
    }

    /**
     * Returns the maxrate
     * @return the maxrate
     */
    public int getMaxrate() {
        return maxrate;
    }

    /**
     * Sets the maxrate
     * @param maxrate the maxrate to set
     */
    public void setMaxrate(int maxrate) {
        this.maxrate = maxrate;
    }

    /**
     * Returns the inputWidth
     * @return the inputWidth
     */
    public int getInputWidth() {
        return inputWidth;
    }

    /**
     * Sets the inputWidth
     * @param inputWidth the inputWidth to set
     */
    public void setInputWidth(int inputWidth) {
        this.inputWidth = inputWidth;
    }

    /**
     * Returns the inputHeight
     * @return the inputHeight
     */
    public int getInputHeight() {
        return inputHeight;
    }

    /**
     * Sets the inputHeight
     * @param inputHeight the inputHeight to set
     */
    public void setInputHeight(int inputHeight) {
        this.inputHeight = inputHeight;
    }

    /**
     * Returns the outputWidth
     * @return the outputWidth
     */
    public int getOutputWidth() {
        return outputWidth;
    }

    /**
     * Sets the outputWidth
     * @param outputWidth the outputWidth to set
     */
    public void setOutputWidth(int outputWidth) {
        this.outputWidth = outputWidth;
    }

    /**
     * Returns the outputHeight
     * @return the outputHeight
     */
    public int getOutputHeight() {
        return outputHeight;
    }

    /**
     * Sets the outputHeight
     * @param outputHeight the outputHeight to set
     */
    public void setOutputHeight(int outputHeight) {
        this.outputHeight = outputHeight;
    }

    /**
     * Returns the pixelFmt
     * @return the pixelFmt
     */
    public int getPixelFmt() {
        return pixelFmt;
    }

    /**
     * Sets the pixelFmt
     * @param pixelFmt the pixelFmt to set
     */
    public void setPixelFmt(int pixelFmt) {
        this.pixelFmt = pixelFmt;
    }

    /**
     * Returns the outputDataSize
     * @return the outputDataSize
     */
    public int getOutputDataSize() {
        return outputDataSize;
    }

    /**
     * Sets the outputDataSize
     * @param outputDataSize the outputDataSize to set
     */
    public void setOutputDataSize(int outputDataSize) {
        this.outputDataSize = outputDataSize;
    }
}
