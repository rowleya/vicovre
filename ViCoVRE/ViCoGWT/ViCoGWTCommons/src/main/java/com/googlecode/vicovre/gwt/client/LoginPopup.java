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

package com.googlecode.vicovre.gwt.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class LoginPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler, KeyPressHandler {

    private TextBox username = new TextBox();

    private PasswordTextBox password = new PasswordTextBox();

    private Button ok = new Button("OK");

    private Button create = new Button("Register");

    private Button cancel = new Button("Cancel");

    private String url = null;

    public LoginPopup(String url) {
        super(new VerticalPanel());
        this.url = url;

        VerticalPanel panel = getWidget();
        panel.setWidth("400px");
        panel.setHeight("120px");

        panel.add(new Label("You must be logged in to access this recording."));
        panel.add(new Label(
                "Please login or register by entering your details below."));

        Grid grid = new Grid(2, 2);
        grid.setWidth("100%");
        grid.setWidget(0, 0, new Label("Email Address:"));
        grid.setWidget(0, 1, username);
        grid.setWidget(1, 0, new Label("Password:"));
        grid.setWidget(1, 1, password);
        username.setWidth("100%");
        password.setWidth("100%");
        grid.getColumnFormatter().setWidth(0, "120px");

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(cancel);
        buttonPanel.add(create);
        buttonPanel.add(ok);
        buttonPanel.setCellHorizontalAlignment(cancel,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(create,
                HorizontalPanel.ALIGN_CENTER);
        buttonPanel.setCellHorizontalAlignment(ok,
                HorizontalPanel.ALIGN_RIGHT);

        panel.add(grid);
        panel.add(buttonPanel);

        cancel.addClickHandler(this);
        create.addClickHandler(this);
        ok.addClickHandler(this);

        username.addKeyPressHandler(this);
        password.addKeyPressHandler(this);
    }

    public void center() {
        super.center();
        username.setFocus(true);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == ok) {
            Login.login(url, username.getText(), password.getText());
        } else if (event.getSource() == cancel) {
            History.back();
        } else if (event.getSource() == create) {
            Registerer.register(url, username.getText(),
                    password.getText(), Location.getHref());
        }
    }

    public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == 13) {
            if (event.getSource() == username) {
                password.setFocus(true);
            } else if (event.getSource() == password) {
                ok.click();
            }
        }
    }

}
