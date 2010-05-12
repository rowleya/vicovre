#include "com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec.h"

#define INT64_C(val) val##LL

extern "C" {
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavformat/rtpdec.h"
}

extern jlong ptr2jlong(void *ptr);
extern void *jlong2ptr(jlong jl);

class Video {
public:
        Video(JNIEnv *env);
        ~Video();
        int open(bool encode, int codecId, int logLevel);
        void fillInCodecContext(JNIEnv *env, jobject context);
        int init(JNIEnv *env, jobject context);
        int encode(JNIEnv *env, jobject input, jobject output);
        int decodeFirst(JNIEnv *env, jobject input, jobject context);
        int decode(JNIEnv *env, jobject input, jobject output);

    private:
        int decodeVideo(JNIEnv *env, jobject input);

        jint BUFFER_PROCESSED_OK;
        jint BUFFER_PROCESSED_FAILED;
        jint INPUT_BUFFER_NOT_CONSUMED;
        jint OUTPUT_BUFFER_NOT_FILLED;

        jint FLAG_RTP_MARKER;
        jint FLAG_KEY_FRAME;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;
        jmethodID getLengthMethod;
        jmethodID getSequenceNumberMethod;
        jmethodID getBufferFlagsMethod;
        jmethodID getTimestampMethod;
        jmethodID setOffsetMethod;
        jmethodID setLengthMethod;
        jmethodID setTimestampMethod;
        jmethodID setSequenceNumberMethod;

        jmethodID getFlagsMethod;
        jmethodID getFlags2Method;
        jmethodID getQMinMethod;
        jmethodID getQMaxMethod;
        jmethodID getMaxQDiffMethod;
        jmethodID getLowResMethod;
        jmethodID getDctAlgoMethod;
        jmethodID getDebugMethod;
        jmethodID getBitRateMethod;
        jmethodID getMaxRateMethod;
        jmethodID getInputWidthMethod;
        jmethodID getInputHeightMethod;
        jmethodID getOutputWidthMethod;
        jmethodID getOutputHeightMethod;
        jmethodID getPixelFormatMethod;
        jmethodID setFlagsMethod;
        jmethodID setFlags2Method;
        jmethodID setQMinMethod;
        jmethodID setQMaxMethod;
        jmethodID setMaxQDiffMethod;
        jmethodID setLowResMethod;
        jmethodID setDctAlgoMethod;
        jmethodID setDebugMethod;
        jmethodID setBitRateMethod;
        jmethodID setMaxRateMethod;
        jmethodID setInputWidthMethod;
        jmethodID setInputHeightMethod;
        jmethodID setOutputWidthMethod;
        jmethodID setOutputHeightMethod;
        jmethodID setPixelFormatMethod;

        jmethodID setOutputDataSizeMethod;

        AVCodec *codec;
        AVCodecContext *codecContext;
        SwsContext *scaleContext;
        AVFrame *frame;
        AVFrame *scaleFrame;
        uint8_t *scaleBuffer;
        bool isEncoding;
        bool firstDecodeRead;
        int outputDataSize;

        int inputWidth;
        int inputHeight;
        int outputWidth;
        int outputHeight;
        PixelFormat pixelFormat;
};

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_open
        (JNIEnv *env, jobject obj, jboolean encode, jint codecId, jint logLevel) {
    Video *video = new Video(env);
    int result = video->open(encode, codecId, logLevel);
    if (result < 0) {
        return result;
    }
    return ptr2jlong(video);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_fillInCodecContext
        (JNIEnv *env, jobject obj, jlong ref, jobject context) {
    Video *video = (Video *) jlong2ptr(ref);
    video->fillInCodecContext(env, context);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_init
        (JNIEnv *env, jobject obj, jlong ref, jobject context) {
    Video *video = (Video *) jlong2ptr(ref);
    return video->init(env, context);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_encode
        (JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output) {
    Video *video = (Video *) jlong2ptr(ref);
    return video->encode(env, input, output);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_decode
        (JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output) {
    Video *video = (Video *) jlong2ptr(ref);
    return video->decode(env, input, output);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_decodeFirst
        (JNIEnv *env, jobject obj, jlong ref, jobject input, jobject context) {
    Video *video = (Video *) jlong2ptr(ref);
    return video->decodeFirst(env, input, context);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_close
        (JNIEnv *env, jobject obj, jlong ref) {
    Video *video = (Video *) jlong2ptr(ref);
    delete video;
}

Video::Video(JNIEnv *env) {
    scaleContext = NULL;
    firstDecodeRead = false;

    jclass contextClass = env->FindClass(
                "com/googlecode/vicovre/codecs/ffmpeg/video/VideoCodecContext");
    getFlagsMethod = env->GetMethodID(contextClass, "getFlags", "()I");
    getFlags2Method = env->GetMethodID(contextClass, "getFlags2", "()I");
    getQMinMethod = env->GetMethodID(contextClass, "getQmin", "()I");
    getQMaxMethod = env->GetMethodID(contextClass, "getQmax", "()I");
    getMaxQDiffMethod = env->GetMethodID(contextClass, "getMaxQdiff", "()I");
    getLowResMethod = env->GetMethodID(contextClass, "getLowres", "()I");
    getDctAlgoMethod = env->GetMethodID(contextClass, "getDctAlgo", "()I");
    getDebugMethod = env->GetMethodID(contextClass, "getDebug", "()I");
    getBitRateMethod = env->GetMethodID(contextClass, "getBitrate", "()I");
    getMaxRateMethod = env->GetMethodID(contextClass, "getMaxrate", "()I");
    getInputWidthMethod = env->GetMethodID(contextClass,
            "getInputWidth", "()I");
    getInputHeightMethod = env->GetMethodID(contextClass,
            "getInputHeight", "()I");
    getOutputWidthMethod = env->GetMethodID(contextClass,
            "getOutputWidth", "()I");
    getOutputHeightMethod = env->GetMethodID(contextClass,
            "getOutputHeight", "()I");
    getPixelFormatMethod = env->GetMethodID(contextClass,
            "getPixelFmt", "()I");

    setFlagsMethod = env->GetMethodID(contextClass, "setFlags", "(I)V");
    setFlags2Method = env->GetMethodID(contextClass, "setFlags2", "(I)V");
    setQMinMethod = env->GetMethodID(contextClass, "setQmin", "(I)V");
    setQMaxMethod = env->GetMethodID(contextClass, "setQmax", "(I)V");
    setMaxQDiffMethod = env->GetMethodID(contextClass, "setMaxQdiff", "(I)V");
    setLowResMethod = env->GetMethodID(contextClass, "setLowres", "(I)V");
    setDctAlgoMethod = env->GetMethodID(contextClass, "setDctAlgo", "(I)V");
    setDebugMethod = env->GetMethodID(contextClass, "setDebug", "(I)V");
    setBitRateMethod = env->GetMethodID(contextClass, "setBitrate", "(I)V");
    setMaxRateMethod = env->GetMethodID(contextClass, "setMaxrate", "(I)V");
    setInputWidthMethod = env->GetMethodID(contextClass,
            "setInputWidth", "(I)V");
    setInputHeightMethod = env->GetMethodID(contextClass,
            "setInputHeight", "(I)V");
    setOutputWidthMethod = env->GetMethodID(contextClass,
            "setOutputWidth", "(I)V");
    setOutputHeightMethod = env->GetMethodID(contextClass,
            "setOutputHeight", "(I)V");
    setPixelFormatMethod = env->GetMethodID(contextClass,
            "setPixelFmt", "(I)V");

    setOutputDataSizeMethod = env->GetMethodID(contextClass,
                "setOutputDataSize", "(I)V");

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

    jclass bufferCl = env->FindClass("javax/media/Buffer");
    getDataMethod = env->GetMethodID(bufferCl, "getData",
            "()Ljava/lang/Object;");
    getOffsetMethod = env->GetMethodID(bufferCl, "getOffset", "()I");
    getLengthMethod = env->GetMethodID(bufferCl, "getLength", "()I");
    getSequenceNumberMethod = env->GetMethodID(bufferCl, "getSequenceNumber",
                "()J");
    getBufferFlagsMethod = env->GetMethodID(bufferCl, "getFlags", "()I");
    getTimestampMethod = env->GetMethodID(bufferCl, "getTimeStamp", "()J");
    setOffsetMethod = env->GetMethodID(bufferCl, "setOffset", "(I)V");
    setLengthMethod = env->GetMethodID(bufferCl, "setLength", "(I)V");
    setTimestampMethod = env->GetMethodID(bufferCl, "setTimeStamp", "(J)V");
    setSequenceNumberMethod = env->GetMethodID(bufferCl,
        "setSequenceNumber", "(J)V");

    jfieldID rtpMarker = env->GetStaticFieldID(bufferCl,
            "FLAG_RTP_MARKER", "I");
    jfieldID keyFrame = env->GetStaticFieldID(bufferCl,
            "FLAG_KEY_FRAME", "I");
    FLAG_RTP_MARKER = env->GetStaticIntField(bufferCl, rtpMarker);
    FLAG_KEY_FRAME = env->GetStaticIntField(bufferCl, keyFrame);
}

Video::~Video() {
    avcodec_close(codecContext);
}

int Video::open(bool encode, int codecId, int logLevel) {
    avcodec_init();
    av_log_set_level(logLevel);
    avcodec_register_all();
    av_register_all();

    codecContext = avcodec_alloc_context();
    codecContext->codec_id = CodecID(codecId);
    codecContext->debug = 0;
    isEncoding = encode;
    if (isEncoding) {
        codec = avcodec_find_encoder(codecContext->codec_id);
    } else {
        codec = avcodec_find_decoder(codecContext->codec_id);
    }
    if (codec) {
        return 0;
    }
    return -1;
}

void Video::fillInCodecContext(JNIEnv *env, jobject context) {
    env->CallVoidMethod(context, setFlagsMethod, codecContext->flags);
    env->CallVoidMethod(context, setFlags2Method, codecContext->flags2);
    env->CallVoidMethod(context, setQMinMethod, codecContext->qmin);
    env->CallVoidMethod(context, setQMaxMethod, codecContext->qmax);
    env->CallVoidMethod(context, setMaxQDiffMethod, codecContext->max_qdiff);
    env->CallVoidMethod(context, setLowResMethod, codecContext->lowres);
    env->CallVoidMethod(context, setDctAlgoMethod, codecContext->dct_algo);
    env->CallVoidMethod(context, setDebugMethod, codecContext->debug);
    env->CallVoidMethod(context, setBitRateMethod, codecContext->bit_rate);
}

int Video::init(JNIEnv *env, jobject context) {
    codecContext->flags = env->CallIntMethod(context, getFlagsMethod);
    codecContext->flags2 = env->CallIntMethod(context, getFlags2Method);
    codecContext->debug = env->CallIntMethod(context, getDebugMethod);

    inputWidth = env->CallIntMethod(context, getInputWidthMethod);
    inputHeight = env->CallIntMethod(context, getInputHeightMethod);
    outputWidth = env->CallIntMethod(context, getOutputWidthMethod);
    outputHeight = env->CallIntMethod(context, getOutputHeightMethod);
    pixelFormat = PixelFormat(env->CallIntMethod(context,
            getPixelFormatMethod));
    if (isEncoding) {
        codecContext->width = outputWidth;
        codecContext->height = outputHeight;
        codecContext->coded_width = codecContext->width;
        codecContext->coded_height = codecContext->height;
        codecContext->pix_fmt = codec->pix_fmts[0];

        codecContext->time_base.num = 1;
        codecContext->time_base.den = 90000;

        codecContext->qmax = env->CallIntMethod(context, getQMaxMethod);
        codecContext->qmin = env->CallIntMethod(context, getQMinMethod);
        codecContext->max_qdiff = env->CallIntMethod(context,
                getMaxQDiffMethod);
        codecContext->lowres = env->CallIntMethod(context, getLowResMethod);
        codecContext->dct_algo = env->CallIntMethod(context, getDctAlgoMethod);
        codecContext->bit_rate = env->CallIntMethod(context, getBitRateMethod);
        if (codecContext->rc_max_rate > 0) {
            codecContext->rc_buffer_size = codecContext->bit_rate
                    * av_q2d(codecContext->time_base);
        }
    }

    int result = avcodec_open(codecContext, codec);
    if (result >= 0) {
        frame = avcodec_alloc_frame();
        scaleFrame = avcodec_alloc_frame();
        if (isEncoding) {
            int scalePictureSize = avpicture_get_size(
                codecContext->pix_fmt, codecContext->width,
                codecContext->height);
            scaleBuffer = (uint8_t *) av_malloc(scalePictureSize + 4);
            avpicture_fill((AVPicture *) scaleFrame, scaleBuffer,
                            codecContext->pix_fmt, codecContext->width,
                            codecContext->height);
            scaleContext = sws_getContext(
                    inputWidth, inputHeight, pixelFormat,
                    outputWidth, outputHeight, codecContext->pix_fmt,
                    SWS_BICUBIC, NULL, NULL, NULL);
            return outputWidth * outputHeight * 4;
        } else {
            return 0;
        }
    }
    return result;
}

int Video::encode(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    int outlength = env->CallIntMethod(output, getLengthMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    avpicture_fill((AVPicture *) frame, (in + inoffset), pixelFormat,
            inputWidth, inputHeight);
    sws_scale(scaleContext, frame->data, frame->linesize,
            0, inputHeight, scaleFrame->data, scaleFrame->linesize);
    int bytesEncoded = avcodec_encode_video(codecContext, (out + outoffset),
            outlength, scaleFrame);

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
    if (bytesEncoded < 0) {
        return BUFFER_PROCESSED_FAILED;
    }

    env->CallVoidMethod(output, setLengthMethod, bytesEncoded);
    return BUFFER_PROCESSED_OK;
}

int Video::decodeVideo(JNIEnv *env, jobject input) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    int inlength = env->CallIntMethod(input, getLengthMethod);
    int flags = env->CallIntMethod(input, getBufferFlagsMethod);
    long sequence = env->CallLongMethod(input, getSequenceNumberMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
                (jarray) indata, 0);

    int bytesProcessed = 0;
    int frameFinished = 0;
    bytesProcessed = avcodec_decode_video(codecContext, scaleFrame,
        &frameFinished, in + inoffset, inlength);
    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);

    if (bytesProcessed > 0) {
        if (frameFinished > 0) {
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

    return BUFFER_PROCESSED_FAILED;
}

int Video::decodeFirst(JNIEnv *env, jobject input, jobject context) {
    int result = decodeVideo(env, input);
    if ((result == INPUT_BUFFER_NOT_CONSUMED)
                || (result == BUFFER_PROCESSED_OK)) {
        inputWidth = codecContext->width;
        inputHeight = codecContext->height;
        env->CallVoidMethod(context, setInputWidthMethod, inputWidth);
        env->CallVoidMethod(context, setInputHeightMethod, inputHeight);
        if (outputWidth == 0) {
            outputWidth = inputWidth;
            env->CallVoidMethod(context, setOutputWidthMethod, outputWidth);
        }
        if (outputHeight == 0) {
            outputHeight = inputHeight;
            env->CallVoidMethod(context, setOutputHeightMethod, outputHeight);
        }
        outputDataSize = avpicture_get_size(pixelFormat, outputWidth,
                outputHeight);
        env->CallVoidMethod(context, setOutputDataSizeMethod,
                outputDataSize + 4);

        scaleContext = sws_getContext(
                    inputWidth, inputHeight, codecContext->pix_fmt,
                    outputWidth, outputHeight, pixelFormat,
                    SWS_BICUBIC, NULL, NULL, NULL);

    } else if (result == BUFFER_PROCESSED_FAILED) {
        return OUTPUT_BUFFER_NOT_FILLED;
    }

    return result;
}

int Video::decode(JNIEnv *env, jobject input, jobject output) {
    int result = 0;
    if (firstDecodeRead) {
        result = decodeVideo(env, input);
    } else {
        firstDecodeRead = true;
        result = INPUT_BUFFER_NOT_CONSUMED;
    }

    if ((result == INPUT_BUFFER_NOT_CONSUMED)
            || (result == BUFFER_PROCESSED_OK)) {
        jobject outdata = env->CallObjectMethod(output, getDataMethod);
        int outoffset = env->CallIntMethod(output, getOffsetMethod);

        uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
            (jarray) outdata, 0);
        avpicture_fill((AVPicture *) frame, (out + outoffset),
            pixelFormat, outputWidth, outputHeight);
        sws_scale(scaleContext, scaleFrame->data, scaleFrame->linesize, 0,
                inputHeight, frame->data, frame->linesize);

        env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
        env->CallVoidMethod(output, setLengthMethod, outputDataSize);
    } else if (result == BUFFER_PROCESSED_FAILED) {
        return OUTPUT_BUFFER_NOT_FILLED;
    }

    return result;
}
