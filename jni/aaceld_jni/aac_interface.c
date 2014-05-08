#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "aac_interface.h"
#include "aacdecoder_lib.h"


#include <android/log.h>
#define  LOG_TAG    "aac-eld"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)

static uint8_t *make_asc(int frequency, int channel, int constant_duration);


static HANDLE_AACDECODER pDecoderHandle = NULL;
static short int *decoder_output_buffer = NULL;
static int pcm_pkt_size = 0;



int init_aaceld_decoder(int frequency, int channel, int constant_duration)
{
    int ret;
    uint8_t *asc = make_asc(frequency, channel, constant_duration);
    if (NULL == asc)
        return -1;

    if (NULL != decoder_output_buffer) {
        free(decoder_output_buffer);
        decoder_output_buffer = NULL;
    }

    decoder_output_buffer = (short int *)malloc(2 * channel * constant_duration); // s16 * num_channels * framesize
    if (NULL == decoder_output_buffer)
        return -2;
    pcm_pkt_size = 2 * channel * constant_duration;

    pDecoderHandle = aacDecoder_Open(TT_MP4_RAW, 1);
    if (pDecoderHandle == NULL) {
        return -3;
    }
    
    UCHAR *conf[1] = {asc};
    UINT length[1] = {4};
    ret = aacDecoder_ConfigRaw(pDecoderHandle, conf, length);
    if (ret != AAC_DEC_OK) {
        return -4;
    }
    CStreamInfo *mStreamInfo = aacDecoder_GetStreamInfo(pDecoderHandle);
    LOGI("Initially configuring decoder: %d Hz, %d channels, frame size:%d",
                mStreamInfo->sampleRate,
                mStreamInfo->numChannels,
                mStreamInfo->frameSize);
    return 0;
}


int deinit_aaceld_decoder()
{
    if (NULL != pDecoderHandle) {
        aacDecoder_Close(pDecoderHandle);
        pDecoderHandle = NULL;
    }

    if (NULL != decoder_output_buffer) {
        free(decoder_output_buffer);
        decoder_output_buffer = NULL;
    }

    pcm_pkt_size = 0;

    return 0;
}

uint8_t *decode_aaceld_frame(uint8_t *data, int *size)
{
    CStreamInfo *mStreamInfo = aacDecoder_GetStreamInfo(pDecoderHandle);
    
    UINT data_left = *size;
    UINT tmp = *size;
    int ret;
    while (data_left > 0) {
        UCHAR *mbuf = (UCHAR*)(data + *size -data_left);
        UINT msize = data_left;
        ret = aacDecoder_Fill(pDecoderHandle, &mbuf, &msize, &tmp);
        if (ret != AAC_DEC_OK) {
            LOGE("aacDecoder_Fill returns:0x%x", ret);
    
            return NULL;
        }
        data_left = tmp;

        ret = aacDecoder_DecodeFrame(pDecoderHandle, decoder_output_buffer, pcm_pkt_size, 0);
        if (ret == AAC_DEC_NOT_ENOUGH_BITS) {
            LOGD("aacDecoder_DecodeFrame AAC_DEC_NOT_ENOUGH_BITS");
            continue;
        }
        if (ret != AAC_DEC_OK) {
            LOGE("aacDecoder_DecodeFrame returns:0x%x", ret);
            return NULL;
        }
    }
    *size = pcm_pkt_size;
    return (uint8_t*)decoder_output_buffer;
}

static uint8_t *make_asc(int frequency, int channel, int constant_duration)
{
    static uint8_t asc[4];
    memset(asc, 0, 4);

    // object type 5 bits, == 31
    asc[0] |= 0xf8;
    // object type 6 bits, +32, 7, 000111
    asc[1] |= 0xe0;

    switch (frequency) {
        case 48000:
            asc[1] |= 0x06; break;
        case 44100:
            asc[1] |= 0x08; break;
        default:
            return NULL;
    };

    switch (channel) {
        case 1:
            asc[2] |= 0x20; break;
        case 2:
            asc[2] |= 0x40; break;
        default:
            return NULL;
    }

    switch (constant_duration) {
        case 512:
            asc[2] |= 0x00;
            break;
        case 480:
            asc[2] |= 0x10;
            break;
        default:
            return NULL;
    }

    return asc;
}
