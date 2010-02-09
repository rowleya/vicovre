/*
 * Copyright (c) 1993-1994 The Regents of the University of California.
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
#include "decoder-jpeg.h"

/*
 * Initial size of each reassembly buffer.
 */
#define JPEG_BUFSIZE (200*1024)

#define STAT_BADOFF 0
#define STAT_HUGEFRM 1

JpegReassembler::JpegReassembler()
    : decimate_(0), ndec_(0), hugefrm_(0), badoff_(0)
{
    rbsize_ = JPEG_BUFSIZE;
    rb0_.bp = new u_char[2 * JPEG_BUFSIZE];
    rb0_.ts = ~0;
    rb0_.drop = 0;
    rb1_.bp = &rb0_.bp[JPEG_BUFSIZE];
    rb1_.ts = ~0;
    rb1_.drop = 0;
    memset(slots_, 0, sizeof(slots_));
}

JpegReassembler::~JpegReassembler()
{
    delete[] rb0_.bp;
}

/*
 * Reassemble an RTP/JPEG stream.  Return a pointer to a buffer
 * each time we encounter an entire frame.  Otherwise, return 0.
 * Set len to the length of the jpeg data in the buffer.
 */
u_char* JpegReassembler::reassemble(const rtphdr* rh, const jpeghdr *p, const u_char* bp, int& len)
{
    int off = (int)ntohl(p->off);
    int cc = len;
    if (off < 0) {
        ++badoff_;
        return (0);
    }
    if (off + cc > rbsize_) {
        fprintf(stderr, "Growing buffer\n");
        fflush(stderr);
        /*
         * Check for outrageous frame size.
         */
        if (off + cc > 250*1024) {
            ++hugefrm_;
            return (0);
        }
        /*
         * Grow reassembly buffers.
         */
        int nsize = rbsize_;
        do {
            nsize <<= 1;
        } while (off + cc > nsize);
        u_char* p = new u_char[2 * nsize];
        memcpy(p, rb0_.bp, rbsize_);
        memcpy(p + nsize, rb1_.bp, rbsize_);
        delete[] rb0_.bp;
        rb0_.bp = p;
        rb1_.bp = p + nsize;
        rbsize_ = nsize;
    }
    /*
     * Initialize the slot data structure.
     */
    int seqno = ntohs(rh->rh_seqno);
    int s = seqno & JPEG_SLOTMASK;
    u_int32_t ts = ntohl(rh->rh_ts);
    slots_[s].seqno = seqno;
    slots_[s].off = off;
    slots_[s].ts = ts;
    /*
     * Figure out which reassembly-buffer to use.  If we're not
     * already reassembling this frame, take over the older buffer.
     */
    rbuf* rb;
    if (ts == rb0_.ts)
        rb = &rb0_;
    else if (ts == rb1_.ts)
        rb = &rb1_;
    else {
        rb = ((int)(rb0_.ts - rb1_.ts) < 0) ? &rb0_ : &rb1_;
        rb->ts = ts;
        rb->drop = 0;
        /*
         * If we're decimating frames (to save cycles),
         * remember that we might want to drop the rest
         * of the packets from this frame.
         */
        if (decimate_) {
            if (--ndec_ <= 0)
                ndec_ = decimate_;
            else
                rb->drop = 1;
        }
    }
    if (rb->drop) {
        return (0);
    }

    memcpy((char*)&rb->bp[off], (char*)bp, cc);

    /*
     * Check if we're at end-of-frame.  If not, see if we're
     * filling a hole.  If not, return.  Otherwise, drop out
     * below and check for an entire frame.  We set cc to be
     * the entire frame size in the if-else below.
     */
    if ((ntohs(rh->rh_flags) & RTP_M) != 0) {
        slots_[s].eof = cc;
        cc += off;
    } else {
        slots_[s].eof = 0;
        int ns = s;
        do {
            ns = (ns + 1) & JPEG_SLOTMASK;
            if (slots_[ns].ts != ts || ns == s) {
                return (0);
            }
        } while (slots_[ns].eof != 0);
        cc = int(slots_[ns].eof + slots_[ns].off);
    }
    /*
     * At this point, we know we have an end-of-frame, and
     * all packets from slot 's' up until the end-of-frame.
     * Scan backward from slot 's' making sure we have all
     * packets from the start-of-frame (off == 0) to 's'.
     */
    int ps = s;
    do {
        ps = (ps - 1) & JPEG_SLOTMASK;
        if (slots_[ps].ts != ts || ps == s) {
            return (0);
        }
    } while (slots_[ps].off != 0);

    len = cc;
    return (rb->bp);
}

MotionJpegDecoder::MotionJpegDecoder()
    : Decoder(sizeof(jpeghdr)), codec_(0)
{
    JpegDecoder::defaults(config_);

    inw_ = 0;
    inh_ = 0;
    inq_ = 0;
    /* guess type 0 */
    type_ = 0;
    decimation_ = 422;
}

MotionJpegDecoder::~MotionJpegDecoder()
{
    delete codec_;
}

void MotionJpegDecoder::configure()
{
    config_.comp[0].hsf = 2;
    int old_decimation = decimation_;
    if (type_ == 1) {
        decimation_ = 420;
        config_.comp[0].vsf = 2;
    } else {
        decimation_ = 422;
        config_.comp[0].vsf = 1;
    }
    config_.comp[1].hsf = 1;
    config_.comp[1].vsf = 1;
    config_.comp[2].hsf = 1;
    config_.comp[2].vsf = 1;
    config_.width = inw_;
    config_.height = inh_;
    JpegDecoder::quantizer(config_, inq_);

    delete codec_;
    codec_ = JpegPixelDecoder::create(config_, inw_, inh_);
    int q = JpegDecoder::q_to_thresh(inq_);
    codec_->thresh(q);
    codec_->cthresh(6);
}

void MotionJpegDecoder::recvHeader(pktbuf* pb)
{
    const jpeghdr* p = (const jpeghdr*)(pb->dp);
    int needConfig = 0;
    if (p->q != inq_ || p->type != type_) {
        type_ = p->type;
        inq_ = p->q;
        needConfig = 1;
    }
    int inw = p->width << 3;
    int inh = p->height << 3;
    if (inw_ !=  inw || inh_ != inh) {
        resize(inw, inh);
        needConfig = 1;
    }
    if (needConfig)
        configure();
}

void MotionJpegDecoder::recv(pktbuf* pb)
{
    rtphdr* rh = (rtphdr*)pb->rtp_header;
    const jpeghdr* p = (const jpeghdr*)(pb->dp);
    int needConfig = 0;
    if (p->q != inq_ || p->type != type_) {
        type_ = p->type;
        inq_ = p->q;
        needConfig = 1;
    }
    int inw = p->width << 3;
    int inh = p->height << 3;
    if (inw_ !=  inw || inh_ != inh) {
        resize(inw, inh);
        needConfig = 1;
    }
    if (needConfig)
        configure();

    u_int8_t* bp = (u_int8_t*)(p + 1);
    int cc = pb->len - sizeof(*p);
    bp = reasm_.reassemble(rh, p, bp, cc);
    if (bp != 0) {
        codec_->decode(bp, cc, rvts_, now_);
        ndblk_ = codec_->ndblk();
        render_frame(codec_->frame());
        codec_->resetndblk();
    }
    pb->release();
}
