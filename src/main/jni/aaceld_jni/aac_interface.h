#ifndef _AAC_INTERFACE_H_
#define _AAC_INTERFACE_H_

#include <stdint.h>
#include "aacenc_lib.h"

#ifdef __cplusplus
extern "C" {
#endif
struct AAC_ENCODER_CONTEXT;
typedef struct AAC_ENCODER_CONTEXT* AAC_ENCODER_CONTEXT_HANDLE;

int init_aaceld_decoder(int frequency, int channel, int constant_duration);
int deinit_aaceld_decoder();
/* for input, size holds the size of data, for output, size holds the size of static buffer returned */
uint8_t *decode_aaceld_frame(uint8_t *data, int *size);

AACENC_ERROR init_aaceld_encoder(AAC_ENCODER_CONTEXT_HANDLE *pEncoderContext, const UINT bitrate, const UINT sampleRate);
AACENC_ERROR deinit_aaceld_encoder(const AAC_ENCODER_CONTEXT_HANDLE encoderContext);
/* for input, size holds the size of data, for output, size holds the size of static buffer returned */
uint8_t *encode_aaceld_frame(const AAC_ENCODER_CONTEXT_HANDLE encoderContext, const uint8_t *data, size_t *size);

#ifdef __cplusplus
}
#endif

#endif /* _AAC_INTERFACE_H_ */
