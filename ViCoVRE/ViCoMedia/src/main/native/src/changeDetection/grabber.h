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

#ifndef vic_grabber_h
#define vic_grabber_h

#include "config.h"

#ifdef USE_FASTMEMCPY
extern "C" {
#include "postproc/fastmemcpy.h"
}
#endif

class Module;
class Transmitter;

/*
 * Number of horizontal lines to pad at the top and bottom of the
 * framer buffers that are handed to the Encoder encode method.
 * Some encoders can operate more efficiently with this scratch
 * space (i.e., the bvc coder).
 */
#define GRABBER_VPAD 1

class Grabber {
 public:
    Grabber();
    ~Grabber();
    u_char* cr(const u_char* devbuf);
    void saveblks(u_char* lum);
    void set_size(int w, int h);
    inline int getInW() {return inw_;}
    inline int getInH() {return inh_;}
    void reset();
    void setThreshold(int threshold) {threshold_ = threshold;}
 protected:
    /* hooks for conditional replenishment algorithm */
    void crinit(int w, int h);
    void allocref();

    int vstart_;
    int vstop_;
    int hstart_;
    int hstop_;
    int threshold_ ; // when a block is changed ? (was constant 48)

    u_int framesize_;
    u_char* crvec_;
    u_char* ref_;/*XXX*/
    int inw_;
    int inh_;
    int outw_;
    int outh_;
    int blkw_;
    int blkh_;

    int scan_;
    int nblk_;
};

#endif
