#include "config.h"
#include "rtp.h"
#include "decoder.h"
#include "p64/p64as.h"

class H261ASDecoder : public Decoder {
    public:
    H261ASDecoder();
    virtual ~H261ASDecoder();
    virtual void recvHeader(pktbuf*);
    virtual void recv(pktbuf*);
    protected:
    void decode(const u_char* vh, const u_char* bp, int cc);
    P64ASDecoder* codec_;
};
