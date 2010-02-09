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

#ifndef vic_decoder_h
#define vic_decoder_h

#include "pktbuf.h"

/*
 * Rendering vector.  We keep a vector of timestamps of when each individual
 * block of frame has been updated by a decoder.  Then, different renderers
 * can keep track of the last time they've updated their view of the frame
 * (and dither only those blocks that need to be updated).
 */

#define RV_PAST(now, ts) ((((now) - (ts)) & 0x80) == 0)

class DecoderCallbackInterface {
    public:
        virtual void resize(int inw, int inh) = 0;
        virtual void render_frame(const u_char* frm, int decimation) = 0;
};

class Decoder {
 protected:
    Decoder(int hdrlen);
 public:
    virtual ~Decoder();

    static double gettimeofday_usecs();

    virtual void resize(int inw, int inh);

    virtual void setOut(u_char *out);

    virtual void recvHeader(pktbuf*) = 0;

    virtual void recv(pktbuf*) = 0;

    inline int width() const { return (inw_); }
    inline int height() const { return (inh_); }
    inline int decimation() const { return (decimation_); }
    inline int ndblk() const { return (ndblk_); }
    inline void resetndblk() { ndblk_ = 0; }
    inline void setCallback(DecoderCallbackInterface *callback) {
        callback_ = callback;
    };
 protected:

    DecoderCallbackInterface *callback_;

    /*XXX steal back from rcvr */
    int color_;
    int decimation_;	/* 422 or 420 */
    int inw_;		/* native image width */
    int inh_;		/* native image height */

    void render_frame(const u_char* frm);
    int now_;
    u_char* rvts_;
    int nblk_;		/* number of 8x8 blocks */
    int ndblk_;	/* number of blocks decoded in most recent frame */
    u_char *out_;
};

class PlaneDecoder : public Decoder {
 public:
    PlaneDecoder(int hdrlen);
    virtual ~PlaneDecoder();
    void sync();
 protected:
    void resize(int width, int height);

    u_char* frm_;		/* storage for YUV representation */
};

#endif
