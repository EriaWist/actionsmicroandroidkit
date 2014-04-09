#include "code98.h"
void v4833() {
#define var_1 (-(0x1))
#define arg_0 (0x8)
#define arg_4 (0x0C)
#define arg_8 (0x10)
v5530 -= 4; *(uint32_t*)v5530 = v7297;
v7297 = v5530;
v3688 = *(uint32_t*)(((v7297) + (arg_8)));
v5690 = *(uint32_t*)(((v7297) + (arg_4)));
v5530 = v5530 - 0x8;
v5530 -= 4; *(uint32_t*)v5530 = v7019;
v7019 = ((v7297) + (var_1));
v3100(v5690);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (arg_8))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= *(uint32_t*)(((v7297) + (arg_0)));v5661 = !v7024;
v5530 = v5530 + 0x8;
v7019 = *(uint32_t*)v5530; v5530 += 4;
*(uint16_t*)(v3688) = *v2042;
v5530 = v7297;
v7297 = *(uint32_t*)v5530; v5530 += 4;
v5530 += 4; return;
#undef var_1
#undef arg_0
#undef arg_4
#undef arg_8
}
