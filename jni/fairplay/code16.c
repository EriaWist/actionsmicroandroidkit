#include "code98.h"
void v4386() {
#define var_24 (-(0x24))
#define var_18 (-(0x18))
#define var_14 (-(0x14))
#define var_10 (-(0x10))
#define var_C (-(0x0C))
#define var_8 (-(0x8))
#define var_1 (-(0x1))
#define arg_0 (0x8)
v5530 -= 4; *(uint32_t*)v5530 = v7297;
v7297 = v5530;
v5530 = v5530 - 0x28;
v5530 -= 4; *(uint32_t*)v5530 = v7299;
v5530 -= 4; *(uint32_t*)v5530 = v5534;
v5530 -= 4; *(uint32_t*)v5530 = v7019;
v7019 = v3379;
v5530 -= 4; *(uint32_t*)v5530 = 0x6E;
v3688 = (((uint32_t)v221) + v7019);
v5534 = ((v7019) - (0x34000000));
v5530 -= 4; *(uint32_t*)v5530 = 0x0;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v5562();
v3688 = v3382;
v7019 = v7019 + 0x6E;
*(uint32_t*)((((uint32_t)v217 + 0x60) + ((v3688) * (0x4)))) = 0x6E;
v5690 = ((v5534) + (0x8));
v3379 = v7019;
v3688 += 1;
v7019 = ((v7297) + (var_1));
v3382 = v3688;
v7299 = ((v5534) + (0x10));
v5530 = v5530 + 0x0C;
v3100(v5690);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= v7299;
*(uint32_t*)(v3688) = v7024;
v3688 = ((v5534) + (0x4));
v7019 = ((v7297) + (var_1));
v3100(v3688);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v5690 = ((v5534) + (0x0C));
v7024 ^= v5690;
v7019 = ((v7297) + (var_1));
*(uint32_t*)(v3688) = v7024;
v3100(v7299);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x46571624;
*(uint32_t*)(v3688) = v7024;
v3688 = ((v5534) + (0x0C));
v7019 = ((v7297) + (var_1));
v3100(v3688);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_24))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x6A2F38C1;
*(uint32_t*)(v3688) = v7024;
v3688 = 0;
v7019 = ((v7297) + (var_1));
v7299 = v5534;
*(uint32_t*)(((v7297) + (var_24))) = v3688;
v3100(v5534);
v5690 = *(uint8_t*)(((v7297) + (var_1)));
*(uint32_t*)(((v7297) + (var_18))) = v7024;
v7024 = v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 <<= 0x8;
v7024 |= v5690;
v7024 ^= 0x6A2F38BF;
*(uint32_t*)(v3688) = v7024;
v7019 = v7019;
v5530 = v5530 + 0x28;
v4446:
v3688 = *(uint32_t*)(((v7297) + (var_24)));
v7019 = ((v7297) + (var_1));
v3100(v7299);
*(uint32_t*)(((v7297) + (var_18))) = v7024;
v7024 = *(uint8_t*)(((v7297) + (var_1)));
v5690 = v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 <<= 0x8;
v5690 |= v7024;
v5690 ^= *(uint32_t*)(v3688);
v5690 = v5690 - 0x6A2F38BF;
v5530 = v5530 + 0x8;
if ((v5690) > (0x6)) {goto v4446;}
switch (v5690) {
case 0:      goto v2815;
case 1:      goto v1161;
case 2:      goto v2184;
case 3:      goto v2772;
case 4:      goto v133;
case 5:      goto v1029;
case 6:      goto v782;
}
v2815:
v1880(((*(uint32_t*)(((v7297) + (arg_0)))) + 0x4));
v7299 = 0;
v7019 = ((v5534) + (0x40));
v3171(v3688, v7019);
v7024 = *(uint32_t*)(((v7297) + (arg_0)));
v5530 = v5530 + 0x14;
v1880((v7024 + 0x10));
v7024 = 0;
v3171(v3688, (((v5534) + (0x44))));
v5530 = v5530 + 0x14;
v1880(v7019);
*(uint32_t*)(((v7297) + (var_C))) = v3688;
v1880((((v5534) + (0x44))));
*(uint32_t*)(((v7297) + (var_18))) = v3688;
v1880(((*(uint32_t*)(((v7297) + (arg_0)))) + 0x14));
v7019 = ((v5534) + (0x48));
v3171(v3688, v7019);
*(uint32_t*)(((v7297) + (var_10))) = v7299;
v5530 = v5530 + 0x24;
v1880(v7019);
v5690 = (*(uint32_t*)(((v7297) + (arg_0)))) + 0x0C;
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v1880(v5690);
v7299 = ((v5534) + (0x4C));
v3171(v3688, v7299);
v7024 = ((v5534) + (0x18));
v7019 = 0;
v5530 = v5530 + 0x1C;
v4349(0, v7024);
v4349(v7019, (((v5534) + (0x1C))));
v4349(v7019, (((v5534) + (0x14))));
if ((*(uint32_t*)(((v7297) + (var_18)))) != (v7019)) {goto v174;}
*(uint32_t*)(((v7297) + (var_10))) = 0x1;
v174:
v7019 = *(uint32_t*)(((v7297) + (var_C)));
*(uint32_t*)(((v7297) + (var_C))) = 0x0;
v1880(v7299);
v7024 = ((v5534) + (0x3C));
v7299 = v3688;
v3171(0x0FFFF586C, v7024);
v5530 = v5530 + 0x14;
if (v7019 != 0) {goto v5002;}
*(uint32_t*)(((v7297) + (var_C))) = 0x1;
v5002:
v7024 = *(uint32_t*)(((v7297) + (var_10)));
v5690 = *(uint32_t*)(((v7297) + (var_8)));
*(uint32_t*)(((v7297) + (var_C))) |= v7024;
v3688 = 0;
if (v5690 != 0) {goto v1316;}
v3688 = 0x1;
v1316:
v3688 |= *(uint32_t*)(((v7297) + (var_C)));
v5690 = v7299;
v7019 = 0;
if (v5690 != 0) {goto v5243;}
v7019 = 0x1;
v5243:
v7019 |= v3688;v5661 = !v7019;
v3688 = ((v5534) + (0x0C));
if (v5661 != 0) {goto v5235;}
v3688 = ((v5534) + (0x10));
v5235:
v1880(v3688);
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v3688 = *(uint32_t*)(((v7297) + (var_24)));
*(uint32_t*)(((v7297) + (var_C))) = 0x4C05D09;
v1880(v5534);
v5690 = ((v5534) + (0x4));
v7299 = v3688;
v1880(v5690);
v7024 = ((v5534) + (0x8));
*(uint32_t*)(((v7297) + (var_14))) = v3688;
v1880(v7024);
v5690 = 0x3;
v5530 = v5530 + 0x20;
if (v7019 == 0) {goto v72;}
v5690 = *(uint32_t*)(((v7297) + (var_C)));
v72:
v5530 -= 4; *(uint32_t*)v5530 = 0x0;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v3688 = ((v7299) + (v5690));
*(uint32_t*)(((v7297) + (var_18))) = v7019;
*(uint32_t*)(((v7297) + (var_C))) = 0x0;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v5355();
v3688 = 0x0DC27DD65;
v5530 = v5530 + 0x0C;
if (v7019 == 0) {goto v5932;}
v3688 = 0x0D2F94F95;
goto v5932;
v1161:
v1880((((v5534) + (0x1C))));
v7024 = ((v5534) + (0x54));
v7019 = v3688;
v3171(v7019, v7024);
v5530 = v5530 + 0x14;
v1880((((v5534) + (0x48))));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v1880(v3688);
v5690 = *(uint32_t*)(((v7297) + (var_8)));
v5530 -= 4; *(uint32_t*)v5530 = 0;
v651((v3688 + v7019), v5690);
v5530 = v5530 + 0x1C;
v1880((((v5534) + (0x44))));
v1370((v3688 + 0x4));
v7019 = *(uint8_t*)v873;
v3171(0x0FFFF586F, (((v5534) + (0x3C))));
v3688 = ((v5534) + (0x0C));
v5530 = v5530 + 0x1C;
if ((v7019) != (0x1)) {goto v393;}
v3688 = ((v5534) + (0x10));
v393:
v1880(v3688);
*(uint32_t*)(((v7297) + (var_8))) = v3688;
*(uint32_t*)(((v7297) + (var_10))) = 0x0D98620F1;
v5530 = v5530 + 0x8;
if ((v7019) != (0x1)) {goto v7106;}
*(uint32_t*)(((v7297) + (var_10))) = 0x0A6E8CD59;
v7106:
v5690 = *(uint32_t*)(((v7297) + (var_24)));
v1880(v7299);
v7024 = ((v5534) + (0x4));
v7299 = v3688;
v1880(v7024);
*(uint32_t*)(((v7297) + (var_14))) = v3688;
v1880((((v5534) + (0x8))));
v5690 = (*(uint32_t*)(((v7297) + (var_10)))) + v7299;
*(uint32_t*)(((v7297) + (var_18))) = v7019;
*(uint32_t*)(((v7297) + (var_C))) = 0x1;
v3171(v5690, v3688);
v3688 = 0x0CCFFF2B0;
v5530 = v5530 + 0x24;
if ((v7019) != (0x1)) {goto v5932;}
v3688 = 0x0DC27DD64;
goto v5932;
v2184:
v1880((((v5534) + (0x48))));
v5530 -= 4; *(uint32_t*)v5530 = 0x0;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v5530 -= 4; *(uint32_t*)v5530 = 0;
v5355();
v5530 = v5530 + 0x14;
v1880((((v5534) + (0x40))));
v5690 = ((v5534) + (0x28));
v5530 -= 4; *(uint32_t*)v5530 = 0x0;
v651(v3688, v5690);
v7024 = ((v5534) + (0x30));
v5530 = v5530 + 0x14;
v4349((((v5534) + (0x18))), v7024);
v1880((((v5534) + (0x4C))));
v3171(v3688, (((v5534) + (0x24))));
v3688 = ((v5534) + (0x20));
v5530 = v5530 + 0x14;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v5530 -= 4;v2757();
v5530 = v5530 + 0x4;
v1880((((v5534) + (0x2C))));
v5690 = ((v5534) + (0x3C));
v7019 = v3688;
v3688 = ((v5534) + (0x0C));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v3171(v7019, v5690);
v3688 = ((v5534) + (0x10));
v5530 = v5530 + 0x14;
if (v7019 == 0) {goto v5516;}
v3688 = *(uint32_t*)(((v7297) + (var_8)));
v5516:
v1880(v3688);
v7024 = *(uint32_t*)(((v7297) + (var_24)));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
*(uint32_t*)(((v7297) + (var_C))) = 0x0B2BE8E1;
v1880(v7299);
v7299 = v3688;
v1880((((v5534) + (0x4))));
v5690 = ((v5534) + (0x8));
*(uint32_t*)(((v7297) + (var_14))) = v3688;
v1880(v5690);
v5530 = v5530 + 0x20;
if (v7019 != 0) {goto v2185;}
v5690 |= 0x0FFFFFFFF;v5661 = !v5690;
goto v2187;
v2185:
v5690 = *(uint32_t*)(((v7297) + (var_C)));
v2187:
v3171((((v7299) + (v5690))), v3688);
v5690 = 0x0DC27DD63;
v3688 = 0x9BEC5D8C;
goto v1697;
v2772:
v1880((((v5534) + (0x18))));
v5690 = ((v5534) + (0x50));
v7019 = v3688;
v3171(v7019, v5690);
v5530 = v5530 + 0x14;
v1880((((v5534) + (0x48))));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v1880((((v5534) + (0x40))));
v5690 = *(uint32_t*)(((v7297) + (var_8)));
*(uint32_t*)(((v7297) + (var_10))) = v3688;
v1880(v5690);
v3171((v3688 + v7019), (*(uint32_t*)(((v7297) + (var_8)))));
v5690 = *(uint32_t*)(((v7297) + (var_10)));
v3688 = ((v5534) + (0x24));
v5530 = v5530 + 0x24;
v4349((v5690 + v7019), v3688);
v1880((((v5534) + (0x44))));
v3171(v3688, (((v5534) + (0x30))));
v7024 = ((v5534) + (0x2C));
v5530 = v5530 + 0x14;
v4349((((v5534) + (0x1C))), v7024);
v1880((((v5534) + (0x4C))));
v3171(v3688, (((v5534) + (0x28))));
v3688 = ((v5534) + (0x20));
v5530 = v5530 + 0x14;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v5530 -= 4;v3518();
v5530 = v5530 + 0x4;
v1880((((v5534) + (0x34))));
v7019 = v3688;
v3688 = ((v5534) + (0x0C));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v3171(v7019, (((v5534) + (0x3C))));
v3688 = ((v5534) + (0x10));
v5530 = v5530 + 0x14;
if (v7019 == 0) {goto v3997;}
v3688 = *(uint32_t*)(((v7297) + (var_8)));
v3997:
v1880(v3688);
v7024 = *(uint32_t*)(((v7297) + (var_24)));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
*(uint32_t*)(((v7297) + (var_C))) = 0x94747DB;
v1880(v7299);
v7299 = v3688;
v1880((((v5534) + (0x4))));
v5690 = ((v5534) + (0x8));
*(uint32_t*)(((v7297) + (var_14))) = v3688;
v1880(v5690);
v5690 = 0x1;
v5530 = v5530 + 0x20;
if (v7019 == 0) {goto v5638;}
v5690 = *(uint32_t*)(((v7297) + (var_C)));
v5638:
v3171((((v7299) + (v5690))), v3688);
v5690 = 0x0DC27DD62;
v3688 = 0x0BDFDBE7A;
v1697:
*(uint32_t*)(((v7297) + (var_18))) = v7019;
*(uint32_t*)(((v7297) + (var_C))) = 0x0;
v5530 = v5530 + 0x0C;
if (v7019 != 0) {goto v5932;}
v3688 = v5690;
v5932:
v7299 = v7299 + v3688;
v3171(v7299, (*(uint32_t*)(((v7297) + (var_14)))));
v5690 = *(uint32_t*)(((v7297) + (var_24)));
v7024 = *(uint32_t*)(((v7297) + (var_8)));
v7299 = v5534;
v5530 = v5530 + 0x0C;
v3171(v7024, v7299);
v3688 = *(uint32_t*)(((v7297) + (var_18)));
v1292 = v3688; v1294 = *(uint32_t*)(((v7297) + (var_C))); v4111 = v1292 - v1294; v5661 = ((v4111 == 0) ? 1 : 0); v2325 = (v4111 >> 31); v4048 = ((v1292 < v1294) ? 1 : 0); v1259 = (((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1));
v5530 = v5530 + 0x0C;
goto v748;
v133:
v1880((((v5534) + (0x50))));
v7024 = ((v5534) + (0x54));
v7019 = v3688;
v1880(v7024);
v7019 = v7019 + v3688;
v1880((((v5534) + (0x40))));
v3171((v3688 + v7019), (((v5534) + (0x30))));
v7024 = ((v5534) + (0x24));
v5530 = v5530 + 0x24;
v4349((((v5534) + (0x14))), v7024);
v1880((((v5534) + (0x4C))));
v3171(v3688, (((v5534) + (0x28))));
v3688 = ((v5534) + (0x20));
v5530 = v5530 + 0x14;
v5530 -= 4; *(uint32_t*)v5530 = v3688;
v5530 -= 4;v2752();
v5530 = v5530 + 0x4;
v1880((((v5534) + (0x2C))));
v7019 = v3688;
v3688 = ((v5534) + (0x0C));
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v3171(v7019, (((v5534) + (0x3C))));
v3688 = ((v5534) + (0x10));
v5530 = v5530 + 0x14;
if (v7019 == 0) {goto v7085;}
v3688 = *(uint32_t*)(((v7297) + (var_8)));
v7085:
v1880(v3688);
*(uint32_t*)(((v7297) + (var_8))) = v3688;
*(uint32_t*)(((v7297) + (var_10))) = 0x0D5F60876;
v5530 = v5530 + 0x8;
if (v7019 != 0) {goto v6304;}
*(uint32_t*)(((v7297) + (var_10))) = 0x0D9C30B04;
v6304:
v5690 = *(uint32_t*)(((v7297) + (var_24)));
v1880(v7299);
v7024 = ((v5534) + (0x4));
*(uint32_t*)(((v7297) + (var_C))) = v3688;
v1880(v7024);
*(uint32_t*)(((v7297) + (var_14))) = v3688;
v1880((((v5534) + (0x8))));
v3171(((*(uint32_t*)(((v7297) + (var_10)))) + *(uint32_t*)(((v7297) + (var_C)))), v3688);
v3688 = 0x0C674BA2C;
v5530 = v5530 + 0x24;
if (v7019 != 0) {goto v1266;}
v3688 = 0x0D253D7F4;
v1266:
v3171((v3688 + *(uint32_t*)(((v7297) + (var_C)))), (*(uint32_t*)(((v7297) + (var_14)))));
v3688 = *(uint32_t*)(((v7297) + (var_24)));
v5690 = *(uint32_t*)(((v7297) + (var_8)));
v5530 = v5530 + 0x0C;
v4349(v5690, v7299);
if (v7019 != 0) {goto v782;}
v1029:
v7024 = *(uint32_t*)(((v7297) + (var_24)));
v1880(v7299);
v5530 = v5530 + 0x8;
if ((v3688) == (0x11180619)) {goto v6066;}
v1292 = v3688; v1294 = 0x1118061A; v4111 = v1292 - v1294; v5661 = ((v4111 == 0) ? 1 : 0); v2325 = (v4111 >> 31); v4048 = ((v1292 < v1294) ? 1 : 0); v1259 = (((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1));
v748:
if (v5661 != 0) {goto v4446;}
v782:
v3688 = *(uint32_t*)(((v7297) + (var_24)));
v1880(v7299);
v5530 = v5530 + 0x8;
if ((v3688) != (0x46571624)) {goto v1029;}
v5534 = v5534 + 0x3C;
v1880(v5534);
v5534 = v3688;
v5530 = v5530 + 0x8;
v6268:
v3171(v5534, ((*(uint32_t*)(((v7297) + (arg_0)))) + 0x8));
v3688 = v3382;
v5690 = *(uint32_t*)((((uint32_t)v217 + 0x5C) + ((v3688) * (0x4))));
v3688 = v3688 - 1; ;
v3379 = v3379 - v5690;
v5530 = v5530 + 0x0C;
v7019 = *(uint32_t*)v5530; v5530 += 4;
v3382 = v3688;
v3688 = v5534;
v5534 = *(uint32_t*)v5530; v5530 += 4;
v7299 = *(uint32_t*)v5530; v5530 += 4;
v5530 = v7297;
v7297 = *(uint32_t*)v5530; v5530 += 4;
v5530 += 4; return;
v6066:
v1880((((v5534) + (0x48))));
v7299 = 0;
v7019 = v3688;
v1880(v7019);
v5534 = v5534 + 0x14;
*(uint32_t*)(((v7297) + (var_8))) = v3688;
v1880(v5534);
v3171((v3688 + *(uint32_t*)(((v7297) + (var_8)))), v7019);
v5534 = 0;
v5530 = v5530 + 0x24;
goto v6268;
#undef var_24
#undef var_18
#undef var_14
#undef var_10
#undef var_C
#undef var_8
#undef var_1
#undef arg_0
}
