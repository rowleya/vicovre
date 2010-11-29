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
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.recorder.client.rest.PasswordSetter;

public class ChangePasswordPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler, KeyPressHandler {

    private PasswordTextBox oldPassword = new PasswordTextBox();

    private PasswordTextBox password = new PasswordTextBox();

    private PasswordTextBox passwordAgain = new PasswordTextBox();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private String url = null;

    public ChangePasswordPopup(String url) {
        super(new VerticalPanel());
        this.url = url;

        VerticalPanel panel = getWidget();
        panel.setWidth("400px");
        panel.setHeight("120px");

        Grid grid = new Grid(3, 2);
        grid.setWidth("100%");
        grid.setWidget(0, 0, new Label("Old Password:"));
        grid.setWidget(0, 1, oldPassword);
        grid.setWidget(1, 0, new Label("New Password:"));
        grid.setWidget(1, 1, password);
        grid.setWidget(2, 0, new Label("New Password Again:"));
        grid.setWidget(2, 1, passwordAgain);
        oldPassword.setWidth("100%");
        password.setWidth("100%");
        passwordAgain.setWidth("100%");
        grid.getColumnFormatter().setWidth(0, "130px");

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(cancel);
        buttonPanel.add(ok);
        buttonPanel.setCellHorizontalAlignment(cancel,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(ok,
                HorizontalPanel.ALIGN_RIGHT);

        panel.add(grid);
        panel.add(buttonPanel);

        cancel.addClickHandler(this);
        ok.addClickHandler(this);

        oldPassword.addKeyPressHandler(this);
        password.addKeyPressHandler(this);
        passwordAgain.addKeyPressHandler(this);
    }

    public void center() {
        super.center();
        oldPassword.setFocus(true);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == ok) {
            if (password.getText().equals(passwordAgain.getText())) {
                PasswordSetter.setPassword(url, this,
                        oldPassword.getText(), password.getText());
            } else {
                MessagePopup popup = new MessagePopup(
                        "The passwords do not match!", null,
                        MessagePopup.ERROR, MessageResponse.OK);
                popup.center();
            }
        } else if (event.getSource() == cancel) {
            hide();
        }
    }

    public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == 13) {
            if (event.getSource() == oldPassword) {
                password.setFocus(true);
            } else if (event.getSource() == password) {
                passwordAgain.setFocus(true);
            } else if (event.getSource() == passwordAgain) {
                ok.click();
            }
        }
    }

}
