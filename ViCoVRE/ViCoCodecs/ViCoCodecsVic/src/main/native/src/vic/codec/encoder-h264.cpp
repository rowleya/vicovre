#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <errno.h>
#include <assert.h>
#include <iostream>

#include "encoder-h264.h"


H264Encoder::H264Encoder():TransmitterModule(FT_YUV_420)
{
    enc = new x264Encoder();
    state = false;
    fptr = NULL;
    frame_seq = 0;
    fps = 20;
    kbps = 512;
    gop = 20;
    fOut=NULL;
}

H264Encoder::~H264Encoder()
{
    delete enc;
    if (fOut) delete fOut;
}

void H264Encoder::size(int w, int h)
{
    Module::size(w, h);
    fOut = new DataBuffer(w * h * 3 >> 2);
}

int H264Encoder::consume(const VideoFrame * vf)
{
    pktbuf *pb;
    rtphdr *rh;
    ts = vf->ts_;

    int numNAL, i, sent_size = 0;
    int frame_size = 0;
    unsigned char f_seq = 0;
    //unsigned char f_total_pkt = 0;
    int RTP_HDR_LEN = sizeof(rtphdr);
    int NAL_FRAG_THRESH = mtu_ - RTP_HDR_LEN; /* payload max in one packet */
    //debug_msg( "MTU=%d, RTP_HDR_LEN=%d\n", NAL_FRAG_THRESH, RTP_HDR_LEN);

    if (!state) {
        state = true;
        size(vf->width_, vf->height_);
        enc->setGOP(gop);
        enc->init(vf->width_, vf->height_, kbps, fps);
        frame_size = vf->width_ * vf->height_;
    }

    frame_size = vf->width_ * vf->height_;
    enc->encodeFrame(vf->bp_);
    numNAL = enc->numNAL();

    //Send out numNAL packets framed according to RFC3984
    for (i = 0; i < numNAL; i++) {

    bool firstFragment = true;
    int FU_HDR_LEN = 1;
    int offset = 0;

    enc->getNALPacket(i, fOut);
    int nalSize1 = fOut->getDataSize();
    int nalSize = nalSize1-5;
    char *data1 = fOut->getData();
    char *data = fOut->getData();
     uint8_t NALhdr = data1[4]; //SV-XXX why does our x.264 provide 4-byte StartSync in the NALU?
    uint8_t NALtype = NALhdr & 0x1f;
    //debug_msg( "Got NALhdr=0x%02x, NALtype=0x%02x from encoded frame.\n", NALhdr, NALtype);
    memcpy(data, &data1[5], nalSize);

    sent_size += nalSize;


    while (nalSize > 0) {
        pb = pool_->alloc(vf->ts_, RTP_PT_H264);
        rh = (rtphdr*) pb->rtp_header;

        if (nalSize <= NAL_FRAG_THRESH) {
        //==============================================
        //Single NAL or last fragment of FU-A
        //==============================================

        rh->rh_flags |= htons(RTP_M);	// set M bit
        pb->len = nalSize + FU_HDR_LEN;

#ifdef H264DEBUG
        debug_msg( "NAL : ");
#endif
        if (FU_HDR_LEN==2) {
            //==============================================
            //Last fragment of FU-A
            //==============================================
                   pb->dp[0] = 0x00 | (NALhdr & 0x60) | 28; 	//FU indicator
                   pb->dp[1] = 0x40  | NALtype; 		//FU header

#ifdef H264DEBUG
            debug_msg( "FU_Indicator=0x%02x, FU_Header=0x%02x, ", pb->data[0], pb->data[1]);
#endif
        }
        else {
                   pb->dp[0] = NALhdr; 				//NAL Header
#ifdef H264DEBUG
            debug_msg( "-----------------, --------------, ");
#endif
        }

        memcpy(&pb->dp[FU_HDR_LEN], data + offset, nalSize);

#ifdef H264DEBUG
        debug_msg( "i=%d/%d, nalSize=%4d len=%4d firstFrag=%d offset=%4d\n", i, numNAL, nalSize, pb->len, firstFragment, offset);
#endif

        nalSize = 0;
        offset = 0;

        } else {
        //==============================================
        //FU-A (not the last fragment though)
        //==============================================

        FU_HDR_LEN = 2;
        pb->len = (NAL_FRAG_THRESH - FU_HDR_LEN) + FU_HDR_LEN;

               pb->dp[0] = 0x00 | (NALhdr & 0x60) | 28; 			//FU indicator
               pb->dp[1] = ( (firstFragment) ? 0x80 : 0x00 ) | NALtype;	//FU header

        memcpy(&pb->dp[FU_HDR_LEN], data + offset, NAL_FRAG_THRESH - FU_HDR_LEN);

#ifdef H264DEBUG
        debug_msg( "FU-A: FU_Indicator=0x%02x, FU_Header=0x%02x, i=%d/%d, nalSize=%4d len=%4d firstFrag=%d offset=%4d\n",  pb->data[12], pb->data[13], i, numNAL, nalSize, pb->len, firstFragment, offset);
#endif

        nalSize -= (NAL_FRAG_THRESH-FU_HDR_LEN);
        offset += (NAL_FRAG_THRESH-FU_HDR_LEN);
        firstFragment = false;
        }

        send(pb);
        f_seq++;
    }

    }

    frame_seq++;


    return (kbps*1024) / (fps*8);
}
