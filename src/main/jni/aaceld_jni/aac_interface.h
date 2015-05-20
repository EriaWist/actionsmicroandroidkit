#ifndef _AAC_INTERFACE_H_
#define _AAC_INTERFACE_H_

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int init_aaceld_decoder(int frequency, int channel, int constant_duration);
int deinit_aaceld_decoder();
/* for input, size holds the size of data, for output, size holds the size of static buffer returned */
uint8_t *decode_aaceld_frame(uint8_t *data, int *size);

#ifdef __cplusplus
}
#endif

#endif /* _AAC_INTERFACE_H_ */
