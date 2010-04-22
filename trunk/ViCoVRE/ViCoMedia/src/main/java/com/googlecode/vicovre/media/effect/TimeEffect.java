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

package com.googlecode.vicovre.media.effect;

import javax.media.Buffer;
import javax.media.Effect;
import javax.media.Format;
import javax.media.Time;

public class TimeEffect implements Effect {

    private long timeOffset = 0;

    private long pauseTime = -1;

    public Format[] getSupportedInputFormats() {
        return new Format[]{new Format(null)};
    }

    public Format[] getSupportedOutputFormats(Format format) {
        return new Format[]{format};
    }

    public int process(Buffer input, Buffer output) {
        output.copy(input);
        output.setTimeStamp(input.getTimeStamp() + timeOffset);
        return BUFFER_PROCESSED_OK;
    }

    public Format setInputFormat(Format format) {
        return format;
    }

    public Format setOutputFormat(Format format) {
        return format;
    }

    public void close() {
        // Does Nothing
    }

    public String getName() {
        return "TimeEffect";
    }

    public void open() {
        // Does Nothing
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public void pause() {
        pauseTime = System.currentTimeMillis();
    }

    public void resume() {
        if (pauseTime != -1) {
            timeOffset += (System.currentTimeMillis() - pauseTime) * 1000000;
            pauseTime = -1;
        }
    }

    public void seek(Time currentTime, Time seekTime) {
        timeOffset += currentTime.getNanoseconds() - seekTime.getNanoseconds();
    }

}
