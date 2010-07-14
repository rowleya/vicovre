#include "com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec.h"

#define INT64_C(val) val##LL
#define UINT64_C(val) val##ULL

extern "C" {
#include "libavcodec/avcodec.h"
}

extern jlong ptr2jlong(void *ptr);
extern void *jlong2ptr(jlong jl);

class Audio {

    public:
        Audio(JNIEnv *env);
        ~Audio();
        int open(bool encode, int codecId, int logLevel);
        void fillInCodecContext(JNIEnv *env, jobject context);
        int init(JNIEnv *env, jobject context);
        int encode(JNIEnv *env, jobject input, jobject output);
        int decode(JNIEnv *env, jobject input, jobject output);

    private:
        jint BUFFER_PROCESSED_OK;
        jint BUFFER_PROCESSED_FAILED;
        jint INPUT_BUFFER_NOT_CONSUMED;
        jint OUTPUT_BUFFER_NOT_FILLED;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;
        jmethodID getLengthMethod;
        jmethodID getTimestampMethod;
        jmethodID setOffsetMethod;
        jmethodID setLengthMethod;
        jmethodID setTimestampMethod;
        jmethodID setSequenceNumberMethod;

        jmethodID getChannelsMethod;
        jmethodID getSampleRateMethod;
        jmethodID getBitRateMethod;
        jmethodID getCompressionLevelMethod;
        jmethodID getGlobalQualityMethod;
        jmethodID getFlagsMethod;
        jmethodID getFlags2Method;
        jmethodID setChannelsMethod;
        jmethodID setSampleRateMethod;
        jmethodID setBitRateMethod;
        jmethodID setCompressionLevelMethod;
        jmethodID setGlobalQualityMethod;
        jmethodID setFlagsMethod;
        jmethodID setFlags2Method;
        jmethodID setFrameSizeMethod;

        jmethodID createExtraDataMethod;

        AVCodec *codec;
        AVCodecContext *codecContext;
        bool isEncoding;
        long sequenceNumber;
};

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec_open
        (JNIEnv *env, jobject obj, jboolean encode, jint codecId, jint logLevel) {
    Audio *audio = new Audio(env);
    int result = audio->open(encode, codecId, logLevel);
    if (result < 0) {
        return result;
    }
    return ptr2jlong(audio);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec_fillInCodecContext
        (JNIEnv *env, jobject obj, jlong ref, jobject context) {
    Audio *audio = (Audio *) jlong2ptr(ref);
    audio->fillInCodecContext(env, context);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec_init
        (JNIEnv *env, jobject obj, jlong ref, jobject context) {
    Audio *audio = (Audio *) jlong2ptr(ref);
    return audio->init(env, context);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec_encode
        (JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output) {
    Audio *audio = (Audio *) jlong2ptr(ref);
    return audio->encode(env, input, output);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec_decode
        (JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output) {
    Audio *audio = (Audio *) jlong2ptr(ref);
    return audio->decode(env, input, output);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_FFMPEGAudioCodec_close
        (JNIEnv *env, jobject obj, jlong ref) {
    Audio *audio = (Audio *) jlong2ptr(ref);
    delete audio;
}

Audio::Audio(JNIEnv *env) {
    sequenceNumber = 0;

    jclass contextClass = env->FindClass(
                "com/googlecode/vicovre/codecs/ffmpeg/audio/AudioCodecContext");
    getChannelsMethod = env->GetMethodID(contextClass, "getChannels", "()I");
    getSampleRateMethod = env->GetMethodID(contextClass, "getSampleRate",
            "()I");
    getBitRateMethod = env->GetMethodID(contextClass, "getBitRate", "()I");
    getCompressionLevelMethod = env->GetMethodID(contextClass,
            "getCompressionLevel", "()I");
    getGlobalQualityMethod = env->GetMethodID(contextClass, "getGlobalQuality",
            "()I");
    getFlagsMethod = env->GetMethodID(contextClass, "getFlags", "()I");
    getFlags2Method = env->GetMethodID(contextClass, "getFlags2", "()I");
    setChannelsMethod = env->GetMethodID(contextClass, "setChannels", "(I)V");
    setSampleRateMethod = env->GetMethodID(contextClass, "setSampleRate",
            "(I)V");
    setBitRateMethod = env->GetMethodID(contextClass, "setBitRate", "(I)V");
    setCompressionLevelMethod = env->GetMethodID(contextClass,
            "setCompressionLevel", "(I)V");
    setGlobalQualityMethod = env->GetMethodID(contextClass, "setGlobalQuality",
            "(I)V");
    setFlagsMethod = env->GetMethodID(contextClass, "setFlags", "(I)V");
    setFlags2Method = env->GetMethodID(contextClass, "setFlags2", "(I)V");
    setFrameSizeMethod = env->GetMethodID(contextClass, "setFrameSize", "(I)V");

    createExtraDataMethod = env->GetMethodID(contextClass,
                "createExtraData", "(I)[B");

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
    getTimestampMethod = env->GetMethodID(bufferCl, "getTimeStamp", "()J");
    setOffsetMethod = env->GetMethodID(bufferCl, "setOffset", "(I)V");
    setLengthMethod = env->GetMethodID(bufferCl, "setLength", "(I)V");
    setTimestampMethod = env->GetMethodID(bufferCl, "setTimeStamp", "(J)V");
    setSequenceNumberMethod = env->GetMethodID(bufferCl,
        "setSequenceNumber", "(J)V");
}

Audio::~Audio() {
    avcodec_close(codecContext);
}


int Audio::open(bool encode, int codecId, int logLevel) {
    avcodec_init();
    av_log_set_level(logLevel);
    avcodec_register_all();

    codecContext = avcodec_alloc_context();
    codecContext->codec_id = CodecID(codecId);
    codecContext->codec_type = CODEC_TYPE_AUDIO;
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

void Audio::fillInCodecContext(JNIEnv *env, jobject context) {
    env->CallVoidMethod(context, setChannelsMethod, codecContext->channels);
    env->CallVoidMethod(context, setSampleRateMethod,
            codecContext->sample_rate);
    env->CallVoidMethod(context, setBitRateMethod, codecContext->bit_rate);
    env->CallVoidMethod(context, setCompressionLevelMethod,
            codecContext->compression_level);
    env->CallVoidMethod(context, setGlobalQualityMethod,
            codecContext->global_quality);
    env->CallVoidMethod(context, setFlagsMethod, codecContext->flags);
    env->CallVoidMethod(context, setFlags2Method, codecContext->flags2);
}

int Audio::init(JNIEnv *env, jobject context) {
    codecContext->channels = env->CallIntMethod(context, getChannelsMethod);
    codecContext->sample_rate = env->CallIntMethod(context,
            getSampleRateMethod);
    codecContext->bit_rate = env->CallIntMethod(context, getBitRateMethod);
    codecContext->compression_level = env->CallIntMethod(context,
            getCompressionLevelMethod);
    codecContext->global_quality = env->CallIntMethod(context,
            getGlobalQualityMethod);
    codecContext->flags = env->CallIntMethod(context, getFlagsMethod);
    codecContext->flags2 = env->CallIntMethod(context, getFlags2Method);

    int result = avcodec_open(codecContext, codec);
    if (result >= 0) {
        if (isEncoding) {
            env->CallVoidMethod(context, setFrameSizeMethod,
                    codecContext->frame_size);
            if (codecContext->extradata_size > 0) {
                jarray jextradata = (jarray) env->CallObjectMethod(context,
                        createExtraDataMethod, codecContext->extradata_size);
                uint8_t *extradata = (uint8_t *) env->GetPrimitiveArrayCritical(
                        (jarray) jextradata, 0);
                memcpy(extradata, codecContext->extradata,
                        codecContext->extradata_size);
                env->ReleasePrimitiveArrayCritical(jextradata, extradata, 0);
            }
        }
        return AVCODEC_MAX_AUDIO_FRAME_SIZE;
    }
    return result;
}

int Audio::encode(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);
    int outlength = env->CallIntMethod(output, getLengthMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    int bytesEncoded = avcodec_encode_audio(codecContext, out + outoffset,
            outlength, (const short int*) (in + inoffset));
    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);
    if (bytesEncoded < 0) {
        fprintf(stderr, "Error encoding: %i\n", bytesEncoded);
        return BUFFER_PROCESSED_FAILED;
    }

    env->CallVoidMethod(output, setLengthMethod, bytesEncoded);
    return BUFFER_PROCESSED_OK;
}

int Audio::decode(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    int inlength = env->CallIntMethod(input, getLengthMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);

    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
            (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    int outputSize = AVCODEC_MAX_AUDIO_FRAME_SIZE;
    int bytesProcessed = avcodec_decode_audio2(codecContext,
            (int16_t *) (out + outoffset), &outputSize,
            in + inoffset, inlength);

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);

    if (bytesProcessed > 0) {
        env->CallVoidMethod(output, setLengthMethod, bytesProcessed);
        env->CallVoidMethod(output, setTimestampMethod,
                    (sequenceNumber * codecContext->time_base.num) /
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

    fprintf(stderr, "Error decoding %i\n", bytesProcessed);
    return BUFFER_PROCESSED_FAILED;
}
