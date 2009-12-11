#include "com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavformat/rtpdec.h>
#include <libavutil/avutil.h>
#include <libswscale/swscale.h>
}
#include <map>

class FFMpegJ {
    private:
        static bool av_codec_initialized;

        std::map<int, CodecID> codecMap;
        int BUFFER_PROCESSED_OK;
        int BUFFER_PROCESSED_FAILED;
        int INPUT_BUFFER_NOT_CONSUMED;
        int OUTPUT_BUFFER_NOT_FILLED;
        int FLAG_RTP_MARKER;
        int FLAG_KEY_FRAME;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;
        jmethodID getLengthMethod;
        jmethodID getSequenceNumberMethod;
        jmethodID getFlagsMethod;
        jmethodID setOffsetMethod;
        jmethodID setLengthMethod;
        jmethodID setTimestampMethod;
        jmethodID setSequenceNumberMethod;

        jclass contextClass;
        jmethodID contextConstructor;
        jmethodID getCFlagsMethod;
        jmethodID getFlags2Method;
        jmethodID getQminMethod;
        jmethodID getQmaxMethod;
        jmethodID getMaxQdiffMethod;
        jmethodID getLowresMethod;
        jmethodID getDctAlgoMethod;
        jmethodID getDebugMethod;
        jmethodID getBitrateMethod;
        jmethodID getMaxrateMethod;
        jmethodID setFlagsMethod;
        jmethodID setFlags2Method;
        jmethodID setQminMethod;
        jmethodID setQmaxMethod;
        jmethodID setMaxQdiffMethod;
        jmethodID setLowresMethod;
        jmethodID setDctAlgoMethod;
        jmethodID setDebugMethod;
        jmethodID setBitrateMethod;
        jmethodID setMaxrateMethod;

        AVCodecParserContext *parser;
        RTPDynamicProtocolHandler *rtpHandler;
        PayloadContext *rtpPayloadContext;
        AVFormatContext *rtpFormatContext;
        AVStream *rtpStream;
        long lastSequence;
        long firstSequence;
        AVPacket *rtpPackets[1024];
        uint8_t rtpData[1024*1600];

        AVCodecContext *codecContext;
        AVCodec *codec;
        bool isEncoding;
        int pictureSize;
        AVFrame *frame;
        AVFrame *intermediateFrame;
        int frameFinished;
        int bytesProcessed;
        PixelFormat pixFmt;
        PixelFormat intermediatePixFmt;
        int width;
        int height;
        bool flipped;
        int frameCount;
        SwsContext *swScaleContext;
        bool swinit;
        uint8_t *buffer;
        bool codecOpened;

    public:
        FFMpegJ(JNIEnv *env, jobject peer, int logLevel);
        ~FFMpegJ();
        long openCodec(bool isEncoding, int codecId);
        bool init(int pixFmt, int width, int height, int intermediatePixFmt,
            int intermediateWidth, int intermediateHeight, bool flipped,
            JNIEnv *env, jstring rtpSdp, jobject context);
        int decode(JNIEnv *env, jobject input, jobject output);
        int encode(JNIEnv *env, jobject input, jobject output);
        bool closeCodec();
        int getOutputSize();
        void getCodecContext(JNIEnv *env, jobject context);
};
