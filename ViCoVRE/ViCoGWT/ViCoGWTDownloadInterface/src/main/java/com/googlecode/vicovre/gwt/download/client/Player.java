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

package com.googlecode.vicovre.gwt.download.client;

import com.google.gwt.http.client.URL;

import pl.rmalinowski.gwt2swf.client.ui.SWFWidget;

public class Player extends SWFWidget {

    private String file = null;

    private long start = 0;

    private long duration = 0;

    public Player(String baseUrl, int width, int height, String file,
            String timeParam, boolean audio, long start, long duration,
            long startTime) {
        super(baseUrl + "ViCoPlayerSimple.swf", width, height);
        this.file = file;
        this.start = start;
        this.duration = duration;
        addFlashVar("timeParam", timeParam);
        addFlashVar("audio", String.valueOf(audio));
        addFlashVar("startTime", String.valueOf(startTime));
        addFlashVar("duration", String.valueOf(duration));
        addFlashVar("file", URL.encodeComponent(getFile()));
    }

    private String getFile() {
        return file + "&start=" + (start * 1000)
            + "&duration=" + (duration * 1000);
    }

    private static native void setFile(String swfId, String file) /*-{
        $doc.getElementById(swfId).setFile(file);
    }-*/;

    private static native double getTime(String swfId) /*-{
        return $doc.getElementById(swfId).getTimeInSeconds();
    }-*/;

    private static native void stop(String swfId) /*-{
        $doc.getElementById(swfId).stop();
    }-*/;

    private static native void setDuration(String swfId, double duration) /*-{
        $doc.getElementById(swfId).setDuration(duration);
    }-*/;

    public void stop() {
        stop(getSwfId());
    }

    public double getTime() {
        return getTime(getSwfId());
    }

    public void setTimes(long start, long duration) {
        this.start = start;
        this.duration = duration;
        setFile(getSwfId(), getFile());
        setDuration(getSwfId(), duration);
    }
}
