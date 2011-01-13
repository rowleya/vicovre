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

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class PlayItemMover extends AbstractVoidRestCall
        implements MessageResponseHandler {

    private PlayItem item = null;

    private VerticalPanel parent = null;

    private int position = 0;

    private String url = null;

    private String newFolder = null;

    public static void moveRecording(PlayItem item, String url,
            String newFolder) {
        PlayItemMover deleter = new PlayItemMover(item, url, newFolder);
        deleter.go();
    }

    public PlayItemMover(PlayItem item, String url, String newFolder) {
        this.item = item;
        this.url = url + "recording" + item.getFolder();
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += item.getId() + "/move?newFolder="
            + URL.encodeQueryString(newFolder);
        this.newFolder = newFolder;
        parent = (VerticalPanel) item.getParent();
        position = parent.getWidgetIndex(item);
    }

    public void go() {
        MessagePopup message = new MessagePopup(
            "Are you sure that you would like to move this recording to "
                + newFolder + "?",
            this, MessagePopup.QUESTION,
            MessageResponse.YES, MessageResponse.NO);
        message.center();
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.YES) {
            item.removeFromParent();
            go(url, Method.POST);
        }
    }

    protected void onError(String message) {
        parent.insert(item, position);
        MessagePopup popup = new MessagePopup(
                "Error moving item: " + message, null,
                MessagePopup.ERROR, MessageResponse.OK);
        popup.center();
    }

    protected void onSuccess() {
        // Do Nothing
    }


}
