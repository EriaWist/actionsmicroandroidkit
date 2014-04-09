#include <unistd.h>
#include <stdio.h>
#include "code98.h"

void v7()
{
    uint32_t arg1 = *((uint32_t*)v5530);    /* v5534 */
    //edx = 0;

    if (arg1 < 0x311000) {
        *(uint8_t*)v7019 = 0xB3;
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
            return;
        }

        //if ((arg1 >= 0x115aa3) /* && (arg1 <= 0x1e202a) */) {
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        return;
        //}
    }

    *(uint8_t*)v7019 = 0x1E;
    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));

    return;
}

void v3100(uint32_t arg1)
{
    //uint32_t arg1 = *((uint32_t*)esp);    /* esi */
    //edx = 0;

    v5530 -= 8;

    if (arg1 < 0x311000) {
        *(uint8_t*)v7019 = 0xB3;
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
            return;
        }

        //if ((arg1 >= 0x115aa3) /* && (arg1 <= 0x1e202a) */) {
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        return;
        //}
    }

    *(uint8_t*)v7019 = 0x1E;
    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));

    return;
}

void v7110(uint32_t arg1)
{
    //uint32_t arg1 = *((uint32_t*)esp);    /* esi */
    //edx = 0;

    if (arg1 < 0x311000) {
        *(uint8_t*)v7019 = 0xB3;
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
            return;
        }

        //if ((arg1 >= 0x115aa3) /* && (arg1 <= 0x1e202a) */) {
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        return;
        //}
    }

    *(uint8_t*)v7019 = 0x1E;
    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));

    return;
}
