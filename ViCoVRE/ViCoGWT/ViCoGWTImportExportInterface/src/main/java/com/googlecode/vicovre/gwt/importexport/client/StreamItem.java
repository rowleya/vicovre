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

package com.googlecode.vicovre.gwt.importexport.client;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Label;

public class StreamItem extends Label implements HasDoubleClickHandlers {

    private String sessionId = null;

    private Stream stream = null;

    private SubStream substream = null;

    private String url = null;

    private StreamPanel panel = null;

    public StreamItem(String sessionId, Stream stream, String url) {
        super("Stream " + stream.getId());
        this.sessionId = sessionId;
        this.stream = stream;
        this.url = url;
    }

    public StreamItem(String sessionId, SubStream substream, String url) {
        super("Substream " + substream.getIdString()
                + " " + substream.getType());
        this.sessionId = sessionId;
        this.substream = substream;
        this.stream = substream.getStream();
        this.url = url;
    }

    public Stream getStream() {
        return stream;
    }

    public SubStream getSubStream() {
        return substream;
    }

    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler
            handler) {
        return addDomHandler(handler, DoubleClickEvent.getType());
    }

    public StreamPanel getPanel() {
        if (panel == null) {
            panel = new StreamPanel(sessionId, stream, substream, url);
        }
        return panel;
    }


}
