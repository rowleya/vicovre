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

import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;

public class VideoPreviewPanel extends Image
        implements MouseOverHandler, MouseOutHandler, LoadHandler {

    private String streamId = null;

    private int imageHeight = 0;

    private int width = 0;

    private int height = 0;

    private int pos = 0;

    private ChangeTimer timer = null;

    private boolean loaded = false;

    private class ChangeTimer extends Timer {

        public void run() {
            pos = (pos + height);
            if (pos >= imageHeight) {
                timer.cancel();
            } else {
                setVisibleRect(0, pos, width, height);
            }
        }

    }

    public VideoPreviewPanel(String baseUrl, String folder, String recordingId,
            String streamId, int width, int height) {
        this.width = width;
        this.height = height;
        this.streamId = streamId;
        addLoadHandler(this);
        setUrl(baseUrl + folder + "/" + recordingId
                + "/preview.do?ssrc=" + streamId
                + "&width=" + width + "&height=" + height);
    }

    public void onMouseOver(MouseOverEvent event) {
        if (loaded) {
            timer = new ChangeTimer();
            timer.scheduleRepeating(250);
        }
    }

    public void onMouseOut(MouseOutEvent event) {
        stop();
    }

    public void onLoad(LoadEvent event) {
        if (!loaded) {
            loaded = true;
            this.imageHeight = getHeight();
            setVisibleRect(0, 0, width, height);
            addMouseOverHandler(this);
            addMouseOutHandler(this);
        }
    }

    public String getStreamId() {
        return streamId;
    }

    public void stop() {
        if (loaded) {
            timer.cancel();
            setVisibleRect(0, 0, width, height);
            pos = 0;
        }
    }

}
