#ifndef __IIII_H__
#define __IIII_H__

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

int v6562();
uint8_t *v5423(uint8_t *data, int size, int isaudio);
uint8_t *v5426(uint8_t *data, int size, int isaudio);
uint8_t *v6495(uint8_t *data, int size);

extern uint8_t *v217;
#define PHASE_THREAD_MUTEX_ADDR (v217 + 0x1a080)
#define PHASE_THREAD_COND_ADDR  (v217 + 0x1a180)
#define PHASE_THREAD_COND_ADDR1 (v217 + 0x1a280)
#define KEYIV_THREAD_MUTEX_ADDR (v217 + 0x1b080)
#define KEYIV_THREAD_COND_ADDR  (v217 + 0x1b180)
#define KEYIV_THREAD_COND_ADDR1 (v217 + 0x1b280)
#define THREAD_DATA_STORAGE     (v217 + 0x12000)

#define CALL_VIDEO_PHASE1 (0x10A0)
#define CALL_VIDEO_PHASE2 (0x2300)
#define CALL_AUDIO_PHASE1 (0xA800)
#define CALL_AUDIO_PHASE2 (0xF000)
#define CALL_KEYIV        (0x871B0000)

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif
