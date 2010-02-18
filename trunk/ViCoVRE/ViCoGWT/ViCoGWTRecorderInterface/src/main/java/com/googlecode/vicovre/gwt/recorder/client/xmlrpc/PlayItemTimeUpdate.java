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

package com.googlecode.vicovre.gwt.recorder.client.xmlrpc;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwt.recorder.client.Application;
import com.googlecode.vicovre.gwt.recorder.client.PlayToVenuePopup;

public class PlayItemTimeUpdate extends Timer
        implements AsyncCallback<Integer> {

    private PlayToVenuePopup popup = null;

    private long time = 0;

    private long lastTime = 0;

    private int schedule = 1000;

    private boolean hasFirstTime = false;

    private boolean cancelled = false;

    public static PlayItemTimeUpdate getUpdater(PlayToVenuePopup popup) {
        return new PlayItemTimeUpdate(popup);
    }

    private PlayItemTimeUpdate(PlayToVenuePopup popup) {
        this.popup = popup;
    }

    public void start() {
        cancelled = false;
        hasFirstTime = false;
        time = popup.getTime();
        run();
    }

    public void onFailure(Throwable error) {
        if (!cancelled) {
            schedule(schedule);
        }
    }

    public void onSuccess(Integer time) {
        if (!cancelled) {
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
            schedule(schedule);
        }
    }

    public void run() {
        if (!cancelled) {
            XmlRpcClient client = Application.getXmlRpcClient();
            XmlRpcRequest<Integer> request = new XmlRpcRequest<Integer>(client,
                    "playback.getTime",
                    new Object[]{popup.getId()}, this);
            request.execute();
        }
    }

    public void stop() {
        cancelled = true;
        cancel();
    }
}
