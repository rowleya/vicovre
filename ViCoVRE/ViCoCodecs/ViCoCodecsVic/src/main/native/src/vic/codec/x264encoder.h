#ifndef _X264_ENCODER_H_
#define _X264_ENCODER_H_

typedef unsigned char uint8;

class DataBuffer;
class x264Encoder
{
  public:
    x264Encoder();
    ~x264Encoder();
    bool init(int, int, int, int);
    bool encodeFrame(uint8 *);
    int numNAL();
    bool getNALPacket(int, DataBuffer *);
    void setGOP(int);
    void setBitRate(int);
    void setFPS(int);
    bool isInitialized();

  private:
    void *encoder;
    bool isFrameEncoded;
};

#endif
