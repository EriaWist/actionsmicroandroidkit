#include <stdio.h>
#include "code98.h"

void v6270()
{
    uint32_t arg1 = *((uint32_t*)v5530 + 1);    /* v5534 */

    if (arg1 >= 0x1e7980 && arg1 <= 0x1fd9b7) {
        v7024 = 0;
        v3688 = (uint32_t)(arg1 - 0x1e7980 + v219);
        return;
    }

    if (arg1 >= 0x30f978 && arg1 < 0x310344) {
        v7024 = 0;
        v3688 = (uint32_t)(arg1 - 0x2f9940 + v219);
        return;
    }

    if ((arg1 >= 0x115aa3) && (arg1 <= 0x1e202a)) {
        v7024 = 0;
        v3688 = (uint32_t)(arg1 - 0xfe6d3 + v219);
        return;
    }

    return;
}
