#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "code99.h"
#include "code98.h"

#include <android/log.h>
#define  LOG_TAG    "fairplay"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
void logMem6(char * prefix, uint8_t* bufferPtr) {
    LOGI("%s:%02x%02x%02x%02x%02x%02x", prefix, bufferPtr[0], bufferPtr[1], bufferPtr[2], bufferPtr[3], bufferPtr[4], bufferPtr[5]);
}


typedef void *(*m_thread_func_t)(void *);
typedef int (*m_pthread_create_t)(pthread_t *, const pthread_attr_t *, m_thread_func_t, void *);
typedef int (*m_mutex_func_t)(pthread_mutex_t *);
typedef int (*m_cond_wait_t)(pthread_cond_t *, pthread_mutex_t *);
typedef int (*m_cond_signal_t)(pthread_cond_t *);
typedef int (*m_mutex_init_t)(pthread_mutex_t *, const pthread_mutexattr_t *);
typedef int (*m_cond_init_t)(pthread_cond_t *, const pthread_condattr_t *);
typedef void *(*m_memcpy_t)(void *, const void *, size_t);
typedef int (*m_stat_t)(const char *, struct stat *);
typedef unsigned int (*m_sleep_t)(unsigned int);

static m_pthread_create_t v1330 = pthread_create;
static m_mutex_func_t v4228 = pthread_mutex_lock;
static m_mutex_func_t v3647 = pthread_mutex_unlock;
static m_mutex_func_t v1820 = pthread_mutex_trylock;
static m_cond_wait_t v6755 = pthread_cond_wait;
static m_cond_signal_t v496 = pthread_cond_signal;
static m_mutex_init_t v2200 = pthread_mutex_init;
static m_cond_init_t v3450 = pthread_cond_init;
static pthread_t v6786;
static pthread_t v5799;
static m_memcpy_t v2695 = memcpy;
static m_stat_t v3717 = stat;
static m_sleep_t v4851 = sleep;

static pthread_mutex_t v4848 = PTHREAD_MUTEX_INITIALIZER;
static int v6501 = 0;

struct timeval v824, v825;
static double v3040;

static uint8_t v6825[4 + 0x14 + 4] = {
    0x14, 0x00, 0x00, 0x00,
    0x29, 0x23, 0xBE, 0x84, 0xE1, 0x6C, 0xD6, 0xAE,
    0x52, 0x90, 0x49, 0xF1, 0xF1, 0xBB, 0xE9, 0xEB,
    0xB3, 0xA6, 0xDB, 0x3C
};

static uint8_t v1179[0x0c] = {
    0x20, 0x3a, 0xcd, 0x99, 0x76, 0x3b, 0xac, 0xae, 0x50, 0x78, 0xc0, 0x13
};

static uint8_t *v6020(uint8_t *data, int size);
static uint8_t *v6021(uint8_t *data, int size);
static uint8_t *v6920(uint8_t *data, int size);
static uint8_t *v6919(uint8_t *data, int size);

static int v1072 = 0;
static int v5753 = 0;
static pthread_t v2871;
static void *v1372(void *arg);

static void *v2929(void *arg)
{
    LOGD("v2929 start");    
    int i;
    uint8_t *p;

    while (1) {
        v4228((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        v6755((pthread_cond_t*)PHASE_THREAD_COND_ADDR, (pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);

        v4228(&v4848);
        switch (*(uint32_t*)THREAD_DATA_STORAGE) {
            case CALL_VIDEO_PHASE1:
                LOGD("CALL_VIDEO_PHASE1");
    
                if (v6501 == 1) {
                    for (i = 0; i < 3; ++i) {
                        v3647(&v4848);
                        v4851(1);
                        v4228(&v4848);
                        if (v6501 == 0)
                            break;
                    }
                    if (v6501 == 1) {
                    }
                }

                p = v6020((uint8_t*)(THREAD_DATA_STORAGE + 4), 0x10);
                v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), p, 0x8E);
                break;
            case CALL_VIDEO_PHASE2:
                LOGD("CALL_VIDEO_PHASE2");
                p = v6021((uint8_t*)(THREAD_DATA_STORAGE + 4), 0xA4);
                v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), p, 0x20);
                break;
            case CALL_AUDIO_PHASE1:
                LOGD("CALL_AUDIO_PHASE1");
                v6501 = 1;
                p = v6920((uint8_t*)(THREAD_DATA_STORAGE + 4), 0x10);
                v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), p, 0x8E);
                LOGD("CALL_AUDIO_PHASE1 ends");
                break;
            case CALL_AUDIO_PHASE2:
                LOGD("CALL_AUDIO_PHASE2");
                p = v6919((uint8_t*)(THREAD_DATA_STORAGE + 4), 0xA4);
                v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), p, 0x20);
                break;
            default:
                LOGD("default");                
                break;
        }
        v3647(&v4848);
        v496((pthread_cond_t*)PHASE_THREAD_COND_ADDR1);
        LOGD("signal PHASE_THREAD_COND_ADDR1");
                
        v3647((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        LOGD("unlock PHASE_THREAD_MUTEX_ADDR");
        
    }
    LOGD("v2929 ends");    
    
    return NULL;
}

static void *v1018(void *arg)
{
    int i;
    uint8_t *p;
    while (1) {
        v4228((pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR);
        v6755((pthread_cond_t*)KEYIV_THREAD_COND_ADDR, (pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR);

        v4228(&v4848);
        switch (*(uint32_t*)THREAD_DATA_STORAGE) {
            case CALL_KEYIV:
                p = (uint8_t*)(THREAD_DATA_STORAGE + 4);
                uint32_t key_size;
                uint32_t ret_key;
                {
                    v440((uint32_t)&key_size);
                    v440((uint32_t)&ret_key);
                    v440(0x48);
                    v440((uint32_t)(THREAD_DATA_STORAGE + 4));
                    p = (uint8_t*)*(uint32_t*)(v6825 + 4 + 0x14);
                    v440((uint32_t)p);

                    v440(0x89898989);
                    v6811();
                }

                p = (uint8_t*)ret_key;
                v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), p, 0x10);
                break;
            default:
                break;
        }
        v6501 = 0;
        v3647(&v4848);
        v496((pthread_cond_t*)KEYIV_THREAD_COND_ADDR1);
        v3647((pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR);
    }

    return NULL;
}

int v6562()
{
    v3480();
    v1179[0] = 0x01;
    srand(time(NULL));
    v6825[0] = 0x14;
    int i = 0;
    for (i = 0; i < 0x14; ++i) {
        v6825[0x04 + i] = rand();
    }
    if (v1072 == 0) {
        v1072 = 1;
        v1330(&v2871, NULL, v1372, NULL);
    }
    return 0;
}

uint8_t *v5423(uint8_t *data, int size, int isaudio)
{
    uint8_t *ret = NULL;
    v4228((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
    if (isaudio) {

        gettimeofday(&v824, NULL);

        *(uint32_t*)THREAD_DATA_STORAGE = CALL_AUDIO_PHASE1;
        v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), data, size);
        v496((pthread_cond_t*)PHASE_THREAD_COND_ADDR);
        LOGD("wait PHASE_THREAD_COND_ADDR1");
        v6755((pthread_cond_t*)PHASE_THREAD_COND_ADDR1, (pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        LOGD("check PHASE_THREAD_COND_ADDR1");
        v3647((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        LOGD("interface unlock PHASE_THREAD_MUTEX_ADDR");
        ret = (uint8_t*)(THREAD_DATA_STORAGE + 4);

        gettimeofday(&v825, NULL);
        v3040 = ((double)v825.tv_sec + ((double)v825.tv_usec) / 1000000) - ((double)v824.tv_sec + ((double)v824.tv_usec) / 1000000);
    } else {

        gettimeofday(&v824, NULL);

        *(uint32_t*)THREAD_DATA_STORAGE = CALL_VIDEO_PHASE1;
        v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), data, size);
        v496((pthread_cond_t*)PHASE_THREAD_COND_ADDR);
        v6755((pthread_cond_t*)PHASE_THREAD_COND_ADDR1, (pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        v3647((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        ret = (uint8_t*)(THREAD_DATA_STORAGE + 4);

        gettimeofday(&v825, NULL);
        v3040 = ((double)v825.tv_sec + ((double)v825.tv_usec) / 1000000) - ((double)v824.tv_sec + ((double)v824.tv_usec) / 1000000);
    }
    return ret;
}

uint8_t *v5426(uint8_t *data, int size, int isaudio)
{
    uint8_t *ret = NULL;
    v4228((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
    if (isaudio) {

        gettimeofday(&v824, NULL);

        *(uint32_t*)THREAD_DATA_STORAGE = CALL_AUDIO_PHASE2;
        v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), data, size);
        v496((pthread_cond_t*)PHASE_THREAD_COND_ADDR);
        v6755((pthread_cond_t*)PHASE_THREAD_COND_ADDR1, (pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        v3647((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        ret = (uint8_t*)(THREAD_DATA_STORAGE + 4);

        gettimeofday(&v825, NULL);
        v3040 = ((double)v825.tv_sec + ((double)v825.tv_usec) / 1000000) - ((double)v824.tv_sec + ((double)v824.tv_usec) / 1000000);
    } else {
        gettimeofday(&v824, NULL);

        *(uint32_t*)THREAD_DATA_STORAGE = CALL_VIDEO_PHASE2;
        v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), data, size);
        v496((pthread_cond_t*)PHASE_THREAD_COND_ADDR);
        v6755((pthread_cond_t*)PHASE_THREAD_COND_ADDR1, (pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        v3647((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR);
        ret = (uint8_t*)(THREAD_DATA_STORAGE + 4);

        gettimeofday(&v825, NULL);
        v3040 = ((double)v825.tv_sec + ((double)v825.tv_usec) / 1000000) - ((double)v824.tv_sec + ((double)v824.tv_usec) / 1000000);
    }
    return ret;
}

static uint8_t *v6020(uint8_t *data, int size)
{
    uint8_t *p = NULL;

    memset(v6825, 0, sizeof(v6825));
    v3379 = 0;
    v3378 = 0;
    v3382 = 0;

    v6825[0] = 0x14;
    v6825[1] = 0x00;
    v6825[2] = 0x00;
    v6825[3] = 0x00;
    int i = 0;
    for (i = 0; i < 0x14; ++i) {
        v6825[0x04 + i] = rand();
    }

    v440((uint32_t)&v6825[4 + 0x14]);
    v440((uint32_t)&v6825[0]);
    v440(0x89898989);
    v5131();

    uint32_t v5 = 0x00000000;
    uint32_t ret_length;
    uint8_t *fplyret = NULL;
    v440((uint32_t)&v5);
    v440((uint32_t)&ret_length);
    v440((uint32_t)&fplyret);
    v440((uint32_t)data);
    p = (uint8_t*)*(uint32_t*)(v6825 + 4 + 0x14);
    v440((uint32_t)p);
    v440((uint32_t)&v6825[0]);
    v440(data[4]);

    v440(0x89898989);
    v3320();
    return fplyret;
}

static uint8_t *v6021(uint8_t *data, int size)
{
    uint32_t v5 = 0;
    uint32_t ret_length;
    uint8_t *fplyret = NULL;
    uint8_t *p;

    v440((uint32_t)&v5);
    v440((uint32_t)&ret_length);
    v440((uint32_t)&fplyret);
    v440((uint32_t)data);
    p = (uint8_t*)*(uint32_t*)(v6825 + 4 + 0x14);
    v440((uint32_t)p);
    v440((uint32_t)&v6825[0]);
    v440(data[4]);

    v440(0x89898989);
    v3320();
    return fplyret;
}

uint8_t *v6495(uint8_t *data, int size)
{
    uint8_t *ret;
    gettimeofday(&v824, NULL);

    v4228((pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR);
    *(uint32_t*)THREAD_DATA_STORAGE = CALL_KEYIV;
    v2695((uint8_t*)(THREAD_DATA_STORAGE + 4), data, size);
    v496((pthread_cond_t*)KEYIV_THREAD_COND_ADDR);
    v6755((pthread_cond_t*)KEYIV_THREAD_COND_ADDR1, (pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR);
    v3647((pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR);
    ret = (uint8_t*)(THREAD_DATA_STORAGE + 4);

    gettimeofday(&v825, NULL);
    v3040 = ((double)v825.tv_sec + ((double)v825.tv_usec) / 1000000) - ((double)v824.tv_sec + ((double)v824.tv_usec) / 1000000);
    return ret;
}

static uint8_t *v6920(uint8_t *data, int size)
{
    v6935();
    v5937();

    v440((uint32_t)&v6825[4 + 0x14]);
    v440((uint32_t)&v6825[0]);
    v440(0x89898989);
    v5131();

    uint32_t v5 = 0x00000000;
    uint32_t ret_length;
    uint8_t *fplyret = NULL;
    uint8_t *p = (uint8_t*)*(uint32_t*)(v6825 + 4 + 0x14);
    v440((uint32_t)&v5);
    v440((uint32_t)&ret_length);
    v440((uint32_t)&fplyret);
    v440((uint32_t)data);
    v440((uint32_t)p);
    v440((uint32_t)&v6825[0]);
    v440(data[4]);

    v440(0x89898989);
    v3320();
    return fplyret;
}

static uint8_t *v6919(uint8_t *data, int size)
{
    uint32_t v5 = 0;
    uint32_t ret_length;
    uint8_t *fplyret = NULL;
    uint8_t *p = (uint8_t*)*(uint32_t*)(v6825 + 4 + 0x14);

    v440((uint32_t)&v5);
    v440((uint32_t)&ret_length);
    v440((uint32_t)&fplyret);
    v440((uint32_t)data);
    v440((uint32_t)p);
    v440((uint32_t)&v6825[0]);
    v440(data[4]);
    v440(0x89898989);
    v3320();
    return fplyret;
}

static void *v1372(void *arg)
{
    if (v5753 == 0) {
        v5753 = 1;
        v2200((pthread_mutex_t*)PHASE_THREAD_MUTEX_ADDR, NULL);
        v3450((pthread_cond_t*)PHASE_THREAD_COND_ADDR, NULL);
        v3450((pthread_cond_t*)PHASE_THREAD_COND_ADDR1, NULL);
        v2200((pthread_mutex_t*)KEYIV_THREAD_MUTEX_ADDR, NULL);
        v3450((pthread_cond_t*)KEYIV_THREAD_COND_ADDR, NULL);
        v3450((pthread_cond_t*)KEYIV_THREAD_COND_ADDR1, NULL);
        v1330(&v6786, NULL, v2929, NULL);
        v1330(&v5799, NULL, v1018, NULL);
    }

    while (1) {
        v4851(60);
    }
    return NULL;
}
