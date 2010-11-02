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

package com.googlecode.vicovre.gwt.annotations.client;

import org.restlet.gwt.data.MediaType;
import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Response;

import com.google.gwt.core.client.GWT;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;

public class TimeReceiver extends AbstractRestCall {

    private Application application = null;

    private LiveAnnotationType type = null;

    public static void getTime(Application application,
            LiveAnnotationType type) {
        TimeReceiver receiver = new TimeReceiver(application, type);
        receiver.go();
    }

    public TimeReceiver(Application application, LiveAnnotationType type) {
        this.application = application;
        this.type = type;
    }

    public void go() {
        application.clearPanel();
        go(application.getUrl() + "date", Method.GET, MediaType.TEXT_PLAIN);
    }

    protected void onError(String message) {
        application.displayButtonPanel();
        String errorMessage = "Error receiving time: "
            + message;
        MessagePopup error = new MessagePopup(errorMessage,
                null, MessagePopup.ERROR, MessageResponse.OK);
        error.center();
    }

    protected void onSuccess(Response response) {
        String timestamp = response.getEntity().getText();
        GWT.log("Timestamp = " + timestamp, null);
        type.setTimestamp(timestamp);
        application.displayAnnotationPanel(type);
    }

}
