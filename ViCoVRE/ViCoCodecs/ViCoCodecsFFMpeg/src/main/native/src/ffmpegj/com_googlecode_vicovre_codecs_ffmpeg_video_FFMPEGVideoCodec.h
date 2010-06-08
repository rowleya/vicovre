/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec */

#ifndef _Included_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
#define _Included_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    open
 * Signature: (ZII)J
 */
JNIEXPORT jlong JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_open
  (JNIEnv *, jobject, jboolean, jint, jint);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    fillInCodecContext
 * Signature: (JLcom/googlecode/vicovre/codecs/ffmpeg/video/VideoCodecContext;)V
 */
JNIEXPORT void JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_fillInCodecContext
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    init
 * Signature: (JLcom/googlecode/vicovre/codecs/ffmpeg/video/VideoCodecContext;)I
 */
JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_init
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    decode
 * Signature: (JLjavax/media/Buffer;Ljavax/media/Buffer;)I
 */
JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_decode
  (JNIEnv *, jobject, jlong, jobject, jobject);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    decodeFirst
 * Signature: (JLjavax/media/Buffer;Lcom/googlecode/vicovre/codecs/ffmpeg/video/VideoCodecContext;)I
 */
JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_decodeFirst
  (JNIEnv *, jobject, jlong, jobject, jobject);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    encode
 * Signature: (JLjavax/media/Buffer;Ljavax/media/Buffer;)I
 */
JNIEXPORT jint JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_encode
  (JNIEnv *, jobject, jlong, jobject, jobject);

/*
 * Class:     com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_googlecode_vicovre_codecs_ffmpeg_video_FFMPEGVideoCodec_close
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif