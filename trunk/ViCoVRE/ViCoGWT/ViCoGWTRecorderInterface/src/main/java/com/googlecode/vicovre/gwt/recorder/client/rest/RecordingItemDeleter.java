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

import org.restlet.client.data.Method;

import com.google.gwt.core.client.GWT;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.RecordingItem;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class RecordingItemDeleter extends AbstractVoidRestCall
        implements MessageResponseHandler {

    private RecordingItem item = null;

    private String url = null;

    public static void deleteRecording(RecordingItem item, String url) {
        RecordingItemDeleter deleter = new RecordingItemDeleter(item, url);
        deleter.go();
    }

    private RecordingItemDeleter(RecordingItem item, String url) {
        this.item = item;
        this.url = url + "record" + item.getFolder();
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += item.getId();
    }

    public void go() {
        MessagePopup message = new MessagePopup(
            "Are you sure that you would like to delete this recording?",
            this, MessagePopup.QUESTION,
            MessageResponse.YES, MessageResponse.NO);
        message.center();
    }

    protected void onError(String message) {
        item.setStatus("Error: Deletion failed: " + message);
        GWT.log("Error deleting item: " + message);
    }

    protected void onSuccess() {
        item.removeFromParent();
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.YES) {
            item.setCreated(false);
            item.setStatus("Deleting...");
            GWT.log("Deleting from " + url);
            go(url, Method.DELETE);
        }
    }

}
