#include "com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec.h"

#define INT64_C(val) val##LL

extern "C" {
#include "libavcodec/avcodec.h"
}

extern jlong ptr2jlong(void *ptr);
extern void *jlong2ptr(jlong jl);

class Resample {
    public:
        Resample(JNIEnv *env);
        int open(int inputRate, int outputRate,
                int inputChannels, int outputChannels);
        int process(JNIEnv *env, jobject input, jobject output);
        ~Resample();

    private:
        jint BUFFER_PROCESSED_OK;
        jint BUFFER_PROCESSED_FAILED;
        jint INPUT_BUFFER_NOT_CONSUMED;
        jint OUTPUT_BUFFER_NOT_FILLED;

        jmethodID getDataMethod;
        jmethodID getOffsetMethod;
        jmethodID getLengthMethod;
        jmethodID setLengthMethod;

        ReSampleContext *context;
        int inChannels;
        int outChannels;
};

JNIEXPORT jlong JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec_open
        (JNIEnv *env, jobject obj, jint inputRate, jint outputRate,
                jint inputChannels, jint outputChannels) {
    Resample *resample = new Resample(env);
    int result = resample->open(inputRate, outputRate,
            inputChannels, outputChannels);
    if (result < 0) {
        return result;
    }
    return ptr2jlong(resample);
}

JNIEXPORT jint JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec_process
        (JNIEnv *env, jobject obj, jlong ref, jobject input, jobject output) {
    Resample *resample = (Resample *) jlong2ptr(ref);
    return resample->process(env, input, output);
}

JNIEXPORT void JNICALL
    Java_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec_close
        (JNIEnv *env, jobject obj, jlong ref) {
    Resample *resample = (Resample *) jlong2ptr(ref);
    delete resample;
}

Resample::Resample(JNIEnv *env) {
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
    setLengthMethod = env->GetMethodID(bufferCl, "setLength", "(I)V");
}

int Resample::open(int inputRate, int outputRate,
        int inputChannels, int outputChannels) {

    context = av_audio_resample_init(outputChannels, inputChannels,
            outputRate, inputRate, SAMPLE_FMT_S16, SAMPLE_FMT_S16,
            16, 10, 0, 0.8);
    inChannels = inputChannels;
    outChannels = outputChannels;
    if (context <= 0) {
        return -1;
    }
    return 0;
}

int Resample::process(JNIEnv *env, jobject input, jobject output) {
    jobject indata = env->CallObjectMethod(input, getDataMethod);
    int inoffset = env->CallIntMethod(input, getOffsetMethod);
    int inlength = env->CallIntMethod(input, getLengthMethod);
    jobject outdata = env->CallObjectMethod(output, getDataMethod);
    int outoffset = env->CallIntMethod(output, getOffsetMethod);

    int inSamples = inlength / (2 * inChannels);
    uint8_t *in = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) indata, 0);
    uint8_t *out = (uint8_t *) env->GetPrimitiveArrayCritical(
        (jarray) outdata, 0);

    int samples = audio_resample(context, (short *) (out + outoffset),
            (short *) (in + inoffset), inSamples);

    env->ReleasePrimitiveArrayCritical((jarray) indata, in, 0);
    env->ReleasePrimitiveArrayCritical((jarray) outdata, out, 0);

    if (samples < 0) {
        return BUFFER_PROCESSED_FAILED;
    }

    int outBytes = samples * 2 * outChannels;
    env->CallVoidMethod(output, setLengthMethod, outBytes);
    return BUFFER_PROCESSED_OK;
}

Resample::~Resample() {

}
