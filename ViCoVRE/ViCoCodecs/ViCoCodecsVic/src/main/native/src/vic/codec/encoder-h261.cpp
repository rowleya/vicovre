/*
 * Copyright (c) 1994-1995 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the names of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

static const char rcsid[] =
    "@(#) $Header$ (LBL)";

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "encoder-h261.h"

#define HDRSIZE 4
#define	CIF_WIDTH	352
#define	CIF_HEIGHT	288
#define	QCIF_WIDTH	176
#define	QCIF_HEIGHT	144
#define	BMB		6	/* # blocks in a MB */
#define MBPERGOB	33	/* # of Macroblocks per GOB */

#if BYTE_ORDER == LITTLE_ENDIAN
#if NBIT == 64
#define STORE_BITS(bb, bc) \
    bc[0] = bb >> 56; \
    bc[1] = bb >> 48; \
    bc[2] = bb >> 40; \
    bc[3] = bb >> 32; \
    bc[4] = bb >> 24; \
    bc[5] = bb >> 16; \
    bc[6] = bb >> 8; \
    bc[7] = bb;
#define LOAD_BITS(bc) \
    ((BB_INT)bc[0] << 56 | \
     (BB_INT)bc[1] << 48 | \
     (BB_INT)bc[2] << 40 | \
     (BB_INT)bc[3] << 32 | \
     (BB_INT)bc[4] << 24 | \
     (BB_INT)bc[5] << 16 | \
     (BB_INT)bc[6] << 8 | \
     (BB_INT)bc[7])
#else
#define STORE_BITS(bb, bc) \
    bc[0] = bb >> 24; \
    bc[1] = bb >> 16; \
    bc[2] = bb >> 8; \
    bc[3] = bb;
#define LOAD_BITS(bc) (ntohl(*(BB_INT*)(bc)))
#endif
#else
#define STORE_BITS(bb, bc) *(BB_INT*)bc = (bb);
#define LOAD_BITS(bc) (*(BB_INT*)(bc))
#endif

#define PUT_BITS(bits, n, nbb, bb, bc) \
{ \
    nbb += (n); \
    if (nbb > NBIT)  { \
        u_int extra = (nbb) - NBIT; \
        bb |= (BB_INT)(bits) >> extra; \
        STORE_BITS(bb, bc) \
        bc += sizeof(BB_INT); \
        bb = (BB_INT)(bits) << (NBIT - extra); \
        nbb = extra; \
    } else \
        bb |= (BB_INT)(bits) << (NBIT - (nbb)); \
}

H261Encoder::H261Encoder(int ft) : TransmitterModule(ft),
    bs_(0), bc_(0), ngob_(12)
{
    for (int q = 0; q < 32; ++q) {
        llm_[q] = 0;
        clm_[q] = 0;
    }
}

H261Encoder::~H261Encoder()
{
    for (int q = 0; q < 32; ++q) {
        if (llm_[q] != 0)
            delete llm_[q]; //SV-XXX: Debian
        if (clm_[q] != 0)
            delete clm_[q]; //SV-XXX: Debian
    }
}

H261PixelEncoder::H261PixelEncoder() : H261Encoder(FT_YUV_CIF)
{
    quant_required_ = 0;
    setq(10);
}

H261DCTEncoder::H261DCTEncoder() : H261Encoder(FT_DCT)
{
    quant_required_ = 1;
    setq(10);
}

/*
 * Set up the forward DCT quantization table for
 * INTRA mode operation.
 */
void
H261Encoder::setquantizers(int lq, int mq, int hq)
{
    int qt[64];
    if (lq > 31)
        lq = 31;
    if (lq <= 0)
        lq = 1;
    lq_ = lq;

    if (mq > 31)
        mq = 31;
    if (mq <= 0)
        mq = 1;
    mq_ = mq;

    if (hq > 31)
        hq = 31;
    if (hq <= 0)
        hq = 1;
    hq_ = hq;

    /*
     * quant_required_ indicates quantization is not folded
     * into fdct [because fdct is not performed]
     */
    if (quant_required_ == 0) {
        /*
         * Set the DC quantizer to 1, since we want to do this
         * coefficient differently (i.e., the DC is rounded while
         * the AC terms are truncated).
         */
        qt[0] = 1;
        int i;
        for (i = 1; i < 64; ++i)
            qt[i] = lq_ << 1;
        fdct_fold_q(qt, lqt_);

        qt[0] = 1;
        for (i = 1; i < 64; ++i)
            qt[i] = mq_ << 1;
        fdct_fold_q(qt, mqt_);

        qt[0] = 1;
        for (i = 1; i < 64; ++i)
            qt[i] = hq_ << 1;
        fdct_fold_q(qt, hqt_);
    }
}

void
H261Encoder::setq(int q)
{
    setquantizers(q, q / 2, 1);
}

void
H261PixelEncoder::size(int w, int h)
{
    Module::size(w, h);
    if (w == CIF_WIDTH && h == CIF_HEIGHT) {
        /* CIF */
        cif_ = 1;
        ngob_ = 12;
        bstride_ = 11;
        lstride_ = 16 * CIF_WIDTH - CIF_WIDTH / 2;
        cstride_ = 8 * 176 - 176 / 2;
        loffsize_ = 16;
        coffsize_ = 8;
        bloffsize_ = 1;
    } else if (w == QCIF_WIDTH && h == QCIF_HEIGHT) {
        /* QCIF */
        cif_ = 0;
        ngob_ = 6; /* not really number of GOBs, just loop limit */
        bstride_ = 0;
        lstride_ = 16 * QCIF_WIDTH - QCIF_WIDTH;
        cstride_ = 8 * 88 - 88;
        loffsize_ = 16;
        coffsize_ = 8;
        bloffsize_ = 1;
    } else {
        /*XXX*/
        fprintf(stderr, "H261PixelEncoder: H.261 bad geometry: %dx%d\n",
            w, h);
        exit(1);
    }
    u_int loff = 0;
    u_int coff = 0;
    u_int blkno = 0;
    for (u_int gob = 0; gob < ngob_; gob += 2) {
        loff_[gob] = loff;
        coff_[gob] = coff;
        blkno_[gob] = blkno;
        /* width of a GOB (these aren't ref'd in QCIF case) */
        loff_[gob + 1] = loff + 11 * 16;
        coff_[gob + 1] = coff + 11 * 8;
        blkno_[gob + 1] = blkno + 11;

        /* advance to next GOB row */
        loff += (16 * 16 * MBPERGOB) << cif_;
        coff += (8 * 8 * MBPERGOB) << cif_;
        blkno += MBPERGOB << cif_;
    }
}

void
H261DCTEncoder::size(int w, int h)
{

    Module::size(w, h);
    if (w == CIF_WIDTH && h == CIF_HEIGHT) {
        /* CIF */
        cif_ = 1;
        ngob_ = 12;
        bstride_ = 11;
        lstride_ = - (11 * (64*BMB)) + 2 * 11 * 64 * BMB;
        cstride_ = - (11 * (64*BMB)) + 2 * 11 * 64 * BMB;
        loffsize_ = 64 * BMB;
        coffsize_ = 64 * BMB;
        bloffsize_ = 1;
    } else if (w == QCIF_WIDTH && h == QCIF_HEIGHT) {
        /* QCIF */
        cif_ = 0;
        ngob_ = 6; /* not really number of GOBs, just loop limit */
        bstride_ = 0;
        lstride_ = 0;
        cstride_ = 0;
        loffsize_ = 64 * BMB;
        coffsize_ = 64 * BMB;
        bloffsize_ = 1;
    } else {
        /*XXX*/
        fprintf(stderr, "H261DCTEncoder: H.261 bad geometry: %dx%d\n",
            w, h);
        exit(1);
    }

    u_int gob;
    for (gob = 0; gob < ngob_; gob += 2) {

        if (gob != 0) {
            loff_[gob] = loff_[gob-2] +
                (MBPERGOB << cif_) * BMB * 64;
            coff_[gob] = coff_[gob-2] +
                (MBPERGOB << cif_) * BMB * 64;
            blkno_[gob] = blkno_[gob-2] +
                (MBPERGOB << cif_);
        } else {
            loff_[0] = 0;
            coff_[0] = loff_[0] + 4 * 64;	// 4 Y's
            blkno_[0] = 0;
        }

        loff_[gob + 1] = loff_[gob] + 11 * BMB * 64;
        coff_[gob + 1] = coff_[gob] + 11 * BMB * 64;
        blkno_[gob + 1] = blkno_[gob] + 11;
    }
}


/*
 * Make a map to go from a 12 bit dct value to an 8 bit quantized
 * 'level' number.  The 'map' includes both the quantizer (for the
 * dct encoder) and the perceptual filter 'threshhold' (for both
 * the pixel & dct encoders).  The first 4k of the map is for the
 * unfiltered coeff (the first 20 in zigzag order; roughly the
 * upper left quadrant) and the next 4k of the map are for the
 * filtered coef.
 */
char*
H261Encoder::make_level_map(int q, u_int fthresh)
{
    /* make the luminance map */
    char* lm = new char[0x2000];
    char* flm = lm + 0x1000;
    int i;
    lm[0] = 0;
    flm[0] = 0;
    q = quant_required_? q << 1 : 0;
    for (i = 1; i < 0x800; ++i) {
        int l = i;
        if (q)
            l /= q;
        lm[i] = l;
        lm[-i & 0xfff] = -l;

        if ((u_int)l <= fthresh)
            l = 0;
        flm[i] = l;
        flm[-i & 0xfff] = -l;
    }
    return (lm);
}

/*
 * encode_blk:
 *	encode a block of DCT coef's
 */
void
H261Encoder::encode_blk(const short* blk, const char* lm)
{
    BB_INT bb = bb_;
    u_int nbb = nbb_;
    u_char* bc = bc_;

    /*
     * Quantize DC.  Round instead of truncate.
     */
    int dc = (blk[0] + 4) >> 3;

    if (dc <= 0)
        /* shouldn't happen with CCIR 601 black (level 16) */
        dc = 1;
    else if (dc > 254)
        dc = 254;
    else if (dc == 128)
        /* per Table 6/H.261 */
        dc = 255;
    /* Code DC */
    PUT_BITS(dc, 8, nbb, bb, bc);
    int run = 0;
    const u_char* colzag = &COLZAG[0];
    for (int zag; (zag = *++colzag) != 0; ) {
        if (colzag == &COLZAG[20])
            lm += 0x1000;
        int level = lm[((const u_short*)blk)[zag] & 0xfff];
        if (level != 0) {
            int val, nb;
            huffent* he;
            if (u_int(level + 15) <= 30 &&
                (nb = (he = &hte_tc[((level&0x1f) << 6)|run])->nb))
                /* we can use a VLC. */
                val = he->val;
            else {
                 /* Can't use a VLC.  Escape it. */
                val = (1 << 14) | (run << 8) | (level & 0xff);
                nb = 20;
            }
            PUT_BITS(val, nb, nbb, bb, bc);
            run = 0;
        } else
            ++run;
    }
    /* EOB */
    PUT_BITS(2, 2, nbb, bb, bc);

    bb_ = bb;
    nbb_ = nbb;
    bc_ = bc;
}

/*
 * H261PixelEncoder::encode_mb
 *	encode a macroblock given a set of input YUV pixels
 */
void
H261PixelEncoder::encode_mb(u_int mba, const u_char* frm,
                u_int loff, u_int coff, int how)
{
    register int q;
    float* qt;
    if (how == CR_MOTION) {
        q = lq_;
        qt = lqt_;
    } else if (how == CR_BG) {
        q = hq_;
        qt = hqt_;
    } else {
        /* must be at age threshold */
        q = mq_;
        qt = mqt_;
    }

    /*
     * encode all 6 blocks of the macro block to find the largest
     * coef (so we can pick a new quantizer if gquant doesn't have
     * enough range).
     */
    /*XXX this can be u_char instead of short but need smarts in fdct */
    short blk[64 * 6];
    register int stride = width_;
    /* luminance */
    const u_char* p = &frm[loff];
    fdct(p, stride, blk + 0, qt);
    fdct(p + 8, stride, blk + 64, qt);
    fdct(p + 8 * stride, stride, blk + 128, qt);
    fdct(p + (8 * stride + 8), stride, blk + 192, qt);
    /* chominance */
    int fs = framesize_;
    p = &frm[fs + coff];
    stride >>= 1;
    fdct(p, stride, blk + 256, qt);
    fdct(p + (fs >> 2), stride, blk + 320, qt);

    /*
     * if the default quantizer is too small to handle the coef.
     * dynamic range, spin through the blocks and see if any
     * coef. would significantly overflow.
     */
    if (q < 8) {
        register int cmin = 0, cmax = 0;
        register short* bp = blk;
        for (register int i = 6; --i >= 0; ) {
            ++bp;	// ignore dc coef
            for (register int j = 63; --j >= 0; ) {
                register int v = *bp++;
                if (v < cmin)
                    cmin = v;
                else if (v > cmax)
                    cmax = v;
            }
        }
        if (cmax < -cmin)
            cmax = -cmin;
        if (cmax >= 128) {
            /* need to re-quantize */
            register int s;
            for (s = 1; cmax >= (128 << s); ++s) {
            }
            q <<= s;
            register short* bp = blk;
            for (register int i = 6; --i >= 0; ) {
                ++bp;	// ignore dc coef
                for (register int j = 63; --j >= 0; ) {
                    register int v = *bp;
                    *bp++ = v >> s;
                }
            }
        }
    }

    u_int m = mba - mba_;
    mba_ = mba;
    huffent* he = &hte_mba[m - 1];
    /* MBA */
    PUT_BITS(he->val, he->nb, nbb_, bb_, bc_);
    if (q != mquant_) {
        /* MTYPE = INTRA + TC + MQUANT */
        PUT_BITS(1, 7, nbb_, bb_, bc_);
        PUT_BITS(q, 5, nbb_, bb_, bc_);
        mquant_ = q;
    } else {
        /* MTYPE = INTRA + TC (no quantizer) */
        PUT_BITS(1, 4, nbb_, bb_, bc_);
    }

    /* luminance */
    const char* lm = llm_[q];
    if (lm == 0) {
        lm = make_level_map(q, 1);
        llm_[q] = lm;
        clm_[q] = make_level_map(q, 2);
    }
    encode_blk(blk + 0, lm);
    encode_blk(blk + 64, lm);
    encode_blk(blk + 128, lm);
    encode_blk(blk + 192, lm);
    /* chominance */
    lm = clm_[q];
    encode_blk(blk + 256, lm);
    encode_blk(blk + 320, lm);
}


/*
 * H261DCTEncoder::encode_mb
 *	encode a macroblock given a set of input DCT coefs
 *	each coef is stored as a short
 */
void
H261DCTEncoder::encode_mb(u_int mba, const u_char* frm,
              u_int loff, u_int coff, int how)
{
    short *lblk = (short *)frm + loff;
    short *ublk = (short *)frm + coff;
    short *vblk = (short *)frm + coff + 64;

    register u_int q;
    if (how == CR_MOTION)
        q = lq_;
    else if (how == CR_BG)
        q = hq_;
    else
        /* must be at age threshold */
        q = mq_;

    /*
     * if the default quantizer is too small to handle the coef.
     * dynamic range, spin through the blocks and see if any
     * coef. would significantly overflow.
     */
    if (q < 8) {
        register int cmin = 0, cmax = 0;
        register short* bp = lblk;
        register int i, j;

        // Y U and V blocks
        for (i = 6; --i >= 0; ) {
            ++bp;	// ignore dc coef
            for (j = 63; --j >= 0; ) {
                register int v = *bp++;
                if (v < cmin)
                    cmin = v;
                else if (v > cmax)
                    cmax = v;
            }
        }

        if (cmax < -cmin)
            cmax = -cmin;
        cmax /= (q << 1);
        if (cmax >= 128) {
            /* need to re-quantize */
            register int s;

            for (s = 1; cmax >= (128 << s); ++s) {
            }
            q <<= s;

        }
    }

    u_int m = mba - mba_;
    mba_ = mba;
    huffent* he = &hte_mba[m - 1];
    /* MBA */
    PUT_BITS(he->val, he->nb, nbb_, bb_, bc_);
    if (q != mquant_) {
        /* MTYPE = INTRA + TC + MQUANT */
        PUT_BITS(1, 7, nbb_, bb_, bc_);
        PUT_BITS(q, 5, nbb_, bb_, bc_);
        mquant_ = q;
    } else {
        /* MTYPE = INTRA + TC (no quantizer) */
        PUT_BITS(1, 4, nbb_, bb_, bc_);
    }

    /* luminance */
    const char* lm = llm_[q];
    if (lm == 0) {
        /*
         * the filter thresh is 0 since we assume the jpeg percept.
         * quantizer already did the filtering.
         */
        lm = make_level_map(q, 0);
        llm_[q] = lm;
        clm_[q] = make_level_map(q, 0);
    }
    encode_blk(lblk + 0, lm);
    encode_blk(lblk + 64, lm);
    encode_blk(lblk + 128, lm);
    encode_blk(lblk + 192, lm);
    /* chominance */
    lm = clm_[q];
    encode_blk(ublk, lm);
    encode_blk(vblk, lm);
}

int
H261Encoder::flush(pktbuf* pb, int nbit, pktbuf* npb)
{
    /* flush bit buffer */
    STORE_BITS(bb_, bc_);

    int cc = (nbit + 7) >> 3;
    int ebit = (cc << 3) - nbit;

    /*XXX*/
    if (cc == 0 && npb != 0)
        abort();

    pb->len = cc + HDRSIZE;
    rtphdr* rh = (rtphdr*)pb->rtp_header;
    if (npb == 0)
        rh->rh_flags |= htons(RTP_M);

    int h = *(u_int*)(rh + 1) | ebit << 26 | sbit_ << 29;
    *(u_int*)(rh + 1) = htonl(h);

    if (npb != 0) {
        u_char* nbs = &npb->dp[HDRSIZE];
        u_int bc = (bc_ - bs_) << 3;
        int tbit = bc + nbb_;
        int extra = ((tbit + 7) >> 3) - (nbit >> 3);
        if (extra > 0)
            memcpy(nbs, bs_ + (nbit >> 3), extra);
        bs_ = nbs;
        sbit_ = nbit & 7;
        tbit -= nbit &~ 7;
        bc = tbit &~ (NBIT - 1);
        nbb_ = tbit - bc;
        bc_ = bs_ + (bc >> 3);
        /*
         * Prime the bit buffer.  Be careful to set bits that
         * are not yet in use to 0, since output bits are later
         * or'd into the buffer.
         */
        if (nbb_ > 0) {
            u_int n = NBIT - nbb_;
            bb_ = (LOAD_BITS(bc_) >> n) << n;
        } else
            bb_ = 0;
    }
    send(pb);

    return (cc + HDRSIZE);
}

int H261DCTEncoder::consume(const VideoFrame *vf)
{
    if (!samesize(vf))
        size(vf->width_, vf->height_);

    DCTFrame* df = (DCTFrame *)vf;

    return(encode(df, df->crvec_));
}

int H261PixelEncoder::consume(const VideoFrame *vf)
{
    if (!samesize(vf))
        size(vf->width_, vf->height_);

    YuvFrame* p = (YuvFrame*)vf;
    return(encode(p, p->crvec_));
}


int
H261Encoder::encode(const VideoFrame* vf, const u_int8_t *crvec)
{
    pktbuf* pb = pool_->alloc(vf->ts_, RTP_PT_H261);
    bs_ = &pb->dp[HDRSIZE];
    bc_ = bs_;
    u_int ec = (mtu_ - HDRSIZE) << 3;
    bb_ = 0;
    nbb_ = 0;
    sbit_ = 0;
    /* RTP/H.261 header */
    *(u_int*)(pb->dp) = 1 << 25 | lq_ << 10;

    /* PSC */
    PUT_BITS(0x0001, 16, nbb_, bb_, bc_);
    /* GOB 0 -> picture header */
    PUT_BITS(0, 4, nbb_, bb_, bc_);
    /* TR (XXX should do this right) */
    PUT_BITS(0, 5, nbb_, bb_, bc_);
    /* PTYPE = CIF */
    int pt = cif_ ? 4 : 0;
    PUT_BITS(pt, 6, nbb_, bb_, bc_);
    /* PEI */
    PUT_BITS(0, 1, nbb_, bb_, bc_);

    int step = cif_ ? 1 : 2;
    int cc = 0;

    u_int8_t* frm = vf->bp_;
    for (u_int gob = 0; gob < ngob_; gob += step) {
        u_int loff = loff_[gob];
        u_int coff = coff_[gob];
        u_int blkno = blkno_[gob];
        u_int nbit = ((bc_ - bs_) << 3) + nbb_;

        /* GSC/GN */
        PUT_BITS(0x10 | (gob + 1), 20, nbb_, bb_, bc_);
        /* GQUANT/GEI */
        mquant_ = lq_;
        PUT_BITS(mquant_ << 1, 6, nbb_, bb_, bc_);

        mba_ = 0;
        int line = 11;
        for (u_int mba = 1; mba <= 33; ++mba) {
            /*
             * If the conditional replenishment algorithm
             * has decided to send any of the blocks of
             * this macroblock, code it.
             */
            u_int s = crvec[blkno];

            if ((s & CR_SEND) != 0) {
                u_int mbpred = mba_;
                encode_mb(mba, frm, loff, coff, CR_STATE(s));
                u_int cbits = ((bc_ - bs_) << 3) + nbb_;
                if (cbits > ec) {
                    pktbuf* npb;
                    npb = pool_->alloc(vf->ts_, RTP_PT_H261);
                    cc += flush(pb, nbit, npb);
                    cbits -= nbit;
                    pb = npb;
                    /* RTP/H.261 header */
                    u_int m = mbpred;
                    u_int g;
                    if (m != 0) {
                        g = gob + 1;
                        m -= 1;
                    } else
                        g = 0;

                    *(u_int*)(pb->dp) =
                        1 << 25 |
                        m << 15 |
                        g << 20 |
                        mquant_ << 10;
                }
                nbit = cbits;
            }

            loff += loffsize_;
            coff += coffsize_;
            blkno += bloffsize_;
            if (--line <= 0) {
                line = 11;
                blkno += bstride_;
                loff += lstride_;
                coff += cstride_;
            }

        }
    }
    cc += flush(pb, ((bc_ - bs_) << 3) + nbb_, 0);
    return (cc);
}
