#include "code98.h"
void v3174()
{
    uint32_t arg1 = *((uint32_t*)v5530);
    //edx = 0;

    if (arg1 < 0x311000) {
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
            v3688 = (0xB3B3 ^ *(uint16_t*)v3688);
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
            v3688 = (0xB3B3 ^ *(uint16_t*)v3688);
            return;
        }

        //if ((arg1 >= 0x115aa3) /* && (arg1 <= 0x1e202a) */) {
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        v3688 = (0xB3B3 ^ *(uint16_t*)v3688);
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    v3688 = (0x1E1E ^ *(uint16_t*)v3688);
    return;
}
