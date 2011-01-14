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

package com.googlecode.vicovre.gwt.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.layout.LayoutPosition;
import com.googlecode.vicovre.gwt.client.layoutcreator.LayoutNamePopup;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class LayoutCreator extends AbstractVoidRestCall {

    private Layout layout = null;

    private LayoutNamePopup popup = null;

    private String url = null;

    private WaitPopup waitPopup = new WaitPopup("Adding Layout", true);

    public static final void create(Layout layout, LayoutNamePopup popup,
            String url) {
        LayoutCreator creator = new LayoutCreator(layout, popup, url);
        creator.go();
    }

    public LayoutCreator(Layout layout, LayoutNamePopup popup, String url) {
        this.layout = layout;
        this.popup = popup;
        this.url = url + "layout/custom/"
        + URL.encodeQueryString(layout.getName());
    }

    public void go() {
        waitPopup.show();
        String layoutUrl = url + "?";
        boolean first = true;
        for (LayoutPosition position : layout.getPositions()) {
            if (first) {
                first = false;
            } else {
                layoutUrl += "&";
            }
            String name = URL.encodeQueryString(position.getName());
            layoutUrl += "position=" + name;
            layoutUrl += "&" + name + "X=" + position.getX();
            layoutUrl += "&" + name + "Y=" + position.getY();
            layoutUrl += "&" + name + "Width=" + position.getWidth();
            layoutUrl += "&" + name + "Height=" + position.getHeight();
            layoutUrl += "&" + name + "Assignable=" + position.isAssignable();
            layoutUrl += "&" + name + "HasChanges=" + position.hasChanges();
            layoutUrl += "&" + name + "HasAudio=" + position.hasAudio();
            layoutUrl += "&" + name + "Opacity=" + position.getOpacity();
        }
        go(layoutUrl, Method.PUT);
    }

    protected void onError(String message) {
        waitPopup.hide();
        MessagePopup error = new MessagePopup("Error adding layout: " + message,
                null, MessagePopup.ERROR, MessageResponse.OK);
        error.center();
    }

    protected void onSuccess() {
        if (!waitPopup.wasCancelled()) {
            waitPopup.hide();
            popup.addSuccessful();
        }
    }

}