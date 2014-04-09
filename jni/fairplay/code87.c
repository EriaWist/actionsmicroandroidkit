#include "code98.h"
void v7401()
{
    uint32_t arg1 = *((uint32_t*)v5530);    /* v5534 */
    //edx = 0;

    if (arg1 < 0x311000) {

        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
            v3688 = 0xB3B3B3B3 ^ *(uint32_t*)v3688;
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
            v3688 = 0xB3B3B3B3 ^ *(uint32_t*)v3688;
            return;
        }

        //if ((arg1 >= 0x115aa3) /* && (arg1 <= 0x1e202a) */) {
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        v3688 = 0xB3B3B3B3 ^ *(uint32_t*)v3688;
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    v3688 = 0x1E1E1E1E ^ *(uint32_t*)v3688;
    return;
}

void v1880(uint32_t arg1)
{
    //uint32_t arg1 = *((uint32_t*)esp);    /* esi */
    //edx = 0;

    v5530 -= 8;

    if (arg1 < 0x311000) {

        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
            v3688 = 0xB3B3B3B3 ^ *(uint32_t*)v3688;
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
            v3688 = 0xB3B3B3B3 ^ *(uint32_t*)v3688;
            return;
        }

        //if ((arg1 >= 0x115aa3) /* && (arg1 <= 0x1e202a) */) {
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        v3688 = 0xB3B3B3B3 ^ *(uint32_t*)v3688;
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    v3688 = 0x1E1E1E1E ^ *(uint32_t*)v3688;
    return;
}
