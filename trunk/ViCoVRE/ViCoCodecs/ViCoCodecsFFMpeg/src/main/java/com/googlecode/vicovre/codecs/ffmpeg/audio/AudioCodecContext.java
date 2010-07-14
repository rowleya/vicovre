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

package com.googlecode.vicovre.codecs.ffmpeg.audio;

public class AudioCodecContext {

    private int channels = 1;

    private int sampleRate = 0;

    private int bitRate = 0;

    private int compressionLevel = 0;

    private int globalQuality = 5;

    private int frameSize = 0;

    private int flags = 0;

    private int flags2 = 0;

    private byte[] extraData = null;

    /**
     * Returns the channels
     * @return the channels
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Sets the channels
     * @param channels the channels to set
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    /**
     * Returns the sampleRate
     * @return the sampleRate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets the sampleRate
     * @param sampleRate the sampleRate to set
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Returns the bitRate
     * @return the bitRate
     */
    public int getBitRate() {
        return bitRate;
    }

    /**
     * Sets the bitRate
     * @param bitRate the bitRate to set
     */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * Returns the compressionLevel
     * @return the compressionLevel
     */
    public int getCompressionLevel() {
        return compressionLevel;
    }

    /**
     * Sets the compressionLevel
     * @param compressionLevel the compressionLevel to set
     */
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    /**
     * Returns the global_quality
     * @return the global_quality
     */
    public int getGlobalQuality() {
        return globalQuality;
    }

    /**
     * Sets the global_quality
     * @param global_quality the global_quality to set
     */
    public void setGlobalQuality(int globalQuality) {
        this.globalQuality = globalQuality;
    }

    /**
     * Returns the frameSize
     * @return the frameSize
     */
    public int getFrameSize() {
        return frameSize;
    }

    /**
     * Sets the frameSize
     * @param frameSize the frameSize to set
     */
    public void setFrameSize(int frameSize) {
        this.frameSize = frameSize;
    }

    /**
     * Returns the flags
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Sets the flags
     * @param flags the flags to set
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * Returns the flags2
     * @return the flags2
     */
    public int getFlags2() {
        return flags2;
    }

    /**
     * Sets the flags2
     * @param flags2 the flags2 to set
     */
    public void setFlags2(int flags2) {
        this.flags2 = flags2;
    }

    public byte[] createExtraData(int size) {
        extraData = new byte[size];
        return extraData;
    }

    public byte[] getExtraData() {
        return extraData;
    }
}
