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

import com.google.gwt.user.client.Timer;
import com.googlecode.vicovre.gwt.client.rest.AbstractPlainRestCall;
import com.googlecode.vicovre.gwt.recorder.client.PlayToVenuePopup;

public class PlayItemTimeUpdate extends AbstractPlainRestCall {

    private PlayToVenuePopup popup = null;

    private long time = 0;

    private long lastTime = 0;

    private int schedule = 1000;

    private boolean hasFirstTime = false;

    private boolean cancelled = false;

    private ItemTimer timer = new ItemTimer(this);

    private String url = null;

    private class ItemTimer extends Timer {

        private PlayItemTimeUpdate responseHandler = null;

        public ItemTimer(PlayItemTimeUpdate responseHandler) {
            this.responseHandler = responseHandler;
        }

        public void run() {
            if (!cancelled) {
                responseHandler.go();
            }
        }
    }

    public static PlayItemTimeUpdate getUpdater(PlayToVenuePopup popup,
            String url) {
        return new PlayItemTimeUpdate(popup, url);
    }

    public PlayItemTimeUpdate(PlayToVenuePopup popup, String url) {
        this.popup = popup;
        this.url = url + "play/" + popup.getId() + "/time";
    }

    public void go() {
        go(url);
    }

    public void start() {
        cancelled = false;
        hasFirstTime = false;
        time = popup.getTime();
        timer.run();
    }

    public void stop() {
        cancelled = true;
        timer.cancel();
    }

    protected void onError(String message) {
        if (!cancelled) {
            timer.schedule(schedule);
        }
    }

    protected void onSuccess(String timeString) {
        if (!cancelled) {
            long time = Long.parseLong(timeString);
            if (!hasFirstTime) {
                hasFirstTime = true;
            } else if (time > this.time) {
                long diff = (long) (((double) (time - (this.time)) /
                    (System.currentTimeMillis() - lastTime)) * 1000);
                schedule = (int) diff;
            }
            lastTime = System.currentTimeMillis();
            this.time = time;
            popup.setTime(this.time);
            timer.schedule(schedule);
        }
    }
}
