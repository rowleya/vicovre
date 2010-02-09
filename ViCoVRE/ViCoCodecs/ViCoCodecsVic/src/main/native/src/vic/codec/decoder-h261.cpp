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
#include "decoder-h261.h"

H261Decoder::H261Decoder() : Decoder(4), codec_(0), h261_rtp_bug_(0)
{

    decimation_ = 420;
    /*
     * Assume CIF.  Picture header will trigger a resize if
     * we encounter QCIF instead.
     */
    inw_ = 352;
    inh_ = 288;

    /*XXX*/
    resize(inw_, inh_);
}

H261Decoder::~H261Decoder()
{
    delete codec_;
}

void H261Decoder::recvHeader(pktbuf* pb) {

}

void H261Decoder::recv(pktbuf* pb)
{
    rtphdr* rh = (rtphdr*)pb->rtp_header;
    u_int8_t* vh = (u_int8_t*)(pb->dp);
    if (codec_ == 0) {
        if ((vh[0] & 2) != 0)
            codec_ = new IntraP64Decoder();
        else
            codec_ = new FullP64Decoder();
        codec_->marks(rvts_);
    }
    /*XXX*/
    u_int v = ntohl(*(u_int32_t*)vh);
    int sbit = v >> 29;
    int ebit = (v >> 26) & 7;
    int quant = (v >> 10) & 0x1f;
    int mvdh = (v >> 5) & 0x1f;
    int mvdv = v & 0x1f;
    int mba, gob;
    /*
     * vic-2.7 swapped the GOB and MBA fields in the RTP packet header
     * with respect to the spec.  To maintain backward compat, whenever
     * we see an out of range gob, we change our assumption about the
     * stream and continue.
     */
    if (!h261_rtp_bug_) {
        mba = (v >> 15) & 0x1f;
        gob = (v >> 20) & 0xf;
        if (gob > 12) {
            h261_rtp_bug_ = 1;
            mba = (v >> 19) & 0x1f;
            gob = (v >> 15) & 0xf;
        }
    } else {
        mba = (v >> 19) & 0x1f;
        gob = (v >> 15) & 0xf;
        if (gob > 12) {
            h261_rtp_bug_ = 0;
            mba = (v >> 15) & 0x1f;
            gob = (v >> 20) & 0xf;
        }
    }

    if (gob > 12) {
        pb->release();
        return;
    }
    int cc = pb->len - 4;
    codec_->mark(now_);
    (void)codec_->decode(vh + 4, cc, sbit, ebit,
                 mba, gob, quant, mvdh, mvdv);
#ifdef CR_STATS
    dumpShadow(ntohl(rh->rh_ts), rvts_, nblk_);
#endif
    /*
     * If the stream changes format, issue a resize.
     */
    if (codec_->width() != inw_) {
        resize(codec_->width(), codec_->height());
        codec_->marks(rvts_);
    }

    /*XXX*/
    if (ntohs(rh->rh_flags) & RTP_M) {
        codec_->sync();
        ndblk_ = codec_->ndblk();
        render_frame(codec_->frame());
        codec_->resetndblk();
    }
    pb->release();
}
