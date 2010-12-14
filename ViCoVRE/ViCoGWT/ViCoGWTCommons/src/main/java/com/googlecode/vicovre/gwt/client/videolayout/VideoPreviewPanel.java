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

package com.googlecode.vicovre.gwt.client.videolayout;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;

public class VideoPreviewPanel extends VerticalPanel
        implements MouseOverHandler, MouseOutHandler, LoadHandler,
        ErrorHandler, HasMouseDownHandlers, HasMouseUpHandlers,
        HasMouseMoveHandlers, HasMouseOutHandlers {

    private Image image = new Image();

    private HTML message = new HTML();

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
                image.setVisibleRect(0, pos, width, height);
            }
        }

    }

    public VideoPreviewPanel(String folder, String recordingId,
            String streamId, int width, int height) {
        this.width = width;
        this.height = height;
        this.streamId = streamId;
        image.addLoadHandler(this);
        image.addErrorHandler(this);
        image.setUrl(GWT.getModuleBaseURL() + folder + "/" + recordingId
                + "/preview.do?ssrc=" + streamId
                + "&width=" + width + "&height=" + height);
        setWidth(width + "px");
        setHeight(height + "px");
        add(image);
        image.setVisible(false);
        add(message);
        message.setHTML("Loading...");
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
            message.setVisible(false);
            image.setVisible(true);
            loaded = true;
            this.imageHeight = image.getHeight();
            image.setVisibleRect(0, 0, width, height);
            image.addMouseOverHandler(this);
            image.addMouseOutHandler(this);
        }
    }

    public String getStreamId() {
        return streamId;
    }

    public void stop() {
        if (loaded) {
            timer.cancel();
            image.setVisibleRect(0, 0, width, height);
            pos = 0;
        }
    }

    public void onError(ErrorEvent event) {
        if (!loaded) {
            message.setHTML("Error loading preview image!");
            loaded = true;
        }
    }

    public HandlerRegistration addMouseDownHandler(
            MouseDownHandler handler) {
        return addDomHandler(handler, MouseDownEvent.getType());
    }

    public HandlerRegistration addMouseUpHandler(
            MouseUpHandler handler) {
        return addDomHandler(handler, MouseUpEvent.getType());
    }

    public HandlerRegistration addMouseMoveHandler(
            MouseMoveHandler handler) {
        return addDomHandler(handler, MouseMoveEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(
            MouseOutHandler handler) {
        return addDomHandler(handler, MouseOutEvent.getType());
    }

}
