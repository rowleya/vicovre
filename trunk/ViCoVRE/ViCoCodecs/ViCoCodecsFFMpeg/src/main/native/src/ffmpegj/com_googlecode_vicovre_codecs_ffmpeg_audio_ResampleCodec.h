/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec */

#ifndef _Included_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec
#define _Included_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec
#ifdef __cplusplus
extern "C" {
#endif
/* Inaccessible static: FORMAT */
/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec
 * Method:    open
 * Signature: (IIII)J
 */
JNIEXPORT jlong JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec_open
  (JNIEnv *, jobject, jint, jint, jint, jint);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec
 * Method:    process
 * Signature: (JLjavax/media/Buffer;Ljavax/media/Buffer;)I
 */
JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec_process
  (JNIEnv *, jobject, jlong, jobject, jobject);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_audio_ResampleCodec_close
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
