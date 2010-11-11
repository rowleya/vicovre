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

import java.util.List;

import pl.rmalinowski.gwt2swf.client.ui.SWFWidget;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.LoginPopup;
import com.googlecode.vicovre.gwt.client.Logout;
import com.googlecode.vicovre.gwt.client.json.JSONACL;
import com.googlecode.vicovre.gwt.client.json.JSONGroups;
import com.googlecode.vicovre.gwt.client.json.JSONUsers;

public class Application implements EntryPoint, ClickHandler {

    private Dictionary parameters = Dictionary.getDictionary("Parameters");

    private SWFWidget player = null;

    private Button securityButton = new Button("Set Permissions");

    private Button changePassword = new Button("Change Password");

    private Button login = new Button("Login");

    private Button logout = new Button("Logout");

    private Button makePublic = new Button("Make Public");

    private Button makePrivate = new Button("Make Private");

    protected String getBaseUrl() {
        String url = GWT.getHostPageBaseURL();
        String recording = getFolder() + "/" + getRecordingId() + "/";
        if (url.endsWith(recording)) {
            url = url.substring(0, url.length() - recording.length() + 1);
        }
        return url;
    }

    protected String getUrl() {
        String url = getBaseUrl();
        String paramUrl = parameters.get("url");
        if (paramUrl.startsWith("/")) {
            paramUrl = paramUrl.substring(1);
        }
        if (!paramUrl.endsWith("/")) {
            paramUrl += "/";
        }
        return url + paramUrl;
    }

    protected String getPlayUrl() {
        String url = getBaseUrl();
        String playUrl = parameters.get("playUrl");
        if (playUrl.startsWith("/")) {
            playUrl = playUrl.substring(1);
        }
        return url + playUrl;
    }

    protected String getRecordingId() {
        String id = parameters.get("recording");
        return id;
    }

    protected String getFolder() {
        String folder = parameters.get("folder");
        if (folder.endsWith("/")) {
            folder = folder.substring(1);
        }
        return folder;
    }

    protected boolean canEdit() {
        return parameters.get("canEdit").equals("true");
    }

    protected boolean canPlay() {
        return parameters.get("canPlay").equals("true");
    }

    protected String getRole() {
        return parameters.get("role");
    }

    protected JsArrayString getUsers() {
        JSONUsers users = JSONUsers.parse(parameters.get("users"));
        return users.getUsers();
    }

    protected JsArrayString getGroups() {
        JSONGroups groups = JSONGroups.parse(parameters.get("groups"));
        return groups.getGroups();
    }

    protected JSONACL getAcl() {
        return JSONACL.parse(parameters.get("acl"));
    }

    protected JSONACL getReadAcl() {
        return JSONACL.parse(parameters.get("readAcl"));
    }

    public void onModuleLoad() {
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        mainPanel.setHeight("100%");
        RootPanel.get().add(mainPanel);

        String url = getUrl();
        String playUrl = getPlayUrl();
        String recordingId = getRecordingId();
        String folder = getFolder();
        boolean canEdit = canEdit();
        boolean canPlay = canPlay();
        String role = getRole();

        if (canPlay) {
            player = new SWFWidget(getBaseUrl() + "Player.swf");
            String urlToPlay = playUrl + "?folder=" + folder + "%26recordingId="
                + recordingId + "%26startTime=0";
            GWT.log("Playing " + urlToPlay);
            player.addParam("wmode", "opaque");
            player.addFlashVar("uri", urlToPlay);
            player.setWidth("100%");
            player.setHeight("100%");
            mainPanel.add(player);

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setHorizontalAlignment(
                    HorizontalPanel.ALIGN_CENTER);
            buttonPanel.setWidth("100%");
            mainPanel.add(buttonPanel);
            mainPanel.setCellHeight(buttonPanel, "20px");
            mainPanel.setCellHeight(player, "100%");

            if (role.equals("User")) {
                buttonPanel.add(login);
                login.addClickHandler(this);
            } else {
                buttonPanel.add(changePassword);
                buttonPanel.add(logout);
                changePassword.addClickHandler(this);
                logout.addClickHandler(this);
            }

            if (canEdit) {
                securityButton.setTitle("Change who can play this Recording");
                makePublic.setTitle("Allow anyone to play this Recording");
                makePrivate.setTitle("Stop anyone from playing this Recording");

                buttonPanel.add(makePublic);
                buttonPanel.add(makePrivate);
                buttonPanel.add(securityButton);
                securityButton.addClickHandler(this);
                makePublic.addClickHandler(this);
                makePrivate.addClickHandler(this);
            }

        } else if (role.equals("User")) {
            LoginPopup popup = new LoginPopup(getBaseUrl(), url);
            popup.center();
        } else {
            RequestAccessPopup popup = new RequestAccessPopup(getBaseUrl(),
                    url, folder, recordingId);
            popup.center();
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == makePublic) {
            PermissionSetter.setPermissions(getBaseUrl(), getUrl(), getFolder(),
                    getRecordingId(), new String[]{"play", "read"}, null,
                    new boolean[]{true, true}, new List[]{null, null},
                    new List[]{null, null});
        } else if (event.getSource() == makePrivate) {
            PermissionSetter.setPermissions(getBaseUrl(), getUrl(), getFolder(),
                    getRecordingId(), new String[]{"play", "read"}, null,
                    new boolean[]{false, false}, new List[]{null, null},
                    new List[]{null, null});
        } else if (event.getSource() == securityButton) {
            PermissionPopup popup = new PermissionPopup(getBaseUrl(), getUrl(),
                    getFolder(), getRecordingId(), getUsers(), getGroups(),
                    getAcl(), getReadAcl());
            popup.center();
        } else if (event.getSource() == login) {
            LoginPopup popup = new LoginPopup(getBaseUrl(), getUrl());
            popup.center();
        } else if (event.getSource() == changePassword) {
            ChangePasswordPopup popup = new ChangePasswordPopup(getBaseUrl(),
                    getUrl());
            popup.center();
        } else if (event.getSource() == logout) {
            Logout.logout(getBaseUrl(), getUrl());
        }
    }
}

