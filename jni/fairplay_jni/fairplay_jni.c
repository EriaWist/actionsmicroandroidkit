#include <jni.h>
#include <android/log.h>
#include <stdint.h>

int fp_setup_init();
uint8_t *fp_setup_phase1(uint8_t *data, int32_t size, int isaudio);
uint8_t *fp_setup_phase2(uint8_t *data, int32_t size, int isaudio);
uint8_t *fp_decrypt(uint8_t *data, int32_t size);

#define  LOG_TAG    "fairplay"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)

JNIEXPORT jint JNICALL
Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1setup_1init() {
	LOGD("fp_setup_init");
	return fp_setup_init();
}
void logMem(jbyte* bufferPtr, jsize len) {
	jsize i = 0;
	for (; i < len; i ++) {
		LOGI("%02x", (uint8_t)bufferPtr[i]);
	}
}
JNIEXPORT jbyteArray JNICALL
Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1setup_1phase1(JNIEnv* env, jclass clazz, jbyteArray data, jint size, jboolean isaudio) {
	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	jbyteArray bArray = (*env)->NewByteArray(env, 142);
	(*env)->SetByteArrayRegion(env, bArray, 0, 142, (jbyte*) fp_setup_phase1(bufferPtr, size, isaudio));
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	return bArray;
}

JNIEXPORT jbyteArray JNICALL
Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1setup_1phase2(JNIEnv* env, jclass clazz, jbyteArray data, jint size, jboolean isaudio) {

	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	jbyteArray bArray = (*env)->NewByteArray(env, 32);
	(*env)->SetByteArrayRegion(env, bArray, 0, 32, (jbyte*) fp_setup_phase2(bufferPtr, size, isaudio));
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	return bArray;
}

JNIEXPORT jbyteArray JNICALL
Java_com_actionsmicro_airplay_crypto_FairPlay_fp_1decrypt(JNIEnv* env, jclass clazz, jbyteArray data, jint size) {
	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	jbyteArray bArray = (*env)->NewByteArray(env, size);
	LOGD("fp_decrypt");
	uint8_t *decrypyedData = fp_decrypt(bufferPtr, size);
	LOGD("SetByteArrayRegion");
	(*env)->SetByteArrayRegion(env, bArray, 0, size, (jbyte*) decrypyedData);
	LOGD("ReleaseByteArrayElements");
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	return bArray;
}