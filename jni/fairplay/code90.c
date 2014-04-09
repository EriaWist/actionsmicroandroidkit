#include "code98.h"
void v5355()
{
    uint32_t arg1 = *((uint32_t*)v5530 + 1);

    if (arg1 < 0x311000) {
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            *(uint32_t*)(arg1 - 0x2f9940 + v219) = 0xB3B3B3B3 ^ *((uint32_t*)v5530);
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            *(uint32_t*)(arg1 - 0x1e7980 + v219) = 0xB3B3B3B3 ^ *((uint32_t*)v5530);
            return;
        }

        //if ((arg1 >= 0x115aa3) /*&& (arg1 <= 0x1e202a) */) {
        *(uint32_t*)(arg1 - 0xfe6d3 + v219) = 0xB3B3B3B3 ^ *((uint32_t*)v5530);
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    *(uint32_t*)v3688 = 0x1E1E1E1E ^ *((uint32_t*)v5530);
    return;
}

void v3171(uint32_t arg2, uint32_t arg1)
{
    //uint32_t arg1 = *((uint32_t*)esp + 1);

    v5530 -= 0x0c;

    if (arg1 < 0x311000) {
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            *(uint32_t*)(arg1 - 0x2f9940 + v219) = 0xB3B3B3B3 ^ arg2;
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            *(uint32_t*)(arg1 - 0x1e7980 + v219) = 0xB3B3B3B3 ^ arg2;
            return;
        }

        //if ((arg1 >= 0x115aa3) /*&& (arg1 <= 0x1e202a) */) {
        *(uint32_t*)(arg1 - 0xfe6d3 + v219) = 0xB3B3B3B3 ^ arg2;
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    *(uint32_t*)v3688 = 0x1E1E1E1E ^ arg2;
    return;
}

void v4349(uint32_t arg2, uint32_t arg1)
{
    //uint32_t arg1 = *((uint32_t*)esp + 1);

    if (arg1 < 0x311000) {
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            *(uint32_t*)(arg1 - 0x2f9940 + v219) = 0xB3B3B3B3 ^ arg2;
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            *(uint32_t*)(arg1 - 0x1e7980 + v219) = 0xB3B3B3B3 ^ arg2;
            return;
        }

        //if ((arg1 >= 0x115aa3) /*&& (arg1 <= 0x1e202a) */) {
        *(uint32_t*)(arg1 - 0xfe6d3 + v219) = 0xB3B3B3B3 ^ arg2;
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    *(uint32_t*)v3688 = 0x1E1E1E1E ^ arg2;
    return;
}

void v651(uint32_t arg2, uint32_t arg1)
{
    //uint32_t arg1 = *((uint32_t*)esp + 1);

    v5530 -= 0x08;

    if (arg1 < 0x311000) {
        if (arg1 >= 0x30f978 /* && arg1 < 0x310344 */) {
            *(uint32_t*)(arg1 - 0x2f9940 + v219) = 0xB3B3B3B3 ^ arg2;
            return;
        }

        if (arg1 >= 0x1e7980 /* && arg1 <= 0x1fd9b7 */) {
            *(uint32_t*)(arg1 - 0x1e7980 + v219) = 0xB3B3B3B3 ^ arg2;
            return;
        }

        //if ((arg1 >= 0x115aa3) /*&& (arg1 <= 0x1e202a) */) {
        *(uint32_t*)(arg1 - 0xfe6d3 + v219) = 0xB3B3B3B3 ^ arg2;
        return;
        //}
    }

    v3688 = (uint32_t)(v1455[(arg1 >> 28) & 0x1] + (arg1 & 0x00ffffff));
    *(uint32_t*)v3688 = 0x1E1E1E1E ^ arg2;
    return;
}
