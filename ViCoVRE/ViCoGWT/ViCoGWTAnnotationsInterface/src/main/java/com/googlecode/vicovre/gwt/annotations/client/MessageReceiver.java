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

import com.google.gwt.core.client.GWT;
import com.google.gwt.json.client.JSONObject;
import com.googlecode.vicovre.gwt.annotations.client.json.JSONAddAnnotationMessage;
import com.googlecode.vicovre.gwt.annotations.client.json.JSONMessage;
import com.googlecode.vicovre.gwt.annotations.client.json.JSONUserMessage;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class MessageReceiver extends AbstractJSONRestCall
        implements MessageResponseHandler {

    private Application application = null;

    private boolean done = false;

    private String url = null;

    public MessageReceiver(Application application) {
        super(true);
        this.application = application;
        this.url = application.getUrl();
    }

    public void start() {
        done = false;
        getNextMessage();
    }

    public void stop() {
        done = true;
    }

    private void getNextMessage() {
        go(url);
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.YES) {
            getNextMessage();
        } else {
            application.close();
        }
    }

    protected void onError(String message) {
        if (!done) {
            String errorMessage = "Error receiving messages: "
                + message + "\n"
                + "Do you want to retry?  If not, the client will stop!";
            MessagePopup error = new MessagePopup(errorMessage,
                    this, MessagePopup.ERROR, MessageResponse.YES,
                    MessageResponse.NO);
            error.center();
        }
    }

    protected void onSuccess(JSONObject object) {
        if (done) {
            return;
        }

        GWT.log("Message = " + object.toString());
        JSONMessage message = JSONMessage.parse(object.toString());
        String type = message.getType();
        if (type.equals(JSONUserMessage.TYPE_ADD)) {
            JSONUserMessage userMessage = message.cast();
            User user = new User(userMessage.getName(),
                    userMessage.getEmail());
            application.addUser(user);
        } else if (type.equals(JSONUserMessage.TYPE_DELETE)) {
            JSONUserMessage userMessage = message.cast();
            application.removeUser(userMessage.getEmail());
        } else if (type.equals(JSONAddAnnotationMessage.TYPE)) {
            JSONAddAnnotationMessage addMessage = message.cast();
            application.addAnnotation(addMessage.getAnnotation());
        }

        if (!type.equals(JSONMessage.TYPE_DONE)) {
            getNextMessage();
        } else {
            application.close();
        }
    }

}
