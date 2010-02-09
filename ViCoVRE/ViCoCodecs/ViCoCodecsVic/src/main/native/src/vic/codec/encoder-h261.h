#include "config.h"
#include "rtp.h"
#include "dct.h"
#include "p64/p64-huff.h"
#include "bsd-endian.h"
#include "crdef.h"
#include "pktbuf-rtp.h"
#include "module.h"

#ifdef INT_64
#define NBIT 64
#define BB_INT INT_64
#else
#define NBIT 32
#define BB_INT u_int
#endif

class H261Encoder : public TransmitterModule {
    public:
    void setq(int q);
    protected:
    H261Encoder(int ft);
    ~H261Encoder();
    int encode(const VideoFrame*, const u_int8_t *crvec);
    void encode_blk(const short* blk, const char* lm);
    int flush(pktbuf* pb, int nbit, pktbuf* npb);
    char* make_level_map(int q, u_int fthresh);
    void setquantizers(int lq, int mq, int hq);

    virtual void size(int w, int h) = 0;
    virtual void encode_mb(u_int mba, const u_char* frm,
               u_int loff, u_int coff, int how) = 0;

    /* bit buffer */
    BB_INT bb_;
    u_int nbb_;

    u_char* bs_;
    u_char* bc_;
    int sbit_;

    u_char lq_;		/* low quality quantizer */
    u_char mq_;		/* medium quality quantizer */
    u_char hq_;		/* high quality quantizer */
    u_char mquant_;		/* the last quantizer we sent to other side */
    int quant_required_;	/* 1 if not quant'd in dct */
    u_int ngob_;
    u_int mba_;

    u_int cif_;		/* 1 for CIF, 0 for QCIF */
    u_int bstride_;
    u_int lstride_;
    u_int cstride_;

    u_int loffsize_;	/* amount of 1 luma block */
    u_int coffsize_;	/* amount of 1 chroma block */
    u_int bloffsize_;	/* amount of 1 block advance */

    const char* llm_[32];	/* luma dct val -> level maps */
    const char* clm_[32];	/* chroma dct val -> level maps */

    float lqt_[64];		/* low quality quantizer */
    float mqt_[64];		/* medium quality quantizer */
    float hqt_[64];		/* high quality quantizer */

    u_int coff_[12];	/* where to find U given gob# */
    u_int loff_[12];	/* where to find Y given gob# */
    u_int blkno_[12];	/* for CR */
};

class H261DCTEncoder : public H261Encoder {
    public:
    H261DCTEncoder();
    int consume(const VideoFrame*);
    void size(int w, int h);
    protected:
    void encode_mb(u_int mba, const u_char* frm,
               u_int loff, u_int coff, int how);
};

class H261PixelEncoder : public H261Encoder {
    public:
    H261PixelEncoder();
    int consume(const VideoFrame*);
    void size(int w, int h);
    protected:
    void encode_mb(u_int mba, const u_char* frm,
               u_int loff, u_int coff, int how);
};
