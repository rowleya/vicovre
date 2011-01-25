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
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.googlecode.vicovre.gwt.client.json.JSONUser;
import com.googlecode.vicovre.gwt.recorder.client.rest.Login;
import com.googlecode.vicovre.gwt.recorder.client.rest.Logout;
import com.googlecode.vicovre.gwt.recorder.client.rest.PasswordResetter;
import com.googlecode.vicovre.gwt.recorder.client.rest.UserLoader;
import com.googlecode.vicovre.gwt.utils.client.Space;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class StatusPanel extends DockPanel implements ClickHandler,
        KeyPressHandler {

    private static final String LOGGED_OUT = "You are not logged in";

    private static final String LOGGED_IN = " logged in";

    private static final String ERROR = "Invalid Login Credentials";

    private static final String LOG_IN = "Log in";

    private static final String LOG_OUT = "Log out";

    private static final String RETRY = "Retry";

    private Label status = new Label(LOGGED_OUT);

    private HorizontalPanel loginPanel = new HorizontalPanel();

    private HorizontalPanel logoutPanel = new HorizontalPanel();

    private HorizontalPanel retryPanel = new HorizontalPanel();

    private TextBox username = new TextBox();

    private String role = null;

    private PasswordTextBox password = new PasswordTextBox();

    private Button loginButton = new Button(LOG_IN);

    private Button logoutButton = new Button(LOG_OUT);

    private Button retryButton = new Button(RETRY);

    private Button changePasswordButton = new Button("Change Password");

    private Button editUsersButton = new Button("Edit Users");

    private Button resetPasswordButton = new Button("Reset Password");

    private WaitPopup loginPopup = new WaitPopup("Logging in...", true);

    private String url = null;

    private FolderPanel folderPanel = null;

    public StatusPanel(String url, FolderPanel folderPanel) {
        this.url = url;
        this.folderPanel = folderPanel;
        add(status, WEST);
        setCellVerticalAlignment(status, ALIGN_MIDDLE);
        DOM.setStyleAttribute(status.getElement(), "color", "black");

        loginPanel.setVerticalAlignment(ALIGN_MIDDLE);
        Label usernameLabel = new Label("Email Address: ");
        loginPanel.add(usernameLabel);
        loginPanel.add(username);
        Label passwordLabel = new Label("Password: ");
        loginPanel.add(passwordLabel);
        loginPanel.add(password);
        loginPanel.add(loginButton);
        loginPanel.add(resetPasswordButton);
        username.addKeyPressHandler(this);
        password.addKeyPressHandler(this);
        loginButton.addClickHandler(this);
        resetPasswordButton.addClickHandler(this);

        logoutPanel.setVerticalAlignment(ALIGN_MIDDLE);
        logoutPanel.add(editUsersButton);
        logoutPanel.add(Space.getHorizontalSpace(5));
        logoutPanel.add(changePasswordButton);
        logoutPanel.add(Space.getHorizontalSpace(5));
        logoutPanel.add(logoutButton);
        editUsersButton.addClickHandler(this);
        changePasswordButton.addClickHandler(this);
        logoutButton.addClickHandler(this);

        retryPanel.setVerticalAlignment(ALIGN_MIDDLE);
        retryPanel.add(retryButton);
        retryButton.addClickHandler(this);

        add(loginPanel, EAST);
        add(logoutPanel, EAST);
        add(retryPanel, EAST);
        setCellHorizontalAlignment(loginPanel, ALIGN_RIGHT);
        setCellHorizontalAlignment(logoutPanel, ALIGN_RIGHT);
        setCellHorizontalAlignment(retryPanel, ALIGN_RIGHT);

        loginPanel.setVisible(true);
        logoutPanel.setVisible(false);
        retryPanel.setVisible(false);

    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == loginButton) {
            loginPopup.show();
            Login.login(this, url, username.getText(), password.getText());
        } else if (event.getSource() == logoutButton) {
            Logout.logout(this, url);
        } else if (event.getSource() == retryButton) {
            loginPanel.setVisible(true);
            logoutPanel.setVisible(false);
            retryPanel.setVisible(false);
            status.setText(LOGGED_OUT);
            DOM.setStyleAttribute(status.getElement(), "color", "black");
        } else if (event.getSource() == changePasswordButton) {
            ChangePasswordPopup popup = new ChangePasswordPopup(url);
            popup.center();
        } else if (event.getSource() == editUsersButton) {
            UserLoader.load(url);
        } else if (event.getSource() == resetPasswordButton) {
            PasswordResetter.reset(url, username.getText());
        }
    }

    public void setLogin(String username, String role) {
        loginPanel.setVisible(false);
        logoutPanel.setVisible(true);
        retryPanel.setVisible(false);
        status.setText(username + LOGGED_IN);
        this.role = role;
        if (role.equals(JSONUser.ROLE_ADMINISTRATOR)
                || role.equals(JSONUser.ROLE_WRITER)) {
            folderPanel.setUserIsWriter(true);
        } else {
            folderPanel.setUserIsWriter(false);
        }
        if (role.equals(JSONUser.ROLE_ADMINISTRATOR)) {
            folderPanel.setUserIsAdministrator(true);
            editUsersButton.setVisible(true);
        } else {
            folderPanel.setUserIsAdministrator(false);
            editUsersButton.setVisible(false);
        }
        folderPanel.setUsername(username);
        DOM.setStyleAttribute(status.getElement(), "color",
                "black");
    }

    public void loginSuccessful(String username, String role) {
        loginPopup.hide();
        setLogin(username, role);
        folderPanel.reload();
    }

    public void loginFailed(String error) {
        loginPopup.hide();
        String message = error;
        if (message == null) {
            message = ERROR;
        }
        loginPanel.setVisible(false);
        logoutPanel.setVisible(false);
        retryPanel.setVisible(true);
        status.setText(message);
        DOM.setStyleAttribute(status.getElement(), "color", "red");
    }

    public void loggedOut() {
        loginPanel.setVisible(true);
        logoutPanel.setVisible(false);
        retryPanel.setVisible(false);
        loginButton.setText(LOG_IN);
        this.role = null;
        DOM.setStyleAttribute(status.getElement(), "color", "black");
        folderPanel.setUserIsWriter(false);
        folderPanel.setUserIsAdministrator(false);
        folderPanel.setUsername(null);
        folderPanel.reload();
    }

    public String getRole() {
        return role;
    }

    public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == KeyCodes.KEY_ENTER) {
            if (event.getSource() == username) {
                password.setFocus(true);
            } else if (event.getSource() == password) {
                loginPopup.show();
                Login.login(this, url, username.getText(), password.getText());
            }
        }
    }
}
