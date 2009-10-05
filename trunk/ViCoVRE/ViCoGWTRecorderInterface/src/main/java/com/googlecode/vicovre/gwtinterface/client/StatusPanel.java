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

package com.googlecode.vicovre.gwtinterface.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

public class StatusPanel extends HorizontalPanel implements ClickHandler {

    private static final String LOGGED_OUT = "You are not logged in";

    private static final String LOGGED_IN = " logged in";

    private static final String ERROR = "Invalid Login Credentials";

    private static final String LOG_IN = "Log in";

    private static final String LOG_OUT = "Log out";

    private static final String RETRY = "Retry";

    private Label status = new Label(LOGGED_OUT);

    private HorizontalPanel loginPanel = new HorizontalPanel();

    private TextBox username = new TextBox();

    private PasswordTextBox password = new PasswordTextBox();

    private Button loginButton = new Button(LOG_IN);

    private boolean loggedIn = false;

    private boolean loginError = false;

    private WaitPopup loginPopup = new WaitPopup("Logging in...", true);

    public StatusPanel() {
        add(status);
        setCellVerticalAlignment(status, ALIGN_MIDDLE);
        DOM.setStyleAttribute(status.getElement(), "color", "black");

        Label usernameLabel = new Label("Username:  ");
        loginPanel.add(usernameLabel);
        loginPanel.add(username);
        loginPanel.add(new Label("   "));
        Label passwordLabel = new Label("Password:  ");
        loginPanel.add(passwordLabel);
        loginPanel.add(password);
        loginPanel.setCellVerticalAlignment(usernameLabel, ALIGN_MIDDLE);
        loginPanel.setCellVerticalAlignment(passwordLabel, ALIGN_MIDDLE);

        add(loginPanel);
        setCellHorizontalAlignment(loginPanel, ALIGN_RIGHT);
        setCellVerticalAlignment(loginPanel, ALIGN_MIDDLE);

        add(loginButton);
        setCellHorizontalAlignment(loginButton, ALIGN_RIGHT);
        loginButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == loginButton) {
            if (loggedIn) {
                loggedIn = false;
                loginPanel.setVisible(true);
                loginButton.setText(LOG_IN);
                status.setText(LOGGED_OUT);
                DOM.setStyleAttribute(status.getElement(), "color", "black");
            } else if (loginError) {
                loginError = false;
                loginPanel.setVisible(true);
                loginButton.setText(LOG_IN);
                status.setText(LOGGED_OUT);
                DOM.setStyleAttribute(status.getElement(), "color", "black");
            } else {
                loginPopup.show();
                loggedIn = !username.getText().trim().equals("");
                if (!loggedIn) {
                    loginError = true;
                    loginPanel.setVisible(false);
                    loginButton.setText(RETRY);
                    status.setText(ERROR);
                    DOM.setStyleAttribute(status.getElement(), "color", "red");
                } else {
                    loginError = false;
                    loginPanel.setVisible(false);
                    loginButton.setText(LOG_OUT);
                    status.setText(username.getText() + LOGGED_IN);
                    DOM.setStyleAttribute(status.getElement(), "color",
                            "black");
                }
            }
        }
    }


}
