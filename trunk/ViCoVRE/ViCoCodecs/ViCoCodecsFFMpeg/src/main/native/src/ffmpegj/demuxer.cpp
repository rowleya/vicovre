#include "com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer.h"

#define INT64_C(val) val##LL

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

    private:
        PktQueueElement *first;
        PktQueueElement *last;

};

class Demuxer {
    public:
        Demuxer(JNIEnv *env, jobject object);
        ~Demuxer();
        bool init(JNIEnv *env, jobject object, const char *filename,
                int bufferSize);
        int readDatasource(uint8_t *buf, int size);
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
        bool readNextFrame(JNIEnv *env, jobject object, jobject output,
                int stream);

    private:
        int nextPacket(JNIEnv *env, jobject object);

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
        AVPacket packet;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;
        jmethodID getLengthMethod;
        jmethodID setTimestampMethod;
        jmethodID setLengthMethod;
        jmethodID readNextBufferMethod;
        jmethodID finishedProbeMethod;
};

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_init
        (JNIEnv *env, jobject object, jstring filename, jint bufferSize) {
    fprintf(stderr, "Init called\n");
    fflush(stderr);
    Demuxer *demuxer = new Demuxer(env, object);
    const char *fname = env->GetStringUTFChars(filename, NULL);
    bool result = demuxer->init(env, object, fname, bufferSize);
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

JNIEXPORT jboolean JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_demuxer_FFMPEGDemuxer_readNextFrame
      (JNIEnv *env, jobject object, jlong ref, jobject buffer, jint stream) {
    Demuxer *demuxer = (Demuxer *) jlong2ptr(ref);
    return demuxer->readNextFrame(env, object, buffer, stream);
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

    jclass bufferCl = env->FindClass("javax/media/Buffer");
    getDataMethod = env->GetMethodID(bufferCl, "getData",
            "()Ljava/lang/Object;");
    getOffsetMethod = env->GetMethodID(bufferCl, "getOffset", "()I");
    getLengthMethod = env->GetMethodID(bufferCl, "getLength", "()I");
    setTimestampMethod = env->GetMethodID(bufferCl, "setTimeStamp", "(J)V");
    setLengthMethod = env->GetMethodID(bufferCl, "setLength", "(I)V");
}

int readPacket(Demuxer *demuxer, uint8_t *buf, int size) {
    return demuxer->readDatasource(buf, size);
}

#define PROBE_BUF_MIN 2048
#define PROBE_BUF_MAX (1<<20)

bool Demuxer::init(JNIEnv *env, jobject object, const char *filename,
        int bufferSize) {
    this->env = env;
    this->object = object;

    AVProbeData pd;

    pd.filename = filename;
    pd.buf = NULL;
    pd.buf_size = 0;

    fmt = av_probe_input_format(&pd, 0);

    buffer = (uint8_t *) malloc(bufferSize);
    ByteIOContext *data = av_alloc_put_byte(buffer, bufferSize, 0, this,
            (int (*) (void*, uint8_t *, int)) readPacket, NULL, NULL);
    data->is_streamed = 1;

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
    data = av_alloc_put_byte(buffer, bufferSize, 0, this,
                (int (*) (void*, uint8_t *, int)) readPacket, NULL, NULL);
    data->is_streamed = 1;
    env->CallVoidMethod(object, finishedProbeMethod);
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

                swScaleContext[stream] = sws_getContext(
                    codecCtx->width, codecCtx->height, codecCtx->pix_fmt,
                    newWidth, newHeight, newPixelFmt,
                    SWS_BICUBIC, NULL, NULL, NULL);

                if (swScaleContext[stream] == NULL) {
                    return false;
                }

                intermediateFrame[stream] = avcodec_alloc_frame();
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
                fprintf(stderr, "%f\n", av_q2d(fmtContext->streams[stream]->r_frame_rate));
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
        return result;
    }
    int stream = packet.stream_index;
    fprintf(stderr, "Read packet from stream %i (%i bytes)\n", stream, packet.size);
    fflush(stderr);
    streamQueue[stream]->addLast(&packet);
    return stream;
}

bool Demuxer::readNextFrame(JNIEnv *env, jobject object, jobject output,
        int stream) {
    AVPacket *packet = streamQueue[stream]->removeFirst();
    if (packet == NULL) {
        int lastStream = nextPacket(env, object);
        /*while ((lastStream >= 0) && (lastStream != stream)) {
            lastStream = nextPacket(env, object);
        }
        if (lastStream < 0) {
            return false;
        } */
        if (lastStream != stream) {
            return false;
        }
        packet = streamQueue[stream]->removeFirst();
    }

    int64_t timestamp = currentTimestamp[stream];

    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);
    int outlength = 0;
    bool error = false;

    if (decoder[stream] != NULL) {
        AVCodecContext *codecCtx = fmtContext->streams[stream]->codec;
        if (codecCtx->codec_type == CODEC_TYPE_VIDEO) {
            avpicture_fill((AVPicture *) finalFrame[stream], (out + outoffset),
                    outputPixelFormat[stream], outputWidth[stream],
                    outputHeight[stream]);
            int frameFinished = 0;
            int bytesProcessed = avcodec_decode_video(codecCtx,
                    intermediateFrame[stream], &frameFinished,
                    packet->data, packet->size);
            if ((bytesProcessed > 0) && (frameFinished > 0)) {
                int result = sws_scale(swScaleContext[stream],
                        intermediateFrame[stream]->data,
                        intermediateFrame[stream]->linesize,
                        0, codecCtx->height,
                        finalFrame[stream]->data,
                        finalFrame[stream]->linesize);
                if (!result) {
                    error = true;
                } else {
                    if (packet->dts == (int64_t) AV_NOPTS_VALUE) {
                        timestamp +=
                            av_q2d(fmtContext->streams[stream]->r_frame_rate)
                                * 1000000000;
                    } else {
                        timestamp = packet->dts
                            * av_q2d(fmtContext->streams[stream]->time_base)
                            * 1000000000;
                    }
                    outlength = outputDataSize[stream];
                }
            } else {
                error = true;
            }
        } else {
            int outputSize = AVCODEC_MAX_AUDIO_FRAME_SIZE;
            fprintf(stderr, "Decoding audio\n");
            fflush(stderr);
            int bytesProcessed = avcodec_decode_audio2(codecCtx,
                (int16_t *) out + outoffset, &outputSize,
                packet->data, packet->size);

            fprintf(stderr, "Finished decoding audio\n");
            fflush(stderr);

            if (bytesProcessed > 0) {
                if (packet->dts == (int64_t) AV_NOPTS_VALUE) {
                    timestamp += lastAudioDataSize * 1000000000
                        / codecCtx->channels / 2 / codecCtx->sample_rate;
                } else {
                    timestamp = packet->dts
                        * av_q2d(fmtContext->streams[stream]->time_base)
                        * 1000000000;
                }
                lastAudioDataSize = outputSize;
                outlength = outputSize;
            } else {
                error = true;
            }
        }
    } else {
        memcpy(out, packet->data, packet->size);
        outlength = packet->size;
    }

    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
    av_free_packet(packet);

    if (!error) {
        env->CallVoidMethod(output, setTimestampMethod, timestamp);
        env->CallVoidMethod(output, setLengthMethod, outlength);
        fprintf(stderr, "Reading stream %i, timestamp = %i\n", stream, timestamp / 1000000);
        fflush(stderr);
        currentTimestamp[stream] = timestamp;
    }
    stream = -1;
    return !error;
}
