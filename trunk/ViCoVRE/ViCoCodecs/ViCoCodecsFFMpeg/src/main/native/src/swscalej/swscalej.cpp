#include "com_googlecode_vicovre_codecs_ffmpeg_SWScaleCodec.h"

#define INT64_C(val) val##LL
#define UINT64_C(val) val##ULL

extern "C" {
#include <libavutil/avutil.h>
#include <libswscale/swscale.h>
}

typedef struct AVFrame {
    uint8_t *data[4];
    int linesize[4];
} AVFrame;

class SWScaleJ {
    private:
        int BUFFER_PROCESSED_OK;
        int BUFFER_PROCESSED_FAILED;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;

        int frameCount;
        SwsContext *swScaleContext;
        AVFrame *inFrame;
        AVFrame *outFrame;
        int inWidth;
        int inHeight;
        int pixFmtIn;
        int outWidth;
        int outHeight;
        int pixFmtOut;

    public:
        SWScaleJ(JNIEnv *env, int pixFmtIn, int inWidth, int inHeight,
                int pixFmtOut, int outWidth, int outHeight);
        ~SWScaleJ();
        int process(JNIEnv *env, jobject input, jobject output);
};

void getLinesize(AVFrame *frame, enum PixelFormat pix_fmt, int width,
        bool flipped) {
    int multiplier = 1;
    if (flipped) {
        multiplier = -1;
    }
    frame->linesize[0] = 0;
    frame->linesize[1] = 0;
    frame->linesize[2] = 0;
    frame->linesize[3] = 0;
    switch(pix_fmt) {
        case PIX_FMT_YUV420P:
        case PIX_FMT_YUV422P:
            frame->linesize[0] = multiplier * width;
            frame->linesize[1] = multiplier * (width / 2);
            frame->linesize[2] = multiplier * (width / 2);
            break;
        case PIX_FMT_YUV444P:
            frame->linesize[0] = multiplier * width;
            frame->linesize[1] = multiplier * width;
            frame->linesize[2] = multiplier * width;
            break;
        case PIX_FMT_YUV411P:
            frame->linesize[0] = multiplier * width;
            frame->linesize[1] = multiplier * (width / 4);
            frame->linesize[2] = multiplier * (width / 4);
            break;
        case PIX_FMT_YUYV422:
        case PIX_FMT_UYVY422:
            frame->linesize[0] = multiplier * (width * 2);
            break;

        case PIX_FMT_RGB24:
        case PIX_FMT_BGR24:
            frame->linesize[0] = multiplier * (width * 3);
            break;
        case PIX_FMT_BGR32_1:
        case PIX_FMT_RGB32_1:
        case PIX_FMT_RGB32:
        case PIX_FMT_BGR32:
            frame->linesize[0] = multiplier * (width * 4);
            break;
        case PIX_FMT_BGR565:
        case PIX_FMT_BGR555:
        case PIX_FMT_GRAY16BE:
        case PIX_FMT_GRAY16LE:
            frame->linesize[0] = multiplier * (width * 2);
            break;
        case PIX_FMT_RGB8:
        case PIX_FMT_RGB4_BYTE:
        case PIX_FMT_BGR8:
        case PIX_FMT_BGR4_BYTE:
        case PIX_FMT_GRAY8:
            frame->linesize[0] = multiplier * width;
            frame->linesize[1] = 4;
            break;
    }
}

void getData(AVFrame *frame, uint8_t *inData, enum PixelFormat pix_fmt,
        int width, int height, bool flipped) {
    frame->data[0] = NULL;
    frame->data[1] = NULL;
    frame->data[2] = NULL;
    frame->data[3] = NULL;
    switch(pix_fmt) {
        case PIX_FMT_YUV420P:
            frame->data[0] = inData;
            frame->data[1] = frame->data[0] + (width * height);
            frame->data[2] = frame->data[1] + ((width / 2) * (height / 2));
            break;
        case PIX_FMT_YUV422P:
            frame->data[0] = inData;
            frame->data[1] = frame->data[0] + (width * height);
            frame->data[2] = frame->data[1] + ((width / 2) * height);
            break;
        case PIX_FMT_YUV444P:
            frame->data[0] = inData;
            frame->data[1] = frame->data[0] + (width * height);
            frame->data[2] = frame->data[1] + (width * height);
            break;
        case PIX_FMT_YUV411P:
            frame->data[0] = inData;
            frame->data[1] = frame->data[0] + (width * height);
            frame->data[2] = frame->data[1] + ((width / 4) * height);
            break;
        case PIX_FMT_YUYV422:
        case PIX_FMT_UYVY422:
            frame->data[0] = inData;
            break;

        case PIX_FMT_RGB24:
        case PIX_FMT_BGR24:
            frame->data[0] = inData;
            if (flipped) {
                frame->data[0] += (height - 1) * width * 3;
            }
            break;
        case PIX_FMT_BGR32_1:
        case PIX_FMT_RGB32_1:
        case PIX_FMT_RGB32:
        case PIX_FMT_BGR32:
            frame->data[0] = inData;
            if (flipped) {
                frame->data[0] += (height - 1) * width * 4;
            }
            break;
        case PIX_FMT_BGR565:
        case PIX_FMT_BGR555:
        case PIX_FMT_GRAY16BE:
        case PIX_FMT_GRAY16LE:
            frame->data[0] = inData;
            if (flipped) {
                frame->data[0] += (height - 1) * width * 2;
            }
            break;
        case PIX_FMT_RGB8:
        case PIX_FMT_RGB4_BYTE:
        case PIX_FMT_BGR8:
        case PIX_FMT_BGR4_BYTE:
        case PIX_FMT_GRAY8:
            frame->data[0] = inData;
            if (flipped) {
                frame->data[0] += (height - 1) * width;
            }
            break;
        default:
            fprintf(stderr, "Unknown pixel format %i\n", pix_fmt);
            fflush(stderr);
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

JNIEXPORT jlong JNICALL
        Java_com_googlecode_vicovre_codecs_ffmpeg_SWScaleCodec_openCodec(
            JNIEnv *env, jobject obj,
            jint pixFmtIn, jint inWidth, jint inHeight,
            jint pixFmtOut, jint outWidth, jint outHeight) {
    SWScaleJ *scaler = new SWScaleJ(env,
            pixFmtIn, inWidth, inHeight, pixFmtOut, outWidth, outHeight);
    return ptr2jlong(scaler);
}

JNIEXPORT jint JNICALL
        Java_com_googlecode_vicovre_codecs_ffmpeg_SWScaleCodec_process(
            JNIEnv *env, jobject obj,
            jlong ref, jobject input, jobject output) {
    SWScaleJ *scaler = (SWScaleJ *) jlong2ptr(ref);
    return scaler->process(env, input, output);
}

JNIEXPORT void JNICALL
        Java_com_googlecode_vicovre_codecs_ffmpeg_SWScaleCodec_closeCodec(
                JNIEnv *env, jobject obj, jlong ref) {
    SWScaleJ *scaler = (SWScaleJ *) jlong2ptr(ref);
    delete scaler;
}

SWScaleJ::SWScaleJ(JNIEnv *env, int pixFmtIn, int inWidth, int inHeight,
        int pixFmtOut, int outWidth, int outHeight) {
    frameCount = 0;

    jclass plugin = env->FindClass("javax/media/PlugIn");
    jfieldID ok = env->GetStaticFieldID(plugin, "BUFFER_PROCESSED_OK", "I");
    jfieldID failed = env->GetStaticFieldID(plugin,
        "BUFFER_PROCESSED_FAILED", "I");

    BUFFER_PROCESSED_OK = env->GetStaticIntField(plugin, ok);
    BUFFER_PROCESSED_FAILED = env->GetStaticIntField(plugin, failed);

    jclass bufferCl = env->FindClass("javax/media/Buffer");
    getDataMethod = env->GetMethodID(bufferCl, "getData",
            "()Ljava/lang/Object;");
    getOffsetMethod = env->GetMethodID(bufferCl, "getOffset", "()I");

    swScaleContext = sws_getContext(inWidth, inHeight, PixelFormat(pixFmtIn),
            outWidth, outHeight, PixelFormat(pixFmtOut),
            SWS_BICUBIC, NULL, NULL, NULL);
    inFrame = (AVFrame *) malloc(sizeof(AVFrame));
    getLinesize(inFrame, PixelFormat(pixFmtIn), inWidth, false);
    outFrame = (AVFrame *) malloc(sizeof(AVFrame));
    getLinesize(outFrame, PixelFormat(pixFmtOut), outWidth, false);
    this->inWidth = inWidth;
    this->inHeight = inHeight;
    this->pixFmtIn = pixFmtIn;
    this->outWidth = outWidth;
    this->outHeight = outHeight;
    this->pixFmtOut = pixFmtOut;
}

SWScaleJ::~SWScaleJ() {
    sws_freeContext(swScaleContext);
    free(inFrame);
    free(outFrame);
}

int SWScaleJ::process(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    getData(inFrame, in + inoffset, PixelFormat(pixFmtIn),
            inWidth, inHeight, false);
    getData(outFrame, out + outoffset, PixelFormat(pixFmtOut),
            outWidth, outHeight, false);

    int result = sws_scale(swScaleContext, inFrame->data, inFrame->linesize, 0,
            inHeight, outFrame->data, outFrame->linesize);

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);

    if (result < 0) {
        return BUFFER_PROCESSED_FAILED;
    }
    return BUFFER_PROCESSED_OK;

}
