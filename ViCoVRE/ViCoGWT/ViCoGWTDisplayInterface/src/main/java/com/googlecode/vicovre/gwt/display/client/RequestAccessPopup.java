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

package com.googlecode.vicovre.gwt.display.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.ModalPopup;

public class RequestAccessPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private TextBox emailBox = new TextBox();

    private Button emailButton = new Button("Request Access");

    private String url = null;

    private String baseUrl = null;

    private String folder = null;

    private String recordingId = null;

    public RequestAccessPopup(String baseUrl, String url, String folder,
            String recordingId) {
        super(new VerticalPanel());
        this.baseUrl = baseUrl;
        this.url = url;
        this.folder = folder;
        this.recordingId = recordingId;

        VerticalPanel panel = getWidget();
        panel.setWidth("400px");
        panel.setHeight("100px");

        panel.add(new Label("You do not have access to this recording."));
        panel.add(new Label(
                "To request access, please enter your e-mail address below:"));
        panel.add(emailBox);
        panel.add(emailButton);

        emailBox.setWidth("100%");
        emailButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        AccessRequester.requestAccess(baseUrl, url, folder, recordingId,
                emailBox.getText());
    }

}
