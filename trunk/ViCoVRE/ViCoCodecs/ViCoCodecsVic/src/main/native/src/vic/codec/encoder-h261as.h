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

class H261ASEncoder : public TransmitterModule {
    public:
    H261ASEncoder();
    ~H261ASEncoder();
    void setq(int q);
    int consume(const VideoFrame*);
    protected:
    int encode(const VideoFrame*, const u_int8_t *crvec);
    void encode_blk(const short* blk, const char* lm);
    int flush(pktbuf* pb, int nbit, pktbuf* npb);
    char* make_level_map(int q, u_int fthresh);
    void setquantizers(int lq, int mq, int hq);
    void size(int w, int h);
    void encode_mb(u_int mba, const u_char* frm,
               u_int loff, u_int coff, int how);

        u_int width;
        u_int height;
        u_char *frame_buffer;
        u_int frame_buffer_size;

        u_char *crref;
        u_char *crvec;

        int nblk;
        int blkw;
        int blkh;
        int rover;
        int threshold;
        int scan;

        u_char *bs;
        u_char *bc;
        int bb;
        int nbb;
        int sbit;
        int tx;
        int lq;
        int mq;
        int hq;
        u_int ngob;
        u_int nblocks;
        int mquant;
        int mba;

        int *loff_table;
        int *coff_table;
        int *blkno_table;

        float lqt[64];
        float mqt[64];
        float hqt[64];

        const char *llm[32];
        const char *clm[32];
};
