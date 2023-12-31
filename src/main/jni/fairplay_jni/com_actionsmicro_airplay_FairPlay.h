/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_actionsmicro_airplay_FairPlay */

#ifndef _Included_com_actionsmicro_airplay_FairPlay
#define _Included_com_actionsmicro_airplay_FairPlay
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_actionsmicro_airplay_FairPlay
 * Method:    fp_setup_init
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1setup_1init
  (JNIEnv *, jclass);

/*
 * Class:     com_actionsmicro_airplay_FairPlay
 * Method:    fp_setup_phase1
 * Signature: ([BIZ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1setup_1phase1
  (JNIEnv *, jclass, jbyteArray, jint, jboolean);

/*
 * Class:     com_actionsmicro_airplay_FairPlay
 * Method:    fp_setup_phase2
 * Signature: ([BIZ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1setup_1phase2
  (JNIEnv *, jclass, jbyteArray, jint, jboolean);

/*
 * Class:     com_actionsmicro_airplay_FairPlay
 * Method:    fp_decrypt
 * Signature: ([BI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1decrypt
  (JNIEnv *, jclass, jbyteArray, jint);

#ifdef __cplusplus
}
#endif
#endif
