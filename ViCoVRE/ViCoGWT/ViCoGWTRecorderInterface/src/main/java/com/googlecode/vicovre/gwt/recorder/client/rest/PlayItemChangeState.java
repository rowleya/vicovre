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

package com.googlecode.vicovre.gwt.recorder.client.rest;

import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.PlayToVenuePopup;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;

public class PlayItemChangeState extends AbstractVoidRestCall {

    private static final String STOP = "stop";

    private static final String PAUSE = "pause";

    private static final String RESUME = "resume";

    private String operation = null;

    private PlayToVenuePopup popup = null;

    private String url = null;

    public static void stop(PlayToVenuePopup popup, String url) {
        PlayItemChangeState changer = new PlayItemChangeState(popup, STOP, url);
        changer.go();
    }

    public static void pause(PlayToVenuePopup popup, String url) {
        PlayItemChangeState changer = new PlayItemChangeState(popup, PAUSE,
                url);
        changer.go();
    }

    public static void resume(PlayToVenuePopup popup, String url) {
        PlayItemChangeState changer = new PlayItemChangeState(popup, RESUME,
                url);
        changer.go();
    }

    public PlayItemChangeState(PlayToVenuePopup popup, String operation,
            String url) {
        this.popup = popup;
        this.operation = operation;
        this.url = url + "play/" + popup.getId() + "/" + operation;
    }

    public void go() {
        go(url);
    }

    protected void onError(String message) {
        popup.setStopped();
        MessagePopup errorPopup = new MessagePopup(
                "Error: " + message, null,
                MessagePopup.ERROR, MessageResponse.OK);
        errorPopup.center();
    }

    protected void onSuccess() {
        if (operation.equals(RESUME)) {
            popup.setPlaying();
        } else if (operation.equals(STOP)) {
            popup.setStopped();
            popup.hide();
        } else if (operation.equals(PAUSE)) {
            popup.setPaused();
        }
    }
}
