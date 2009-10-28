#include "ffmpegj.h"
#include <string.h>

bool FFMpegJ::av_codec_initialized = false;

static void getSubSampleFactors(int *h, int *v, int format){
    switch(format){
    case PIX_FMT_UYVY422:
    case PIX_FMT_YUYV422:
        *h=1;
        *v=0;
        break;
    case PIX_FMT_YUV420P:
    case PIX_FMT_YUVA420P:
    case PIX_FMT_GRAY16BE:
    case PIX_FMT_GRAY16LE:
    case PIX_FMT_GRAY8: //FIXME remove after different subsamplings are fully implemented
    case PIX_FMT_NV12:
    case PIX_FMT_NV21:
        *h=1;
        *v=1;
        break;
    case PIX_FMT_YUV440P:
        *h=0;
        *v=1;
        break;
    case PIX_FMT_YUV410P:
        *h=2;
        *v=2;
        break;
    case PIX_FMT_YUV444P:
        *h=0;
        *v=0;
        break;
    case PIX_FMT_YUV422P:
        *h=1;
        *v=0;
        break;
    case PIX_FMT_YUV411P:
        *h=2;
        *v=0;
        break;
    default:
        *h=0;
        *v=0;
        break;
    }
}

jlong ptr2jlong(void *ptr) {
    jlong jl = 0;
    if (sizeof(void *) > sizeof(jlong)) {
        fprintf(stderr, "sizeof(void *) > sizeof(jlong)\n");
        return 0;
    }

    memcpy(&jl, &ptr, sizeof(void *));
    return jl;
}

void *jlong2ptr(jlong jl) {

    void *ptr = 0;
    if (sizeof(void *) > sizeof(jlong)) {
        fprintf(stderr, "sizeof(void *) > sizeof(jlong)\n");
        return 0;
    }

    memcpy(&ptr, &jl, sizeof(void *));
    return ptr;
}

static FFMpegJ *getFFMpegJ(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jmethodID getFFMpegJMethod = env->GetMethodID(cls, "getFFMpegJ", "()J");
    jlong ffmpegj = env->CallLongMethod(obj, getFFMpegJMethod);
    return (FFMpegJ *) jlong2ptr(ffmpegj);
}

JNIEXPORT jlong JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec_openCodec
        (JNIEnv *env, jobject obj, jboolean isEncoding, jint codecId,
        jint logLevel) {
    FFMpegJ *ffmpegj = new FFMpegJ(env, obj, logLevel);
    if (ffmpegj) {
        return ffmpegj->openCodec(isEncoding, codecId);
    }
    return true;
}

JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec_init
        (JNIEnv *env, jobject obj, jint pixFmt, jint width,
        jint height, jint convertToPixFmt, jint convertToWidth,
        jint convertToHeight, jboolean flipped, jstring rtpSdp,
        jobject context) {
    FFMpegJ *ffmpegj = getFFMpegJ(env, obj);
    ffmpegj->init(pixFmt, width, height, convertToPixFmt, convertToWidth,
        convertToHeight, flipped, env, rtpSdp, context);
    return ffmpegj->getOutputSize();
}

JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec_decodeNative
        (JNIEnv *env, jobject obj, jobject input, jobject output) {
    FFMpegJ *ffmpegj = getFFMpegJ(env, obj);
    return ffmpegj->decode(env, input, output);
}

JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec_encodeNative
        (JNIEnv *env, jobject obj, jobject input, jobject output) {
    FFMpegJ *ffmpegj = getFFMpegJ(env, obj);
    return ffmpegj->encode(env, input, output);
}

JNIEXPORT jboolean JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec_closeCodec
        (JNIEnv *env, jobject obj) {
    FFMpegJ *ffmpegj = getFFMpegJ(env, obj);
    ffmpegj->closeCodec();
    delete ffmpegj;
    return true;
}

JNIEXPORT void JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_FFMPEGCodec_getCodecContext
          (JNIEnv *env, jobject obj, jobject context) {
    FFMpegJ *ffmpegj = getFFMpegJ(env, obj);
    ffmpegj->getCodecContext(env, context);
}

FFMpegJ::FFMpegJ(JNIEnv *env, jobject peer, int logLevel) {
    intermediateFrame = NULL;
    frame = NULL;
    codecContext = NULL;
    buffer = NULL;

    avcodec_init();
    firstSequence = -1;
    av_log_set_level(logLevel);
    swinit = false;
    jclass cls = env->GetObjectClass(peer);
    jmethodID setFFMpegJMethod = env->GetMethodID(cls, "setFFMpegJ", "(J)V");
    env->CallVoidMethod(peer, setFFMpegJMethod, ptr2jlong(this));

    jclass plugin = env->FindClass("javax/media/PlugIn");
    jfieldID ok = env->GetStaticFieldID(plugin, "BUFFER_PROCESSED_OK", "I");
    jfieldID failed = env->GetStaticFieldID(plugin,
        "BUFFER_PROCESSED_FAILED", "I");
    jfieldID notConsumed = env->GetStaticFieldID(plugin,
        "INPUT_BUFFER_NOT_CONSUMED", "I");
    jfieldID notFilled = env->GetStaticFieldID(plugin,
        "OUTPUT_BUFFER_NOT_FILLED", "I");

    BUFFER_PROCESSED_OK = env->GetStaticIntField(plugin, ok);
    BUFFER_PROCESSED_FAILED = env->GetStaticIntField(plugin, failed);
    INPUT_BUFFER_NOT_CONSUMED = env->GetStaticIntField(plugin, notConsumed);
    OUTPUT_BUFFER_NOT_FILLED = env->GetStaticIntField(plugin, notFilled);

    jclass buffer = env->FindClass("javax/media/Buffer");
    getDataMethod = env->GetMethodID(buffer, "getData", "()Ljava/lang/Object;");
    getOffsetMethod = env->GetMethodID(buffer, "getOffset", "()I");
    getLengthMethod = env->GetMethodID(buffer, "getLength", "()I");
    getSequenceNumberMethod = env->GetMethodID(buffer, "getSequenceNumber",
            "()J");
    getFlagsMethod = env->GetMethodID(buffer, "getFlags", "()I");
    setOffsetMethod = env->GetMethodID(buffer, "setOffset", "(I)V");
    setLengthMethod = env->GetMethodID(buffer, "setLength", "(I)V");
    setTimestampMethod = env->GetMethodID(buffer, "setTimeStamp", "(J)V");
    setSequenceNumberMethod = env->GetMethodID(buffer,
        "setSequenceNumber", "(J)V");

    jfieldID rtpMarker = env->GetStaticFieldID(buffer, "FLAG_RTP_MARKER", "I");
    jfieldID keyFrame = env->GetStaticFieldID(buffer, "FLAG_KEY_FRAME", "I");

    FLAG_RTP_MARKER = env->GetStaticIntField(buffer, rtpMarker);
    FLAG_KEY_FRAME = env->GetStaticIntField(buffer, keyFrame);

    contextClass = env->FindClass(
            "com/googlecode/vicovre/codecs/ffmpeg/CodecContext");
    contextConstructor = env->GetMethodID(contextClass, "<init>", "()V");
    getCFlagsMethod = env->GetMethodID(contextClass, "getFlags", "()I");
    getFlags2Method = env->GetMethodID(contextClass, "getFlags2", "()I");
    getQmaxMethod = env->GetMethodID(contextClass, "getQmax", "()I");
    getQminMethod = env->GetMethodID(contextClass, "getQmin", "()I");
    getMaxQdiffMethod = env->GetMethodID(contextClass, "getMaxQdiff", "()I");
    getLowresMethod = env->GetMethodID(contextClass, "getLowres", "()I");
    getDctAlgoMethod = env->GetMethodID(contextClass, "getDctAlgo", "()I");
    getDebugMethod = env->GetMethodID(contextClass, "getDebug", "()I");
    setFlagsMethod = env->GetMethodID(contextClass, "setFlags", "(I)V");
    setFlags2Method = env->GetMethodID(contextClass, "setFlags2", "(I)V");
    setQmaxMethod = env->GetMethodID(contextClass, "setQmax", "(I)V");
    setQminMethod = env->GetMethodID(contextClass, "setQmin", "(I)V");
    setMaxQdiffMethod = env->GetMethodID(contextClass, "setMaxQdiff", "(I)V");
    setLowresMethod = env->GetMethodID(contextClass, "setLowres", "(I)V");
    setDctAlgoMethod = env->GetMethodID(contextClass, "setDctAlgo", "(I)V");
    setDebugMethod = env->GetMethodID(contextClass, "setDebug", "(I)V");
}

FFMpegJ::~FFMpegJ() {
    // Do Nothing
}

long FFMpegJ::openCodec(bool isEncoding, int codecId) {
    if (!av_codec_initialized) {
        avcodec_register_all();
        av_register_all();
        av_codec_initialized = true;
    }
    codecContext = avcodec_alloc_context();
    codecContext->codec_id = CodecID(codecId);
    codecContext->debug = 0;
    this->isEncoding = isEncoding;
    if (isEncoding) {
        codec = avcodec_find_encoder(codecContext->codec_id);
    } else {
        codec = avcodec_find_decoder(codecContext->codec_id);
    }
    if (codec) {
        return ptr2jlong(codecContext);
    }
    return 0;
}

bool FFMpegJ::init(int pixFmt, int width, int height, int intermediatePixFmt,
        int intermediateWidth, int intermediateHeight, bool flipped,
        JNIEnv *env, jstring rtpSdpString, jobject context) {
    this->pixFmt = PixelFormat(pixFmt);
    this->intermediatePixFmt = PixelFormat(intermediatePixFmt);
    this->width = width;
    this->height = height;
    this->flipped = flipped;
    if (context != NULL) {
        codecContext->flags = env->CallIntMethod(context, getCFlagsMethod);
        codecContext->flags2 = env->CallIntMethod(context, getFlags2Method);
        codecContext->qmin = env->CallIntMethod(context, getQminMethod);
        codecContext->qmax = env->CallIntMethod(context, getQmaxMethod);
        codecContext->max_qdiff = env->CallIntMethod(context, getMaxQdiffMethod);
        codecContext->lowres = env->CallIntMethod(context, getLowresMethod);
        codecContext->dct_algo = env->CallIntMethod(context, getDctAlgoMethod);
        codecContext->debug = env->CallIntMethod(context, getDebugMethod);
    }
    if (isEncoding) {
        codecContext->pix_fmt = this->intermediatePixFmt;
        codecContext->time_base.num = 1;
        codecContext->time_base.den = 90000;
    }
    codecContext->width = intermediateWidth;
    codecContext->height = intermediateHeight;
    codecContext->coded_width = intermediateWidth;
    codecContext->coded_height = intermediateHeight;
    int result = avcodec_open(codecContext, codec);
    if (result >= 0) {
        pictureSize = avpicture_get_size(this->pixFmt, width, height);
        frame = avcodec_alloc_frame();
        intermediateFrame = avcodec_alloc_frame();
        if (isEncoding) {
            int intermediatePictureSize = avpicture_get_size(
                codecContext->pix_fmt, codecContext->width,
                codecContext->height);
            buffer = (uint8_t *) av_malloc(
                intermediatePictureSize + 4);
            avpicture_fill((AVPicture *) intermediateFrame, buffer,
                codecContext->pix_fmt, codecContext->width,
                codecContext->height);
            frameCount = 0;
        } else if (rtpSdpString != NULL) {
            rtpHandler = RTPFirstDynamicPayloadHandler;
            while (rtpHandler != NULL) {
                if (rtpHandler->codec_id == codecContext->codec_id) {
                    break;
                }
                rtpHandler = rtpHandler->next;
            }
            if (rtpHandler != NULL) {
                rtpPayloadContext = rtpHandler->open();
                char *sdpLine = NULL;
                int nChar;
                rtpFormatContext = avformat_alloc_context();
                rtpStream = av_new_stream(rtpFormatContext, 0);
                rtpStream->codec = codecContext;
                jboolean isCopy;
                const char *rtpSdp = env->GetStringUTFChars(rtpSdpString,
                        &isCopy);
                while ((nChar = strcspn(rtpSdp, "\n")) != 0) {
                    sdpLine = (char*) realloc((void*) sdpLine, nChar + 1);
                    memset(sdpLine, '\0', nChar + 1);
                    strncpy(sdpLine, rtpSdp, nChar);
                    rtpSdp += nChar + 1;
                    rtpHandler->parse_sdp_a_line(rtpFormatContext, 0,
                            rtpPayloadContext, sdpLine);
                }
                env->ReleaseStringUTFChars(rtpSdpString, rtpSdp);
            }
        }
        return true;
    }
    return false;
}

int FFMpegJ::getOutputSize() {
    if (isEncoding) {
        return (codecContext->width * codecContext->height * 4) + 4;
    }
    return pictureSize + 4;
}

int FFMpegJ::decode(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    int inlength = env->CallIntMethod(input, getLengthMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    long sequence = env->CallLongMethod(input, getSequenceNumberMethod);
    int flags = env->CallIntMethod(input, getFlagsMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    uint8_t *inbytes = in + inoffset;
    int insize = inlength;

    /*if (parser != NULL) {
        uint8_t *data = NULL;
        int size = 0;
        int consumed = av_parser_parse(parser, codecContext, &data, &size,
                in + inoffset, inlength, 0, 0);
        if (size <= 0) {
            env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
            env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
            return OUTPUT_BUFFER_NOT_FILLED;
        } else {
            inbytes = data;
            insize = size;
        }
    } */

    if (rtpHandler != NULL) {
        if (abs(lastSequence - sequence) > 5) {
            firstSequence = -1;
        }
        lastSequence = sequence;

        if (firstSequence == -1) {
            firstSequence = sequence;
            lastSequence = firstSequence;
            for (int i = 0; i < 1024; i++) {
                if (rtpPackets[i] != NULL) {
                    free(rtpPackets[i]);
                }
                rtpPackets[i] = NULL;
            }
        }
        int index = sequence - firstSequence;
        if (index < 0) {
            index = ((0xFFFF - firstSequence) + sequence);
        }
        rtpPackets[index] = new AVPacket;
        AVPacket *rtpPacket = rtpPackets[index];

        uint32_t timestamp;
        int rtpFlags = 0;
        if (flags & FLAG_KEY_FRAME) {
            rtpFlags |= RTP_FLAG_KEY;
        }
        if (flags & FLAG_RTP_MARKER) {
            rtpFlags |= RTP_FLAG_MARKER;
        }
        int result = rtpHandler->parse_packet(rtpFormatContext,
                rtpPayloadContext, rtpStream, rtpPacket, &timestamp, inbytes,
                insize, rtpFlags);
        if (result < 0) {
            env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
            env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
            return OUTPUT_BUFFER_NOT_FILLED;
        } else {
            if ((flags & FLAG_RTP_MARKER) == 0) {
                env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
                env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
                return OUTPUT_BUFFER_NOT_FILLED;
            }

            firstSequence = -1;

            int noPackets = index + 1;
            int totalSize = 0;
            for (int i = 0; i < noPackets; i++) {
                if (rtpPackets[i] == NULL) {
                    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
                    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
                    return OUTPUT_BUFFER_NOT_FILLED;
                }
                totalSize += rtpPackets[i]->size;
            }

            //rtpData = (uint8_t *) realloc(rtpData, totalSize);
            uint8_t *dest = rtpData;
            for (int i = 0; i < noPackets; i++) {
                memcpy(dest, rtpPackets[i]->data, rtpPackets[i]->size);
                dest += rtpPackets[i]->size;
            }

            inbytes = rtpData;
            insize = totalSize;
        }
    }

    avpicture_fill((AVPicture *) frame, (out + outoffset),
        pixFmt, width, height);
    frameFinished = 0;
    bytesProcessed = avcodec_decode_video(codecContext, intermediateFrame,
        &frameFinished, inbytes, insize);

    if ((bytesProcessed > 0) && (frameFinished > 0)) {
        if (!swinit) {
            swScaleContext = sws_getContext(codecContext->width,
                codecContext->height, codecContext->pix_fmt,
                width, height, pixFmt, SWS_BICUBIC, NULL, NULL, NULL);
            swinit = true;
        }
        int srcStride2[4] = {intermediateFrame->linesize[0],
                intermediateFrame->linesize[1],
                intermediateFrame->linesize[2]};
        uint8_t* src2[4]= {intermediateFrame->data[0],
                intermediateFrame->data[1], intermediateFrame->data[2]};
        if (flipped) {
            int vsub = 0;
            int hsub = 0;
            getSubSampleFactors(&hsub, &vsub, codecContext->pix_fmt);
            srcStride2[0] = -intermediateFrame->linesize[0];
            srcStride2[1] = -intermediateFrame->linesize[1];
            srcStride2[2] = -intermediateFrame->linesize[2];
            src2[0] += (height - 1) * intermediateFrame->linesize[0];
            src2[1] += ((height >> vsub) - 1) * intermediateFrame->linesize[1];
            src2[2] += ((height >> vsub) - 1) * intermediateFrame->linesize[2];
        }
        int result = sws_scale(swScaleContext, intermediateFrame->data,
            intermediateFrame->linesize, 0, codecContext->height,
            frame->data, frame->linesize);
    }

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
    fflush(stderr);
    fflush(stdout);

    if (bytesProcessed < 0) {
        return OUTPUT_BUFFER_NOT_FILLED;
    }

    if (frameFinished) {
        frameCount += 1;
        if (frame->pts != -1) {
            frameCount = frame->pts;
        }
        env->CallVoidMethod(output, setSequenceNumberMethod, frameCount);
        env->CallVoidMethod(output, setTimestampMethod,
            (frameCount * codecContext->time_base.num) /
            codecContext->time_base.den);

        if (bytesProcessed < inlength) {
            env->CallVoidMethod(input, setOffsetMethod,
                inoffset + bytesProcessed);
            env->CallVoidMethod(input, setLengthMethod,
                inlength - bytesProcessed);
            return INPUT_BUFFER_NOT_CONSUMED;
        }
        return BUFFER_PROCESSED_OK;
    }
    return OUTPUT_BUFFER_NOT_FILLED;
}

int FFMpegJ::encode(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    int inlength = env->CallIntMethod(input, getLengthMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    int outlength = env->CallIntMethod(output, getLengthMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    avpicture_fill((AVPicture *) frame, (in + inoffset), pixFmt, width, height);
    if (intermediateFrame) {
        if (!swinit) {
            swScaleContext = sws_getContext(width, height, pixFmt,
                codecContext->width, codecContext->height, intermediatePixFmt,
                SWS_BICUBIC, NULL, NULL, NULL);
            swinit = true;
        }
        int srcStride2[4] = {frame->linesize[0], frame->linesize[1],
                frame->linesize[2]};
        uint8_t* src2[4]= {frame->data[0], frame->data[1], frame->data[2]};
        if (flipped) {
            int vsub = 0;
            int hsub = 0;
            getSubSampleFactors(&hsub, &vsub, pixFmt);
            srcStride2[0] = -frame->linesize[0];
            srcStride2[1] = -frame->linesize[1];
            srcStride2[2] = -frame->linesize[2];
            src2[0] += (height - 1) * frame->linesize[0];
            src2[1] += ((height >> vsub) - 1) * frame->linesize[1];
            src2[2] += ((height >> vsub) - 1) * frame->linesize[2];
        }
        int result = sws_scale(swScaleContext, src2, srcStride2,
            0, height, intermediateFrame->data, intermediateFrame->linesize);
        bytesProcessed = avcodec_encode_video(
            codecContext, (out + outoffset), outlength, intermediateFrame);
    } else {
        bytesProcessed = avcodec_encode_video(
            codecContext, (out + outoffset), outlength, frame);
    }

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);

    if (bytesProcessed < 0) {
        return BUFFER_PROCESSED_FAILED;
    }

    frameCount += 1;
    if (frame->pts > 0) {
        frameCount = frame->pts;
    }
    env->CallVoidMethod(output, setLengthMethod, bytesProcessed);
    env->CallVoidMethod(output, setSequenceNumberMethod, frameCount);

    if (bytesProcessed < inlength) {
        env->CallVoidMethod(input, setOffsetMethod,
            inoffset + bytesProcessed);
        env->CallVoidMethod(input, setLengthMethod,
            inlength - bytesProcessed);
        return INPUT_BUFFER_NOT_CONSUMED;
    }
    return BUFFER_PROCESSED_OK;
}

bool FFMpegJ::closeCodec() {
    if (intermediateFrame != NULL) {
        av_free(intermediateFrame);
        intermediateFrame = NULL;
    }
    if (frame != NULL) {
        av_free(frame);
        frame = NULL;
    }
    if (codecContext != NULL) {
        avcodec_close(codecContext);
        codecContext = NULL;
    }
    /*
    if (buffer != NULL) {
        av_free(buffer);
        buffer = NULL;
    } */
    return true;
}

void FFMpegJ::getCodecContext(JNIEnv *env, jobject context) {
    env->CallVoidMethod(context, setFlagsMethod, codecContext->flags);
    env->CallVoidMethod(context, setFlags2Method, codecContext->flags2);
    env->CallVoidMethod(context, setQmaxMethod, codecContext->qmax);
    env->CallVoidMethod(context, setQminMethod, codecContext->qmin);
    env->CallVoidMethod(context, setMaxQdiffMethod, codecContext->max_qdiff);
    env->CallVoidMethod(context, setLowresMethod, codecContext->lowres);
    env->CallVoidMethod(context, setDctAlgoMethod, codecContext->dct_algo);
    env->CallVoidMethod(context, setDebugMethod, codecContext->debug);
}
