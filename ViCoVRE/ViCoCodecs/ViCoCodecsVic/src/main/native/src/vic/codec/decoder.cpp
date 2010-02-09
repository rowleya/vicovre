/*
 * Copyright (c) 1993-1995 The Regents of the University of California.
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
#include "config.h"
#include "sys-time.h"
#include "rtp.h"
#include "decoder.h"

//SV-XXX: rearranged intialistaion order to shut upp gcc4
Decoder::Decoder(int hdrlen) :
    color_(1), decimation_(422), inw_(0), inh_(0),
    rvts_(0), nblk_(0), ndblk_(0), callback_(0), out_(0)
{
    /*XXX*/
    now_ = 1;
}


Decoder::~Decoder()
{
    if (rvts_)
        delete[] rvts_; //SV-XXX: Debian
}

/*
 * Return time of day in microseconds.
 */
double Decoder::gettimeofday_usecs()
{
    timeval tv;
    ::gettimeofday(&tv, 0);
    return (1e6 * double(tv.tv_sec) + double(tv.tv_usec));
}

void Decoder::setOut(u_char* out) {
    out_ = out;
}

void Decoder::render_frame(const u_char* frm)
{
    if (out_ != 0) {
        int size = inw_ * inh_;
        if (decimation_ == 422) {
            memcpy(out_, frm, size * 2);
        } else if (decimation_ == 420) {
            memcpy(out_, frm, size + (size >> 1));
        }
    }
    if (callback_ != 0) {
        callback_->render_frame(frm, decimation_);
    }

}

void Decoder::resize(int width, int height)
{
    inw_ = width;
    inh_ = height;
    nblk_ = (width * height) / 64;
    delete[] rvts_; //SV-XXX: Debian
    rvts_ = new u_char[nblk_];
    memset(rvts_, 0, nblk_);
    if (callback_ != 0) {
        callback_->resize(width, height);
    }
}

PlaneDecoder::PlaneDecoder(int hdrlen) : Decoder(hdrlen), frm_(0)
{
}

PlaneDecoder::~PlaneDecoder()
{
    delete[] frm_; //SV-XXX: Debian
}

void PlaneDecoder::resize(int width, int height)
{
    delete[] frm_; //SV-XXX: Debian
    int size = width * height;
    frm_ = new u_char[2 * size];
    /*
     * Initialize image to gray.
     */
    memset(frm_, 0x80, 2 * size);
    Decoder::resize(width, height);
}
