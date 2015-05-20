#define _GNU_SOURCE
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>

static uint8_t aesConvertTable[256] = {
    0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5, 0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76,
    0xCA, 0x82, 0xC9, 0x7D, 0xFA, 0x59, 0x47, 0xF0, 0xAD, 0xD4, 0xA2, 0xAF, 0x9C, 0xA4, 0x72, 0xC0,
    0xB7, 0xFD, 0x93, 0x26, 0x36, 0x3F, 0xF7, 0xCC, 0x34, 0xA5, 0xE5, 0xF1, 0x71, 0xD8, 0x31, 0x15,
    0x04, 0xC7, 0x23, 0xC3, 0x18, 0x96, 0x05, 0x9A, 0x07, 0x12, 0x80, 0xE2, 0xEB, 0x27, 0xB2, 0x75,
    0x09, 0x83, 0x2C, 0x1A, 0x1B, 0x6E, 0x5A, 0xA0, 0x52, 0x3B, 0xD6, 0xB3, 0x29, 0xE3, 0x2F, 0x84,
    0x53, 0xD1, 0x00, 0xED, 0x20, 0xFC, 0xB1, 0x5B, 0x6A, 0xCB, 0xBE, 0x39, 0x4A, 0x4C, 0x58, 0xCF,
    0xD0, 0xEF, 0xAA, 0xFB, 0x43, 0x4D, 0x33, 0x85, 0x45, 0xF9, 0x02, 0x7F, 0x50, 0x3C, 0x9F, 0xA8,
    0x51, 0xA3, 0x40, 0x8F, 0x92, 0x9D, 0x38, 0xF5, 0xBC, 0xB6, 0xDA, 0x21, 0x10, 0xFF, 0xF3, 0xD2,
    0xCD, 0x0C, 0x13, 0xEC, 0x5F, 0x97, 0x44, 0x17, 0xC4, 0xA7, 0x7E, 0x3D, 0x64, 0x5D, 0x19, 0x73,
    0x60, 0x81, 0x4F, 0xDC, 0x22, 0x2A, 0x90, 0x88, 0x46, 0xEE, 0xB8, 0x14, 0xDE, 0x5E, 0x0B, 0xDB,
    0xE0, 0x32, 0x3A, 0x0A, 0x49, 0x06, 0x24, 0x5C, 0xC2, 0xD3, 0xAC, 0x62, 0x91, 0x95, 0xE4, 0x79,
    0xE7, 0xC8, 0x37, 0x6D, 0x8D, 0xD5, 0x4E, 0xA9, 0x6C, 0x56, 0xF4, 0xEA, 0x65, 0x7A, 0xAE, 0x08,
    0xBA, 0x78, 0x25, 0x2E, 0x1C, 0xA6, 0xB4, 0xC6, 0xE8, 0xDD, 0x74, 0x1F, 0x4B, 0xBD, 0x8B, 0x8A,
    0x70, 0x3E, 0xB5, 0x66, 0x48, 0x03, 0xF6, 0x0E, 0x61, 0x35, 0x57, 0xB9, 0x86, 0xC1, 0x1D, 0x9E,
    0xE1, 0xF8, 0x98, 0x11, 0x69, 0xD9, 0x8E, 0x94, 0x9B, 0x1E, 0x87, 0xE9, 0xCE, 0x55, 0x28, 0xDF,
    0x8C, 0xA1, 0x89, 0x0D, 0xBF, 0xE6, 0x42, 0x68, 0x41, 0x99, 0x2D, 0x0F, 0xB0, 0x54, 0xBB, 0x16
};

# define aesConvertByte(x) (aesConvertTable[x])

// all inputs have 16 bytes memory
int32_t ezAesDecrypt(uint8_t *iv, uint8_t *out, uint8_t *key)
{
    uint32_t vv1, vv2, vv3, vv4, vv5, vv6, vv7;
    uint32_t vvv1, vvv2;
    uint32_t *p1;
    uint32_t *p2;
    uint32_t *p3 = (uint32_t*)(key + 0x10);
    p1 = (uint32_t*)iv;
    p2 = (uint32_t*)key;

    vv1 = *p1 ^ *p2;
    p1++; p2++;
    vv2 = *p1 ^ *p2;
    p1++; p2++;
    vv3 = *p1 ^ *p2;
    p1++; p2++;
    vv4 = *p1 ^ *p2;
  
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    // vv1 ^= *(uint32_t *)(arg10 + 0x00);
    // vv2 ^= *(uint32_t *)(arg10 + 0x04);
    // vv3 ^= *(uint32_t *)(arg10 + 0x08);
    // vv4 ^= *(uint32_t *)(arg10 + 0x0c);
    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;

    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;

    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;
    ///////////////////////////
    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;


    vvv2 = aesConvertByte(vv2 & 0xff);
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;


    vv5 = aesConvertByte(vv3 & 0xff);
    vv5 ^= aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv2 >>= 0x10;
    vv5 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;

    vv1 = vvv1;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv2 = vvv2;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    vv3 = vv5;
    vv5 &= 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv3 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv3;
    vv5 ^= vv6;
    vv3 ^= vv5;
    vv3 = (vv3 << 0x18) | (vv3 >> 0x08);
    vv3 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv3 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv3 ^= vv7;

    vv5 = vv4 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv4 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv4;
    vv5 ^= vv6;
    vv4 ^= vv5;
    vv4 = (vv4 << 0x18) | (vv4 >> 0x08);
    vv4 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv4 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv4 ^= vv7;

    vv5 = vv1 & 0x80808080;
    vv5 -= (vv5 >> 7);
    vv6 = (vv1 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv1;
    vv5 ^= vv6;
    vv1 ^= vv5;
    vv1 = (vv1 << 0x18) | (vv1 >> 0x08);
    vv1 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv1 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv1 ^= vv7;
    vv5 = vv2;
    vv5 &= 0x80808080;

    vv5 -= (vv5 >> 7);
    vv6 = (vv2 << 1) & 0xfefefefe;
    vv5 &= 0x1b1b1b1b;
    vv7 = vv2;
    vv5 ^= vv6;
    vv2 ^= vv5;
    vv2 = (vv2 << 0x18) | (vv2 >> 0x08);
    vv2 ^= vv5;
    vv7 = (vv7 << 0x10) | (vv7 >> 0x10);
    vv2 ^= vv7;
    vv7 = (vv7 << 0x18) | (vv7 >> 0x08);
    vv2 ^= vv7;

    vv1 ^= *p3++;
    vv2 ^= *p3++;
    vv3 ^= *p3++;
    vv4 ^= *p3++;




    vvv1 = aesConvertByte(vv1 & 0xff);
    vvv1 ^= aesConvertByte((vv2 >> 8) & 0xff) << 0x08;
    vvv1 ^= aesConvertByte((vv3 >> 0x10) & 0xff) << 0x10;
    vvv1 ^= aesConvertByte(vv4 >> 0x18) << 0x18;

    vvv2 = aesConvertByte(vv2 & 0xff);
    vv2 >>= 0x10;
    vvv2 ^= aesConvertByte((vv3 >> 8) & 0xff) << 8;
    vvv2 ^= aesConvertByte((vv4 >> 0x10) & 0xff) << 0x10;
    vvv2 ^= aesConvertByte(vv1 >> 0x18) << 0x18;

    vv5 = aesConvertByte(vv3 & 0xff);
    vv6 = aesConvertByte((vv4 >> 8) & 0xff) << 8;
    vv5 ^= vv6;
    vv6 = aesConvertByte((vv1 >> 0x10) & 0xff) << 0x10;
    vv5 ^= vv6;
    vv6 = aesConvertByte((vv2 >> 8) & 0xff) << 0x18;
    vv5 ^= vv6;

    vv4 = aesConvertByte(vv4 & 0xff);
    vv4 ^= aesConvertByte((vv1 >> 8) & 0xff) << 8;
    vv4 ^= aesConvertByte(vv2 & 0xff) << 0x10;
    vv4 ^= aesConvertByte(vv3 >> 0x18) << 0x18;

    p1 = (uint32_t *)out;
    *p1 = vvv1 ^ *p3;
    p1++; p3++;
    *p1 = vvv2 ^ *p3;
    p1++; p3++;
    *p1 = vv5 ^ *p3;
    p1++; p3++;
    *p1 = vv4 ^ *p3;
    return 0;
}

static uint8_t aesExtendKeyTable1[16 *4] = { /* we need 40 */
    0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00,
    0x10, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00,
    0x1B, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
};

// key should point to memory of size 16 + 16 * 10
void aesExtendKey(uint8_t *key)
{
    int i = 0;
    uint8_t *p = key;
    uint8_t s[4];
    uint8_t *extendKeyTable = aesConvertTable;
    for (i = 0; i < 0x0a; ++i, p += 0x10)
    {
        memcpy(s, p, 4);
        s[3] ^= extendKeyTable[p[12]];
        s[0] ^= extendKeyTable[p[13]];
        s[1] ^= extendKeyTable[p[14]];
        s[2] ^= extendKeyTable[p[15]];

        uint32_t *s1 = (uint32_t *)s;
        *s1 ^= *(uint32_t *)(aesExtendKeyTable1 + 4 * i);
        memcpy(p + 0x10, s, 4);
        *s1 ^= *(uint32_t *)(p + 0x04);
        memcpy(p + 0x14, s, 4);
        *s1 ^= *(uint32_t *)(p + 0x08);
        memcpy(p + 0x18, s, 4);
        *s1 ^= *(uint32_t *)(p + 0x0c);
        memcpy(p + 0x1c, s, 4);
    }
}

struct aesDecryptor {
    uint8_t key[16 + 16 * 10];
    uint8_t iv[16];
    uint8_t last_aes_str[16];
    int n_excess_bytes_last_time;
} aesDecryptor;

int ezAesDecrytInit(uint8_t *key, uint8_t *iv)
{
    memset(&aesDecryptor, 0, sizeof(aesDecryptor));
    memcpy(aesDecryptor.key, key, 16);
    aesExtendKey(aesDecryptor.key);
    memcpy(aesDecryptor.iv, iv, 16);
    aesDecryptor.n_excess_bytes_last_time = 0;
	return 0;
}

int ezAesDecryptBlock(uint8_t *data, int len, uint8_t *out)
{
    int i;
    int j;
    uint8_t *tmp;
    /* process excess bytes last time */
    if (aesDecryptor.n_excess_bytes_last_time > 0) 
    {
        j = len >= (16 - aesDecryptor.n_excess_bytes_last_time) ? (16 - aesDecryptor.n_excess_bytes_last_time) : (len);
        for (i = 0; i < j; ++i) 
        {
            out[i] = data[i] ^ aesDecryptor.last_aes_str[aesDecryptor.n_excess_bytes_last_time + i];
        }
        data += j;
        len -= j;
        out += j;
        /* no need to add iv, we did it last time */
        aesDecryptor.n_excess_bytes_last_time += j;
#if 0
        aesDecryptor.n_excess_bytes_last_time %= 16;
#else
        aesDecryptor.n_excess_bytes_last_time &= 0x0f;
#endif
    }

    int cnt = len / 16;
    int res = len & 0x0f;

    for (i = 0; i < cnt; ++i)
    {
        ezAesDecrypt(aesDecryptor.iv, aesDecryptor.last_aes_str, aesDecryptor.key);

        j = i << 4;
        tmp = data + j;
        *(uint32_t*)(out + j + 0) = *(uint32_t*)(tmp + 0) ^ *(uint32_t*)(aesDecryptor.last_aes_str + 0);
        *(uint32_t*)(out + j + 4) = *(uint32_t*)(tmp + 4) ^ *(uint32_t*)(aesDecryptor.last_aes_str + 4);
        *(uint32_t*)(out + j + 8) = *(uint32_t*)(tmp + 8) ^ *(uint32_t*)(aesDecryptor.last_aes_str + 8);
        *(uint32_t*)(out + j + 12) = *(uint32_t*)(tmp + 12) ^ *(uint32_t*)(aesDecryptor.last_aes_str + 12);

        /* renew iv */
        tmp = aesDecryptor.iv + 15;
        while (0xff == (*tmp))
        {
            *tmp = 0x00;
            tmp--;
        }
        *tmp += 1;
    }

    if (res > 0)
    {
        ezAesDecrypt(aesDecryptor.iv, aesDecryptor.last_aes_str, aesDecryptor.key);
        aesDecryptor.n_excess_bytes_last_time = res;
        tmp = data + 16 * cnt;
        out += (cnt << 4);
        for (j = 0; j < res; ++j)
        {
            out[j] = tmp[j] ^ aesDecryptor.last_aes_str[j];
        }

        /* renew iv */
        tmp = aesDecryptor.iv + 15;
        while (0xff == (*tmp))
        {
            *tmp = 0x00;
            tmp--;
        }
        *tmp += 1;
    }
    return 0;
}

#if 0 //def TEST_DECRYPTION_SPEED
using namespace BUTIL;
int main(int argc, char *argv[])
{
#if 1
    uint8_t key[16 + 16 * 10] = {
        /* 0x53, 0xAF, 0xA2, 0x13, 0x37, 0xAC, 0x19, 0x38, */
        /* 0x88, 0x6F, 0xB6, 0x70, 0x52, 0xDB, 0x41, 0xB4 */
        0xA8, 0x3E, 0xDF, 0x2A, 0xB3, 0x2C, 0x21, 0xBE,
        0xC6, 0x66, 0x31, 0x24, 0x57, 0xEB, 0x41, 0x68
    };
    uint8_t iv[16] = {
        /* 0x5B, 0xA0, 0xCD, 0xA1, 0xE1, 0x48, 0xDB, 0x9B, */
        /* 0x38, 0xA7, 0xA5, 0xA4, 0x7D, 0xE7, 0x5E, 0xD3 */
        0xB4, 0x00, 0x21, 0x48, 0x73, 0x98, 0x32, 0xF5,
        0x9A, 0xB2, 0x65, 0xD3, 0x83, 0xE2, 0x93, 0x67
    };
    uint8_t src[16] = {
        /* 0x49, 0x77, 0xcf, 0x5e, 0xba, 0xf2, 0x15, 0x18, */
        /* 0x9d, 0x3e, 0xf2, 0x41, 0xb8, 0x0f, 0x69, 0x39 */
        0x71, 0x47, 0xEC, 0xCA, 0xD6, 0x54, 0xF7, 0xD9,
        0xF0, 0xF8, 0xE7, 0x8A, 0x35, 0xA0, 0x66, 0xBF
    };
    uint8_t out[16];
    memset(out, 0, 16);

    //printf(" will extend key \n");
    aesExtendKey(key);

    //printf(" will enter ezAesDecrypt \n");

    struct timeval tv1, tv2;
    double elapsedtime;

    gettimeofday(&tv1, NULL);
    int i = 0;
    for (i = 0; i < 10000000; ++i) {
        ezAesDecrypt(iv, out, key);
    }

    gettimeofday(&tv2, NULL);
    elapsedtime = ((double)tv2.tv_sec + ((double)tv2.tv_usec) / 1000000) - ((double)tv1.tv_sec + ((double)tv1.tv_usec) / 1000000);
    //printf("time :  %f\n", elapsedtime);

    // for (i = 0; i < 16; ++i) {
    //     out[i] = out[i] ^ src[i];
    //     printf("%02x ", out[i]);
    // }
    // printf("\n");


#else

    uint8_t key[16 + 16 * 10] = {
        0x6D, 0x0C, 0x7A, 0x2B, 0x14, 0x75, 0x20, 0xE6, 0x65, 0xB5, 0x2C, 0xB2, 0x47, 0x6F, 0x7D, 0xF8
    };

    aesExtendKey(key);
    int i = 0;
    uint8_t *p = NULL;
    for (i = 0; i < 10; ++i) {
        p = key + 0x10 * (i + 1);
        int j = 0;
        // for (j = 0; j < 0x10; ++j)
        //     printf("%02x ", p[j]);
        // printf("\n");
    }

#endif /* test extend key */

    return 0;
}
#endif

