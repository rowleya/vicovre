#include "config.h"
#include "rtp.h"
#include "pktbuf-rtp.h"
#include "module.h"

#include "databuffer.h"
#include "x264encoder.h"

class H264Encoder : public TransmitterModule
{
  public:
    H264Encoder();
    ~H264Encoder();

    void setq(int q);

    void size(int w, int h);
    int consume(const VideoFrame *);

  protected:
    u_int32_t ts;
    unsigned char frame_seq;
    int fps, kbps, gop;
    bool state;

    x264Encoder *enc;
    DataBuffer *fOut;

    FILE *fptr;
};
