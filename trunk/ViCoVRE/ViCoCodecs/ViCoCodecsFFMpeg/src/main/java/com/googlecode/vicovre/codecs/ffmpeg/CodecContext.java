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

package com.googlecode.vicovre.codecs.ffmpeg;

/**
 * Allows setting of CodecContext values.
 * @author Andrew G D Rowley
 * @version 1.0
 */
public final class CodecContext {

    public static final int FF_DEBUG_PICT_INFO   = 1;
    public static final int FF_DEBUG_RC          = 2;
    public static final int FF_DEBUG_BITSTREAM   = 4;
    public static final int FF_DEBUG_MB_TYPE     = 8;
    public static final int FF_DEBUG_QP          = 16;
    public static final int FF_DEBUG_MV          = 32;
    public static final int FF_DEBUG_DCT_COEFF   = 0x00000040;
    public static final int FF_DEBUG_SKIP        = 0x00000080;
    public static final int FF_DEBUG_STARTCODE   = 0x00000100;
    public static final int FF_DEBUG_PTS         = 0x00000200;
    public static final int FF_DEBUG_ER          = 0x00000400;
    public static final int FF_DEBUG_MMCO        = 0x00000800;
    public static final int FF_DEBUG_BUGS        = 0x00001000;
    public static final int FF_DEBUG_VIS_QP      = 0x00002000;
    public static final int FF_DEBUG_VIS_MB_TYPE = 0x00004000;
    public static final int FF_DEBUG_BUFFERS     = 0x00008000;

    public static final int CODEC_FLAG_QSCALE = 0x0002;   ///< Use fixed qscale.
    public static final int CODEC_FLAG_4MV    = 0x0004;   ///< 4 MV per MB allowed / advanced prediction for H.263.
    public static final int CODEC_FLAG_QPEL   = 0x0010;   ///< Use qpel MC.
    public static final int CODEC_FLAG_GMC    = 0x0020;   ///< Use GMC.
    public static final int CODEC_FLAG_MV0    = 0x0040;   ///< Always try a MB with MV=<0,0>.
    public static final int CODEC_FLAG_PART   = 0x0080;   ///< Use data partitioning.

    /**
     * The parent program guarantees that the input for B-frames containing
     * streams is not written to for at least s->max_b_frames+1 frames, if
     * this is not set the input will be copied.
     */
    public static final int CODEC_FLAG_INPUT_PRESERVED = 0x0100;
    public static final int CODEC_FLAG_PASS1           = 0x0200;    ///< Use internal 2pass ratecontrol in first pass mode.
    public static final int CODEC_FLAG_PASS2           = 0x0400;    ///< Use internal 2pass ratecontrol in second pass mode.
    public static final int CODEC_FLAG_EXTERN_HUFF     = 0x1000;    ///< Use external Huffman table (for MJPEG).
    public static final int CODEC_FLAG_GRAY            = 0x2000;    ///< Only decode/encode grayscale.
    public static final int CODEC_FLAG_EMU_EDGE        = 0x4000;    ///< Don't draw edges.
    public static final int CODEC_FLAG_PSNR            = 0x8000;    ///< error[?] variables will be set during encoding.
    public static final int CODEC_FLAG_TRUNCATED       = 0x00010000;  /** Input bitstream might be truncated at a random
                                                      location instead of only at frame boundaries. */
    public static final int CODEC_FLAG_NORMALIZE_AQP  = 0x00020000;  ///< Normalize adaptive quantization.
    public static final int CODEC_FLAG_INTERLACED_DCT = 0x00040000;  ///< Use interlaced DCT.
    public static final int CODEC_FLAG_LOW_DELAY      = 0x00080000;  ///< Force low delay.
    public static final int CODEC_FLAG_ALT_SCAN       = 0x00100000;  ///< Use alternate scan.
    public static final int CODEC_FLAG_TRELLIS_QUANT  = 0x00200000;  ///< Use trellis quantization.
    public static final int CODEC_FLAG_GLOBAL_HEADER  = 0x00400000;  ///< Place global headers in extradata instead of every keyframe.
    public static final int CODEC_FLAG_BITEXACT       = 0x00800000;  ///< Use only bitexact stuff (except (I)DCT).

    public static final int CODEC_FLAG_H263P_AIC      = 0x01000000;  ///< H.263 advanced intra coding / MPEG-4 AC prediction (remove this)
    public static final int CODEC_FLAG_AC_PRED        = 0x01000000;  ///< H.263 advanced intra coding / MPEG-4 AC prediction
    public static final int CODEC_FLAG_H263P_UMV      = 0x02000000;  ///< unlimited motion vector
    public static final int CODEC_FLAG_CBP_RD         = 0x04000000;  ///< Use rate distortion optimization for cbp.
    public static final int CODEC_FLAG_QP_RD          = 0x08000000;  ///< Use rate distortion optimization for qp selectioon.
    public static final int CODEC_FLAG_H263P_AIV      = 0x00000008;  ///< H.263 alternative inter VLC
    public static final int CODEC_FLAG_OBMC           = 0x00000001;  ///< OBMC
    public static final int CODEC_FLAG_LOOP_FILTER    = 0x00000800;  ///< loop filter
    public static final int CODEC_FLAG_H263P_SLICE_STRUCT = 0x10000000;
    public static final int CODEC_FLAG_INTERLACED_ME  = 0x20000000;  ///< interlaced motion estimation
    public static final int CODEC_FLAG_SVCD_SCAN_OFFSET = 0x40000000;  ///< Will reserve space for SVCD scan offset user data.
    public static final int CODEC_FLAG_CLOSED_GOP     = 0x80000000;
    public static final int CODEC_FLAG2_FAST          = 0x00000001;  ///< Allow non spec compliant speedup tricks.
    public static final int CODEC_FLAG2_STRICT_GOP    = 0x00000002;  ///< Strictly enforce GOP size.
    public static final int CODEC_FLAG2_NO_OUTPUT     = 0x00000004;  ///< Skip bitstream encoding.
    public static final int CODEC_FLAG2_LOCAL_HEADER  = 0x00000008;  ///< Place global headers at every keyframe instead of in extradata.
    public static final int CODEC_FLAG2_BPYRAMID      = 0x00000010;  ///< H.264 allow B-frames to be used as references.
    public static final int CODEC_FLAG2_WPRED         = 0x00000020;  ///< H.264 weighted biprediction for B-frames
    public static final int CODEC_FLAG2_MIXED_REFS    = 0x00000040;  ///< H.264 one reference per partition, as opposed to one reference per macroblock
    public static final int CODEC_FLAG2_8X8DCT        = 0x00000080;  ///< H.264 high profile 8x8 transform
    public static final int CODEC_FLAG2_FASTPSKIP     = 0x00000100;  ///< H.264 fast pskip
    public static final int CODEC_FLAG2_AUD           = 0x00000200;  ///< H.264 access unit delimiters
    public static final int CODEC_FLAG2_BRDO          = 0x00000400;  ///< B-frame rate-distortion optimization
    public static final int CODEC_FLAG2_INTRA_VLC     = 0x00000800;  ///< Use MPEG-2 intra VLC table.
    public static final int CODEC_FLAG2_MEMC_ONLY     = 0x00001000;  ///< Only do ME/MC (I frames -> ref, P frame -> ME+MC).
    public static final int CODEC_FLAG2_DROP_FRAME_TIMECODE = 0x00002000;  ///< timecode is in drop frame format.
    public static final int CODEC_FLAG2_SKIP_RD       = 0x00004000;  ///< RD optimal MB level residual skipping
    public static final int CODEC_FLAG2_CHUNKS        = 0x00008000;  ///< Input bitstream might be truncated at a packet boundaries instead of only at frame boundaries.
    public static final int CODEC_FLAG2_NON_LINEAR_QUANT = 0x00010000;  ///< Use MPEG-2 nonlinear quantizer.

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

}
