#include "config.h"
#include "rtp.h"
#include "decoder.h"
#include "bsd-endian.h"
#include "jpeg/jpeg.h"
#include "pktbuf.h"

class JpegReassembler {
public:
    JpegReassembler();
    ~JpegReassembler();
    u_char* reassemble(const rtphdr* rh, const jpeghdr *p, const u_char* bp, int& len);
    inline int badoff() const { return (badoff_); }
    inline int hugefrm() const { return (hugefrm_); }
protected:
    int decimate_;
    int ndec_;

    /*
     * Reassembly buffer.  This should be a dma buffer in the
     * jvdriver case but it's not clear if jvdriver allows buffers
     * to be shared across sockets. XXX ask Lance
     * If it does, then we can pass dma buffers betwee
     * the decoder and renderers.
     */
#define JPEG_SLOTS 64
#define JPEG_SLOTMASK (JPEG_SLOTS - 1)
    struct slot {
        int seqno;
        int eof;	/* nbytes in last pkt of frame, o.w. 0 */
        u_int32_t off;
        u_int32_t ts;
    } slots_[JPEG_SLOTS];

    /*
     * Reassembly buffers.  We do double-buffering, which allows
     * packets to arrive out of order across frame boundaries,
     * but not across an entire frame (i.e., we don't want to see
     * any packets from frame k+2 until we're done with frame k).
     * We use RTP timestamps to keep track of which frame is
     * allocated to which buffer.  (RTP guarantees that the
     * timestamps are constant across a frame, and increase
     * between succesive frames.  XXX is latter really true?)
     */
    struct rbuf {
        int drop;
        u_int32_t ts;
        u_char* bp;
    };
    rbuf rb0_;
    rbuf rb1_;
    int rbsize_;
    int hugefrm_;
    int badoff_;
};

class MotionJpegDecoder : public Decoder {
public:
    MotionJpegDecoder();
    virtual ~MotionJpegDecoder();
    virtual void recvHeader(pktbuf*);
    virtual void recv(pktbuf*);
protected:

    int inq_;		/* input quantization */
    int type_;		/* JPEG/RTP parameters type code */

    void configure();
    JpegPixelDecoder* codec_;
    JpegDecoder::config config_;
    JpegReassembler reasm_;
};

