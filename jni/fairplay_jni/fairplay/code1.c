#include "code98.h"

static uint8_t v5225[0x200];
static uint8_t *v2928 = v5225 + 0x100;
static uint8_t *v2960 = v5225 + 0x101;
static uint32_t *v71 = (uint32_t*)(v5225 + 0x104);

void v2935() {
#define var_11C (-(0x11C)) 
#define var_118 (-(0x118)) 
#define var_114 (-(0x114)) 
#define var_110 (-(0x110)) 
#define var_109 (-(0x109)) 
#define var_108 (-(0x108)) 
#define var_104 (-(0x104)) 
#define var_88 (-(0x88)) 
#define var_87 (-(0x87)) 
#define var_86 (-(0x86)) 
#define var_85 (-(0x85)) 
#define var_4 (-(0x4)) 
#define arg_0 (0x8) 
#define arg_4 (0x0C) 
#define arg_8 (0x10) 
#define arg_C (0x14) 

// .text:0040CBA0 ; =============== S U B R O U T I N E =======================================

// .text:0040CBA0

// .text:0040CBA0 ; Attributes: bp-based frame

// .text:0040CBA0

// .text:0040CBA0 sub_40CBA0      proc near               ; CODE XREF: sub_40CBA0+1BBp

// .text:0040CBA0                                         ; sub_40CBA0+1D8p ...

// .text:0040CBA0

// .text:0040CBA0 var_11C         = dword ptr -11Ch

// .text:0040CBA0 var_118         = dword ptr -118h

// .text:0040CBA0 var_114         = dword ptr -114h

// .text:0040CBA0 var_110         = dword ptr -110h

// .text:0040CBA0 var_109         = byte ptr -109h

// .text:0040CBA0 var_108         = dword ptr -108h

// .text:0040CBA0 var_104         = dword ptr -104h

// .text:0040CBA0 var_88          = byte ptr -88h

// .text:0040CBA0 var_87          = byte ptr -87h

// .text:0040CBA0 var_86          = byte ptr -86h

// .text:0040CBA0 var_85          = byte ptr -85h

// .text:0040CBA0 var_4           = dword ptr -4

// .text:0040CBA0 arg_0           = dword ptr  8

// .text:0040CBA0 arg_4           = dword ptr  0Ch

// .text:0040CBA0 arg_8           = dword ptr  10h

// .text:0040CBA0 arg_C           = dword ptr  14h

// .text:0040CBA0

// .text:0040CBA0                 push    ebp
v440(v7297);
// .text:0040CBA1                 mov     ebp, esp
v7297 = v5530;
// .text:0040CBA3                 sub     esp, 124h
v5530 = v5530 - 0x124;
// .text:0040CBA9                 mov     eax, dword_699600
v3688 = v38;
// .text:0040CBAE                 xor     eax, ebp
v3688 ^= v7297;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CBB0                 mov     [ebp+var_4], eax
*(uint32_t*)(((v7297) + (var_4))) = v3688;
// .text:0040CBB3                 mov     eax, [ebp+arg_0]
v3688 = *(uint32_t*)(((v7297) + (arg_0)));
// .text:0040CBB6                 push    ebx
v440(v7299);
// .text:0040CBB7                 xor     ebx, ebx
v7299 ^= v7299;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CBB9                 push    esi
v440(v5534);
// .text:0040CBBA                 push    edi
v440(v7019);
// .text:0040CBBB                 mov     [ebp+var_114], ebx
*(uint32_t*)(((v7297) + (var_114))) = v7299;
// .text:0040CBC1                 cmp     eax, 3          ; switch 4 cases
v1292 = v3688; v1294 = 0x3; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CBC4                 ja      loc_40D268      ; jumptable 0040CBCA default case
v4751(); if (v4048 == 0 && v5661 == 0) {v1853(); goto v3257;} else {v1853();}
// .text:0040CBCA                 jmp     ds:off_40D280[eax*4] ; switch jump
switch (v3688) {
case 0:      goto v4793;
case 1:      goto v4741;
case 2:      goto v4289;
case 3:      goto v7280;
}
// .text:0040CBD1

// .text:0040CBD1 loc_40CBD1:                             ; DATA XREF: .text:off_40D280o
v4793:
// .text:0040CBD1                 push    80h             ; jumptable 0040CBCA case 0
v440(0x80);
// .text:0040CBD6                 lea     eax, [ebp+var_108]
v3688 = ((v7297) + (var_108));
// .text:0040CBDC                 push    ebx             ; int
v440(v7299);
// .text:0040CBDD                 push    eax             ; void *
v440(v3688);
// .text:0040CBDE                 call    _memset
v5562();
// .text:0040CBE3                 add     esp, 0Ch
v5530 = v5530 + 0x0C;
// .text:0040CBE6                 push    ebx             ; Time
v440(v7299);
// .text:0040CBE7                 call    __time64
v5453();
// .text:0040CBEC                 xor     [ebp+var_108], eax
*(uint32_t*)(((v7297) + (var_108))) ^= v3688;if (*(uint32_t*)(((v7297) + (var_108))) == 0) v5661 = 1; else v5661 = 0;
// .text:0040CBF2                 xor     [ebp+var_104], edx
*(uint32_t*)(((v7297) + (var_104))) ^= v7024;if (*(uint32_t*)(((v7297) + (var_104))) == 0) v5661 = 1; else v5661 = 0;
// .text:0040CBF8                 add     esp, 4
v5530 = v5530 + 0x4;
// .text:0040CBFB                 call    _clock
v3479();
// .text:0040CC00                 xor     [ebp+var_108], eax
*(uint32_t*)(((v7297) + (var_108))) ^= v3688;if (*(uint32_t*)(((v7297) + (var_108))) == 0) v5661 = 1; else v5661 = 0;
// .text:0040CC06                 call    GetCurrentProcessId
v3029();
// .text:0040CC0B                 xor     [ebp+var_108], eax
*(uint32_t*)(((v7297) + (var_108))) ^= v3688;if (*(uint32_t*)(((v7297) + (var_108))) == 0) v5661 = 1; else v5661 = 0;
// .text:0040CC11                 push    offset LibFileName ; "ADVAPI32.DLL"
v440(0x89898989);                       /* dummy */
// .text:0040CC16                 mov     [ebp+var_110], ebx
*(uint32_t*)(((v7297) + (var_110))) = v7299;
// .text:0040CC1C                 call    ds:LoadLibraryW
v1328(); v5530 += 0x04;
// .text:0040CC22                 mov     edi, eax
v7019 = v3688;
// .text:0040CC24                 cmp     edi, ebx
v1292 = v7019; v1294 = v7299; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CC26                 jz      loc_40CD28
v2673(); if (v5661 == 1) {v1853(); goto v2724;} else {v1853();}
// .text:0040CC2C                 mov     esi, ds:GetProcAddress
v5534 = (uint32_t)v7191;
// .text:0040CC32                 push    offset ProcName ; "CryptAcquireContextA"
v440(0x01);
// .text:0040CC37                 push    edi             ; hModule
v440(0x89898989);
// .text:0040CC38                 call    esi ; GetProcAddress
((null_func_t)v5534)(); v5530 += 0x08;
// .text:0040CC3A                 mov     ebx, eax
v7299 = v3688;
// .text:0040CC3C                 test    ebx, ebx
v4111 = v7299 & v7299; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CC3E                 jz      loc_40CD28
v2673(); if (v5661 == 1) {v1853(); goto v2724;} else {v1853();}
// .text:0040CC44                 push    offset aCryptgenrandom ; "CryptGenRandom"
v440(0x02);
// .text:0040CC49                 push    edi             ; hModule
v440(0x8989899);
// .text:0040CC4A                 call    esi ; GetProcAddress
((null_func_t)v5534)(); v5530 += 0x08;
// .text:0040CC4C                 mov     [ebp+var_11C], eax
*(uint32_t*)(((v7297) + (var_11C))) = v3688;
// .text:0040CC52                 test    eax, eax
v4111 = v3688 & v3688; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CC54                 jz      loc_40CD28
v2673(); if (v5661 == 1) {v1853(); goto v2724;} else {v1853();}
// .text:0040CC5A                 push    offset aCryptreleaseco ; "CryptReleaseContext"
v440(0x03);
// .text:0040CC5F                 push    edi             ; hModule
v440(0x89898989);
// .text:0040CC60                 call    esi ; GetProcAddress
((null_func_t)v5534)(); v5530 += 0x08;
// .text:0040CC62                 mov     [ebp+var_118], eax
*(uint32_t*)(((v7297) + (var_118))) = v3688;
// .text:0040CC68                 test    eax, eax
v4111 = v3688 & v3688; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CC6A                 jz      loc_40CD28
v2673(); if (v5661 == 1) {v1853(); goto v2724;} else {v1853();}
// .text:0040CC70                 push    0F0000000h
v440(0x0F0000000);
// .text:0040CC75                 push    1
v440(0x1);
// .text:0040CC77                 push    0
v440(0x0);
// .text:0040CC79                 lea     ecx, [ebp+var_110]
v5690 = ((v7297) + (var_110));
// .text:0040CC7F                 push    0
v440(0x0);
// .text:0040CC81                 push    ecx
v440(v5690);
// .text:0040CC82                 call    ebx
((null_func_t)v7299)();
// .text:0040CC84                 add     esp, 14h
v5530 = v5530 + 0x14;
// .text:0040CC87                 test    eax, eax
v4111 = v3688 & v3688; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CC89                 jz      loc_40CD28
v2673(); if (v5661 == 1) {v1853(); goto v2724;} else {v1853();}
// .text:0040CC8F                 mov     eax, [ebp+var_110]
v3688 = *(uint32_t*)(((v7297) + (var_110)));
// .text:0040CC95                 lea     edx, [ebp+var_88]
v7024 = ((v7297) + (var_88));
// .text:0040CC9B                 push    edx
v440(v7024);
// .text:0040CC9C                 push    80h
v440(0x80);
// .text:0040CCA1                 push    eax
v440(v3688);
// .text:0040CCA2                 call    [ebp+var_11C]
((null_func_t)*(uint32_t*)((v7297)+(var_11C)))();
// .text:0040CCA8                 add     esp, 0Ch
v5530 = v5530 + 0x0C;
// .text:0040CCAB                 test    eax, eax
v4111 = v3688 & v3688; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CCAD                 jz      short loc_40CD28
v2673(); if (v5661 == 1) {v1853(); goto v2724;} else {v1853();}
// .text:0040CCAF                 mov     esi, 3
v5534 = 0x3;
// .text:0040CCB4                 lea     ecx, [ebp+var_108+2]
v5690 = ((v7297) + (((var_108) + (0x2))));
// .text:0040CCBA                 xor     eax, eax
v3688 ^= v3688;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CCBC                 sub     esi, ecx
v5534 = v5534 - v5690;
// .text:0040CCBE                 cmp     eax, 80h
v1292 = v3688; v1294 = 0x80; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CCC3                 jnb     short loc_40CD28
v5495(); if (v4048 == 0) {v1853(); goto v2724;} else {v1853();}
// .text:0040CCC5

// .text:0040CCC5 loc_40CCC5:                             ; CODE XREF: sub_40CBA0+186j
v397:
// .text:0040CCC5                 mov     dl, [ebp+eax+var_88]
*(uint8_t*)(v2037) = *(uint8_t*)(((v7297) + (((v3688) + (var_88)))));
// .text:0040CCCC                 xor     byte ptr [ebp+eax+var_108], dl
*(uint8_t*)(v7297 + v3688 + (var_108)) ^= *v2037;
// .text:0040CCD3                 lea     ecx, [eax+1]
v5690 = ((v3688) + (0x1));
// .text:0040CCD6                 cmp     ecx, 80h
v1292 = v5690; v1294 = 0x80; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CCDC                 jnb     short loc_40CD28
v5495(); if (v4048 == 0) {v1853(); goto v2724;} else {v1853();}
// .text:0040CCDE                 mov     dl, [ebp+eax+var_87]
*(uint8_t*)(v2037) = *(uint8_t*)(((v7297) + (((v3688) + (var_87)))));
// .text:0040CCE5                 xor     byte ptr [ebp+eax+var_108+1], dl
*(uint8_t*)(v7297 + v3688 + (var_108) + 1) ^= *v2037;
// .text:0040CCEC                 cmp     eax, 7Eh
v1292 = v3688; v1294 = 0x7E; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CCEF                 jnb     short loc_40CD28
v5495(); if (v4048 == 0) {v1853(); goto v2724;} else {v1853();}
// .text:0040CCF1                 mov     dl, [ebp+eax+var_86]
*(uint8_t*)(v2037) = *(uint8_t*)(((v7297) + (((v3688) + (var_86)))));
// .text:0040CCF8                 xor     byte ptr [ebp+eax+var_108+2], dl
*(uint8_t*)(v7297 + v3688 + (var_108) + 2) ^= *v2037;
// .text:0040CCFF                 lea     ecx, [ebp+eax+var_108+2]
v5690 = ((v7297) + (((v3688) + (((var_108) + (0x2))))));
// .text:0040CD06                 add     ecx, esi
v5690 = v5690 + v5534;
// .text:0040CD08                 cmp     ecx, 80h
v1292 = v5690; v1294 = 0x80; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CD0E                 jnb     short loc_40CD28
v5495(); if (v4048 == 0) {v1853(); goto v2724;} else {v1853();}
// .text:0040CD10                 mov     cl, [ebp+eax+var_85]
*(uint8_t*)(v1631) = *(uint8_t*)(((v7297) + (((v3688) + (var_85)))));
// .text:0040CD17                 xor     byte ptr [ebp+eax+var_108+3], cl
*(uint8_t*)(v7297 + v3688 + (var_108) + 3) ^= *v2037;
// .text:0040CD1E                 add     eax, 4
v3688 = v3688 + 0x4;
// .text:0040CD21                 cmp     eax, 80h
v1292 = v3688; v1294 = 0x80; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CD26                 jb      short loc_40CCC5
v5495(); if (v4048 == 1) {v1853(); goto v397;} else {v1853();}
// .text:0040CD28

// .text:0040CD28 loc_40CD28:                             ; CODE XREF: sub_40CBA0+86j
v2724:
// .text:0040CD28                                         ; sub_40CBA0+9Ej ...

// .text:0040CD28                 mov     eax, [ebp+var_110]
v3688 = *(uint32_t*)(((v7297) + (var_110)));
// .text:0040CD2E                 test    eax, eax
v4111 = v3688 & v3688; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CD30                 jz      short loc_40CD3E
v2673(); if (v5661 == 1) {v1853(); goto v6650;} else {v1853();}
// .text:0040CD32                 push    0
v440(0x0);
// .text:0040CD34                 push    eax
v440(v3688);
// .text:0040CD35                 call    [ebp+var_118]
((null_func_t)*(uint32_t*)(v7297+var_118))();
// .text:0040CD3B                 add     esp, 8
v5530 = v5530 + 0x8;
// .text:0040CD3E

// .text:0040CD3E loc_40CD3E:                             ; CODE XREF: sub_40CBA0+190j
v6650:
// .text:0040CD3E                 test    edi, edi
v4111 = v7019 & v7019; v1259 = 0; v4048 = 0; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31;
// .text:0040CD40                 jz      short loc_40CD49
v2673(); if (v5661 == 1) {v1853(); goto v3034;} else {v1853();}
// .text:0040CD42                 push    edi             ; hLibModule
v440(v7019);
// .text:0040CD43                 call    ds:FreeLibrary
v6646(); v5530 += 0x04;
// .text:0040CD49

// .text:0040CD49 loc_40CD49:                             ; CODE XREF: sub_40CBA0+1A0j
v3034:
// .text:0040CD49                 mov     eax, [ebp+arg_4]
v3688 = *(uint32_t*)(((v7297) + (arg_4)));
// .text:0040CD4C                 push    80h
v440(0x80);
// .text:0040CD51                 lea     edx, [ebp+var_108]
v7024 = ((v7297) + (var_108));
// .text:0040CD57                 push    edx
v440(v7024);
// .text:0040CD58                 push    eax
v440(v3688);
// .text:0040CD59                 push    1
v440(0x1);
// .text:0040CD5B                 call    sub_40CBA0
v440(0x89898989);v2935();
// .text:0040CD60                 add     esp, 10h
v5530 = v5530 + 0x10;
// .text:0040CD63                 mov     dword_786134, 186A40h
*v71 = 0x186A40;
// .text:0040CD6D                 mov     esi, 40h
v5534 = 0x40;
// .text:0040CD72

// .text:0040CD72 loc_40CD72:                             ; CODE XREF: sub_40CBA0+1E1j
v7188:
// .text:0040CD72                 mov     ecx, [ebp+arg_4]
v5690 = *(uint32_t*)(((v7297) + (arg_4)));
// .text:0040CD75                 push    ecx
v440(v5690);
// .text:0040CD76                 push    2
v440(0x2);
// .text:0040CD78                 call    sub_40CBA0
v440(0x89898989);v2935();
// .text:0040CD7D                 add     esp, 8
v5530 = v5530 + 0x8;
// .text:0040CD80                 dec     esi
v4111 = v5534 = v5534 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CD81                 jnz     short loc_40CD72
v2673(); if (v5661 == 0) {v1853(); goto v7188;} else {v1853();}
// .text:0040CD83                 mov     eax, [ebp+var_114]
v3688 = *(uint32_t*)(((v7297) + (var_114)));
// .text:0040CD89                 pop     edi
v6374(&v7019);
// .text:0040CD8A                 pop     esi
v6374(&v5534);
// .text:0040CD8B                 pop     ebx
v6374(&v7299);
// .text:0040CD8C                 mov     ecx, [ebp+var_4]
v5690 = *(uint32_t*)(((v7297) + (var_4)));
// .text:0040CD8F                 xor     ecx, ebp
v5690 ^= v7297;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CD91                 call    sub_6132A9

// .text:0040CD96                 mov     esp, ebp
v5530 = v7297;
// .text:0040CD98                 pop     ebp
v6374(&v7297);
// .text:0040CD99                 retn
v6374(&v4111); return;
// .text:0040CD9A ; ---------------------------------------------------------------------------

// .text:0040CD9A

// .text:0040CD9A loc_40CD9A:                             ; CODE XREF: sub_40CBA0+2Aj
v4741:
// .text:0040CD9A                                         ; DATA XREF: .text:off_40D280o

// .text:0040CD9A                 mov     edi, [ebp+arg_C] ; jumptable 0040CBCA case 1
v7019 = *(uint32_t*)(((v7297) + (arg_C)));
// .text:0040CD9D                 cmp     edi, ebx
v1292 = v7019; v1294 = v7299; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CD9F                 jle     loc_40D268      ; jumptable 0040CBCA default case
v476(); if (v5661 == 1 || (v2325 != v1259 && v2325 != -1 && v1259 != -1)) {v1853(); goto v3257;} else {v1853();}
// .text:0040CDA5                 dec     byte_786130
*v2928 -= 1; v4111 = *v2928; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CDAB                 mov     esi, [ebp+arg_8]
v5534 = *(uint32_t*)(((v7297) + (arg_8)));
// .text:0040CDAE                 mov     ecx, 2
v5690 = 0x2;
// .text:0040CDB3

// .text:0040CDB3 loc_40CDB3:                             ; CODE XREF: sub_40CBA0+416j
v1579:
// .text:0040CDB3                 movzx   eax, byte_786130
v3688 = *v2928;
// .text:0040CDBA                 inc     eax
v3688 += 1;
// .text:0040CDBB                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CDC0                 jns     short loc_40CDC9
v152(); if (v2325 == 0) {v1853(); goto v5511;} else {v1853();}
// .text:0040CDC2                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CDC3                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CDC8                 inc     eax
v3688 += 1;
// .text:0040CDC9

// .text:0040CDC9 loc_40CDC9:                             ; CODE XREF: sub_40CBA0+220j
v5511:
// .text:0040CDC9                 movzx   ebx, al
v7299 = *(uint8_t*)v873;
// .text:0040CDCC                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040CDD1                 movzx   eax, byte_786030[ebx]
v3688 = v5225[v7299];
// .text:0040CDD8                 mov     [ebp+var_109], al
*(uint8_t*)(((v7297) + (var_109))) = *v873;
// .text:0040CDDE                 lea     eax, [ecx-2]
v3688 = ((v5690) - (0x2));
// .text:0040CDE1                 cdq
if (*(int32_t*)&v3688 < 0) *(int32_t*)&v7024 = -1; else v7024 = 0;
// .text:0040CDE2                 idiv    edi
*v5828 = v7024; *v2081 = v3688; v1875 = (int32_t)v7019; v3688 = (uint32_t)(v3009 / v1875); v7024 = (uint32_t)(v3009 % v1875);
// .text:0040CDE4                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040CDEB                 movzx   edx, byte ptr [edx+esi]
v7024 = *(uint8_t*)(((v7024) + (v5534)));
// .text:0040CDEF                 add     edx, eax
v7024 = v7024 + v3688;
// .text:0040CDF1                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CDF8                 add     eax, edx
v3688 = v3688 + v7024;
// .text:0040CDFA                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CDFF                 jns     short loc_40CE08
v152(); if (v2325 == 0) {v1853(); goto v4226;} else {v1853();}
// .text:0040CE01                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE02                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE07                 inc     eax
v3688 += 1;
// .text:0040CE08

// .text:0040CE08 loc_40CE08:                             ; CODE XREF: sub_40CBA0+25Fj
v4226:
// .text:0040CE08                 movzx   edx, al
v7024 = *(uint8_t*)v873;
// .text:0040CE0B                 mov     byte_786131, al
*v2960 = *v873;
// .text:0040CE10                 movzx   eax, byte_786030[edx]
v3688 = v5225[v7024];
// .text:0040CE17                 mov     byte_786030[ebx], al
v5225[v7299] = *v873;
// .text:0040CE1D                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CE24                 movzx   edx, byte_786131
v7024 = *v2960;
// .text:0040CE2B                 mov     byte_786030[edx], al
v5225[v7024] = *v873;
// .text:0040CE31                 movzx   eax, byte_786130
v3688 = *v2928;
// .text:0040CE38                 inc     eax
v3688 += 1;
// .text:0040CE39                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE3E                 jns     short loc_40CE47
v152(); if (v2325 == 0) {v1853(); goto v4773;} else {v1853();}
// .text:0040CE40                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE41                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE46                 inc     eax
v3688 += 1;
// .text:0040CE47

// .text:0040CE47 loc_40CE47:                             ; CODE XREF: sub_40CBA0+29Ej
v4773:
// .text:0040CE47                 movzx   ebx, al
v7299 = *(uint8_t*)v873;
// .text:0040CE4A                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040CE4F                 movzx   eax, byte_786030[ebx]
v3688 = v5225[v7299];
// .text:0040CE56                 mov     [ebp+var_109], al
*(uint8_t*)(((v7297) + (var_109))) = *v873;
// .text:0040CE5C                 lea     eax, [ecx-1]
v3688 = ((v5690) - (0x1));
// .text:0040CE5F                 cdq
if (*(int32_t*)&v3688 < 0) *(int32_t*)&v7024 = -1; else v7024 = 0;
// .text:0040CE60                 idiv    edi
*v5828 = v7024; *v2081 = v3688; v1875 = (int32_t)v7019; v3688 = (uint32_t)(v3009 / v1875); v7024 = (uint32_t)(v3009 % v1875);
// .text:0040CE62                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040CE69                 movzx   edx, byte ptr [edx+esi]
v7024 = *(uint8_t*)(((v7024) + (v5534)));
// .text:0040CE6D                 add     edx, eax
v7024 = v7024 + v3688;
// .text:0040CE6F                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CE76                 add     eax, edx
v3688 = v3688 + v7024;
// .text:0040CE78                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE7D                 jns     short loc_40CE86
v152(); if (v2325 == 0) {v1853(); goto v3150;} else {v1853();}
// .text:0040CE7F                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE80                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CE85                 inc     eax
v3688 += 1;
// .text:0040CE86

// .text:0040CE86 loc_40CE86:                             ; CODE XREF: sub_40CBA0+2DDj
v3150:
// .text:0040CE86                 movzx   edx, al
v7024 = *(uint8_t*)v873;
// .text:0040CE89                 mov     byte_786131, al
*v2960 = *v873;
// .text:0040CE8E                 movzx   eax, byte_786030[edx]
v3688 = v5225[v7024];
// .text:0040CE95                 mov     byte_786030[ebx], al
v5225[v7299] = *v873;
// .text:0040CE9B                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CEA2                 movzx   edx, byte_786131
v7024 = *v2960;
// .text:0040CEA9                 mov     byte_786030[edx], al
v5225[v7024] = *v873;
// .text:0040CEAF                 movzx   eax, byte_786130
v3688 = *v2928;
// .text:0040CEB6                 inc     eax
v3688 += 1;
// .text:0040CEB7                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CEBC                 jns     short loc_40CEC5
v152(); if (v2325 == 0) {v1853(); goto v4484;} else {v1853();}
// .text:0040CEBE                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CEBF                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CEC4                 inc     eax
v3688 += 1;
// .text:0040CEC5

// .text:0040CEC5 loc_40CEC5:                             ; CODE XREF: sub_40CBA0+31Cj
v4484:
// .text:0040CEC5                 movzx   ebx, al
v7299 = *(uint8_t*)v873;
// .text:0040CEC8                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040CECD                 movzx   eax, byte_786030[ebx]
v3688 = v5225[v7299];
// .text:0040CED4                 mov     [ebp+var_109], al
*(uint8_t*)(((v7297) + (var_109))) = *v873;
// .text:0040CEDA                 mov     eax, ecx
v3688 = v5690;
// .text:0040CEDC                 cdq
if (*(int32_t*)&v3688 < 0) *(int32_t*)&v7024 = -1; else v7024 = 0;
// .text:0040CEDD                 idiv    edi
*v5828 = v7024; *v2081 = v3688; v1875 = (int32_t)v7019; v3688 = (uint32_t)(v3009 / v1875); v7024 = (uint32_t)(v3009 % v1875);
// .text:0040CEDF                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040CEE6                 movzx   edx, byte ptr [edx+esi]
v7024 = *(uint8_t*)(((v7024) + (v5534)));
// .text:0040CEEA                 add     edx, eax
v7024 = v7024 + v3688;
// .text:0040CEEC                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CEF3                 add     eax, edx
v3688 = v3688 + v7024;
// .text:0040CEF5                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CEFA                 jns     short loc_40CF03
v152(); if (v2325 == 0) {v1853(); goto v5917;} else {v1853();}
// .text:0040CEFC                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CEFD                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF02                 inc     eax
v3688 += 1;
// .text:0040CF03

// .text:0040CF03 loc_40CF03:                             ; CODE XREF: sub_40CBA0+35Aj
v5917:
// .text:0040CF03                 movzx   edx, al
v7024 = *(uint8_t*)v873;
// .text:0040CF06                 mov     byte_786131, al
*v2960 = *v873;
// .text:0040CF0B                 movzx   eax, byte_786030[edx]
v3688 = v5225[v7024];
// .text:0040CF12                 mov     byte_786030[ebx], al
v5225[v7299] = *v873;
// .text:0040CF18                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CF1F                 movzx   edx, byte_786131
v7024 = *v2960;
// .text:0040CF26                 mov     byte_786030[edx], al
v5225[v7024] = *v873;
// .text:0040CF2C                 movzx   eax, byte_786130
v3688 = *v2928;
// .text:0040CF33                 inc     eax
v3688 += 1;
// .text:0040CF34                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF39                 jns     short loc_40CF42
v152(); if (v2325 == 0) {v1853(); goto v5417;} else {v1853();}
// .text:0040CF3B                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF3C                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF41                 inc     eax
v3688 += 1;
// .text:0040CF42

// .text:0040CF42 loc_40CF42:                             ; CODE XREF: sub_40CBA0+399j
v5417:
// .text:0040CF42                 movzx   ebx, al
v7299 = *(uint8_t*)v873;
// .text:0040CF45                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040CF4A                 movzx   eax, byte_786030[ebx]
v3688 = v5225[v7299];
// .text:0040CF51                 mov     [ebp+var_109], al
*(uint8_t*)(((v7297) + (var_109))) = *v873;
// .text:0040CF57                 lea     eax, [ecx+1]
v3688 = ((v5690) + (0x1));
// .text:0040CF5A                 cdq
if (*(int32_t*)&v3688 < 0) *(int32_t*)&v7024 = -1; else v7024 = 0;
// .text:0040CF5B                 idiv    edi
*v5828 = v7024; *v2081 = v3688; v1875 = (int32_t)v7019; v3688 = (uint32_t)(v3009 / v1875); v7024 = (uint32_t)(v3009 % v1875);
// .text:0040CF5D                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040CF64                 movzx   edx, byte ptr [edx+esi]
v7024 = *(uint8_t*)(((v7024) + (v5534)));
// .text:0040CF68                 add     edx, eax
v7024 = v7024 + v3688;
// .text:0040CF6A                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CF71                 add     eax, edx
v3688 = v3688 + v7024;
// .text:0040CF73                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF78                 jns     short loc_40CF81
v152(); if (v2325 == 0) {v1853(); goto v4918;} else {v1853();}
// .text:0040CF7A                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF7B                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CF80                 inc     eax
v3688 += 1;
// .text:0040CF81

// .text:0040CF81 loc_40CF81:                             ; CODE XREF: sub_40CBA0+3D8j
v4918:
// .text:0040CF81                 movzx   edx, al
v7024 = *(uint8_t*)v873;
// .text:0040CF84                 mov     byte_786131, al
*v2960 = *v873;
// .text:0040CF89                 movzx   eax, byte_786030[edx]
v3688 = v5225[v7024];
// .text:0040CF90                 mov     byte_786030[ebx], al
v5225[v7299] = *v873;
// .text:0040CF96                 movzx   edx, byte_786131
v7024 = *v2960;
// .text:0040CF9D                 movzx   eax, [ebp+var_109]
v3688 = *(uint8_t*)(((v7297) + (var_109)));
// .text:0040CFA4                 add     ecx, 4
v5690 = v5690 + 0x4;
// .text:0040CFA7                 mov     byte_786030[edx], al
v5225[v7024] = *v873;
// .text:0040CFAD                 lea     edx, [ecx-2]
v7024 = ((v5690) - (0x2));
// .text:0040CFB0                 cmp     edx, 100h
v1292 = v7024; v1294 = 0x100; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040CFB6                 jl      loc_40CDB3
v1837(); if (v2325 != v1259 && v2325 != -1 && v1259 != -1) {v1853(); goto v1579;} else {v1853();}
// .text:0040CFBC                 mov     al, byte_786130
*v873 = *v2928;
// .text:0040CFC1                 mov     byte_786131, al
*v2960 = *v873;
// .text:0040CFC6                 mov     eax, [ebp+var_114]
v3688 = *(uint32_t*)(((v7297) + (var_114)));
// .text:0040CFCC                 pop     edi
v6374(&v7019);
// .text:0040CFCD                 pop     esi
v6374(&v5534);
// .text:0040CFCE                 pop     ebx
v6374(&v7299);
// .text:0040CFCF                 mov     ecx, [ebp+var_4]
v5690 = *(uint32_t*)(((v7297) + (var_4)));
// .text:0040CFD2                 xor     ecx, ebp
v5690 ^= v7297;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CFD4                 call    sub_6132A9

// .text:0040CFD9                 mov     esp, ebp
v5530 = v7297;
// .text:0040CFDB                 pop     ebp
v6374(&v7297);
// .text:0040CFDC                 retn
v6374(&v4111); return;
// .text:0040CFDD ; ---------------------------------------------------------------------------

// .text:0040CFDD

// .text:0040CFDD loc_40CFDD:                             ; CODE XREF: sub_40CBA0+2Aj
v4289:
// .text:0040CFDD                                         ; DATA XREF: .text:off_40D280o

// .text:0040CFDD                 cmp     dword_786134, ebx ; jumptable 0040CBCA case 2
v1292 = *v71; v1294 = v7299; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1);
// .text:0040CFE3                 jg      short loc_40CFF2
v476(); if (v5661 == 0 && v2325 == v1259 && v2325 != -1) {v1853(); goto v4039;} else {v1853();}
// .text:0040CFE5                 mov     ecx, [ebp+arg_4]
v5690 = *(uint32_t*)(((v7297) + (arg_4)));
// .text:0040CFE8                 push    ecx
v440(v5690);
// .text:0040CFE9                 push    ebx
v440(v7299);
// .text:0040CFEA                 call    sub_40CBA0
v440(0x89898989);v2935();
// .text:0040CFEF                 add     esp, 8
v5530 = v5530 + 0x8;
// .text:0040CFF2

// .text:0040CFF2 loc_40CFF2:                             ; CODE XREF: sub_40CBA0+443j
v4039:
// .text:0040CFF2                 movzx   eax, byte_786130
v3688 = *v2928;
// .text:0040CFF9                 inc     eax
v3688 += 1;
// .text:0040CFFA                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040CFFF                 jns     short loc_40D008
v152(); if (v2325 == 0) {v1853(); goto v3236;} else {v1853();}
// .text:0040D001                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D002                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D007                 inc     eax
v3688 += 1;
// .text:0040D008

// .text:0040D008 loc_40D008:                             ; CODE XREF: sub_40CBA0+45Fj
v3236:
// .text:0040D008                 movzx   edx, byte_786131
v7024 = *v2960;
// .text:0040D00F                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040D014                 movzx   eax, al
v3688 = *(uint8_t*)v873;
// .text:0040D017                 mov     cl, byte_786030[eax]
*v1631 = v5225[v3688];
// .text:0040D01D                 movzx   esi, cl
v5534 = *(uint8_t*)v1631;
// .text:0040D020                 add     edx, esi
v7024 = v7024 + v5534;
// .text:0040D022                 and     edx, 800000FFh
v7024 &= 0x800000FF;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D028                 jns     short loc_40D032
v152(); if (v2325 == 0) {v1853(); goto v6627;} else {v1853();}
// .text:0040D02A                 dec     edx
v4111 = v7024 = v7024 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D02B                 or      edx, 0FFFFFF00h
v7024 |= 0x0FFFFFF00;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D031                 inc     edx
v7024 += 1;
// .text:0040D032

// .text:0040D032 loc_40D032:                             ; CODE XREF: sub_40CBA0+488j
v6627:
// .text:0040D032                 mov     byte_786131, dl
*v2960 = *v2037;
// .text:0040D038                 movzx   edx, dl
v7024 = *(uint8_t*)v2037;
// .text:0040D03B                 mov     dl, byte_786030[edx]
*v2037 = v5225[v7024];
// .text:0040D041                 mov     byte_786030[eax], dl
v5225[v3688] = *v2037;
// .text:0040D047                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040D04E                 mov     byte_786030[eax], cl
v5225[v3688] = *v1631;
// .text:0040D054                 movzx   ecx, dl
v5690 = *(uint8_t*)v2037;
// .text:0040D057                 add     ecx, esi
v5690 = v5690 + v5534;
// .text:0040D059                 and     ecx, 800000FFh
v5690 &= 0x800000FF;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D05F                 jns     short loc_40D069
v152(); if (v2325 == 0) {v1853(); goto v3504;} else {v1853();}
// .text:0040D061                 dec     ecx
v4111 = v5690 = v5690 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D062                 or      ecx, 0FFFFFF00h
v5690 |= 0x0FFFFFF00;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D068                 inc     ecx
v5690 += 1;
// .text:0040D069

// .text:0040D069 loc_40D069:                             ; CODE XREF: sub_40CBA0+4BFj
v3504:
// .text:0040D069                 movzx   edx, byte_786130
v7024 = *v2928;
// .text:0040D070                 movzx   ecx, byte_786030[ecx]
v5690 = v5225[v5690];
// .text:0040D077                 inc     edx
v7024 += 1;
// .text:0040D078                 mov     eax, edx
v3688 = v7024;
// .text:0040D07A                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D07F                 jns     short loc_40D088
v152(); if (v2325 == 0) {v1853(); goto v2119;} else {v1853();}
// .text:0040D081                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D082                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D087                 inc     eax
v3688 += 1;
// .text:0040D088

// .text:0040D088 loc_40D088:                             ; CODE XREF: sub_40CBA0+4DFj
v2119:
// .text:0040D088                 movzx   ebx, byte_786131
v7299 = *v2960;
// .text:0040D08F                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040D094                 movzx   eax, al
v3688 = *(uint8_t*)v873;
// .text:0040D097                 mov     dl, byte_786030[eax]
*v2037 = v5225[v3688];
// .text:0040D09D                 movzx   esi, dl
v5534 = *(uint8_t*)v2037;
// .text:0040D0A0                 add     ebx, esi
v7299 = v7299 + v5534;
// .text:0040D0A2                 and     ebx, 800000FFh
v7299 &= 0x800000FF;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0A8                 jns     short loc_40D0B2
v152(); if (v2325 == 0) {v1853(); goto v4117;} else {v1853();}
// .text:0040D0AA                 dec     ebx
v4111 = v7299 = v7299 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0AB                 or      ebx, 0FFFFFF00h
v7299 |= 0x0FFFFFF00;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0B1                 inc     ebx
v7299 += 1;
// .text:0040D0B2

// .text:0040D0B2 loc_40D0B2:                             ; CODE XREF: sub_40CBA0+508j
v4117:
// .text:0040D0B2                 mov     byte_786131, bl
*v2960 = *v1274;
// .text:0040D0B8                 movzx   edi, bl
v7019 = *(uint8_t*)v1274;
// .text:0040D0BB                 mov     bl, byte_786030[edi]
*v1274 = v5225[v7019];
// .text:0040D0C1                 mov     byte_786030[eax], bl
v5225[v3688] = *v1274;
// .text:0040D0C7                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040D0CE                 mov     byte_786030[eax], dl
v5225[v3688] = *v2037;
// .text:0040D0D4                 movzx   edx, bl
v7024 = *(uint8_t*)v1274;
// .text:0040D0D7                 add     edx, esi
v7024 = v7024 + v5534;
// .text:0040D0D9                 and     edx, 800000FFh
v7024 &= 0x800000FF;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0DF                 jns     short loc_40D0E9
v152(); if (v2325 == 0) {v1853(); goto v1015;} else {v1853();}
// .text:0040D0E1                 dec     edx
v4111 = v7024 = v7024 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0E2                 or      edx, 0FFFFFF00h
v7024 |= 0x0FFFFFF00;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0E8                 inc     edx
v7024 += 1;
// .text:0040D0E9

// .text:0040D0E9 loc_40D0E9:                             ; CODE XREF: sub_40CBA0+53Fj
v1015:
// .text:0040D0E9                 movzx   eax, byte_786030[edx]
v3688 = v5225[v7024];
// .text:0040D0F0                 shl     ecx, 8
v5690 <<= 0x8;
// .text:0040D0F3                 or      eax, ecx
v3688 |= v5690;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D0F5                 movzx   ecx, byte_786130
v5690 = *v2928;
// .text:0040D0FC                 inc     ecx
v5690 += 1;
// .text:0040D0FD                 and     ecx, 800000FFh
v5690 &= 0x800000FF;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D103                 jns     short loc_40D10D
v152(); if (v2325 == 0) {v1853(); goto v1457;} else {v1853();}
// .text:0040D105                 dec     ecx
v4111 = v5690 = v5690 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D106                 or      ecx, 0FFFFFF00h
v5690 |= 0x0FFFFFF00;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D10C                 inc     ecx
v5690 += 1;
// .text:0040D10D

// .text:0040D10D loc_40D10D:                             ; CODE XREF: sub_40CBA0+563j
v1457:
// .text:0040D10D                 movzx   ebx, byte_786131
v7299 = *v2960;
// .text:0040D114                 mov     byte_786130, cl
*v2928 = *v1631;
// .text:0040D11A                 movzx   ecx, cl
v5690 = *(uint8_t*)v1631;
// .text:0040D11D                 mov     dl, byte_786030[ecx]
*v2037 = v5225[v5690];
// .text:0040D123                 movzx   esi, dl
v5534 = *(uint8_t*)v2037;
// .text:0040D126                 add     ebx, esi
v7299 = v7299 + v5534;
// .text:0040D128                 and     ebx, 800000FFh
v7299 &= 0x800000FF;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D12E                 jns     short loc_40D138
v152(); if (v2325 == 0) {v1853(); goto v5682;} else {v1853();}
// .text:0040D130                 dec     ebx
v4111 = v7299 = v7299 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D131                 or      ebx, 0FFFFFF00h
v7299 |= 0x0FFFFFF00;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D137                 inc     ebx
v7299 += 1;
// .text:0040D138

// .text:0040D138 loc_40D138:                             ; CODE XREF: sub_40CBA0+58Ej
v5682:
// .text:0040D138                 mov     byte_786131, bl
*v2960 = *v1274;
// .text:0040D13E                 movzx   edi, bl
v7019 = *(uint8_t*)v1274;
// .text:0040D141                 mov     bl, byte_786030[edi]
*v1274 = v5225[v7019];
// .text:0040D147                 mov     byte_786030[ecx], bl
v5225[v5690] = *v1274;
// .text:0040D14D                 movzx   ecx, byte_786131
v5690 = *v2960;
// .text:0040D154                 mov     byte_786030[ecx], dl
v5225[v5690] = *v2037;
// .text:0040D15A                 movzx   edx, bl
v7024 = *(uint8_t*)v1274;
// .text:0040D15D                 add     edx, esi
v7024 = v7024 + v5534;
// .text:0040D15F                 and     edx, 800000FFh
v7024 &= 0x800000FF;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D165                 jns     short loc_40D16F
v152(); if (v2325 == 0) {v1853(); goto v2242;} else {v1853();}
// .text:0040D167                 dec     edx
v4111 = v7024 = v7024 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D168                 or      edx, 0FFFFFF00h
v7024 |= 0x0FFFFFF00;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D16E                 inc     edx
v7024 += 1;
// .text:0040D16F

// .text:0040D16F loc_40D16F:                             ; CODE XREF: sub_40CBA0+5C5j
v2242:
// .text:0040D16F                 movzx   ecx, byte_786030[edx]
v5690 = v5225[v7024];
// .text:0040D176                 shl     eax, 8
v3688 <<= 0x8;
// .text:0040D179                 or      ecx, eax
v5690 |= v3688;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D17B                 movzx   eax, byte_786130
v3688 = *v2928;
// .text:0040D182                 inc     eax
v3688 += 1;
// .text:0040D183                 and     eax, 800000FFh
v3688 &= 0x800000FF;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D188                 jns     short loc_40D191
v152(); if (v2325 == 0) {v1853(); goto v4315;} else {v1853();}
// .text:0040D18A                 dec     eax
v4111 = v3688 = v3688 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D18B                 or      eax, 0FFFFFF00h
v3688 |= 0x0FFFFFF00;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D190                 inc     eax
v3688 += 1;
// .text:0040D191

// .text:0040D191 loc_40D191:                             ; CODE XREF: sub_40CBA0+5E8j
v4315:
// .text:0040D191                 movzx   ebx, byte_786131
v7299 = *v2960;
// .text:0040D198                 mov     byte_786130, al
*v2928 = *v873;
// .text:0040D19D                 movzx   eax, al
v3688 = *(uint8_t*)v873;
// .text:0040D1A0                 mov     dl, byte_786030[eax]
*v2037 = v5225[v3688];
// .text:0040D1A6                 movzx   esi, dl
v5534 = *(uint8_t*)v2037;
// .text:0040D1A9                 add     ebx, esi
v7299 = v7299 + v5534;
// .text:0040D1AB                 and     ebx, 800000FFh
v7299 &= 0x800000FF;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1B1                 jns     short loc_40D1BB
v152(); if (v2325 == 0) {v1853(); goto v4929;} else {v1853();}
// .text:0040D1B3                 dec     ebx
v4111 = v7299 = v7299 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1B4                 or      ebx, 0FFFFFF00h
v7299 |= 0x0FFFFFF00;if (v7299 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1BA                 inc     ebx
v7299 += 1;
// .text:0040D1BB

// .text:0040D1BB loc_40D1BB:                             ; CODE XREF: sub_40CBA0+611j
v4929:
// .text:0040D1BB                 mov     byte_786131, bl
*v2960 = *v1274;
// .text:0040D1C1                 movzx   edi, bl
v7019 = *(uint8_t*)v1274;
// .text:0040D1C4                 mov     bl, byte_786030[edi]
*v1274 = v5225[v7019];
// .text:0040D1CA                 mov     byte_786030[eax], bl
v5225[v3688] = *v1274;
// .text:0040D1D0                 movzx   eax, byte_786131
v3688 = *v2960;
// .text:0040D1D7                 mov     byte_786030[eax], dl
v5225[v3688] = *v2037;
// .text:0040D1DD                 movzx   edx, bl
v7024 = *(uint8_t*)v1274;
// .text:0040D1E0                 add     edx, esi
v7024 = v7024 + v5534;
// .text:0040D1E2                 and     edx, 800000FFh
v7024 &= 0x800000FF;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1E8                 jns     short loc_40D1F2
v152(); if (v2325 == 0) {v1853(); goto v5429;} else {v1853();}
// .text:0040D1EA                 dec     edx
v4111 = v7024 = v7024 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1EB                 or      edx, 0FFFFFF00h
v7024 |= 0x0FFFFFF00;if (v7024 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1F1                 inc     edx
v7024 += 1;
// .text:0040D1F2

// .text:0040D1F2 loc_40D1F2:                             ; CODE XREF: sub_40CBA0+648j
v5429:
// .text:0040D1F2                 movzx   eax, byte_786030[edx]
v3688 = v5225[v7024];
// .text:0040D1F9                 shl     ecx, 8
v5690 <<= 0x8;
// .text:0040D1FC                 or      eax, ecx
v3688 |= v5690;if (v3688 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D1FE                 sub     dword_786134, 4
*v71 -= 0x4; if (*v71 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D205                 mov     [ebp+var_114], eax
*(uint32_t*)(((v7297) + (var_114))) = v3688;
// .text:0040D20B                 pop     edi
v6374(&v7019);
// .text:0040D20C                 pop     esi
v6374(&v5534);
// .text:0040D20D                 pop     ebx
v6374(&v7299);
// .text:0040D20E                 mov     ecx, [ebp+var_4]
v5690 = *(uint32_t*)(((v7297) + (var_4)));
// .text:0040D211                 xor     ecx, ebp
v5690 ^= v7297;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D213                 call    sub_6132A9

// .text:0040D218                 mov     esp, ebp
v5530 = v7297;
// .text:0040D21A                 pop     ebp
v6374(&v7297);
// .text:0040D21B                 retn
v6374(&v4111); return;
// .text:0040D21C ; ---------------------------------------------------------------------------

// .text:0040D21C

// .text:0040D21C loc_40D21C:                             ; CODE XREF: sub_40CBA0+2Aj
v7280:
// .text:0040D21C                                         ; DATA XREF: .text:off_40D280o

// .text:0040D21C                 mov     esi, [ebp+arg_C] ; jumptable 0040CBCA case 3
v5534 = *(uint32_t*)(((v7297) + (arg_C)));
// .text:0040D21F                 cmp     esi, ebx
v1292 = v5534; v1294 = v7299; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040D221                 jbe     short loc_40D268 ; jumptable 0040CBCA default case
v4751(); if (v4048 == 1 || v5661 == 1) {v1853(); goto v3257;} else {v1853();}
// .text:0040D223

// .text:0040D223 loc_40D223:                             ; CODE XREF: sub_40CBA0+6C6j
v2712:
// .text:0040D223                 mov     eax, [ebp+arg_4]
v3688 = *(uint32_t*)(((v7297) + (arg_4)));
// .text:0040D226                 push    eax
v440(v3688);
// .text:0040D227                 push    2
v440(0x2);
// .text:0040D229                 call    sub_40CBA0
v440(0x89898989);v2935();
// .text:0040D22E                 mov     ecx, esi
v5690 = v5534;
// .text:0040D230                 and     ecx, 3
v5690 &= 0x3;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D233                 add     esp, 8
v5530 = v5530 + 0x8;
// .text:0040D236                 cmp     ecx, 3          ; switch 4 cases
v1292 = v5690; v1294 = 0x3; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040D239                 ja      short loc_40D264 ; jumptable 0040D23E default case
v4751(); if (v4048 == 0 && v5661 == 0) {v1853(); goto v3258;} else {v1853();}
// .text:0040D23B                 mov     edx, [ebp+arg_8]
v7024 = *(uint32_t*)(((v7297) + (arg_8)));
// .text:0040D23E                 jmp     ds:off_40D290[ecx*4] ; switch jump
switch (v5690) {
case 0:      goto v2997;
case 1:      goto v3259;
case 2:      goto v6644;
case 3:      goto v3000;
}
// .text:0040D245

// .text:0040D245 loc_40D245:                             ; DATA XREF: .text:off_40D290o
v2997:
// .text:0040D245                 dec     esi             ; jumptable 0040D23E case 0
v4111 = v5534 = v5534 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D246                 mov     ecx, eax
v5690 = v3688;
// .text:0040D248                 shr     ecx, 18h
v5690 >>= 0x18;
// .text:0040D24B                 mov     [edx+esi], cl
*(uint8_t*)(((v7024) + (v5534))) = *v1631;
// .text:0040D24E

// .text:0040D24E loc_40D24E:                             ; CODE XREF: sub_40CBA0+69Ej
v3000:
// .text:0040D24E                                         ; DATA XREF: .text:off_40D290o

// .text:0040D24E                 dec     esi             ; jumptable 0040D23E case 3
v4111 = v5534 = v5534 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D24F                 mov     ecx, eax
v5690 = v3688;
// .text:0040D251                 shr     ecx, 10h
v5690 >>= 0x10;
// .text:0040D254                 mov     [edx+esi], cl
*(uint8_t*)(((v7024) + (v5534))) = *v1631;
// .text:0040D257

// .text:0040D257 loc_40D257:                             ; CODE XREF: sub_40CBA0+69Ej
v6644:
// .text:0040D257                                         ; DATA XREF: .text:off_40D290o

// .text:0040D257                 dec     esi             ; jumptable 0040D23E case 2
v4111 = v5534 = v5534 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D258                 mov     ecx, eax
v5690 = v3688;
// .text:0040D25A                 shr     ecx, 8
v5690 >>= 0x8;
// .text:0040D25D                 mov     [edx+esi], cl
*(uint8_t*)(((v7024) + (v5534))) = *v1631;
// .text:0040D260

// .text:0040D260 loc_40D260:                             ; CODE XREF: sub_40CBA0+69Ej
v3259:
// .text:0040D260                                         ; DATA XREF: .text:off_40D290o

// .text:0040D260                 dec     esi             ; jumptable 0040D23E case 1
v4111 = v5534 = v5534 - 1; if (v4111 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D261                 mov     [edx+esi], al
*(uint8_t*)(((v7024) + (v5534))) = *v873;
// .text:0040D264

// .text:0040D264 loc_40D264:                             ; CODE XREF: sub_40CBA0+699j
v3258:
// .text:0040D264                 cmp     esi, ebx        ; jumptable 0040D23E default case
v1292 = v5534; v1294 = v7299; v4111 = v1292 - v1294; v5661 = (v4111 == 0) ? 1 : 0; v2325 = v4111 >> 31; v4048 = (v1292 < v1294) ? 1 : 0; v1259 = ((v1292 >> 31) == (v1294 >> 31)) ? 0 : (((v1292 >> 31) == (v4111 >> 31)) ? 0 : 1); 
// .text:0040D266                 ja      short loc_40D223
v4751(); if (v4048 == 0 && v5661 == 0) {v1853(); goto v2712;} else {v1853();}
// .text:0040D268

// .text:0040D268 loc_40D268:                             ; CODE XREF: sub_40CBA0+24j
v3257:
// .text:0040D268                                         ; sub_40CBA0+1FFj ...

// .text:0040D268                 mov     ecx, [ebp+var_4] ; jumptable 0040CBCA default case
v5690 = *(uint32_t*)(((v7297) + (var_4)));
// .text:0040D26B                 mov     eax, [ebp+var_114]
v3688 = *(uint32_t*)(((v7297) + (var_114)));
// .text:0040D271                 pop     edi
v6374(&v7019);
// .text:0040D272                 pop     esi
v6374(&v5534);
// .text:0040D273                 xor     ecx, ebp
v5690 ^= v7297;if (v5690 == 0) v5661 = 1; else v5661 = 0;
// .text:0040D275                 pop     ebx
v6374(&v7299);
// .text:0040D276                 call    sub_6132A9

// .text:0040D27B                 mov     esp, ebp
v5530 = v7297;
// .text:0040D27D                 pop     ebp
v6374(&v7297);
// .text:0040D27E                 retn
v6374(&v4111); return;
// .text:0040D27E sub_40CBA0      endp

// .text:0040D27E

// .text:0040D27E ; ---------------------------------------------------------------------------

// .text:0040D27F                 align 10h

// .text:0040D280 off_40D280      dd offset loc_40CBD1    ; DATA XREF: sub_40CBA0+2Ar

// .text:0040D280                 dd offset loc_40CD9A    ; jump table for switch statement

// .text:0040D280                 dd offset loc_40CFDD

// .text:0040D280                 dd offset loc_40D21C

// .text:0040D290 off_40D290      dd offset loc_40D245    ; DATA XREF: sub_40CBA0+69Er

// .text:0040D290                 dd offset loc_40D260    ; jump table for switch statement

// .text:0040D290                 dd offset loc_40D257

// .text:0040D290                 dd offset loc_40D24E

// .text:0040D2A0

// 

#undef var_11C
#undef var_118
#undef var_114
#undef var_110
#undef var_109
#undef var_108
#undef var_104
#undef var_88
#undef var_87
#undef var_86
#undef var_85
#undef var_4
#undef arg_0
#undef arg_4
#undef arg_8
#undef arg_C
}
