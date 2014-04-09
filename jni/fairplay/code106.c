#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>

static uint8_t v4368[256] = {
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

#define v722(x) (v4368[x])

int32_t v5572(uint8_t *iv, uint8_t *out, uint8_t *key)
{
    uint32_t v3688, v7299, v5690, v7024, v5534, v7019, v7297;
    uint32_t arg0, arg4;
    uint32_t *p1;
    uint32_t *p2;
    uint32_t *p3 = (uint32_t*)(key + 0x10);
    p1 = (uint32_t*)iv;
    p2 = (uint32_t*)key;

    v3688 = *p1 ^ *p2;
    p1++; p2++;
    v7299 = *p1 ^ *p2;
    p1++; p2++;
    v5690 = *p1 ^ *p2;
    p1++; p2++;
    v7024 = *p1 ^ *p2;
  
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;

    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;

    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;
    ///////////////////////////
    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;


    arg4 = v722(v7299 & 0xff);
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;


    v5534 = v722(v5690 & 0xff);
    v5534 ^= v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v722((v3688 >> 0x10) & 0xff) << 0x10;
    v7299 >>= 0x10;
    v5534 ^= v722((v7299 >> 8) & 0xff) << 0x18;
    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;

    v3688 = arg0;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7299 = arg4;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    v5690 = v5534;
    v5534 &= 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v5690 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v5690;
    v5534 ^= v7019;
    v5690 ^= v5534;
    v5690 = (v5690 << 0x18) | (v5690 >> 0x08);
    v5690 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v5690 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v5690 ^= v7297;

    v5534 = v7024 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v7024 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7024;
    v5534 ^= v7019;
    v7024 ^= v5534;
    v7024 = (v7024 << 0x18) | (v7024 >> 0x08);
    v7024 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7024 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7024 ^= v7297;

    v5534 = v3688 & 0x80808080;
    v5534 -= (v5534 >> 7);
    v7019 = (v3688 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v3688;
    v5534 ^= v7019;
    v3688 ^= v5534;
    v3688 = (v3688 << 0x18) | (v3688 >> 0x08);
    v3688 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v3688 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v3688 ^= v7297;
    v5534 = v7299;
    v5534 &= 0x80808080;

    v5534 -= (v5534 >> 7);
    v7019 = (v7299 << 1) & 0xfefefefe;
    v5534 &= 0x1b1b1b1b;
    v7297 = v7299;
    v5534 ^= v7019;
    v7299 ^= v5534;
    v7299 = (v7299 << 0x18) | (v7299 >> 0x08);
    v7299 ^= v5534;
    v7297 = (v7297 << 0x10) | (v7297 >> 0x10);
    v7299 ^= v7297;
    v7297 = (v7297 << 0x18) | (v7297 >> 0x08);
    v7299 ^= v7297;

    v3688 ^= *p3++;
    v7299 ^= *p3++;
    v5690 ^= *p3++;
    v7024 ^= *p3++;




    arg0 = v722(v3688 & 0xff);
    arg0 ^= v722((v7299 >> 8) & 0xff) << 0x08;
    arg0 ^= v722((v5690 >> 0x10) & 0xff) << 0x10;
    arg0 ^= v722(v7024 >> 0x18) << 0x18;

    arg4 = v722(v7299 & 0xff);
    v7299 >>= 0x10;
    arg4 ^= v722((v5690 >> 8) & 0xff) << 8;
    arg4 ^= v722((v7024 >> 0x10) & 0xff) << 0x10;
    arg4 ^= v722(v3688 >> 0x18) << 0x18;

    v5534 = v722(v5690 & 0xff);
    v7019 = v722((v7024 >> 8) & 0xff) << 8;
    v5534 ^= v7019;
    v7019 = v722((v3688 >> 0x10) & 0xff) << 0x10;
    v5534 ^= v7019;
    v7019 = v722((v7299 >> 8) & 0xff) << 0x18;
    v5534 ^= v7019;

    v7024 = v722(v7024 & 0xff);
    v7024 ^= v722((v3688 >> 8) & 0xff) << 8;
    v7024 ^= v722(v7299 & 0xff) << 0x10;
    v7024 ^= v722(v5690 >> 0x18) << 0x18;

    p1 = (uint32_t*)out;
    *p1 = arg0 ^ *p3;
    p1++; p3++;
    *p1 = arg4 ^ *p3;
    p1++; p3++;
    *p1 = v5534 ^ *p3;
    p1++; p3++;
    *p1 = v7024 ^ *p3;
    return 0;
}

static uint8_t v1645[16 *4] = {
    0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00,
    0x10, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00,
    0x1B, 0x00, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
};

void v4622(uint8_t *key) {
    int i = 0;
    uint8_t *p = key;
    uint8_t s[4];
    uint8_t *extend_key_table = v4368;
    for (i = 0; i < 0x0a; ++i, p += 0x10) {
        memcpy(s, p, 4);
        s[3] ^= extend_key_table[p[12]];
        s[0] ^= extend_key_table[p[13]];
        s[1] ^= extend_key_table[p[14]];
        s[2] ^= extend_key_table[p[15]];

        uint32_t *s1 = (uint32_t *)s;
        *s1 ^= *(uint32_t *)(v1645 + 4 * i);
        memcpy(p + 0x10, s, 4);
        *s1 ^= *(uint32_t *)(p + 0x04);
        memcpy(p + 0x14, s, 4);
        *s1 ^= *(uint32_t *)(p + 0x08);
        memcpy(p + 0x18, s, 4);
        *s1 ^= *(uint32_t *)(p + 0x0c);
        memcpy(p + 0x1c, s, 4);
    }
}

struct v2578 {
    uint8_t key[16 + 16 * 10];
    uint8_t iv[16];
    uint8_t last_aes_str[16];
    int n_excess_bytes_last_time;
} v2578;

int v5095(uint8_t *key, uint8_t *iv)
{
    memset(&v2578, 0, sizeof(v2578));
    memcpy(v2578.key, key, 16);
    v4622(v2578.key);
    memcpy(v2578.iv, iv, 16);
    v2578.n_excess_bytes_last_time = 0;
    return 0;
}

int v6420(uint8_t *data, int len, uint8_t *out)
{
    int i;
    int j;
    uint8_t *v5527;
    if (v2578.n_excess_bytes_last_time > 0) {
        j = len >= (16 - v2578.n_excess_bytes_last_time) ? (16 - v2578.n_excess_bytes_last_time) : (len);
        for (i = 0; i < j; ++i) {
            out[i] = data[i] ^ v2578.last_aes_str[v2578.n_excess_bytes_last_time + i];
        }
        data += j;
        len -= j;
        out += j;
        v2578.n_excess_bytes_last_time += j;
        v2578.n_excess_bytes_last_time &= 0x0f;
    }

    int cnt = len / 16;
    int res = len & 0x0f;

    for (i = 0; i < cnt; ++i) {
        v5572(v2578.iv, v2578.last_aes_str, v2578.key);

        j = i << 4;
        v5527 = data + j;
        *(uint32_t*)(out + j + 0) = *(uint32_t*)(v5527 + 0) ^ *(uint32_t*)(v2578.last_aes_str + 0);
        *(uint32_t*)(out + j + 4) = *(uint32_t*)(v5527 + 4) ^ *(uint32_t*)(v2578.last_aes_str + 4);
        *(uint32_t*)(out + j + 8) = *(uint32_t*)(v5527 + 8) ^ *(uint32_t*)(v2578.last_aes_str + 8);
        *(uint32_t*)(out + j + 12) = *(uint32_t*)(v5527 + 12) ^ *(uint32_t*)(v2578.last_aes_str + 12);

        v5527 = v2578.iv + 15;
        while (0xff == (*v5527)) {
            *v5527 = 0x00;
            v5527--;
        }
        *v5527 += 1;
    }

    if (res > 0) {
        v5572(v2578.iv, v2578.last_aes_str, v2578.key);
        v2578.n_excess_bytes_last_time = res;
        v5527 = data + 16 * cnt;
        out += (cnt << 4);
        for (j = 0; j < res; ++j) {
            out[j] = v5527[j] ^ v2578.last_aes_str[j];
        }

        v5527 = v2578.iv + 15;
        while (0xff == (*v5527)) {
            *v5527 = 0x00;
            v5527--;
        }
        *v5527 += 1;
    }
    return 0;
}
