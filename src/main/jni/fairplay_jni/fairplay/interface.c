#include "interface.h"
#include <android/log.h>

#define  LOG_TAG    "fairplay"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)

int fp_setup_init()
{
    return v6562();
}

uint8_t *fp_setup_phase1(uint8_t *data, int32_t size, int isaudio)
{
    return (uint8_t *)v5423(data, size, isaudio);
}

uint8_t *fp_setup_phase2(uint8_t *data, int32_t size, int isaudio)
{
    return (uint8_t *)v5426(data, size, isaudio);
}

uint8_t *fp_decrypt(uint8_t *data, int32_t size)
{
    return (uint8_t *)v6495(data, size);
}
