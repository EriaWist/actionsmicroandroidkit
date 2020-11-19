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

#define PROFILE_AAC_LC 2
#define PROFILE_AAC_HE 5
#define PROFILE_AAC_HE_v2 29
#define PROFILE_AAC_LD 23
#define PROFILE_AAC_ELD 39

typedef struct AAC_ENCODER_CONTEXT {
    HANDLE_AACENCODER encoderHandle;
    INT_PCM inputBuffer[2*2*480];
    UCHAR outputBuffer[6144 / 8 * 2];
} AAC_ENCODER_CONTEXT;

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
    //asc[0] |= 0x12;
    // object type 6 bits, +32, 7, 000111
     asc[1] |= 0xe0;
    // asc[1] |= 0x10;
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
    LOGE("make_asc complete");
    return asc;
}


AACENC_ERROR init(const HANDLE_AACENCODER m_encHandle, const UINT bitrate, const UINT sample_rate) {
    AACENC_ERROR err = AACENC_OK;
    CHANNEL_MODE mode = MODE_2;

    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_AOT, 39)) != AACENC_OK) {
        LOGE("Unable to set the AOT");
        return err;
    }
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_SBR_MODE, 0)) != AACENC_OK) {
        LOGE("Unable to set SBR mode");
        return err;
    }
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_SAMPLERATE, sample_rate)) != AACENC_OK) {
        LOGE("Unable to set the AOT");
        return err;
    }
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_CHANNELMODE, mode)) != AACENC_OK) {
        LOGE("Unable to set the channel mode");
        return err;
    }
    //For EZCast device
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_GRANULE_LENGTH, 480)) != AACENC_OK) {
        LOGE("Unable to set the channel mode");
        return err;
    }
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_CHANNELORDER, 1)) != AACENC_OK) {
        LOGE("Unable to set the wav channel order");
        return err;
    }
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_BITRATE, bitrate)) != AACENC_OK) {
        LOGE("Unable to set the bitrate");
        return err;
    }
    //CAUTION!! 2: ADTS is for AAC_LC
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_TRANSMUX, 0)) != AACENC_OK) {
        LOGE("Unable to set the transmux");
        return err;
    }
    if ((err = aacEncoder_SetParam(m_encHandle, AACENC_AFTERBURNER, 0)) != AACENC_OK) {
        LOGE("Unable to set the afterburner mode");
        return err;
    }
    if ((err = aacEncEncode(m_encHandle, NULL, NULL, NULL, NULL)) != AACENC_OK) {
        LOGE("Unable to initialize AAC encoder");
        return err;
    }
    AACENC_InfoStruct encInfo;
    if ((err = aacEncInfo(m_encHandle, &encInfo)) != AACENC_OK) {
        LOGE("Unable to get the encoder info");
        return err;
    } else {
        char confHex[256];
        char *pConfHex = confHex;
        for (int i = 0; i < encInfo.confSize; i++) {
            pConfHex += sprintf(pConfHex, "%02X", encInfo.confBuf[i]);
        }
        *pConfHex = 0x00;
        LOGI("aacEncInfo: 0x%s, maxOutBufBytes: %d, frameLength: %d, ", confHex,
             encInfo.maxOutBufBytes, encInfo.frameLength);
    }
    return err;
}

AACENC_ERROR init_aaceld_encoder(AAC_ENCODER_CONTEXT_HANDLE *pEncoderContext, const UINT bitrate,
                                 const UINT sampleRate) {
    *pEncoderContext = malloc(sizeof(AAC_ENCODER_CONTEXT));
    memset(*pEncoderContext, 0, sizeof(AAC_ENCODER_CONTEXT));
    HANDLE_AACENCODER aacEncoder = NULL;

    AACENC_ERROR ret = aacEncOpen(&aacEncoder, 0, 2);
    if (ret != AACENC_OK) {
        LOGE("aacEncOpen failed: %d", ret);
        return ret;
    }

    (*pEncoderContext)->encoderHandle = aacEncoder;
    ret = init(aacEncoder, bitrate, sampleRate);
    if (ret != AACENC_OK) {
        LOGE("setParameters failed: %d", ret);
        deinit_aaceld_encoder(*pEncoderContext);
        *pEncoderContext = NULL;
        return ret;
    }
    return AACENC_OK;
}

AACENC_ERROR deinit_aaceld_encoder(const AAC_ENCODER_CONTEXT_HANDLE encoderContext) {
    AACENC_ERROR ret = AACENC_OK;
    if (encoderContext) {
        if (encoderContext->encoderHandle) {
            ret = aacEncClose(&encoderContext->encoderHandle);
            encoderContext->encoderHandle = NULL;
        }
        free(encoderContext);
    } else {
        ret = AACENC_INVALID_HANDLE;
    }
    return ret;
}

UCHAR *encode_aaceld_frame(const AAC_ENCODER_CONTEXT_HANDLE encoderContext, const uint8_t *data,
                           size_t *size) {
    HANDLE_AACENCODER encoderHandle = encoderContext->encoderHandle;
    memcpy(encoderContext->inputBuffer, data, *size);
    void *inBuffer[] = {encoderContext->inputBuffer};
    INT inBufferIds[] = {IN_AUDIO_DATA};
    INT inBufferSize[] = {sizeof(encoderContext->inputBuffer)};
    INT inBufferElSize[] = {sizeof(INT_PCM)};
    void *outBuffer[] = {encoderContext->outputBuffer};
    INT outBufferIds[] = {OUT_BITSTREAM_DATA};
    INT outBufferSize[] = {sizeof(encoderContext->outputBuffer)};
    INT outBufferElSize[] = {sizeof(UCHAR)};

    AACENC_BufDesc inBufDesc = {0};
    inBufDesc.numBufs = sizeof(inBuffer) / sizeof(void *);
    inBufDesc.bufs = (void **) &inBuffer;
    inBufDesc.bufferIdentifiers = inBufferIds;
    inBufDesc.bufSizes = inBufferSize;
    inBufDesc.bufElSizes = inBufferElSize;

    AACENC_BufDesc outBufDesc = {0};
    outBufDesc.numBufs = sizeof(outBuffer) / sizeof(void *);
    outBufDesc.bufs = (void **) &outBuffer;
    outBufDesc.bufferIdentifiers = outBufferIds;
    outBufDesc.bufSizes = outBufferSize;
    outBufDesc.bufElSizes = outBufferElSize;
    AACENC_InArgs inArgs = {0};
    inArgs.numInSamples = (INT) (*size / 2);
    inArgs.numAncBytes = 0;
    AACENC_OutArgs outArgs = {0};

    AACENC_ERROR ret = aacEncEncode(encoderHandle, &inBufDesc, &outBufDesc, &inArgs, &outArgs);
    if (ret != AACENC_OK) {
        LOGE("aacEncEncode returns:0x%x", ret);
        return NULL;
    } else {
        LOGD("numInSamples: %d, numAncBytes: %d, numOutBytes: %d", outArgs.numInSamples,
             outArgs.numAncBytes, outArgs.numOutBytes);
    }
    *size = (size_t) outArgs.numOutBytes;
    return encoderContext->outputBuffer;
}