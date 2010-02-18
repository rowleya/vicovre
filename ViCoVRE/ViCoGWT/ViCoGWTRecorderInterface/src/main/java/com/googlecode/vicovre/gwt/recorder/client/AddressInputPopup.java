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
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.client.ModalPopup;

public class AddressInputPopup extends ModalPopup<Grid>
        implements ClickHandler {

    private TextBox address = new TextBox();

    private TextBox port = new TextBox();

    private TextBox ttl = new TextBox();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private MessageResponseHandler handler = null;

    public AddressInputPopup(MessageResponseHandler handler) {
        super(new Grid(4, 2));
        this.handler = handler;

        Grid grid = getWidget();
        grid.setWidget(0, 0, new Label("Address:"));
        grid.setWidget(0, 1, address);
        grid.setWidget(1, 0, new Label("Port:"));
        grid.setWidget(1, 1, port);
        grid.setWidget(2, 0, new Label("TTL:"));
        grid.setWidget(2, 1, ttl);
        grid.setWidget(3, 0, cancel);
        grid.setWidget(3, 1, ok);

        ok.addClickHandler(this);
        cancel.addClickHandler(this);

        grid.setWidth("400px");
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(ok)) {
            super.hide();
            handler.handleResponse(new MessageResponse(MessageResponse.OK,
                    this));
        } else if (event.getSource().equals(cancel)) {
            super.hide();
            handler.handleResponse(new MessageResponse(MessageResponse.CANCEL,
                    this));
        }
    }

    public String getAddress() {
        return address.getText();
    }

    public String getPort() {
        return port.getText();
    }

    public String getTtl() {
        return ttl.getText();
    }

    public void setAddress(String address) {
        this.address.setText(address);
    }

    public void setPort(String port) {
        this.port.setText(port);
    }

    public void setTtl(String ttl) {
        this.ttl.setText(ttl);
    }
}
