/*
 * Copyright (c) 1995 The Regents of the University of California.
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

#ifndef vic_module_h
#define vic_module_h

#include "config.h"
#include "pktbuf-rtp.h"

class pktqueelem;

class pktqueelem {

    public:
        pktbuf *buf;
        pktqueelem *next;
};

class pktque {
    public:
        pktque() : first(0), last(0) {

        }

        void add(pktbuf *buf) {
            pktqueelem *elem = new pktqueelem();
            elem->buf = buf;
            if (last != 0) {
                last->next = elem;
            }
            last = elem;
            if (first == 0) {
                first = elem;
            }
        }

        pktbuf *removeFirst() {
            pktqueelem *elem = first;
            if (first != 0) {
                first = first->next;
            }
            if (last == elem) {
                last = 0;
            }
            if (elem != 0) {
                pktbuf *buf = elem->buf;
                delete elem;
                return buf;
            }
            return 0;
        }

        bool hasFirst() {
            return first != 0;
        }

    private:
        pktqueelem *first;
        pktqueelem *last;
};

class VideoFrame {
    public:
    inline VideoFrame(u_int32_t ts, u_int8_t* bp, int w, int h,
              int layer) :
        ts_(ts), bp_(bp), width_(w), height_(h), layer_(layer) { }

    u_int32_t ts_;
    u_int8_t* bp_;
    int width_;
    int height_;
    int layer_;
};

class YuvFrame : public VideoFrame {
    public:
        inline YuvFrame(u_int32_t ts, u_int8_t* bp, u_int8_t* crvec,
            int w, int h, int layer=0) :
            VideoFrame(ts, bp, w, h, layer), crvec_(crvec) {}

        const u_int8_t* crvec_;
};

class JpegFrame : public VideoFrame {
    public:
    inline JpegFrame(u_int32_t ts, u_int8_t* bp, int len, int q, int type,
            int w, int h) :
        VideoFrame(ts, bp, w, h, 0), len_(len), q_(q), type_(type) {}
    int len_;
    int q_;
    int type_;
};

class DCTFrame : public VideoFrame {
    public:
    inline DCTFrame(u_int32_t ts, short* bp, u_int8_t* crv,
        int w, int h, int q = -1) :
        VideoFrame(ts, (u_int8_t*)bp, w, h, 0), crvec_(crv), q_(q) {}

    const u_int8_t *crvec_;
    int q_;			// original q (if applicable)
};

class H261Frame : public VideoFrame {
    public:
    inline H261Frame(u_int32_t ts, short* bp,int len,int w, int h) :
        VideoFrame(ts, (u_int8_t*)bp, w, h, 0),len_(len) {}
        /* additional vars here. */
    int len_;
};

class CellBFrame : public VideoFrame {
    public:
    inline CellBFrame(u_int32_t ts, short* bp,int len,int w, int h) :
        VideoFrame(ts, (u_int8_t*)bp, w, h, 0),len_(len) {}
        /* additional vars here. */
    int len_;
};

#define FT_HW		0x80
#define FT_YUV_420	1
#define FT_YUV_422	2
#define FT_YUV_CIF	3
#define FT_JPEG		(1|FT_HW)
#define FT_DCT		4
#define FT_H261		(5|FT_HW)
#define FT_CELLB	(6|FT_HW)
#define FT_RAW		7
#define FT_LDCT		8
#define FT_PVH		9
#define FT_H264		10
#define FT_MPEG4	11

class Module {
    public:
    virtual int consume(const VideoFrame*) = 0;
    inline int ft() const { return (ft_); }
    virtual inline void size(int w, int h) {
            width_ = w;
            height_ = h;
            framesize_ = w * h;
        }
    protected:
    Module(int ft);
    static int atoft(const char* s);
    static const char* fttoa(int ft);

    inline int samesize(const VideoFrame* vf) {
        return (vf->width_ == width_ && vf->height_ == height_);
    }
    Module* target_;
    int width_;
    int height_;
    int framesize_;
    int ft_;		/* stream type: 422/420/cif/jpeg etc. */
    int mtu_;
};

class pktque;

class TransmitterModule : public Module {
    public:
    ~TransmitterModule();
    void setData(u_int8_t* data, int length);
    pktque *pktsToSend_;
    protected:
    TransmitterModule(int ft);
    RTP_BufferPool *pool_;
    void send(pktbuf *pb);
};

#endif
