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

import pl.rmalinowski.gwt2swf.client.ui.SWFWidget;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.auth.LoginPopup;
import com.googlecode.vicovre.gwt.client.json.JSONACL;
import com.googlecode.vicovre.gwt.client.json.JSONGroups;
import com.googlecode.vicovre.gwt.client.json.JSONUsers;

public class Application implements EntryPoint {

    private Dictionary parameters = Dictionary.getDictionary("Parameters");

    private SWFWidget player = null;

    protected String getUrl() {
        String url = GWT.getModuleBaseURL();
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
        String url = GWT.getModuleBaseURL();
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

    protected boolean canPlay() {
        return parameters.get("canPlay").equals("true");
    }

    protected String getRole() {
        return parameters.get("role");
    }

    protected String getStartTime() {
        String startTime = parameters.get("startTime");
        if (startTime != null) {
            return startTime;
        }
        return "0";
    }

    protected String getAGC() {
        String agc = parameters.get("agc");
        if (agc != null) {
            return agc;
        }
        return "false";
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
        boolean canPlay = canPlay();
        String role = getRole();
        String startTime = getStartTime();
        String agc = getAGC();

        if (canPlay) {
            player = new SWFWidget(GWT.getModuleBaseURL() + "Player.swf");
            String urlToPlay = playUrl + "?folder=" + folder + "%26recordingId="
                + recordingId + "%26startTime=" + startTime + "%26agc=" + agc;
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

        } else if (role.equals("User")) {
            LoginPopup popup = new LoginPopup(url);
            popup.center();
        } else {
            RequestAccessPopup popup = new RequestAccessPopup(
                    url, folder, recordingId);
            popup.center();
        }
    }
}

