/*
 * Java Apple Lossless (ALAC) decoder
 *
 * Copyright (c) 2011-2014 Peter McQuillan
 * Based on the ALAC decoder:
 * Copyright (c) 2005 David Hammerton
 *
 * Source: https://github.com/soiaf/Java-Apple-Lossless-decoder
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.beatofthedrum.alacdecoder;

class AlacFile {

    public int[] predictor_coef_table = new int[1024];
    public int[] predictor_coef_table_a = new int[1024];
    public int[] predictor_coef_table_b = new int[1024];
    byte[] input_buffer;
    int ibIdx = 0;
    int input_buffer_bitaccumulator = 0; /* used so we can do arbitary
						bit reads */
    int samplesize = 0;
    int numchannels = 0;
    int bytespersample = 0;
    LeadingZeros lz = new LeadingZeros();
    /* stuff from setinfo */
    int setinfo_max_samples_per_frame = 0; // 0x1000 = 4096
    int setinfo_7a = 0; // 0x00
    int setinfo_sample_size = 0; // 0x10
    int setinfo_rice_historymult = 0; // 0x28
    int setinfo_rice_initialhistory = 0; // 0x0a
    /* max samples per frame? */
    int setinfo_rice_kmodifier = 0; // 0x0e
    int setinfo_7f = 0; // 0x02
    int setinfo_80 = 0; // 0x00ff
    int setinfo_82 = 0; // 0x000020e7
    /* max sample size?? */
    int setinfo_86 = 0; // 0x00069fe4
    /* bit rate (avarge)?? */
    int setinfo_8a_rate = 0; // 0x0000ac44
    private final int buffer_size = 16384;
    /* buffers */
    int[] predicterror_buffer_a = new int[buffer_size];
    int[] predicterror_buffer_b = new int[buffer_size];
    int[] outputsamples_buffer_a = new int[buffer_size];
    /* end setinfo stuff */
    int[] outputsamples_buffer_b = new int[buffer_size];
    int[] uncompressed_bytes_buffer_a = new int[buffer_size];
    int[] uncompressed_bytes_buffer_b = new int[buffer_size];
}