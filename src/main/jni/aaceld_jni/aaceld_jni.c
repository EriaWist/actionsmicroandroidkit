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
 JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldDecoder_init
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
JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldDecoder_decode
(JNIEnv* env, jclass clazz, jbyteArray data, jint offset, jint length, jbyteArray out)
{
	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	int size = length;
	uint8_t * pcm = decode_aaceld_frame(bufferPtr+offset, &size);
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
 JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldDecoder_release
 (JNIEnv* env, jclass clazz)
 {
 	LOGD("AacEldDecoder_release");
 	return deinit_aaceld_decoder();
 }

 /*
  * Class:     com_actionsmicro_airplay_AacEldEncoder
  * Method:    init
  * Signature: (III)I
  */
  JNIEXPORT jlong JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldEncoder_init
  (JNIEnv* env, jclass clazz, jint bitrate, jint sampleRate)
  {
  	LOGD("AacEldEncoder_init:%d, %d,", bitrate, sampleRate);
      AAC_ENCODER_CONTEXT_HANDLE aacEncoder = NULL;

 	AACENC_ERROR ret = init_aaceld_encoder(&aacEncoder, (const UINT) bitrate,
                                            (const UINT) sampleRate);
 	if (ret != AACENC_OK) {
 		LOGE("AacEldEncoder_init failed: %d", ret);
 	}

  	return (jlong)aacEncoder;
  }

 /*
  * Class:     com_actionsmicro_airplay_AacEldEncoder
  * Method:    decode
  * Signature: ([BI)[B
  */
 JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldEncoder_encode
 (JNIEnv* env, jclass clazz, jlong nativeEncoder, jbyteArray data, jint offset, jint length, jbyteArray out)
 {
     AAC_ENCODER_CONTEXT_HANDLE aacEncoder = (AAC_ENCODER_CONTEXT_HANDLE)nativeEncoder;
 	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
 	size_t size = (size_t) length;
 	const uint8_t* aac = encode_aaceld_frame(aacEncoder, (uint8_t *)(bufferPtr+offset), &size);
 	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
 	if (aac != NULL) {
 		jbyte* outbufferPtr = (*env)->GetByteArrayElements(env, out, NULL);
 		memcpy(outbufferPtr, aac, size);
 		(*env)->ReleaseByteArrayElements(env, out, outbufferPtr, 0);
 		return (jint) size;
 	}
 	return -1;
 }

 /*
  * Class:     com_actionsmicro_airplay_AacEldEncoder
  * Method:    release
  * Signature: ()I
  */
  JNIEXPORT jint JNICALL Java_com_actionsmicro_airplay_airtunes_AacEldEncoder_release
  (JNIEnv* env, jclass clazz, jlong nativeEncoder)
  {
      AAC_ENCODER_CONTEXT_HANDLE aacEncoder = (AAC_ENCODER_CONTEXT_HANDLE)nativeEncoder;
 	 LOGD("AacEldEncoder_release");
  	return deinit_aaceld_encoder(aacEncoder);
  }