/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_actionsmicro_airplay_AacEldDecoder */

#ifndef _Included_com_actionsmicro_airplay_AacEldDecoder
#define _Included_com_actionsmicro_airplay_AacEldDecoder
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_actionsmicro_airplay_AacEldDecoder
 * Method:    init
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldDecoder_init
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     com_actionsmicro_airplay_AacEldDecoder
 * Method:    decode
 * Signature: ([BI)[B
 */
JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldDecoder_decode
(JNIEnv* env, jclass clazz, jbyteArray data, jint length, jbyteArray out);

/*
 * Class:     com_actionsmicro_airplay_AacEldDecoder
 * Method:    release
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldDecoder_release
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
