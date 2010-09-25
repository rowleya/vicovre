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

import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Response;

import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;
import com.googlecode.vicovre.gwt.recorder.client.PlayItem;

public class PlayItemEditor extends AbstractRestCall {

    private String url = null;

    private PlayItem item = null;

    public static void editPlayItem(PlayItem item, String url) {
        PlayItemEditor editor = new PlayItemEditor(item, url);
        editor.go();
    }

    public PlayItemEditor(PlayItem item, String url) {
        this.item = item;
        this.url = url + "recording" + item.getFolder();
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
        this.url += item.getId();
    }

    public void go() {
        String itemUrl = url + "?" + item.getDetailsAsUrl();
        go(itemUrl, Method.PUT);
    }

    protected void onError(String message) {
        MessagePopup popup = new MessagePopup(
                "Error editing item: " + message, null,
                MessagePopup.ERROR, MessageResponse.OK);
        popup.center();
    }

    protected void onSuccess(Response response) {
        // Do Nothing
    }



}
