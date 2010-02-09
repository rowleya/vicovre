#include "com_googlecode_vicovre_codecs_vic_NativeDecoder.h"
#include "config.h"
#include <string.h>
#include <stdint.h>
#include "vic/codec/decoder.h"
#include "vic/codec/decoder-jpeg.h"
#include "vic/codec/decoder-h261.h"
#include "vic/codec/decoder-h261as.h"

class VicDecoder {
    public:
        VicDecoder(JNIEnv *env, int codec);
        ~VicDecoder();
        int decode(JNIEnv *env, jobject input, jobject output);
        void decodeHeader(JNIEnv *env, jobject object, jobject input);
    private:
        Decoder* codec_;
        pktbuf *buf_;
        jobject indata_;

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

        jmethodID resizeMethod;

        int frameCount;

        void createBuffer(JNIEnv *env, jobject input, int rtpType);
        void releaseBuffer(JNIEnv *env);
};

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

JNIEXPORT jlong JNICALL Java_com_googlecode_vicovre_codecs_vic_NativeDecoder_openCodec(JNIEnv *env, jobject obj, jint codec) {
    VicDecoder* decoder = new VicDecoder(env, codec);
    return ptr2jlong(decoder);
}

JNIEXPORT void JNICALL Java_com_googlecode_vicovre_codecs_vic_NativeDecoder_decodeHeader(JNIEnv *env, jobject obj, jlong ref, jobject input) {
    VicDecoder *decoder = (VicDecoder *) jlong2ptr(ref);
    decoder->decodeHeader(env, obj, input);
}

JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_vic_NativeDecoder_decode(JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output) {
    VicDecoder *decoder = (VicDecoder *) jlong2ptr(ref);
    return decoder->decode(env, input, output);
}

JNIEXPORT void JNICALL Java_com_googlecode_vicovre_codecs_vic_NativeDecoder_closeCodec(JNIEnv * env, jobject obj, jlong ref) {
    VicDecoder *decoder = (VicDecoder *) jlong2ptr(ref);
    delete decoder;
}

VicDecoder::VicDecoder(JNIEnv *env, int codec) {
    frameCount = 0;
    buf_ = new pktbuf();
    buf_->rtp_header = (rtphdr *) malloc(sizeof(rtphdr));

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

    jclass decoderClass = env->FindClass(
            "com/googlecode/vicovre/codecs/vic/NativeDecoder");
    resizeMethod = env->GetMethodID(decoderClass, "resize", "(III)V");

    codec_ = 0;
    switch (codec) {
    case 0:
        codec_ = new MotionJpegDecoder();
        break;
    case 1:
        codec_ = new H261Decoder();
        break;
    case 2:
        codec_ = new H261ASDecoder();
    }
}

VicDecoder::~VicDecoder() {
    if (codec_ != 0) {
        delete codec_;
    }
    free(buf_->rtp_header);
    delete buf_;
}


void VicDecoder::createBuffer(JNIEnv *env, jobject input, int rtptype) {
    indata_ = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    int inlength = env->CallIntMethod(input, getLengthMethod);
    long sequenceNumber = env->CallLongMethod(input, getSequenceNumberMethod);
    long timestamp = env->CallLongMethod(input, getTimestampMethod);
    int flags = env->CallIntMethod(input, getFlagsMethod);

    buf_->dp = (uint8_t *) env->GetPrimitiveArrayCritical(
            (jarray) indata_, 0);
    buf_->dp += inoffset;
    buf_->len = inlength;
    buf_->rtp_header->rh_flags = 0x8000;
    if (flags & FLAG_RTP_MARKER) {
        buf_->rtp_header->rh_flags |= RTP_M;
    }
    buf_->rtp_header->rh_flags |= (rtptype & 0x7F);
    buf_->rtp_header->rh_seqno = (short) (sequenceNumber & 0xFFFF);
}

void VicDecoder::releaseBuffer(JNIEnv *env) {
    env->ReleasePrimitiveArrayCritical((jarray) indata_, buf_->dp, 0);
}

void VicDecoder::decodeHeader(JNIEnv *env, jobject object, jobject input) {
    createBuffer(env, input, 0);
    codec_->recvHeader(buf_);
    releaseBuffer(env);
    env->CallVoidMethod(object, resizeMethod, codec_->width(), codec_->height(),
            codec_->decimation());
}

int VicDecoder::decode(JNIEnv *env, jobject input, jobject output) {
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    int flags = env->CallIntMethod(input, getFlagsMethod);
    createBuffer(env, input, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);
    codec_->setOut(out + outoffset);
    codec_->recv(buf_);
    releaseBuffer(env);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);

    if (flags & FLAG_RTP_MARKER) {
        env->CallVoidMethod(output, setSequenceNumberMethod, frameCount++);
        env->CallVoidMethod(output, setTimestampMethod,
                env->CallLongMethod(input, getTimestampMethod));
        return BUFFER_PROCESSED_OK;
    }
    return OUTPUT_BUFFER_NOT_FILLED;
}
