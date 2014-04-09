#include <jni.h>
#include <android/log.h>
#include <stdint.h>
#include <sys/time.h>

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
Java_com_actionsmicro_airplay_FairPlay_fp_1setup_1init() {
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
Java_com_actionsmicro_airplay_FairPlay_fp_1setup_1phase1(JNIEnv* env, jclass clazz, jbyteArray data, jint size, jboolean isaudio) {
	// LOGD("fp_setup_phase1:size=%d", size);	
	// jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	// jsize len = (*env)->GetArrayLength(env, data);
	// LOGD("len=%d", len);
	// LOGD("bufferPtr1:%c%c%c%c", bufferPtr[0],bufferPtr[1],bufferPtr[2],bufferPtr[3]);
	// logMem(bufferPtr, 6);
	// LOGD("bufferPtr2:%c%c%c%c", bufferPtr[0],bufferPtr[1],bufferPtr[2],bufferPtr[3]);
	// uint8_t *p1 = fp_setup_phase1(bufferPtr, size, isaudio);
	// LOGD("bufferPtr22:%c%c%c%c", bufferPtr[0],bufferPtr[1],bufferPtr[2],bufferPtr[3]);
	// (*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	// LOGD("p1:%c%c%c%c", p1[0],p1[1],p1[2],p1[3]);
	// LOGD("p1:%02d %02d %02d %02d", p1[0],p1[1],p1[2],p1[3]);
	// logMem(p1, 6);
	
	// jbyteArray bArray = (*env)->NewByteArray(env, 142);
	// bufferPtr = (*env)->GetByteArrayElements(env, bArray, NULL);
	// (*env)->SetByteArrayRegion(env, bArray, 0, 142, (jbyte*) p1);
	// (*env)->ReleaseByteArrayElements(env, bArray, bufferPtr, 0);
	
	// bufferPtr = (*env)->GetByteArrayElements(env, bArray, NULL);
	// len = (*env)->GetArrayLength(env, bArray);
	// LOGD("len=%d", len);
	// LOGD("bufferPtr2:%c%c%c%c", bufferPtr[0],bufferPtr[1],bufferPtr[2],bufferPtr[3]);
	// logMem(bufferPtr, 6);
	// (*env)->ReleaseByteArrayElements(env, bArray, bufferPtr, 0);
	
	// return bArray;

	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	jbyteArray bArray = (*env)->NewByteArray(env, 142);
	(*env)->SetByteArrayRegion(env, bArray, 0, 142, (jbyte*) fp_setup_phase1(bufferPtr, size, isaudio));
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	return bArray;
}

JNIEXPORT jbyteArray JNICALL
Java_com_actionsmicro_airplay_FairPlay_fp_1setup_1phase2(JNIEnv* env, jclass clazz, jbyteArray data, jint size, jboolean isaudio) {

	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	jbyteArray bArray = (*env)->NewByteArray(env, 32);
	(*env)->SetByteArrayRegion(env, bArray, 0, 32, (jbyte*) fp_setup_phase2(bufferPtr, size, isaudio));
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	return bArray;
}

JNIEXPORT jbyteArray JNICALL
Java_com_actionsmicro_airplay_FairPlay_fp_1decrypt(JNIEnv* env, jclass clazz, jbyteArray data, jint size) {

	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, data, NULL);
	jbyteArray bArray = (*env)->NewByteArray(env, 16);
	(*env)->SetByteArrayRegion(env, bArray, 0, 16, (jbyte*) fp_decrypt(bufferPtr, size));
	(*env)->ReleaseByteArrayElements(env, data, bufferPtr, 0);
	return bArray;
}
uint32_t ezTimeUs(void)
{
	struct timeval t;
	gettimeofday(&t, NULL);
	return (t.tv_sec * 1000000 + t.tv_usec);
}