#include "code98.h"
void v1813() {
#define var_24 (-(0x24))
#define var_1C (-(0x1C))
#define var_10 (-(0x10))
#define var_C (-(0x0C))
#define var_B (-(0x0B))
#define var_A (-(0x0A))
#define var_9 (-(0x9))
#define var_8 (-(0x8))
#define var_7 (-(0x7))
#define var_6 (-(0x6))
#define var_5 (-(0x5))
#define var_4 (-(0x4))
#define var_3 (-(0x3))
#define var_2 (-(0x2))
#define var_1 (-(0x1))
#define arg_0 (0x8)
v5530 -= 4; *(uint32_t*)v5530 = v7297;
v7297 = v5530;
v3688 = v3379;
v5690 = 0;
*(uint32_t*)((((uint32_t)v221) + v3688)) = v5690;
*(uint32_t*)((((uint32_t)v221 + 0x04) + v3688)) = v5690;
*(uint32_t*)((((uint32_t)v221 + 0x08) + v3688)) = v5690;
*(uint32_t*)((((uint32_t)v221 + 0x0C) + v3688)) = v5690;
v5530 = v5530 - 0x28;
v5530 -= 4; *(uint32_t*)v5530 = v7299;
v7299 = ((v3688) - (0x34000000));
*(uint32_t*)((((uint32_t)v221 + 0x10) + v3688)) = v5690;
v3688 = v3688 + 0x14;
v3379 = v3688;
v3688 = v3382;
*(uint32_t*)((((uint32_t)v217 + 0x60) + ((v3688) * (0x4)))) = 0x14;
v3688 += 1;
v3382 = v3688;
v3688 = ((v7299) + (0x8));
v5530 -= 4; *(uint32_t*)v5530 = v5534;
v5530 -= 4; *(uint32_t*)v5530 = v7019;
v7019 = ((v7297) + (var_1));
v3100(v3688);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_1C))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x8D8403D;
*(uint32_t*)(v3688) = v7024;
v3688 = ((v7299) + (0x4));
v7019 = ((v7297) + (var_1));
v3100(v3688);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_1C))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x27579BC3;
*(uint32_t*)(v3688) = v7024;
v3688 = 0;
v7019 = ((v7297) + (var_1));
v5534 = v7299;
*(uint32_t*)(((v7297) + (var_1C))) = v3688;
v3100(v7299);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x458B33CB;
*(uint32_t*)(v3688) = v7024;
v5530 = v5530 + 0x18;
v4271:
v3688 = *(uint32_t*)(((v7297) + (var_1C)));
v7019 = ((v7297) + (var_2));
v3100(v5534);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_2)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v5690 = v5690 - 0x458B33C8;
v5530 = v5530 + 0x8;
if ((v5690) > (0x4)) {goto v4271;}
switch (v5690) {
case 0:      goto v4035;
case 1:      goto v1561;
case 2:      goto v3505;
case 3:      goto v7213;
case 4:      goto v1542;
}
v7213:
v5690 = (*(uint32_t*)(((v7297) + (arg_0)))) + 0x0C;
v7019 = ((v7297) + (var_3));
v3100(v5690);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_3)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v5690 = v5690 + 0x8;
v7019 = ((v7297) + (var_4));
v3100(v5690);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_4)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v7024 = 0x0FC00;
v5534 = ((v5690) + (v5690));
v7019 = v5534;
v7019 &= v7024;
v5534 &= 0x0E4;
v7024 = (v5690) - v5534;
v7024 <<= 0x18;
v3688 = v5690;
v3688 &= 0x0FF00;
v3688 ^= 0x3D7EEF;
v3688 = v3688 + v7019;
v5534 = 0x0F2000000;
v7024 = v7024 + v5534;
v5534 = ((v7024) + (v7024));
v7024 ^= 0x8F9CBDFF;
v7019 = 0x0FA000000;
v5534 &= v7019;
v5534 ^= 0x0E0000000;
v5534 = v5534 + v7024;
v7024 = ((v3688) + (0x28111));
v7024 <<= 0x9;
v7024 &= 0x5B9ED800;
v3688 <<= 0x8;
v3688 = v3688 - v7024;
v5534 = v5534 + 0x82634201;
v3688 = v3688 - 0x0FAF8272;
v3688 ^= 0x2DCF6C8E;
v3688 |= v5534;
v5534 = v5690;
v5534 &= 0x0E0000;
v5534 |= 0x3DF1FF7E;
v7024 = v5690;
v7024 &= 0x0EF0000;
v5534 ^= v7024;
v5534 ^= 0x3DF1FF7E;
v7024 = v5690;
v7024 &= 0x0FF0000;
v5534 = v5534 + v5534;
v7024 = (v7024 - v5534) + 0x21E17576;
v7024 ^= 0x21E17500;
v7024 >>= 0x8;
v3688 |= v7024;
v7024 = v3688;
v7024 &= 0x33BFEBD2;
v7024 = v7024 + v7024;
v3688 = (v3688 - v7024) + 0x33BFEBD2;
v7024 = v5690;
v5534 = v5690;
v5534 &= 0x0FD000000;
v7024 &= 0x30000000;
v7024 |= 0x4FBAFFFF;
v5534 ^= v7024;
v5534 ^= 0x0CFBAFFFF;
v5534 = v5534 + v5534;
v5690 &= 0x0FF000000;
v5690 = v5690 - v5534;
v5534 = ((v5690) + (0x4D9A7FBF));
v3688 ^= 0x33BFEBD2;
v5534 >>= 0x18;
v5534 ^= 0x4D;
v5534 |= v3688;
v3688 = (*(uint32_t*)(((v7297) + (arg_0)))) + 0x8;
v7019 = ((v7297) + (var_7));
v3100(v3688);
*(uint8_t*)(v1631) = *(uint8_t*)(v3688);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = ((v7299) + (0x0C));
v7019 = ((v7297) + (var_5));
*(uint8_t*)(((v7297) + (var_6))) = *v1631;
v3100(v7024);
v5690 = *(uint8_t*)(((v7297) + (var_5)));
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v5690 = *(uint8_t*)(((v7297) + (var_7)));
v7024 ^= v5534;
*(uint32_t*)(v3688) = v7024;
v3688 = *(uint8_t*)(((v7297) + (var_6)));
v3688 ^= v5690;
v3688 = v3688 - 1; ;
v5530 = v5530 + 0x20;
if ((v3688) > (0x3)) {goto v1561;}
switch (v3688) {
case 0:      goto v905;
case 1:      goto v366;
case 2:      goto v4255;
case 3:      goto v4295;
}
v1561:
v7019 = 0x458B33C8;
goto v4296;
v1542:
v7024 = *(uint32_t*)(((v7297) + (var_1C)));
v7019 = ((v7297) + (var_8));
v3100(v5534);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_8)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v5690 = v5690 - 0x631E7B96;
v5530 = v5530 + 0x8;
if ((v5690) > (0x6)) {goto v4271;}
switch (v5690) {
case 0:      goto v7155;
case 1:      goto v1602;
case 2:      goto v366;
case 3:      goto v4255;
case 4:      goto v4295;
case 5:      goto v4271;
case 6:      goto v4034;
}
v366:
*(uint32_t*)(((v7297) + (var_10))) = 0x631E7B97;
goto v75;
v4255:
v3688 = ((v7299) + (0x8));
v7019 = ((v7297) + (var_9));
v3100(v3688);
v5690 = *(uint8_t*)(((v7297) + (var_9)));
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x18733AC0;
*(uint32_t*)(v3688) = v7024;
v3688 = *(uint32_t*)(((v7297) + (var_1C)));
v5534 = v7299;
v7019 = ((v7297) + (var_A));
v3100(v5534);
v5690 = *(uint8_t*)(((v7297) + (var_A)));
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x18733AC0;
*(uint32_t*)(v3688) = v7024;
v5530 = v5530 + 0x10;
v4034:
v3688 = *(uint32_t*)(((v7297) + (var_1C)));
v7019 = ((v7297) + (var_1));
v3100(v5534);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_1)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v5530 = v5530 + 0x8;
if ((v5690) == (0x18733ABF)) {goto v905;}
v5530 = ((v5530) + (0x0));
v4569:
if ((v5690) == (0x18733AC0)) {goto v2527;}
v5690 = *(uint32_t*)(((v7297) + (var_1C)));
v7019 = ((v7297) + (var_1));
v3100(v5534);
*(uint32_t*)(((v7297) + (var_10))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_1)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v5530 = v5530 + 0x8;
if ((v5690) != (0x18733ABF)) {goto v4569;}
v905:
*(uint32_t*)(((v7297) + (var_10))) = 0x631E7B96;
v75:
v7024 = ((v7299) + (0x8));
v7019 = ((v7297) + (var_B));
v3100(v7024);
v5690 = *(uint8_t*)(((v7297) + (var_B)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= *(uint32_t*)(((v7297) + (var_10)));
v5534 = v7299;
*(uint32_t*)(v3688) = v7024;
v3688 = *(uint32_t*)(((v7297) + (var_1C)));
v7019 = ((v7297) + (var_C));
v3100(v5534);
v5690 = *(uint8_t*)(((v7297) + (var_C)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= *(uint32_t*)(((v7297) + (var_10)));v5661 = !v7024;
*(uint32_t*)(v3688) = v7024;
v5530 = v5530 + 0x10;
goto v1542;
v4295:
v7019 = 0x458B33CA;
v4296:
v3171(v7019, (((v7299) + (0x8))));
v5690 = *(uint32_t*)(((v7297) + (var_1C)));
v5534 = v7299;
v5530 = v5530 + 0x0C;
v4349(v7019, v5534);
goto v4271;
v4035:
v7024 = (*(uint32_t*)(((v7297) + (arg_0)))) + 0x4;
v7019 = ((v7297) + (var_C));
v3100(v7024);
v5690 = *(uint8_t*)(((v7297) + (var_C)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x0FFFF5B9F;v5661 = !v7024;
v5530 = v5530 + 0x8;
goto v6756;
v3505:
v7299 = v7299 + 0x0C;
v7019 = ((v7297) + (var_C));
v3100(v7299);
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_C)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);v5661 = !v5690;
v5534 = 0x0FFFF5B9F;
v3688 = 0x14;
v5530 = v5530 + 0x8;
goto v2260;
v7155:
v7299 = v7299 + 0x0C;
v7019 = ((v7297) + (var_C));
v3100(v7299);
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_C)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);v5661 = !v5690;
v5534 = 0x0FFFF5B9F;
v3688 = 0x4;
v5530 = v5530 + 0x8;
goto v2260;
v1602:
v7299 = v7299 + 0x0C;
v7019 = ((v7297) + (var_C));
v3100(v7299);
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_C)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);v5661 = !v5690;
v5534 = 0x0FFFF5B9F;
v3688 = 0x82;
v5530 = v5530 + 0x8;
goto v2260;
v2527:
v7299 = v7299 + 0x0C;
v5534 = 0x0FFFF5B9F;
v1880(v7299);
v5690 = v3688;
v3688 = 0x98;
v5530 = v5530 + 0x8;
v2260:
if ((v5690) != (v3688)) {goto v2239;}
v5534 = 0;
v2239:
v3688 = (*(uint32_t*)(((v7297) + (arg_0)))) + 0x4;
v7019 = ((v7297) + (var_C));
v3100(v3688);
v5690 = *(uint8_t*)(((v7297) + (var_C)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= v5534;
v5530 = v5530 + 0x8;
v6756:
*(uint32_t*)(v3688) = v7024;
v3688 = v3382;
v3688 = v3688 - 1; ;
v3382 = v3688;
v3688 = *(uint32_t*)((((uint32_t)v217 + 0x60) + ((v3688) * (0x4))));
v3379 = v3379 - v3688;v5661 = !v3379;
v3688 = *(uint32_t*)(((v7297) + (arg_0)));
v7019 = *(uint32_t*)v5530; v5530 += 4;
v5534 = *(uint32_t*)v5530; v5530 += 4;
v7299 = *(uint32_t*)v5530; v5530 += 4;
v5530 = v7297;
v7297 = *(uint32_t*)v5530; v5530 += 4;
v5530 += 4; return;
#undef var_24
#undef var_1C
#undef var_10
#undef var_C
#undef var_B
#undef var_A
#undef var_9
#undef var_8
#undef var_7
#undef var_6
#undef var_5
#undef var_4
#undef var_3
#undef var_2
#undef var_1
#undef arg_0
}
