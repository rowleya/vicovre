/*
 * Copyright (c) 1996 The Regents of the University of California.
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

#ifndef vic_pktbuf_h
#define vic_pktbuf_h

#include "config.h"
#include "rtp.h"

class pktbuf;

class Buffer {
public:
    virtual Buffer* copy() = 0;
    virtual void release();
    virtual ~Buffer() {}; //SV-XXX: This solves the "missing" virtual destructor warning from gcc4
};

/*
 * The base object for performing the outbound path of
 * the application level protocol.
 */
class BufferPool {
    public:
    BufferPool();
    void release(pktbuf*);
    pktbuf* alloc();
    void close();
    void setData(u_int8_t*, int);
    private:
    pktbuf* freebufs_;
    pktbuf* allocbufs_;
    pktbuf* lastallocbuf_;
    int nbufs_;
};

/*XXX*/
#define MAXHDR 128
#define PKTBUF_PAD 256
#define RTP_MTU 1024
/* Introduced factor of 2 as the H261 codec seems to over-run the buffer a bit
#define PKTBUF_SIZE (MAXHDR + 1024 + PKTBUF_PAD) from MASH */
#define PKTBUF_SIZE (2 * RTP_MTU)

class pktbuf : public Buffer {
public:
    pktbuf(): len(0),ref(0),manager(0),dp(0),rtp_header(0),next(0),malloced(0) {};
    pktbuf* next;
    int len;
    int ref;
    bool malloced;
    u_int8_t* dp;
    rtphdr* rtp_header;
    BufferPool* manager;
    inline void release() {
        if (manager != 0) {
            ref--;
            if (!ref) {
                manager->release(this);
            }
        }
    }
    inline void attach() {
        if (manager != 0) {
            ref++;
        }
    }
    Buffer* copy();
};

#endif
