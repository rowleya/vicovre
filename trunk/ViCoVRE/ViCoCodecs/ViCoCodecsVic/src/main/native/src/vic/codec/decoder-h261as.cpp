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
#include "decoder-h261as.h"

H261ASDecoder::H261ASDecoder() : Decoder(4), codec_(0)
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

H261ASDecoder::~H261ASDecoder()
{
    delete codec_;
}

void H261ASDecoder::recvHeader(pktbuf* pb)
{
    u_int8_t* vh = (u_int8_t*)(pb->dp);

    /*
     * h261as header is as follows:
         * ebit    3
         * quant   5
         * width  12
         * height 12
         */
    u_int v = ntohl(*(u_int32_t*)vh);
    int pwidth = (v >> 12) & 0x7ff;
    int pheight = v & 0x7ff;

    pwidth = (pwidth + 1) * 16;
    pheight = (pheight + 1) * 16;

    resize(pwidth, pheight);
}

void H261ASDecoder::recv(pktbuf* pb)
{
    rtphdr* rh = (rtphdr*)pb->rtp_header;
    u_int8_t* vh = (u_int8_t*)(pb->dp);
    if (codec_ == 0) {
        codec_ = new P64ASDecoder();
        codec_->marks(rvts_);
    }
    /*
     * h261as header is as follows:
         * ebit    3
         * quant   5
         * width  12
         * height 12
         */
    u_int v = ntohl(*(u_int32_t*)vh);
    int ebit = v >> 29;
    int quant = (v >> 24) & 0x1f;
    int pwidth = (v >> 12) & 0x7ff;
    int pheight = v & 0x7ff;

    int cc = pb->len - 4;

    pwidth = (pwidth + 1) * 16;
    pheight = (pheight + 1) * 16;

    /*
     * If the stream changes format, issue a resize.
     */
    if (pwidth != codec_->width() ||
        pheight != codec_->height()) {
            codec_->set_size(pwidth, pheight);
        resize(codec_->width(), codec_->height());
        codec_->marks(rvts_);
    }

    codec_->mark(now_);
    (void)codec_->decode(vh + 4, cc, ebit, quant);

    /*XXX*/
    if (ntohs(rh->rh_flags) & RTP_M) {
        codec_->sync();
        ndblk_ = codec_->ndblk();
        render_frame(codec_->frame());
        codec_->resetndblk();
    }
    pb->release();
}
