#include "com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer.h"

#define INT64_C(val) val##LL
#define UINT64_C(val) val##ULL

extern "C" {
#include "libavutil/avutil.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
}

extern jlong ptr2jlong(void *ptr);
extern void *jlong2ptr(jlong jl);

class PktQueueElement;
class PktQueue;

class PktQueueElement {
    friend class PktQueue;

    private:
        PktQueueElement() {
            packet = new AVPacket();
        };

        AVPacket *packet;
        PktQueueElement *next;
        PktQueueElement *prev;
};

class PktQueue {
    public:
        PktQueue() {
            first = NULL;
            last = NULL;
        };

        void addLast(AVPacket *packet) {
            PktQueueElement *elem = new PktQueueElement();
            elem->next = NULL;
            elem->prev = last;
            if (last != NULL) {
                last->next = elem;
            }
            last = elem;
            if (first == NULL) {
                first = elem;
            }
            elem->packet = new AVPacket();
            av_dup_packet(packet);
            memcpy(elem->packet, packet, sizeof(AVPacket));
        };

        AVPacket *removeFirst() {
            if (first == NULL) {
                return NULL;
            }
            PktQueueElement *elem = first;
            first = first->next;
            if (elem == last) {
                last = NULL;
            }
            AVPacket *packet = elem->packet;
            delete elem;
            return packet;
        }

        void clear() {
            while (first != NULL) {
                PktQueueElement *elem = first;
                first = first->next;
                AVPacket *packet = elem->packet;
                av_free_packet(packet);
                delete elem;
            }
            last = NULL;
        }

    private:
        PktQueueElement *first;
        PktQueueElement *last;

};

class Demuxer {
    public:
        Demuxer(JNIEnv *env, jobject object);
        ~Demuxer();
        bool init(JNIEnv *env, jobject object, const char *filename,
                int bufferSize, bool seekable);
        int readDatasource(uint8_t *buf, int size);
        int64_t seekDatasource(int64_t offset, int whence);
        int getNoStreams();
        int getCodec(int stream);
        const char* getCodecName(int stream);
        int getCodecType(int stream);
        bool setStreamOutputVideoFormat(int stream, int pixelFmt,
                int width, int height);
        int getVideoPixelFormat(int stream);
        double getVideoFrameRate(int stream);
        int getVideoWidth(int stream);
        int getVideoHeight(int stream);
        bool setStreamAudioDecoded(int stream);
        int getAudioSampleSize(int stream);
        int getNoAudioChannels(int stream);
        int getAudioSampleRate(int stream);
        int getOutputSize(int stream);
        int64_t getDuration();
        int64_t getDuration(int stream);
        int64_t getStartTime(int stream);
        int readNextFrame(JNIEnv *env, jobject object, jobject output,
                int stream);
        int64_t seekTo(JNIEnv *env, jobject object, int64_t position,
                int rounding);

    private:
        int nextPacket(JNIEnv *env, jobject object);
        ByteIOContext *createIOContext(int bufferSize, bool seekable);
        int64_t getTimestamp(AVPacket *packet, int stream);

        JNIEnv *env;
        jobject object;

        uint8_t *buffer;
        AVInputFormat *fmt;
        AVFormatContext *fmtContext;
        bool inited;
        SwsContext **swScaleContext;
        AVFrame **intermediateFrame;
        AVFrame **finalFrame;
        AVCodec **decoder;
        AVCodecContext **encodeContext;
        AVCodec **encoder;
        PixelFormat *outputPixelFormat;
        int *outputWidth;
        int *outputHeight;
        int *outputDataSize;
        int64_t *currentTimestamp;
        int lastAudioDataSize;
        PktQueue **streamQueue;
        AVPacket **currentPacket;
        uint8_t **currentPacketData;
        int *currentPacketLength;
        AVPacket packet;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;
        jmethodID getLengthMethod;
        jmethodID setTimestampMethod;
        jmethodID setLengthMethod;
        jmethodID readNextBufferMethod;
        jmethodID finishedProbeMethod;
        jmethodID seekMethod;

        int RoundUp;
        int RoundDown;
        int RoundNearest;

        int SET;
        int CUR;
        int END;
        int SIZE;

        int NO_FRAME_ERROR;
        int EOF_ERROR;
        int UNKNOWN_ERROR;
};

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_init
        (JNIEnv *env, jobject object, jstring filename, jint bufferSize,
                jboolean seekable) {
    fprintf(stderr, "Init called\n");
    fflush(stderr);
    Demuxer *demuxer = new Demuxer(env, object);
    const char *fname = env->GetStringUTFChars(filename, NULL);
    bool result = demuxer->init(env, object, fname, bufferSize, seekable);
    env->ReleaseStringUTFChars(filename, fname);
    if (!result) {
        delete demuxer;
        return -1;
    }
    return ptr2jlong(demuxer);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getNoStreams
        (JNIEnv *env, jobject object, jlong ref) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getNoStreams();
}

JNIEXPORT jstring JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getCodecName
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    const char *name = demuxer->getCodecName(stream);
    return env->NewStringUTF(name);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getCodecType
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getCodecType(stream);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getOutputSize
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getOutputSize(stream);
}

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getSourceDuration
        (JNIEnv *env, jobject object, jlong ref) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getDuration();
}

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getStreamDuration
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getDuration(stream);
}

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getStartTime
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getStartTime(stream);
}

JNIEXPORT jboolean JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_setStreamOutputVideoFormat
        (JNIEnv *env, jobject object, jlong ref, jint stream,
                jint pixelFmt, jint width, jint height) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->setStreamOutputVideoFormat(stream, pixelFmt, width, height);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getVideoWidth
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getVideoWidth(stream);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getVideoHeight
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getVideoHeight(stream);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getVideoPixelFormat
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getVideoPixelFormat(stream);
}

JNIEXPORT jdouble JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getVideoFrameRate
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getVideoFrameRate(stream);
}

JNIEXPORT jboolean JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_setStreamAudioDecoded
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->setStreamAudioDecoded(stream);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getAudioSampleSize
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getAudioSampleSize(stream);

}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getNoAudioChannels
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getNoAudioChannels(stream);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_getAudioSampleRate
        (JNIEnv *env, jobject object, jlong ref, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->getAudioSampleRate(stream);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_readNextFrame
      (JNIEnv *env, jobject object, jlong ref, jobject buffer, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->readNextFrame(env, object, buffer, stream);
}

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_seek
      (JNIEnv *env, jobject object, jlong ref, jlong position, jint rounding) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->seekTo(env, object, position, rounding);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_dispose
        (JNIEnv *env, jobject object, jlong ref) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    delete demuxer;
}

Demuxer::Demuxer(JNIEnv *env, jobject object) {
    avcodec_init();
    avcodec_register_all();
    av_register_all();
    av_log_set_level(AV_LOG_DEBUG);
    fmtContext = NULL;
    buffer = NULL;
    fmt = NULL;
    inited = false;

    jclass cls = env->GetObjectClass(object);
    readNextBufferMethod = env->GetMethodID(cls, "readNextBuffer",
            "(I)Ljavax/media/Buffer;");
    finishedProbeMethod = env->GetMethodID(cls, "finishedProbe", "()V");
    seekMethod = env->GetMethodID(cls, "seek", "(JI)J");

    jclass bufferCl = env->FindClass("javax/media/Buffer");
    getDataMethod = env->GetMethodID(bufferCl, "getData",
            "()Ljava/lang/Object;");
    getOffsetMethod = env->GetMethodID(bufferCl, "getOffset", "()I");
    getLengthMethod = env->GetMethodID(bufferCl, "getLength", "()I");
    setTimestampMethod = env->GetMethodID(bufferCl, "setTimeStamp", "(J)V");
    setLengthMethod = env->GetMethodID(bufferCl, "setLength", "(I)V");

    jclass positionableCl = env->FindClass("javax/media/protocol/Positionable");
    jfieldID roundDownField = env->GetStaticFieldID(positionableCl,
            "RoundDown", "I");
    RoundDown = env->GetStaticIntField(positionableCl, roundDownField);
    jfieldID roundUpField = env->GetStaticFieldID(positionableCl,
            "RoundUp", "I");
    RoundUp = env->GetStaticIntField(positionableCl, roundUpField);
    jfieldID roundNearestField = env->GetStaticFieldID(positionableCl,
            "RoundNearest", "I");
    RoundNearest = env->GetStaticIntField(positionableCl, roundNearestField);

    jclass datasinkCl = env->FindClass(
            "com/googlecode/vicovre/media/processor/DataSink");
    jfieldID setField = env->GetStaticFieldID(datasinkCl, "SEEK_SET", "I");
    SET = env->GetStaticIntField(datasinkCl, setField);
    jfieldID curField = env->GetStaticFieldID(datasinkCl, "SEEK_CUR", "I");
    CUR = env->GetStaticIntField(datasinkCl, curField);
    jfieldID endField = env->GetStaticFieldID(datasinkCl, "SEEK_END", "I");
    END = env->GetStaticIntField(datasinkCl, endField);
    jfieldID sizeField = env->GetStaticFieldID(datasinkCl, "AV_SEEK_SIZE", "I");
    SIZE = env->GetStaticIntField(datasinkCl, sizeField);

    jclass trackCl = env->FindClass(
            "com/googlecode/vicovre/codecs/ffmpeg/demuxer/FFMPEGTrack");
    jfieldID noFrameErrorField = env->GetStaticFieldID(trackCl,
            "NO_FRAME_ERROR", "I");
    NO_FRAME_ERROR = env->GetStaticIntField(trackCl, noFrameErrorField);
    jfieldID eofErrorField = env->GetStaticFieldID(trackCl,
            "EOF_ERROR", "I");
    EOF_ERROR = env->GetStaticIntField(trackCl, eofErrorField);
    jfieldID unknownErrorField = env->GetStaticFieldID(trackCl,
            "UNKNOWN_ERROR", "I");
    UNKNOWN_ERROR = env->GetStaticIntField(trackCl, unknownErrorField);
}

int readPacket(Demuxer *demuxer, uint8_t *buf, int size) {
    return demuxer->readDatasource(buf, size);
}

int64_t seek(Demuxer *demuxer, int64_t offset, int whence) {
    return demuxer->seekDatasource(offset, whence);
}

ByteIOContext *Demuxer::createIOContext(int bufferSize, bool seekable) {
    if (seekable) {
        ByteIOContext *data = av_alloc_put_byte(buffer, bufferSize, 0, this,
            (int (*) (void*, uint8_t *, int)) readPacket, NULL,
            (int64_t (*) (void*, int64_t, int)) seek);
        data->is_streamed = 0;
        return data;
    } else {
        ByteIOContext *data = av_alloc_put_byte(buffer, bufferSize, 0, this,
            (int (*) (void*, uint8_t *, int)) readPacket, NULL, NULL);
        data->is_streamed = 1;
        return data;
    }
}

#define PROBE_BUF_MIN 2048
#define PROBE_BUF_MAX (1<<20)

bool Demuxer::init(JNIEnv *env, jobject object, const char *filename,
        int bufferSize, bool seekable) {
    this->env = env;
    this->object = object;

    if (seekable) {
        env->CallVoidMethod(object, finishedProbeMethod);
    }

    AVProbeData pd;

    pd.filename = filename;
    pd.buf = NULL;
    pd.buf_size = 0;

    fmt = av_probe_input_format(&pd, 0);

    buffer = (uint8_t *) malloc(bufferSize);
    ByteIOContext *data = createIOContext(bufferSize, seekable);

    if (fmt == NULL) {
        int lastSize = 0;
        for (int size = PROBE_BUF_MIN; (size < PROBE_BUF_MAX)
                && (fmt == NULL); size <<= 1) {

            pd.buf = (uint8_t *) av_realloc(pd.buf,
                    size + AVPROBE_PADDING_SIZE);
            int bytesRead = get_buffer(data, pd.buf + lastSize,
                    size - lastSize);
            lastSize += bytesRead;
            pd.buf_size = lastSize;
            memset(pd.buf + pd.buf_size, 0, AVPROBE_PADDING_SIZE);
            fflush(stderr);
            fmt = av_probe_input_format(&pd, 1);
        }
    }

    if (fmt == NULL) {
        return false;
    }


    av_free(data);
    data = createIOContext(bufferSize, seekable);
    if (!seekable) {
        env->CallVoidMethod(object, finishedProbeMethod);
    } else {
        seekDatasource(0, SEEK_SET);
    }
    fprintf(stderr, "Found format = %s\n", fmt->name);
    fflush(stderr);
    int result = av_open_input_stream(&fmtContext, data, filename, fmt, NULL);
    if (result != 0) {
        fprintf(stderr, "Error opening stream: %i\n", result);
        return false;
    }
    fprintf(stderr, "Opened format\n");
    fflush(stderr);

    result = av_find_stream_info(fmtContext);
    if (result < 0) {
        fprintf(stderr, "Error finding stream information: %i\n", result);
        return false;
    }

    swScaleContext = (SwsContext **) malloc(sizeof(SwsContext *)
            * fmtContext->nb_streams);
    encodeContext = (AVCodecContext **) malloc(sizeof(AVCodecContext *)
            * fmtContext->nb_streams);
    decoder = (AVCodec **) malloc(sizeof(AVCodec *)
            * fmtContext->nb_streams);
    encoder = (AVCodec **) malloc(sizeof(AVCodec *)
            * fmtContext->nb_streams);
    intermediateFrame = (AVFrame **) malloc(sizeof(AVFrame *)
            * fmtContext->nb_streams);
    finalFrame = (AVFrame **) malloc(sizeof(AVFrame *)
            * fmtContext->nb_streams);
    outputPixelFormat = (PixelFormat *) malloc(sizeof(PixelFormat)
            * fmtContext->nb_streams);
    outputWidth = (int *) malloc(sizeof(int)
            * fmtContext->nb_streams);
    outputHeight = (int *) malloc(sizeof(int)
            * fmtContext->nb_streams);
    outputDataSize = (int *) malloc(sizeof(int)
            * fmtContext->nb_streams);
    currentTimestamp = (int64_t *) malloc(sizeof(int64_t)
            * fmtContext->nb_streams);
    streamQueue = (PktQueue **) malloc(sizeof(PktQueue *)
            * fmtContext->nb_streams);
    currentPacket = (AVPacket **) malloc(sizeof(AVPacket *)
            * fmtContext->nb_streams);
    currentPacketData = (uint8_t **) malloc(sizeof(uint8_t *)
            * fmtContext->nb_streams);
    currentPacketLength = (int *) malloc(sizeof(int)
                * fmtContext->nb_streams);
    memset(swScaleContext, 0, sizeof(SwsContext *)
            * fmtContext->nb_streams);
    memset(encodeContext, 0, sizeof(AVCodecContext *)
            * fmtContext->nb_streams);
    memset(decoder, 0, sizeof(AVCodec *)
            * fmtContext->nb_streams);
    memset(encoder, 0, sizeof(AVCodec *)
            * fmtContext->nb_streams);
    memset(intermediateFrame, 0, sizeof(AVFrame *)
            * fmtContext->nb_streams);
    memset(finalFrame, 0, sizeof(AVFrame *)
            * fmtContext->nb_streams);
    memset(currentTimestamp, 0, sizeof(uint64_t)
            * fmtContext->nb_streams);
    memset(currentPacket, 0, sizeof(AVPacket *)
                * fmtContext->nb_streams);
    memset(currentPacketData, 0, sizeof(uint8_t *)
                * fmtContext->nb_streams);
    memset(currentPacketLength, 0, sizeof(int)
                    * fmtContext->nb_streams);

    for (int i = 0; i < fmtContext->nb_streams; i++) {
        streamQueue[i] = new PktQueue();
    }
    inited = true;
    return true;
}

Demuxer::~Demuxer() {
    if (fmtContext != NULL) {
        for (int i = 0; i < fmtContext->nb_streams; i++) {
            if (swScaleContext[i] != NULL) {
                sws_freeContext(swScaleContext[i]);
            }
            if (encoder[i] != NULL) {
                avcodec_close(encodeContext[i]);
            }
            if (decoder[i] != NULL) {
                avcodec_close(fmtContext->streams[i]->codec);
            }
            if (intermediateFrame[i] != NULL) {
                av_free(intermediateFrame[i]);
            }
            if (finalFrame[i] != NULL) {
                av_free(finalFrame[i]);
            }
        }
        free(swScaleContext);
        free(encodeContext);
        free(decoder);
        free(encoder);
        free(intermediateFrame);
        free(finalFrame);
        free(outputPixelFormat);
        free(outputWidth);
        free(outputHeight);
        free(currentTimestamp);
        av_free(fmtContext->pb);
        av_close_input_stream(fmtContext);
    }

    free(buffer);
}

int Demuxer::readDatasource(uint8_t *buf, int size) {
    jobject buffer = env->CallObjectMethod(object, readNextBufferMethod,
            size);
    if (buffer != NULL) {
        jobject data = env->CallObjectMethod(buffer, getDataMethod);
        int offset = env->CallIntMethod(buffer, getOffsetMethod);
        int length = env->CallIntMethod(buffer, getLengthMethod);
        uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
            (jarray) data, 0);
        memcpy(buf, in + offset, length);
        env->ReleasePrimitiveArrayCritical((jarray) data, in, 0);
        return length;
    }
    return -1;
}

int64_t Demuxer::seekDatasource(int64_t offset, int whence) {
    int jwhence = 0;
    switch (whence) {
    case SEEK_SET:
        jwhence = SET;
        break;

    case SEEK_CUR:
        jwhence = CUR;
        break;

    case SEEK_END:
        jwhence = END;
        break;

    case AVSEEK_SIZE:
        jwhence = SIZE;
        break;

    default:
        return -1;
    }
    return env->CallLongMethod(object, seekMethod, offset, jwhence);
}

int Demuxer::getNoStreams() {
    if (fmtContext != NULL) {
        return fmtContext->nb_streams;
    }
    return 0;
}

int Demuxer::getCodec(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            return fmtContext->streams[stream]->codec->codec_id;
        }
    }
    return -1;
}

const char *Demuxer::getCodecName(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            return fmtContext->streams[stream]->codec->codec->name;
        }
    }
    return NULL;
}

int Demuxer::getCodecType(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            return fmtContext->streams[stream]->codec->codec_type;
        }
    }
    return -1;
}

bool Demuxer::setStreamOutputVideoFormat(int stream, int pixelFmt,
                int width, int height) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {

                decoder[stream] = avcodec_find_decoder(codecCtx->codec_id);
                if (decoder[stream] == NULL) {
                    return false;
                }
                if (avcodec_open(codecCtx, decoder[stream]) < 0) {
                    return false;
                }

                int newWidth = width;
                int newHeight = height;
                PixelFormat newPixelFmt = PixelFormat(pixelFmt);
                if (newWidth == -1) {
                    newWidth = codecCtx->width;
                }
                if (newHeight == -1) {
                    newHeight = codecCtx->height;
                }
                if (newPixelFmt == -1) {
                    newPixelFmt = codecCtx->pix_fmt;
                }

                /*if ((width != -1) || (height != -1) || (pixelFmt != -1)) { */

                    swScaleContext[stream] = sws_getContext(
                        codecCtx->width, codecCtx->height, codecCtx->pix_fmt,
                        newWidth, newHeight, newPixelFmt,
                        SWS_BICUBIC, NULL, NULL, NULL);

                    if (swScaleContext[stream] == NULL) {
                        return false;
                    }

                    intermediateFrame[stream] = avcodec_alloc_frame();
                //}
                finalFrame[stream] = avcodec_alloc_frame();

                outputPixelFormat[stream] = newPixelFmt;
                outputWidth[stream] = newWidth;
                outputHeight[stream] = newHeight;
                outputDataSize[stream] = avpicture_get_size(
                        outputPixelFormat[stream],
                        outputWidth[stream], outputHeight[stream]);
            }
        }
    }
    return false;
}

int Demuxer::getVideoPixelFormat(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
                return codecCtx->pix_fmt;
            }
        }
    }
}

double Demuxer::getVideoFrameRate(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
                return av_q2d(fmtContext->streams[stream]->r_frame_rate);
            }
        }
    }
}

int Demuxer::getVideoWidth(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
                return codecCtx->width;
            }
        }
    }
}

int Demuxer::getVideoHeight(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
                return codecCtx->height;
            }
        }
    }
}

bool Demuxer::setStreamAudioDecoded(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_AUDIO) {

                decoder[stream] = avcodec_find_decoder(codecCtx->codec_id);
                if (decoder[stream] == NULL) {
                    return false;
                }
                if (avcodec_open(codecCtx, decoder[stream]) < 0) {
                    return false;
                }
            }
        }
    }
}

int Demuxer::getAudioSampleSize(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_AUDIO) {
                return av_get_bits_per_sample_format(codecCtx->sample_fmt);
            }
        }
    }
}

int Demuxer::getNoAudioChannels(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_AUDIO) {
                return codecCtx->channels;
            }
        }
    }
}

int Demuxer::getAudioSampleRate(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_AUDIO) {
                return codecCtx->sample_rate;
            }
        }
    }
}

int Demuxer::getOutputSize(int stream) {
    if (fmtContext != NULL) {
        if (stream < fmtContext->nb_streams) {
            AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
            if (codecCtx->codec_type == CODEC_TYPE_AUDIO) {
                /*return codecCtx->sample_rate * codecCtx->channels
                    * (av_get_bits_per_sample_format(codecCtx->sample_fmt) / 8);
                */
                return AVCODEC_MAX_AUDIO_FRAME_SIZE;
            }
            if (decoder[stream] == NULL) {
                return codecCtx->width * codecCtx->height * 4;
            }
            return outputDataSize[stream];
        }
    }
    return 0;
}

int64_t Demuxer::getDuration() {
    return (fmtContext->duration * 1000000000) / AV_TIME_BASE;
}

int64_t Demuxer::getDuration(int stream) {
    return fmtContext->streams[stream]->duration * 1000000000
        * av_q2d(fmtContext->streams[stream]->time_base);
}

int64_t Demuxer::getStartTime(int stream) {
    return fmtContext->streams[stream]->start_time * 1000000000
        * av_q2d(fmtContext->streams[stream]->time_base);
}

int Demuxer::nextPacket(JNIEnv *env, jobject object) {
    this->env = env;
    this->object = object;

    int result = av_read_frame(fmtContext, &packet);
    if (result < 0) {
        fprintf(stderr, "Next packet failed: %i\n", result);
        fflush(stderr);
        return result;
    }
    int stream = packet.stream_index;
    streamQueue[stream]->addLast(&packet);
    return stream;
}

int64_t Demuxer::getTimestamp(AVPacket *packet, int stream) {
    int64_t timestamp = currentTimestamp[stream];

    if (packet->dts == (int64_t) AV_NOPTS_VALUE) {
        AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
        if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
            timestamp +=
                av_q2d(fmtContext->streams[stream]->r_frame_rate)
                    * 1000000000;
        } else {
            timestamp += lastAudioDataSize * 1000000000
                / codecCtx->channels / 2 / codecCtx->sample_rate;
        }

    } else {
        timestamp = packet->dts
            * av_q2d(fmtContext->streams[stream]->time_base)
            * 1000000000;
    }
    return timestamp;
}

int Demuxer::readNextFrame(JNIEnv *env, jobject object, jobject output,
        int stream) {

    if ((currentPacket[stream] == NULL) || (currentPacket[stream]->size == 0)) {
        if (currentPacket[stream] != NULL) {
            currentPacket[stream]->data = currentPacketData[stream];
            currentPacket[stream]->size = currentPacketLength[stream];
            av_free_packet(currentPacket[stream]);
        }
        currentPacket[stream] = streamQueue[stream]->removeFirst();
        if (currentPacket[stream] == NULL) {
            int lastStream = nextPacket(env, object);
            if (lastStream == -1) {
                return EOF_ERROR;
            } else if (lastStream < 0) {
                return UNKNOWN_ERROR;
            } else if (lastStream != stream) {
                return NO_FRAME_ERROR;
            }
            currentPacket[stream] = streamQueue[stream]->removeFirst();
        }
        if (currentPacket[stream] != NULL) {
            currentPacketData[stream] = currentPacket[stream]->data;
            currentPacketLength[stream] = currentPacket[stream]->size;
        }
    }

    int64_t timestamp = currentTimestamp[stream];

    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);
    int outlength = 0;
    int error = 0;

    if (decoder[stream] != NULL) {
        AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
        if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
            avpicture_fill((AVPicture *) finalFrame[stream], (out + outoffset),
                    outputPixelFormat[stream], outputWidth[stream],
                    outputHeight[stream]);
            if (intermediateFrame[stream] != NULL) {
                int frameFinished = 0;
                int bytesProcessed = avcodec_decode_video(codecCtx,
                        intermediateFrame[stream], &frameFinished,
                        currentPacket[stream]->data,
                        currentPacket[stream]->size);
                if (bytesProcessed > 0) {
                    currentPacket[stream]->data += bytesProcessed;
                    currentPacket[stream]->size -= bytesProcessed;
                    if (frameFinished > 0) {
                        int result = sws_scale(swScaleContext[stream],
                                intermediateFrame[stream]->data,
                                intermediateFrame[stream]->linesize,
                                0, codecCtx->height,
                                finalFrame[stream]->data,
                                finalFrame[stream]->linesize);
                        if (!result) {
                            error = UNKNOWN_ERROR;
                        }
                        timestamp = getTimestamp(currentPacket[stream], stream);
                        outlength = outputDataSize[stream];
                        int ticks = fmtContext->streams[stream]->parser ?
                            fmtContext->streams[stream]->parser->repeat_pict + 1
                            : codecCtx->ticks_per_frame;
                        currentPacket[stream]->dts += ((int64_t) AV_TIME_BASE *
                                codecCtx->time_base.num * ticks) /
                                codecCtx->time_base.den;
                    } else {
                        error = NO_FRAME_ERROR;
                    }
                } else {
                    error = UNKNOWN_ERROR;
                }
            } else {
                int frameFinished = 0;
                int bytesProcessed = avcodec_decode_video(codecCtx,
                        finalFrame[stream], &frameFinished,
                        currentPacket[stream]->data,
                        currentPacket[stream]->size);
                if (bytesProcessed > 0) {
                    timestamp = getTimestamp(currentPacket[stream], stream);
                    outlength = outputDataSize[stream];
                    currentPacket[stream]->data += bytesProcessed;
                    currentPacket[stream]->size -= bytesProcessed;

                    if (frameFinished < 0) {
                        error = NO_FRAME_ERROR;
                    } else {
                        int ticks = fmtContext->streams[stream]->parser ?
                            fmtContext->streams[stream]->parser->repeat_pict + 1
                            : codecCtx->ticks_per_frame;
                        currentPacket[stream]->dts += ((int64_t) AV_TIME_BASE *
                                codecCtx->time_base.num * ticks) /
                                codecCtx->time_base.den;
                    }
                } else {
                    error = UNKNOWN_ERROR;
                }
            }
        } else {
            int outputSize = AVCODEC_MAX_AUDIO_FRAME_SIZE;
            int bytesProcessed = avcodec_decode_audio2(codecCtx,
                (int16_t *) out + outoffset, &outputSize,
                currentPacket[stream]->data, currentPacket[stream]->size);

            if (bytesProcessed > 0) {
                timestamp = getTimestamp(currentPacket[stream], stream);
                outlength = outputSize;
                currentPacket[stream]->data += bytesProcessed;
                currentPacket[stream]->size -= bytesProcessed;
                currentPacket[stream]->dts +=
                    ((int64_t) AV_TIME_BASE / 2 * outputSize) /
                    (codecCtx->sample_rate * codecCtx->channels);
                lastAudioDataSize = outputSize;
            } else {
                error = UNKNOWN_ERROR;
            }
        }
    } else {
        memcpy(out, currentPacket[stream]->data, currentPacket[stream]->size);
        outlength = currentPacket[stream]->size;
        timestamp = getTimestamp(currentPacket[stream], stream);
        currentPacket[stream]->data += currentPacket[stream]->size;
        currentPacket[stream]->size = 0;
    }

    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);

    if (error == 0) {
        env->CallVoidMethod(output, setTimestampMethod, timestamp);
        env->CallVoidMethod(output, setLengthMethod, outlength);
        currentTimestamp[stream] = timestamp;
    }
    return error;
}

int64_t Demuxer::seekTo(JNIEnv *env, jobject object, int64_t position,
        int rounding) {
    this->env = env;
    this->object = object;

    int flags = AVSEEK_FLAG_BACKWARD;
    if (rounding == RoundUp) {
        flags = 0;
    }
    for (int i = 0; i < fmtContext->nb_streams; i++) {
        streamQueue[i]->clear();
        currentPacket[i] = NULL;
    }
    int64_t pos = (position / 1000000000) * AV_TIME_BASE;
    int result = av_seek_frame(fmtContext, -1, pos, flags);
    if (result < 0) {
        fprintf(stderr, "Failed to seek: %i\n", result);
        fflush(stderr);
        return result;
    }
    int stream = nextPacket(env, object);
    if (stream < 0) {
        fprintf(stderr, "Failed to read packet: %i\n", stream);
        fflush(stderr);
        return stream;
    }
    int64_t timestamp = getTimestamp(&packet, stream);
    return timestamp;
}
