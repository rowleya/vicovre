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

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>

#include "grabber.h"
#include "crdef.h"

#if defined(sun) && !defined(__svr4__)
extern "C" int gettimeofday(struct timeval*, struct timezone*);
#endif

//SV-XXX: rearranged initialisation order to shut up gcc4
Grabber::Grabber()
    : vstart_(0), vstop_(0),
      hstart_(0), hstop_(0),
      threshold_(48),
      crvec_(0), ref_(0),
      outw_(0), outh_(0)
{
}

Grabber::~Grabber()
{
    delete[] crvec_; //SV-XXX: Debian
    delete[] ref_; //SV-XXX: Debian
}

void Grabber::crinit(int w, int h)
{
    blkw_ = w >> 4;
    blkh_ = h >> 4;
    scan_ = 0;
    nblk_ = blkw_ * blkh_;
    delete[] crvec_; //SV-XXX: Debian
    crvec_ = new u_char[nblk_];
    for (int i = 0; i < nblk_; ++i)
        crvec_[i] = CR_MOTION|CR_SEND;
}

void Grabber::reset() {
    for (int i = 0; i < nblk_; ++i)
        crvec_[i] = CR_MOTION|CR_SEND;
}

/* must call after set_size_xxx */
void Grabber::allocref()
{
    delete[] ref_; //SV-XXX: Debian
    ref_ = new u_char[framesize_];
    memset((char*)ref_, 0, framesize_);
}

#define REPLENISH(devbuf, refbuf, ds, bpp, hstart, hstop, vstart, vstop) \
{ \
    scan_ = (scan_ + 3) & 7;\
\
    register int _ds = ds; \
    register int _rs = outw_; \
    const u_char* rb = &(refbuf)[scan_ * _rs]; \
    const u_char* db = &(devbuf)[scan_ * _ds]; \
    int w = blkw_; \
    u_char* crv = crvec_; \
 \
    crv += (vstart) * w; \
    for (int y = vstart; y < vstop; ++y) { \
        const u_char* ndb = db; \
        const u_char* nrb = rb; \
        u_char* ncrv = crv; \
        crv += hstart; \
        for (int x = hstart; x < hstop; x++) { \
            int left = 0; \
            int right = 0; \
            int top = 0; \
            int bottom = 0; \
            DIFFLINE(db, rb, left, top, right); \
            db += _ds << 3; \
            rb += _rs << 3; \
            DIFFLINE(db, rb, left, bottom, right); \
            db -= _ds << 3; \
            rb -= _rs << 3; \
 \
            int center = 0; \
            if (left >= threshold_ && x > 0) { \
                crv[-1] = CR_MOTION|CR_SEND; \
                center = 1; \
            } \
            if (right >= threshold_ && x < w - 1) { \
                crv[1] = CR_MOTION|CR_SEND; \
                center = 1; \
            } \
            if (bottom >= threshold_ && y < blkh_ - 1) { \
                crv[w] = CR_MOTION|CR_SEND; \
                center = 1; \
            } \
            if (top >= threshold_ && y > 0) { \
                crv[-w] = CR_MOTION|CR_SEND; \
                center = 1; \
            } \
            if (center) \
                crv[0] = CR_MOTION|CR_SEND; \
 \
            db += 16 * (bpp); \
            rb += 16; \
            ++crv; \
        } \
        db = ndb + (_ds << 4); \
        rb = nrb + (_rs << 4); \
        crv = ncrv + w; \
    } \
}

/*
 * define these for REPLENISH macro used below
 */
#define ABS(v) if (v < 0) v = -v;

#define DIFF4(in, frm, v) \
    v += (in)[0] - (frm)[0]; \
    v += (in)[1] - (frm)[1]; \
    v += (in)[2] - (frm)[2]; \
    v += (in)[3] - (frm)[3];

#define DIFFLINE(in, frm, left, center, right) \
    DIFF4(in, frm, left); \
    DIFF4(in + 1*4, frm + 1*4, center); \
    DIFF4(in + 2*4, frm + 2*4, center); \
    DIFF4(in + 3*4, frm + 3*4, right); \
    ABS(right); \
    ABS(left); \
    ABS(center);

u_char* Grabber::cr(const u_char* devbuf)
{
    REPLENISH(devbuf, ref_, outw_, 1, 0, blkw_, 0, blkh_);
    return crvec_;
}

inline void save(const u_char* lum, u_char* cache, int stride)
{
    for (int i = 16; --i >= 0; ) {
        ((u_int*)cache)[0] = ((u_int*)lum)[0];
        ((u_int*)cache)[1] = ((u_int*)lum)[1];
        ((u_int*)cache)[2] = ((u_int*)lum)[2];
        ((u_int*)cache)[3] = ((u_int*)lum)[3];
        cache += stride;
        lum += stride;
    }
}

/*
 * Default save routine -- stuff new luma blocks into cache.
 */
void Grabber::saveblks(u_char* lum)
{
    u_char* crv = crvec_;
    u_char* cache = ref_;
    int stride = outw_;
    stride = (stride << 4) - stride;
    for (int y = 0; y < blkh_; y++) {
        for (int x = 0; x < blkw_; x++) {
            if ((*crv++ & CR_SEND) != 0)
                save(lum, cache, outw_);
            cache += 16;
            lum += 16;
        }
        lum += stride;
        cache += stride;
    }
}

void Grabber::set_size(int w, int h)
{

    inw_ = w;
    inh_ = h;
    w &=~ 0xf;
    h &=~ 0xf;
    outw_ = w;
    outh_ = h;

    framesize_ = w * h;
    crinit(w, h);

    vstart_ = 0;
    vstop_ = blkh_;
    hstart_ = 0;
    hstop_ = blkw_;
    allocref();
}
