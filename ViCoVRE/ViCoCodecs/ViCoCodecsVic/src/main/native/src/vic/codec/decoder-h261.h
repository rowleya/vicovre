#include "config.h"
#include "rtp.h"
#include "decoder.h"
#include "p64/p64.h"

class H261Decoder : public Decoder {
    public:
    H261Decoder();
    virtual ~H261Decoder();
    virtual void recvHeader(pktbuf*);
    virtual void recv(pktbuf*);
    protected:
    void decode(const u_char* vh, const u_char* bp, int cc);
    P64Decoder* codec_;
    int h261_rtp_bug_;
};
