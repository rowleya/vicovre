/**
 * Copyright (c) 2009, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the and the University of Manchester nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.googlecode.vicovre.codecs.ffmpeg;

import java.awt.Dimension;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

/**
 * Utility functions
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Utils {

    /**
     * No Pixel Format
     */
    public static final int PIX_FMT_NONE = -1;

    /**
     * Planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
     */
    public static final int PIX_FMT_YUV420P = 0;

    /**
     * Packed YUV 4:2:2, 16bpp, Y0 Cb Y1 Cr
     */
    public static final int PIX_FMT_YUYV422 = 1;

    /**
     * Packed RGB 8:8:8, 24bpp, RGBRGB...
     */
    public static final int PIX_FMT_RGB24 = 2;

    /**
     * Packed RGB 8:8:8, 24bpp, BGRBGR...
     */
    public static final int PIX_FMT_BGR24 = 3;

    /**
     * Planar YUV 4:2:2, 16bpp, (1 Cr & Cb sample per 2x1 Y samples)
     */
    public static final int PIX_FMT_YUV422P = 4;

    /**
     * Planar YUV 4:4:4, 24bpp, (1 Cr & Cb sample per 1x1 Y samples)
     */
    public static final int PIX_FMT_YUV444P = 5;

    /**
     * Packed RGB 8:8:8, 32bpp, (msb)8A 8R 8G 8B(lsb), in cpu endianness
     */
    public static final int PIX_FMT_RGB32 = 6;

    /**
     * Planar YUV 4:1:0,  9bpp, (1 Cr & Cb sample per 4x4 Y samples)
     */
    public static final int PIX_FMT_YUV410P = 7;

    /**
     * Planar YUV 4:1:1, 12bpp, (1 Cr & Cb sample per 4x1 Y samples)
     */
    public static final int PIX_FMT_YUV411P = 8;

    /**
     * Packed RGB 5:6:5, 16bpp, (msb)   5R 6G 5B(lsb), in cpu endianness
     */
    public static final int PIX_FMT_RGB565 = 9;

    /**
     * Packed RGB 5:5:5, 16bpp, (msb)1A 5R 5G 5B(lsb), in cpu endianness most
     * significant bit to 0
     */
    public static final int PIX_FMT_RGB555 = 10;

    /**
     * Y, 8bpp
     */
    public static final int PIX_FMT_GRAY8 = 11;

    /**
     * Y, 1bpp, 0 is white, 1 is black
     */
    public static final int PIX_FMT_MONOWHITE = 12;

    /**
     * Y, 1bpp, 0 is black, 1 is white
     */
    public static final int PIX_FMT_MONOBLACK = 13;

    /**
     * 8 bit with PIX_FMT_RGB32 palette
     */
    public static final int PIX_FMT_PAL8 = 14;

    /**
     * Planar YUV 4:2:0, 12bpp, full scale (jpeg)
     */
    public static final int PIX_FMT_YUVJ420P = 15;

    /**
     * Planar YUV 4:2:2, 16bpp, full scale (jpeg)
     */
    public static final int PIX_FMT_YUVJ422P = 16;

    /**
     * Planar YUV 4:4:4, 24bpp, full scale (jpeg)
     */
    public static final int PIX_FMT_YUVJ444P = 17;

    /**
     * XVideo Motion Acceleration via common packet passing(xvmc_render.h)
     */
    public static final int PIX_FMT_XVMC_MPEG2_MC = 18;

    /**
     * XVideo Motion Acceleration via common packet passing(xvmc_render.h)
     */
    public static final int PIX_FMT_XVMC_MPEG2_IDCT = 19;

    /**
     * Packed YUV 4:2:2, 16bpp, Cb Y0 Cr Y1
     */
    public static final int PIX_FMT_UYVY422 = 20;

    /**
     * Packed YUV 4:1:1, 12bpp, Cb Y0 Y1 Cr Y2 Y3
     */
    public static final int PIX_FMT_UYYVYY411 = 21;

    /**
     * Packed RGB 8:8:8, 32bpp, (msb)8A 8B 8G 8R(lsb), in cpu endianness
     */
    public static final int PIX_FMT_BGR32 = 22;

    /**
     * Packed RGB 5:6:5, 16bpp, (msb)   5B 6G 5R(lsb), in cpu endianness
     */
    public static final int PIX_FMT_BGR565 = 23;

    /**
     * Packed RGB 5:5:5, 16bpp, (msb)1A 5B 5G 5R(lsb), in cpu endianness most
     * significant bit to 1
     */
    public static final int PIX_FMT_BGR555 = 24;

    /**
     * Packed RGB 3:3:2,  8bpp, (msb)2B 3G 3R(lsb)
     */
    public static final int PIX_FMT_BGR8 = 25;

    /**
     * Packed RGB 1:2:1,  4bpp, (msb)1B 2G 1R(lsb)
     */
    public static final int PIX_FMT_BGR4 = 26;

    /**
     * Packed RGB 1:2:1,  8bpp, (msb)1B 2G 1R(lsb)
     */
    public static final int PIX_FMT_BGR4_BYTE = 27;

    /**
     * Packed RGB 3:3:2,  8bpp, (msb)2R 3G 3B(lsb)
     */
    public static final int PIX_FMT_RGB8 = 28;

    /**
     * Packed RGB 1:2:1,  4bpp, (msb)2R 3G 3B(lsb)
     */
    public static final int PIX_FMT_RGB4 = 29;

    /**
     * Packed RGB 1:2:1,  8bpp, (msb)2R 3G 3B(lsb)
     */
    public static final int PIX_FMT_RGB4_BYTE = 30;

    /**
     * Planar YUV 4:2:0, 12bpp, 1 plane for Y and 1 for UV
     */
    public static final int PIX_FMT_NV12 = 31;

    /**
     * Planar YVU 4:2:0, 12bpp, 1 plane for Y and 1 for UV
     */
    public static final int PIX_FMT_NV21 = 32;

    /**
     * Packed RGB 8:8:8, 32bpp, (msb)8R 8G 8B 8A(lsb), in cpu endianness
     */
    public static final int PIX_FMT_RGB32_1 = 33;

    /**
     * Packed RGB 8:8:8, 32bpp, (msb)8B 8G 8R 8A(lsb), in cpu endianness
     */
    public static final int PIX_FMT_BGR32_1 = 34;

    /**
     * Y, 16bpp, big-endian
     */
    public static final int PIX_FMT_GRAY16BE = 35;

    /**
     * Y, 16bpp, little-endian
     */
    public static final int PIX_FMT_GRAY16LE = 36;

    /**
     * Planar YUV 4:4:0 (1 Cr & Cb sample per 1x2 Y samples)
     */
    public static final int PIX_FMT_YUV440P = 37;

    /**
     * Planar YUV 4:4:0 full scale (jpeg)
     */
    public static final int PIX_FMT_YUVJ440P = 38;

    /**
     * number of pixel formats, DO NOT USE THIS if you want to link with shared
     * libav* because the number of formats might differ between versions
     */
    public static final int PIX_FMT_NB = 39;

    /**
     * No Codec
     */
    public static final int CODEC_ID_NONE               = 0;

    /**
     * MPEG1
     */
    public static final int CODEC_ID_MPEG1VIDEO         = 1;

    /**
     * preferred ID for MPEG-1/2 video decoding
     */
    public static final int CODEC_ID_MPEG2VIDEO         = 2;

    /**
     * XVMC MPEG 2
     */
    public static final int CODEC_ID_MPEG2VIDEO_XVMC    = 3;

    /**
     * H261
     */
    public static final int CODEC_ID_H261               = 4;

    /**
     * H263
     */
    public static final int CODEC_ID_H263               = 5;

    /**
     * Real Video 1
     */
    public static final int CODEC_ID_RV10               = 6;

    /**
     * Real Video 2
     */
    public static final int CODEC_ID_RV20               = 7;

    /**
     * Motion JPEG
     */
    public static final int CODEC_ID_MJPEG              = 8;

    /**
     * Motion JPEG B
     */
    public static final int CODEC_ID_MJPEGB             = 9;

    /**
     * Lossless JPEG
     */
    public static final int CODEC_ID_LJPEG              = 10;

    /**
     * Sunplus JPEG
     */
    public static final int CODEC_ID_SP5X               = 11;

    /**
     * Lossless JPEG
     */
    public static final int CODEC_ID_JPEGLS             = 12;

    /**
     * MPEG-4
     */
    public static final int CODEC_ID_MPEG4              = 13;

    /**
     * RAW Video
     */
    public static final int CODEC_ID_RAWVIDEO           = 14;

    /**
     * Microsoft MPEG-4 Version 1
     */
    public static final int CODEC_ID_MSMPEG4V1          = 15;

    /**
     * Microsoft MPEG-4 Version 2
     */
    public static final int CODEC_ID_MSMPEG4V2          = 16;

    /**
     * Microsoft MPEG-4 Version 3
     */
    public static final int CODEC_ID_MSMPEG4V3          = 17;

    /**
     * Windows Media Video 7
     */
    public static final int CODEC_ID_WMV1               = 18;

    /**
     * Windows Media Video 8
     */
    public static final int CODEC_ID_WMV2               = 19;

    /**
     * H263+
     */
    public static final int CODEC_ID_H263P              = 20;

    /**
     * Intel H263
     */
    public static final int CODEC_ID_H263I              = 21;

    /**
     * Sorenson H.263 (Flash Video)
     */
    public static final int CODEC_ID_FLV1               = 22;

    /**
     * Sorenson Vector Quantizer 1
     */
    public static final int CODEC_ID_SVQ1               = 23;

    /**
     * Sorenson Vector Quantizer 3
     */
    public static final int CODEC_ID_SVQ3               = 24;

    /**
     * DV Video
     */
    public static final int CODEC_ID_DVVIDEO            = 25;

    /**
     * HuffYUV
     */
    public static final int CODEC_ID_HUFFYUV            = 26;

    /**
     * Creative YUV
     */
    public static final int CODEC_ID_CYUV               = 27;

    /**
     * H264 AVC / MPEG-4 AVC / MPEG-4 Part 10
     */
    public static final int CODEC_ID_H264               = 28;

    /**
     * Intel Indeo Version 3
     */
    public static final int CODEC_ID_INDEO3             = 29;

    /**
     * On2 VP3
     */
    public static final int CODEC_ID_VP3                = 30;

    /**
     * Theora
     */
    public static final int CODEC_ID_THEORA             = 31;

    /**
     * Asus Version 1
     */
    public static final int CODEC_ID_ASV1               = 32;

    /**
     * Asus Version 2
     */
    public static final int CODEC_ID_ASV2               = 33;

    /**
     * FFmpeg codec #1
     */
    public static final int CODEC_ID_FFV1               = 34;

    /**
     * 4x Technologies Format
     */
    public static final int CODEC_ID_4XM                = 35;

    /**
     * ATI VCR 1
     */
    public static final int CODEC_ID_VCR1               = 36;

    /**
     * Cirrus Logic AccuPak
     */
    public static final int CODEC_ID_CLJR               = 37;

    /**
     * Sony Playstation Motion Decoder
     */
    public static final int CODEC_ID_MDEC               = 38;

    /**
     * ID RoQ (Used in games)
     */
    public static final int CODEC_ID_ROQ                = 39;

    /**
     * Interplay MVE
     */
    public static final int CODEC_ID_INTERPLAY_VIDEO    = 40;

    /**
     * Wing Commander III
     */
    public static final int CODEC_ID_XAN_WC3            = 41;

    /**
     * Wing Commander IV
     */
    public static final int CODEC_ID_XAN_WC4            = 42;

    /**
     * QuickTime Video
     */
    public static final int CODEC_ID_RPZA               = 43;

    /**
     * Cinepak
     */
    public static final int CODEC_ID_CINEPAK            = 44;

    /**
     * Westwood Studios Games Video
     */
    public static final int CODEC_ID_WS_VQA             = 45;

    /**
     * Microsoft Run Length Encoded video
     */
    public static final int CODEC_ID_MSRLE              = 46;

    /**
     * Microsoft Video 1
     */
    public static final int CODEC_ID_MSVIDEO1           = 47;

    /**
     * ID Quake II CIN Video
     */
    public static final int CODEC_ID_IDCIN              = 48;

    /**
     * Planar RGB
     */
    public static final int CODEC_ID_8BPS               = 49;

    /**
     * QuickTime Graphics
     */
    public static final int CODEC_ID_SMC                = 50;

    /**
     * Autodesk Animator Flic Video
     */
    public static final int CODEC_ID_FLIC               = 51;

    /**
     * Duck TrueMotion 1.0
     */
    public static final int CODEC_ID_TRUEMOTION1        = 52;

    /**
     * Sierra VMD Video
     */
    public static final int CODEC_ID_VMDVIDEO           = 53;

    /**
     * Lossless Codec Library MSZH
     */
    public static final int CODEC_ID_MSZH               = 54;

    /**
     * Lossless Codec Library ZLIB
     */
    public static final int CODEC_ID_ZLIB               = 55;

    /**
     * QuickTime Animation RLE Video
     */
    public static final int CODEC_ID_QTRLE              = 56;

    /**
     * Snow Experimental Wavelet
     */
    public static final int CODEC_ID_SNOW               = 57;

    /**
     * Techsmith Sceeen Capture
     */
    public static final int CODEC_ID_TSCC               = 58;

    /**
     * IBM Ultimotion
     */
    public static final int CODEC_ID_ULTI               = 59;

    /**
     * Apple QuickDraw
     */
    public static final int CODEC_ID_QDRAW              = 60;

    /**
     * Miro VideoXL
     */
    public static final int CODEC_ID_VIXL               = 61;

    /**
     * Q-team QPEG
     */
    public static final int CODEC_ID_QPEG               = 62;

    /**
     * X-VID
     */
    public static final int CODEC_ID_XVID               = 63;

    /**
     * PNG Image
     */
    public static final int CODEC_ID_PNG                = 64;

    /**
     * Portable PixelMap Image
     */
    public static final int CODEC_ID_PPM                = 65;

    /**
     * Portable Bitmap Image
     */
    public static final int CODEC_ID_PBM                = 66;

    /**
     * Portable GrayMap image
     */
    public static final int CODEC_ID_PGM                = 67;

    /**
     * Portable GrayMap image with U and V components in YUV 4:2:0
     */
    public static final int CODEC_ID_PGMYUV             = 68;

    /**
     * PNM extension with alpha support
     */
    public static final int CODEC_ID_PAM                = 69;

    /**
     * HuffYUV FFMPEG Variant
     */
    public static final int CODEC_ID_FFVHUFF            = 70;

    /**
     * Real Video Version 3
     */
    public static final int CODEC_ID_RV30               = 71;

    /**
     * Real Video Version 4
     */
    public static final int CODEC_ID_RV40               = 72;

    /**
     * SMPTE VC-1
     */
    public static final int CODEC_ID_VC1                = 73;

    /**
     * Windows Media Video Version 9
     */
    public static final int CODEC_ID_WMV3               = 74;

    /**
     * LOCO
     */
    public static final int CODEC_ID_LOCO               = 75;

    /**
     * Winnov WNV1
     */
    public static final int CODEC_ID_WNV1               = 76;

    /**
     * Autodesk Run Length Encoded
     */
    public static final int CODEC_ID_AASC               = 77;

    /**
     * Intel Indeo Version 2
     */
    public static final int CODEC_ID_INDEO2             = 78;

    /**
     * FRAPS
     */
    public static final int CODEC_ID_FRAPS              = 79;

    /**
     * Duck TrueMotion Version 2.0
     */
    public static final int CODEC_ID_TRUEMOTION2        = 80;

    /**
     * Microsoft Bitmap Image
     */
    public static final int CODEC_ID_BMP                = 81;

    /**
     * CamStudio
     */
    public static final int CODEC_ID_CSCD               = 82;

    /**
     * American Laser Games MM Video
     */
    public static final int CODEC_ID_MMVIDEO            = 83;

    /**
     * Zip Motion Blocks Video
     */
    public static final int CODEC_ID_ZMBV               = 84;

    /**
     * Audio Video Standard (Creature Shock Game)
     */
    public static final int CODEC_ID_AVS                = 85;

    /**
     * Smacker Games Video Format
     */
    public static final int CODEC_ID_SMACKVIDEO         = 86;

    /**
     * NuppelVideo
     */
    public static final int CODEC_ID_NUV                = 87;

    /**
     * Karl Morton's Video Codec (Worms Games)
     */
    public static final int CODEC_ID_KMVC               = 88;

    /**
     * Flash Screen Video Version 1
     */
    public static final int CODEC_ID_FLASHSV            = 89;

    /**
     * Chinese AVS video (AVS1-P2, JiZhun profile)
     */
    public static final int CODEC_ID_CAVS               = 90;

    /**
     * JPEG 2000
     */
    public static final int CODEC_ID_JPEG2000           = 91;

    /**
     * VMWare Screen Codec
     */
    public static final int CODEC_ID_VMNC               = 92;

    /**
     * On2 VP5
     */
    public static final int CODEC_ID_VP5                = 93;

    /**
     * On2 VP6
     */
    public static final int CODEC_ID_VP6                = 94;

    /**
     * On2 VP6 Flash Version
     */
    public static final int CODEC_ID_VP6F               = 95;

    /**
     * Truevision Targa Image
     */
    public static final int CODEC_ID_TARGA              = 96;

    /**
     * Delphine Software International CIN Games video
     */
    public static final int CODEC_ID_DSICINVIDEO        = 97;

    /**
     * Tiertex Limited SEQ Flashback Game video
     */
    public static final int CODEC_ID_TIERTEXSEQVIDEO    = 98;

    /**
     * TIFF Image
     */
    public static final int CODEC_ID_TIFF               = 99;

    /**
     * GIF Image
     */
    public static final int CODEC_ID_GIF                = 100;

    /**
     * H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10 (VDPAU acceleration)
     */
    public static final int CODEC_ID_FFH264             = 101;

    /**
     * his format is used in the non-Windows version of the Feeble Files game
     * and different game cutscenes repacked for use with ScummVM.
     */
    public static final int CODEC_ID_DXA                = 102;

    /**
     * SMPTE VC3
     */
    public static final int CODEC_ID_DNXHD              = 103;

    /**
     * Nintendo Gamecube THP video
     */
    public static final int CODEC_ID_THP                = 104;

    /**
     * SGI RGB Image
     */
    public static final int CODEC_ID_SGI                = 105;

    /**
     * Cyberia Game C93 Video
     */
    public static final int CODEC_ID_C93                = 106;

    /**
     * Bethesda Softworks Games VID Video
     */
    public static final int CODEC_ID_BETHSOFTVID        = 107;

    /**
     * V.Flash PTX Image
     */
    public static final int CODEC_ID_PTX                = 108;

    /**
     * Renderware TeXture Dictionary
     */
    public static final int CODEC_ID_TXD                = 109;

    /**
     * PCM Signed 16-bit Little Endian
     */
    public static final int CODEC_ID_PCM_S16LE          = 0x10000;

    /**
     * PCM Signed 16-bit Big Endian
     */
    public static final int CODEC_ID_PCM_S16BE          = 0x10001;

    /**
     * PCM Unsigned 16-bit Little Endian
     */
    public static final int CODEC_ID_PCM_U16LE          = 0x10002;

    /**
     * PCM Unsigned 16-bit Big Endian
     */
    public static final int CODEC_ID_PCM_U16BE          = 0x10003;

    /**
     * PCM Signed 8-bit
     */
    public static final int CODEC_ID_PCM_S8             = 0x10004;

    /**
     * PCM Unsigned 8-bit
     */
    public static final int CODEC_ID_PCM_U8             = 0x10005;

    /**
     * PCM MU-Law
     */
    public static final int CODEC_ID_PCM_MULAW          = 0x10006;

    /**
     * PCM A-Law
     */
    public static final int CODEC_ID_PCM_ALAW           = 0x10007;

    /**
     * PCM Signed 32-bit Little Endian
     */
    public static final int CODEC_ID_PCM_S32LE          = 0x10008;

    /**
     * PCM Signed 32-bit Big Endian
     */
    public static final int CODEC_ID_PCM_S32BE          = 0x10009;

    /**
     * PCM Unsigned 32-bit Little Endian
     */
    public static final int CODEC_ID_PCM_U32LE          = 0x1000a;

    /**
     * PCM Unsigned 32-bit Big Endian
     */
    public static final int CODEC_ID_PCM_U32BE          = 0x1000b;

    /**
     * PCM Signed 24-bit Little Endian
     */
    public static final int CODEC_ID_PCM_S24LE          = 0x1000c;

    /**
     * PCM Signed 24-bit Big Endian
     */
    public static final int CODEC_ID_PCM_S24BE          = 0x1000d;

    /**
     * PCM Unsigned 24-bit Little Endian
     */
    public static final int CODEC_ID_PCM_U24LE          = 0x1000e;

    /**
     * PCM Unsigned 24-bit Big Endian
     */
    public static final int CODEC_ID_PCM_U24BE          = 0x1000f;

    /**
     * PCM D-Cinema audio signed 24-bit
     */
    public static final int CODEC_ID_PCM_S24DAUD        = 0x10010;

    /**
     * ZORK PCM
     */
    public static final int CODEC_ID_PCM_ZORK           = 0x10011;

    /**
     * ADPCM IMA QuickTime
     */
    public static final int CODEC_ID_ADPCM_IMA_QT       = 0x11000;

    /**
     * ADPCM IMA WAV
     */
    public static final int CODEC_ID_ADPCM_IMA_WAV      = 0x11001;

    /**
     * ADPCM IMA Duck DK3
     */
    public static final int CODEC_ID_ADPCM_IMA_DK3      = 0x11002;

    /**
     * ADPCM IMA Duck DK4
     */
    public static final int CODEC_ID_ADPCM_IMA_DK4      = 0x11003;

    /**
     * ADPCM IMA Westwood Studio Games
     */
    public static final int CODEC_ID_ADPCM_IMA_WS       = 0x11004;

    /**
     * ADPCM IMA Loki Game Ports
     */
    public static final int CODEC_ID_ADPCM_IMA_SMJPEG   = 0x11005;

    /**
     * ADPCM Microsoft
     */
    public static final int CODEC_ID_ADPCM_MS           = 0x11006;

    /**
     * ADPCM 4X Technologies Games
     */
    public static final int CODEC_ID_ADPCM_4XM          = 0x11007;

    /**
     * ADPCM CDROM XA
     */
    public static final int CODEC_ID_ADPCM_XA           = 0x11008;

    /**
     * ADPCM SEGA Dreamcast CRI ADX
     */
    public static final int CODEC_ID_ADPCM_ADX          = 0x11009;

    /**
     * ADPCM Electronic Arts
     */
    public static final int CODEC_ID_ADPCM_EA           = 0x1100a;

    /**
     * ADPCM G.726
     */
    public static final int CODEC_ID_ADPCM_G726         = 0x1100b;

    /**
     * ADPCM Creative Technology
     */
    public static final int CODEC_ID_ADPCM_CT           = 0x1100c;

    /**
     * ADPCM Shockwave Flash
     */
    public static final int CODEC_ID_ADPCM_SWF          = 0x1100d;

    /**
     * ADPCM Yamaha
     */
    public static final int CODEC_ID_ADPCM_YAMAHA       = 0x1100e;

    /**
     * ADPCM SoundBlaster Pro 4-bit
     */
    public static final int CODEC_ID_ADPCM_SBPRO_4      = 0x1100f;

    /**
     * ADPCM SoundBlaster Pro 2.6-bit
     */
    public static final int CODEC_ID_ADPCM_SBPRO_3      = 0x11010;

    /**
     * ADPCM SoundBlaster Pro 4-bit
     */
    public static final int CODEC_ID_ADPCM_SBPRO_2      = 0x11011;

    /**
     * ADPCM Nintendo Gamecube THP
     */
    public static final int CODEC_ID_ADPCM_THP          = 0x11012;

    /**
     * AMR-NB
     */
    public static final int CODEC_ID_AMR_NB             = 0x12000;

    /**
     * AMR-WB
     */
    public static final int CODEC_ID_AMR_WB             = 0x12001;

    /**
     * Real Audio 1 (14.4K)
     */
    public static final int CODEC_ID_RA_144             = 0x13000;

    /**
     * Real Audio 2 (28.8K)
     */
    public static final int CODEC_ID_RA_288             = 0x13001;

    /**
     * DPCM ID RoQ Games
     */
    public static final int CODEC_ID_ROQ_DPCM           = 0x14000;

    /**
     * DPCM Interplay Games
     */
    public static final int CODEC_ID_INTERPLAY_DPCM     = 0x14001;

    /**
     * DPCM Wing Commander
     */
    public static final int CODEC_ID_XAN_DPCM           = 0x14002;

    /**
     * DPCM Sierra Online Games
     */
    public static final int CODEC_ID_SOL_DPCM           = 0x14003;

    /**
     * MPEG Audio Layer 2
     */
    public static final int CODEC_ID_MP2                = 0x15000;

    /**
     * MPEG Audio Layer 3
     * preferred ID for decoding MPEG audio layer 1, 2 or 3
     */
    public static final int CODEC_ID_MP3                = 0x15001;

    /**
     * AAC
     */
    public static final int CODEC_ID_AAC                = 0x15002;

    /**
     * AC-3
     */
    public static final int CODEC_ID_AC3                = 0x15004;

    /**
     * DTS
     */
    public static final int CODEC_ID_DTS                = 0x15005;

    /**
     * OGG Vorbis
     */
    public static final int CODEC_ID_VORBIS             = 0x15006;

    /**
     * DV Audio
     */
    public static final int CODEC_ID_DVAUDIO            = 0x15007;

    /**
     * Windows Media Audio Version 1
     */
    public static final int CODEC_ID_WMAV1              = 0x15008;

    /**
     * Windows Media Audio Version 2
     */
    public static final int CODEC_ID_WMAV2              = 0x15009;

    /**
     * Macintosh Audio Compression/Expansion 3:1
     */
    public static final int CODEC_ID_MACE3              = 0x1500a;

    /**
     * Macintosh Audio Compression/Expansion 6:1
     */
    public static final int CODEC_ID_MACE6              = 0x1500b;

    /**
     * Sierra VMD Audio
     */
    public static final int CODEC_ID_VMDAUDIO           = 0x1500c;

    /**
     * Sonic Experimental
     */
    public static final int CODEC_ID_SONIC              = 0x1500d;

    /**
     * Sonic Lossless Experimental
     */
    public static final int CODEC_ID_SONIC_LS           = 0x1500e;

    /**
     * Free Lossless Audio Codec
     */
    public static final int CODEC_ID_FLAC               = 0x1500f;

    /**
     * ADU MP3
     */
    public static final int CODEC_ID_MP3ADU             = 0x15010;

    /**
     * MP3onMP4 MP3
     */
    public static final int CODEC_ID_MP3ON4             = 0x15011;

    /**
     * Shorten
     */
    public static final int CODEC_ID_SHORTEN            = 0x15012;

    /**
     * Apple Lossless Audio
     */
    public static final int CODEC_ID_ALAC               = 0x15013;

    /**
     * Westwood SND1 Audio
     */
    public static final int CODEC_ID_WESTWOOD_SND1      = 0x15014;

    /**
     * GSM
     */
    public static final int CODEC_ID_GSM                = 0x15015;

    /**
     * QDesign Music Codec 2
     */
    public static final int CODEC_ID_QDM2               = 0x15016;

    /**
     * COOK
     */
    public static final int CODEC_ID_COOK               = 0x15017;

    /**
     * DSP Group TrueSpeech
     */
    public static final int CODEC_ID_TRUESPEECH         = 0x15018;

    /**
     * True Audio
     */
    public static final int CODEC_ID_TTA                = 0x15019;

    /**
     * Smacker Audio
     */
    public static final int CODEC_ID_SMACKAUDIO         = 0x1501a;

    /**
     * QCELP PureVoice
     */
    public static final int CODEC_ID_QCELP              = 0x1501b;

    /**
     * WavPack
     */
    public static final int CODEC_ID_WAVPACK            = 0x1501c;

    /**
     * Delphine Software International CIN audio
     */
    public static final int CODEC_ID_DSICINAUDIO        = 0x1501d;

    /**
     * Intel Music Coder
     */
    public static final int CODEC_ID_IMC                = 0x1501e;

    /**
     * Musepack SV7
     */
    public static final int CODEC_ID_MUSEPACK7          = 0x1501f;

    /**
     * Meridian Lossless Packing
     */
    public static final int CODEC_ID_MLP                = 0x15020;

    /**
     * Microsoft GSM as found in WAV
     */
    public static final int CODEC_ID_GSM_MS             = 0x15021;

    /**
     * Adaptive TRansform Acoustic Coding Version 3
     */
    public static final int CODEC_ID_ATRAC3             = 0x15022;

    /**
     * VOXWare
     */
    public static final int CODEC_ID_VOXWARE            = 0x15023;

    /**
     * DVD Subtitles
     */
    public static final int CODEC_ID_DVD_SUBTITLE       = 0x17000;

    /**
     * DVB Subtitles
     */
    public static final int CODEC_ID_DVB_SUBTITLE       = 0x17001;

    /**
     * Raw UTF-8 text
     */
    public static final int CODEC_ID_TEXT               = 0x17002;

    /**
     * XSUB Subtitles
     */
    public static final int CODEC_ID_XSUB               = 0x17003;

    /**
     * FAKE codec to indicate a raw MPEG-2 TS stream (only used by libavformat)
     */
    public static final int CODEC_ID_MPEG2TS            = 0x20000;

    /**
     * Obsolete Lame MP3
     */
    public static final int CODEC_ID_MP3LAME = CODEC_ID_MP3;

    /**
     * MPEG4 AAC
     */
    public static final int CODEC_ID_MPEG4AAC = CODEC_ID_AAC;

    private Utils() {
        // Does Nothing
    }

    private static class PixFmtInfo {

        private PixFmtInfo(String name, int channels) {
            // Does Nothing
        }
    }

    private static class YUVPixFmtInfo extends PixFmtInfo {
        private int xChromaShift = 0;
        private int yChromaShift = 0;
        private int jmfType = 0;

        private YUVPixFmtInfo(String name, int channels,
                int jmfType, int xChromaShift, int yChromaShift) {
            super(name, channels);
            this.xChromaShift = xChromaShift;
            this.yChromaShift = yChromaShift;
            this.jmfType = jmfType;
        }
    }

    private static class RGBPixFmtInfo extends PixFmtInfo {
        private int depth = 0;
        private int red = 0;
        private int green = 0;
        private int blue = 0;
        private int pixelStride = 0;
        private Class< ? > dataType = null;

        private RGBPixFmtInfo(String name, int channels, boolean isAlpha,
                int depth, int red, int green,
                int blue, int pixelStride, Class< ? > dataType) {
            super(name, channels);
            this.depth = depth;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.pixelStride = pixelStride;
            this.dataType = dataType;
        }
    }

    private static final PixFmtInfo[] PIXFMTINFO =
        new PixFmtInfo[PIX_FMT_NB];

    static {


        // YUV formats
        PIXFMTINFO[PIX_FMT_YUV420P] = new YUVPixFmtInfo(
            "yuv420p", 3, YUVFormat.YUV_420, 1, 1);
        PIXFMTINFO[PIX_FMT_YUV422P] = new YUVPixFmtInfo(
            "yuv422p", 3, YUVFormat.YUV_422, 1, 0);
        PIXFMTINFO[PIX_FMT_YUV444P] = new YUVPixFmtInfo(
            "yuv444p", 3, YUVFormat.YUV_111, 0, 0);
        PIXFMTINFO[PIX_FMT_YUYV422] = new YUVPixFmtInfo(
            "yuyv422", 1, YUVFormat.YUV_YUYV, 1, 0);
        PIXFMTINFO[PIX_FMT_UYVY422] = new YUVPixFmtInfo(
            "uyvy422", 1, YUVFormat.YUV_YUYV, 1, 0);
        PIXFMTINFO[PIX_FMT_YUV411P] = new YUVPixFmtInfo(
            "yuv411p", 3, YUVFormat.YUV_411, 2, 0);

        // RGB formats
        PIXFMTINFO[PIX_FMT_RGB24] = new RGBPixFmtInfo(
            "rgb24", 1, false, 24, 1, 2, 3, 3, Format.byteArray);
        PIXFMTINFO[PIX_FMT_BGR24] = new RGBPixFmtInfo(
            "bgr24", 1, false, 24, 3, 2, 1, 3, Format.byteArray);
        PIXFMTINFO[PIX_FMT_RGB32] = new RGBPixFmtInfo(
            "rgb32", 1, true, 32, 0xFF0000, 0x00FF00, 0x0000FF, 1,
            Format.intArray);
        PIXFMTINFO[PIX_FMT_RGB565] = new RGBPixFmtInfo(
            "rgb565", 1, false, 16, 0xF800, 0x7E0, 0x1F, 1, Format.shortArray);
        PIXFMTINFO[PIX_FMT_RGB555] = new RGBPixFmtInfo(
            "rgb555", 1, true, 16, 0x7C00, 0x3E0, 0x1F, 1, Format.shortArray);
        PIXFMTINFO[PIX_FMT_BGR32] = new RGBPixFmtInfo(
            "bgr32", 1, true, 32, 0x0000FF, 0x00FF00, 0xFF0000, 1,
            Format.intArray);
        PIXFMTINFO[PIX_FMT_BGR565] = new RGBPixFmtInfo(
            "bgr565", 1, false, 16, 0x1F, 0x7E0, 0xF800, 1, Format.shortArray);
        PIXFMTINFO[PIX_FMT_BGR555] = new RGBPixFmtInfo(
            "bgr555", 1, false, 16, 0x1F, 0x3E0, 0x7C00, 1, Format.shortArray);
        PIXFMTINFO[PIX_FMT_RGB8] = new RGBPixFmtInfo(
            "rgb8", 1, false, 8, 0xC0, 0x38, 0x7, 1, Format.byteArray);
        PIXFMTINFO[PIX_FMT_RGB4_BYTE] = new RGBPixFmtInfo(
            "rgb4_byte", 1, false, 4, 0x8, 0x6, 0x1, 1, Format.byteArray);
        PIXFMTINFO[PIX_FMT_BGR8] = new RGBPixFmtInfo(
            "bgr8", 1, false, 8, 0x7, 0x38, 0xC0, 1, Format.byteArray);
        PIXFMTINFO[PIX_FMT_BGR4_BYTE] = new RGBPixFmtInfo(
            "bgr4_byte", 1, false, 4, 0x1, 0x6, 0x8, 1, Format.byteArray);
        PIXFMTINFO[PIX_FMT_BGR32_1] = new RGBPixFmtInfo(
            "bgr32_1", 1, true, 32, 0xFF000000, 0xFF0000, 0xFF00, 1,
            Format.intArray);
        PIXFMTINFO[PIX_FMT_RGB32_1] = new RGBPixFmtInfo(
            "rgb32_1", 1, true, 32, 0xFF00, 0xFF0000, 0xFF000000, 1,
            Format.intArray);

        // gray / mono formats
        PIXFMTINFO[PIX_FMT_GRAY16BE] = new RGBPixFmtInfo(
            "gray16be", 1, false, 16, 1, 1, 1, 1, Format.shortArray);
        PIXFMTINFO[PIX_FMT_GRAY16LE] = new RGBPixFmtInfo(
            "gray16le", 1, false, 16, 1, 1, 1, 1, Format.shortArray);
        PIXFMTINFO[PIX_FMT_GRAY8] = new RGBPixFmtInfo(
            "gray", 1, false, 8, 1, 1, 1, 1, Format.byteArray);
    }

    /**
     * Gets a JMF format from a FFMPEG format
     * @param pixFmt The FFMPEG format identifier
     * @param size The size to use (or null for none)
     * @param frameRate The frame rate to be used
     * @return A JMF VideoFormat or null if incompatible
     */
    public static VideoFormat getVideoFormat(int pixFmt,
            Dimension size, float frameRate) {
        if ((pixFmt < 0) || (pixFmt >= PIXFMTINFO.length)
                || (PIXFMTINFO[pixFmt] == null)) {
            return null;
        }

        VideoFormat result = null;
        final int endian =
            ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder())
                ? RGBFormat.BIG_ENDIAN : RGBFormat.LITTLE_ENDIAN;
        final YUVPixFmtInfo yuvinfo;
        final RGBPixFmtInfo rgbinfo;

        switch (pixFmt) {

        case PIX_FMT_YUV420P:
        case PIX_FMT_YUV422P:
        case PIX_FMT_YUV444P:
        case PIX_FMT_YUV411P:
            yuvinfo = (YUVPixFmtInfo) PIXFMTINFO[pixFmt];
            if (size != null) {
                int w2 = (size.width + (1 << yuvinfo.xChromaShift) - 1)
                        >> yuvinfo.xChromaShift;
                int h2 = (size.height + (1 << yuvinfo.yChromaShift) - 1)
                        >> yuvinfo.yChromaShift;
                final int noPixels = size.width * size.height;
                int size2 = w2 * h2;
                result = new YUVFormat(size,
                        noPixels + size2 + size2, Format.byteArray, frameRate,
                        yuvinfo.jmfType, size.width, w2, 0, noPixels,
                        noPixels + size2);
            } else {
                result = new YUVFormat(size, Format.NOT_SPECIFIED,
                        Format.byteArray, frameRate, yuvinfo.jmfType,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED);
            }
            break;

        case PIX_FMT_YUYV422:
            yuvinfo = (YUVPixFmtInfo) PIXFMTINFO[pixFmt];
            if (size != null) {
                final int noPixels = size.width * size.height;
                result = new YUVFormat(size,
                        noPixels * 2, Format.shortArray, frameRate,
                        yuvinfo.jmfType, size.width / 2, size.width / 2,
                        0, 1, 3);
            } else {
                result = new YUVFormat(size, Format.NOT_SPECIFIED,
                        Format.shortArray, frameRate, yuvinfo.jmfType,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED);
            }
            break;

        case PIX_FMT_UYVY422:
            yuvinfo = (YUVPixFmtInfo) PIXFMTINFO[pixFmt];
            if (size != null) {
                final int noPixels = size.width * size.height;
                int w2 = (size.width + (1 << yuvinfo.xChromaShift) - 1)
                    >> yuvinfo.xChromaShift;
                result = new YUVFormat(size,
                        noPixels * 2, Format.shortArray, frameRate,
                        yuvinfo.jmfType, w2, w2, 1, 0, 2);
            } else {
                result = new YUVFormat(size, Format.NOT_SPECIFIED,
                        Format.shortArray, frameRate, yuvinfo.jmfType,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED);
            }
            break;

        case PIX_FMT_RGB24:
        case PIX_FMT_BGR24:
        case PIX_FMT_RGB32:
        case PIX_FMT_RGB565:
        case PIX_FMT_RGB555:
        //case PIX_FMT_BGR32:
        case PIX_FMT_BGR565:
        case PIX_FMT_BGR555:
        case PIX_FMT_RGB8:
        case PIX_FMT_RGB4_BYTE:
        case PIX_FMT_BGR8:
        case PIX_FMT_BGR4_BYTE:
        /*case PIX_FMT_BGR32_1:
        case PIX_FMT_RGB32_1: */
        case PIX_FMT_GRAY16BE:
        case PIX_FMT_GRAY16LE:
        case PIX_FMT_GRAY8:
            rgbinfo = (RGBPixFmtInfo) PIXFMTINFO[pixFmt];
            if (size != null) {
                final int noPixels = size.width * size.height;
                int maxdataLength = noPixels * rgbinfo.pixelStride;
                int lineStride = rgbinfo.pixelStride * size.width;
                result = new RGBFormat(size,
                        maxdataLength, rgbinfo.dataType, frameRate,
                        rgbinfo.depth, rgbinfo.red, rgbinfo.green, rgbinfo.blue,
                        rgbinfo.pixelStride, lineStride, Format.NOT_SPECIFIED,
                        endian);
            } else {
                result = new RGBFormat(size, Format.NOT_SPECIFIED,
                        rgbinfo.dataType, frameRate, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED);
            }
            break;

        default:
            return null;
        }

        return result;
    }

    /**
     * Gets all the video formats supported
     * @param size The size to use (or null for any size)
     * @param frameRate The framerate to use
     * @return the set of formats supported
     */
    public static VideoFormat[] getVideoFormats(Dimension size,
            int frameRate) {
        Vector<VideoFormat> formats = new Vector<VideoFormat>();
        for (int i = 0; i < PIXFMTINFO.length; i++) {
            VideoFormat format = getVideoFormat(i, size, frameRate);
            if (format != null) {
                formats.add(format);
            }
        }
        return formats.toArray(new VideoFormat[0]);
    }

    /**
     * Gets the pixel format associated with the given format
     * @param format The format to get the pixel format of
     * @return The pixel format
     */
    public static int getPixFormat(VideoFormat format) {
        for (int i = 0; i < PIXFMTINFO.length; i++) {
            VideoFormat videoFormat = getVideoFormat(i, format.getSize(),
                    format.getFrameRate());
            if (videoFormat != null) {
                if (videoFormat.matches(format)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Determines if a format can be converted
     * @param format The format to test
     * @return True if the format is compatible, false otherwise
     */
    public static boolean canBeConverted(VideoFormat format) {
        int pixFormat = getPixFormat(format);
        if (pixFormat != -1) {
            return true;
        }
        return false;
    }
}
