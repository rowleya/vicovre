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

package com.googlecode.vicovre.gwt.recorder.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class EditMetadataNamePopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private boolean multiline = false;

    private TextBox name = new TextBox();

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private MessageResponseHandler handler = null;

    public EditMetadataNamePopup(boolean multiline,
            MessageResponseHandler handler) {
        super(new VerticalPanel());
        this.multiline = multiline;
        this.handler = handler;

        VerticalPanel panel = getWidget();
        panel.setWidth("300px");
        panel.setHeight("55px");

        HorizontalPanel entryPanel = new HorizontalPanel();
        entryPanel.setWidth("100%");
        Label entryLabel = new Label("Item Name:");
        entryPanel.add(entryLabel);
        entryPanel.add(name);
        entryPanel.setCellWidth(entryLabel, "100px");
        name.setWidth("100%");

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        buttonPanel.setCellHorizontalAlignment(cancelButton,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(okButton,
                HorizontalPanel.ALIGN_RIGHT);

        panel.add(entryPanel);
        panel.add(buttonPanel);
    }

    public String getName() {
        return name.getText();
    }

    public boolean isMultiline() {
        return multiline;
    }

    public void onClick(ClickEvent event) {
        hide();
        Object source = event.getSource();
        if (source == okButton) {
            handler.handleResponse(new MessageResponse(MessageResponse.OK,
                    this));
        } else if (source == cancelButton) {
            handler.handleResponse(new MessageResponse(MessageResponse.CANCEL,
                    this));
        }
    }
}
