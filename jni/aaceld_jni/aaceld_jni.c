#include <jni.h>
#include <android/log.h>
#include <stdint.h>
#include <string.h>
#include "aac_interface.h"

#define  LOG_TAG    "aac-eld"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)


/*
 * Class:     com_actionsmicro_airplay_AacEldDecoder
 * Method:    init
 * Signature: (III)I
 */
 JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_AacEldDecoder_init
 (JNIEnv* env, jclass clazz, jint frequency, jint channel, jint constant_duration)
 {
 	LOGD("AacEldDecoder_init:%d, %d, %d,", frequency, channel, constant_duration);

 	return init_aaceld_decoder(frequency, channel, constant_duration);
 }

/*
 * Class:     com_actionsmicro_airplay_AacEldDecoder
 * Method:    decode
 * Signature: ([BI)[B
 */
JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_AacEldDecoder_decode
(JNIEnv* env, jclass clazz, jbyteArray data, jint length, jbyteArray out)
{
	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	int size = length;
	uint8_t * pcm = decode_aaceld_frame(bufferPtr, &size);
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	if (pcm != NULL) {
		jbyte* outbufferPtr = (*env)->GetByteArrayElements(env, out, NULL);
		memcpy(outbufferPtr, pcm, size);
		(*env)->ReleaseByteArrayElements(env, out, outbufferPtr, 0);
		return size;
	}
	return -1;
}

/*
 * Class:     com_actionsmicro_airplay_AacEldDecoder
 * Method:    release
 * Signature: ()I
 */
 JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_AacEldDecoder_release
 (JNIEnv* env, jclass clazz)
 {
 	LOGD("AacEldDecoder_release");
 	return deinit_aaceld_decoder();
 }