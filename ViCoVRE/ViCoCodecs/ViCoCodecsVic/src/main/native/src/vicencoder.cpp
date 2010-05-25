#include "com_googlecode_vicovre_codecs_vic_NativeEncoder.h"
#include "config.h"
#include <string.h>
#include <stdint.h>
#include "vic/module.h"
#include "vic/video/grabber.h"
#include "vic/codec/encoder-h261.h"
#include "vic/codec/encoder-h261as.h"
#include "vic/codec/encoder-h264.h"

class VicEncoder {
    public:
        VicEncoder(JNIEnv *env, int codec);
        ~VicEncoder();
        void setSize(int w, int h);
        int encode(JNIEnv *env, jobject input, jobject output,
                jintArray frameOffsets, jintArray frameLengths);
        void keyFrame();
    private:
        TransmitterModule* codec_;
        Grabber *grabber_;

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
        jmethodID getTimestampMethod;
        jmethodID getFlagsMethod;
        jmethodID setDataMethod;
        jmethodID setOffsetMethod;
        jmethodID setLengthMethod;
        jmethodID setTimestampMethod;
        jmethodID setSequenceNumberMethod;
        jmethodID setFlagsMethod;

        int frameCount;
};

extern jlong ptr2jlong(void *ptr);

extern void *jlong2ptr(jlong jl);

JNIEXPORT jlong JNICALL
        Java_com_googlecode_vicovre_codecs_vic_NativeEncoder_openCodec(
            JNIEnv *env, jobject obj, jint codec) {
    VicEncoder* encoder = new VicEncoder(env, codec);
    return ptr2jlong(encoder);
}

JNIEXPORT void JNICALL
        Java_com_googlecode_vicovre_codecs_vic_NativeEncoder_setSize(
            JNIEnv *env, jobject obj, jlong ref, jint width, jint height) {
    VicEncoder *encoder = (VicEncoder *) jlong2ptr(ref);
    encoder->setSize(width, height);
}

JNIEXPORT jint JNICALL
        Java_com_googlecode_vicovre_codecs_vic_NativeEncoder_encode(
            JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output,
            jintArray frameOffsets, jintArray frameLengths) {
    VicEncoder *encoder = (VicEncoder *) jlong2ptr(ref);
    return encoder->encode(env, input, output, frameOffsets, frameLengths);
}

JNIEXPORT void JNICALL
        Java_com_googlecode_vicovre_codecs_vic_NativeEncoder_keyFrame(
            JNIEnv * env, jobject obj, jlong ref) {
    VicEncoder *encoder = (VicEncoder *) jlong2ptr(ref);
    encoder->keyFrame();
}

JNIEXPORT void JNICALL
        Java_com_googlecode_vicovre_codecs_vic_NativeEncoder_closeCodec(
            JNIEnv * env, jobject obj, jlong ref) {
    VicEncoder *encoder = (VicEncoder *) jlong2ptr(ref);
    delete encoder;
}


VicEncoder::VicEncoder(JNIEnv *env, int codec) {
    grabber_ = new Grabber();
    frameCount = 0;

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
    getTimestampMethod = env->GetMethodID(bufferCl, "getTimeStamp",
                "()J");
    getFlagsMethod = env->GetMethodID(bufferCl, "getFlags", "()I");
    setDataMethod = env->GetMethodID(bufferCl, "setData",
            "(Ljava/lang/Object;)V");
    setOffsetMethod = env->GetMethodID(bufferCl, "setOffset", "(I)V");
    setLengthMethod = env->GetMethodID(bufferCl, "setLength", "(I)V");
    setTimestampMethod = env->GetMethodID(bufferCl, "setTimeStamp", "(J)V");
    setSequenceNumberMethod = env->GetMethodID(bufferCl,
        "setSequenceNumber", "(J)V");
    setFlagsMethod = env->GetMethodID(bufferCl, "setFlags", "(I)V");

    jfieldID rtpMarker = env->GetStaticFieldID(bufferCl, "FLAG_RTP_MARKER", "I");
    jfieldID keyFrame = env->GetStaticFieldID(bufferCl, "FLAG_KEY_FRAME", "I");

    FLAG_RTP_MARKER = env->GetStaticIntField(bufferCl, rtpMarker);
    FLAG_KEY_FRAME = env->GetStaticIntField(bufferCl, keyFrame);

    codec_ = 0;
    switch (codec) {
    case 0:
        codec_ = new H261PixelEncoder();
        break;
    case 1:
        codec_ = new H261ASEncoder();
        break;
    case 2:
        codec_ = new H264Encoder();
    }
}

VicEncoder::~VicEncoder() {
    if (codec_ != 0) {
        delete codec_;
    }
    delete grabber_;
}

void VicEncoder::setSize(int w, int h) {
    grabber_->set_size(w, h);
    codec_->size(w, h);
}

void VicEncoder::keyFrame() {
    grabber_->reset();
}

int VicEncoder::encode(JNIEnv *env, jobject input, jobject output,
        jintArray frameOffsets, jintArray frameLengths) {
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    int outlength = env->CallIntMethod(output, getLengthMethod);

    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    long intimestamp = env->CallLongMethod(input, getTimestampMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
            (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
            (jarray) outdata, 0);

    uint32_t *offsets = (uint32_t *) env->GetPrimitiveArrayCritical(
                frameOffsets, 0);
    uint32_t *lengths = (uint32_t *) env->GetPrimitiveArrayCritical(
                    frameLengths, 0);

    u_char *crvec = grabber_->cr(in + inoffset);
    grabber_->saveblks(in + inoffset);
    YuvFrame *frame = new YuvFrame((u_int32_t) intimestamp, in + inoffset,
            crvec, grabber_->getInW(), grabber_->getInH());
    codec_->setData(out + outoffset, outlength);
    codec_->consume(frame);
    delete frame;

    pktbuf *buf = codec_->pktsToSend_->removeFirst();
    int i = 0;
    while (buf != 0) {
        offsets[i] = buf->dp - (out + outoffset);
        lengths[i] = buf->len;
        i++;
        buf = codec_->pktsToSend_->removeFirst();
    }
    codec_->pool_->close();

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
    env->ReleasePrimitiveArrayCritical(frameOffsets, offsets, 0);
    env->ReleasePrimitiveArrayCritical(frameLengths, lengths, 0);

    return BUFFER_PROCESSED_OK;
}
