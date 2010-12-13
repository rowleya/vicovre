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

package com.googlecode.vicovre.gwt.download.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.download.client.rest.LayoutCreator;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class LayoutNamePopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private static final String PATTERN = "[A-Za-z][A-Za-z0-9]*";

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private TextBox nameBox = new TextBox();

    private MessageResponseHandler handler = null;

    private String url = null;

    private Layout layout = null;

    MessagePopup errorPopup = new MessagePopup(null, null,
            MessagePopup.ERROR, MessageResponse.OK);

    public LayoutNamePopup(String url, Layout layout,
            MessageResponseHandler handler, String name) {
        super(new VerticalPanel());
        this.handler = handler;
        this.nameBox.setText(name);
        this.url = url;
        this.layout = layout;

        VerticalPanel panel = getWidget();
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        panel.setWidth("300px");
        panel.setHeight("90px");

        nameBox.setWidth("100%");
        panel.add(new Label("Enter a name for the layout:"));
        panel.add(nameBox);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setWidth("100%");
        buttons.add(ok);
        buttons.add(cancel);
        buttons.setCellHorizontalAlignment(ok, HorizontalPanel.ALIGN_LEFT);
        buttons.setCellHorizontalAlignment(cancel, HorizontalPanel.ALIGN_RIGHT);
        panel.add(buttons);

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == ok) {
            String nameEntered = nameBox.getText();
            String error = null;
            if (nameEntered.equals("")) {
                error = "Name cannot be blank";
            } else if (!nameEntered.matches(PATTERN)) {
                error = "The name must start with a letter"
                    + " and can only contain letters and numbers";
            }
            if (error != null) {
                errorPopup.setMessage(error);
                errorPopup.center();
            } else {
                layout.setName(nameEntered);
                LayoutCreator.create(layout, this, url);
            }
        } else {
            handler.handleResponse(new MessageResponse(MessageResponse.CANCEL,
                    this));
            hide();
        }
    }

    public String getName() {
        return nameBox.getText();
    }

    public void addSuccessful() {
        handler.handleResponse(new MessageResponse(MessageResponse.OK, this));
        hide();
    }
}
