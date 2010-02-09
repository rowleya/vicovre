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

#ifndef lint
static const char rcsid[] =
    "@(#) $Header$ (LBL)";
#endif
#include "module.h"

const char* Module::fttoa(int ft)
{
    switch (ft) {
    case FT_YUV_420:  return ("420");
    case FT_YUV_422:  return ("422");
    case FT_YUV_CIF:  return ("cif");
    case FT_JPEG:	  return ("jpeg");
    case FT_H261:	  return ("h261");
    case FT_CELLB:	  return ("cellb");
    case FT_DCT:	  return ("dct");
    case FT_RAW:	  return ("raw");
    case FT_LDCT:	  return ("ldct");
    case FT_PVH:	  return ("pvh");
    case FT_H264:	  return ("h264");
    case FT_MPEG4:	  return ("mpeg4");
    }
    return ("");
}

int Module::atoft(const char* s)
{
    if (strcasecmp(s, "420") == 0)
        return FT_YUV_420;
    if (strcasecmp(s, "422") == 0)
        return FT_YUV_422;
    if (strcasecmp(s, "cif") == 0)
        return FT_YUV_CIF;
    if (strcasecmp(s, "jpeg") == 0)
        return FT_JPEG;
    if (strcasecmp(s, "h261") == 0)
        return FT_H261;
    if (strcasecmp(s, "cellb") == 0)
        return FT_CELLB;
    if (strcasecmp(s, "dct") == 0)
        return FT_DCT;
    if (strcasecmp(s, "raw") == 0)
        return FT_RAW;
    if (strcasecmp(s, "ldct") == 0)
        return FT_LDCT;
    if (strcasecmp(s, "pvh") == 0)
        return FT_PVH;
    if (strcasecmp(s, "h264") == 0)
        return FT_H264;
    if (strcasecmp(s, "mpeg4") == 0)
        return FT_MPEG4;
    return (-1);
}

//SV-XXX: rearranged the initalisation order to shut up gcc4
Module::Module(int ft)
    : target_(0), width_(0),
      height_(0), framesize_(0), ft_(ft), mtu_(1024)
{
}

TransmitterModule::TransmitterModule(int ft) : Module(ft)
{
    pool_ = new RTP_BufferPool();
    pktsToSend_ = new pktque();
}

TransmitterModule::~TransmitterModule()
{
    pool_->close();
    delete pool_;
    delete pktsToSend_;
}

void TransmitterModule::send(pktbuf *buf) {
    pktsToSend_->add(buf);
}

void TransmitterModule::setData(u_int8_t *data, int length) {
    pool_->setData(data, length);
}
