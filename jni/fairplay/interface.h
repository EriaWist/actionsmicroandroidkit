#ifndef __INTERFACE_H__
#define __INTERFACE_H__

#include <stdint.h>

int init();

uint8_t *phase1(uint8_t *data, int size, int isaudio);

uint8_t *phase2(uint8_t *data, int size, int isaudio);

uint8_t *decrypt(uint8_t *data, int size);

#endif /* __INTERFACE_H__ */
