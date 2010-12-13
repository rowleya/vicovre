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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.RootPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.json.JSONGroups;
import com.googlecode.vicovre.gwt.client.json.JSONLayout;
import com.googlecode.vicovre.gwt.client.json.JSONLayouts;
import com.googlecode.vicovre.gwt.client.json.JSONUsers;
import com.googlecode.vicovre.gwt.recorder.client.rest.CurrentUserLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.FolderLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.VenueServerLoader;

public class Application implements EntryPoint {

    private static Dictionary parameters = null;

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

    protected Layout[] getLayouts(String parameter) {
        String layoutsJSON = parameters.get(parameter);
        if (layoutsJSON != null && !layoutsJSON.equals("null")) {
            JSONLayouts jsonLayouts = JSONLayouts.parse(layoutsJSON);
            JsArray<JSONLayout> layoutArray = jsonLayouts.getLayouts();
            Layout[] layouts = new Layout[layoutArray.length()];
            for (int i = 0; i < layoutArray.length(); i++) {
                layouts[i] = new Layout(layoutArray.get(i));
            }
            return layouts;
        }
        return new Layout[0];
    }

    protected JsArrayString getUsers() {
        String usersJSON = parameters.get("users");
        if ((usersJSON != null) && !usersJSON.equals("null")
                && !usersJSON.equals("")) {
            JSONUsers users = JSONUsers.parse(usersJSON);
            return users.getUsers();
        }
        return null;
    }

    protected JsArrayString getGroups() {
        String groupsJSON = parameters.get("groups");
        if ((groupsJSON != null) && !groupsJSON.equals("null")
                && !groupsJSON.equals("")) {
            JSONGroups groups = JSONGroups.parse(groupsJSON);
            return groups.getGroups();
        }
        return null;
    }

    public static String getParam(String name) {
        return parameters.get(name);
    }

    public void onModuleLoad() {
        parameters = Dictionary.getDictionary("Parameters");

        DockPanel topPanel = new DockPanel();
        topPanel.setWidth("100%");
        topPanel.setHeight("100%");
        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        topPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        String restUrl = getUrl();
        Layout[] layouts = getLayouts("layouts");
        Layout[] customLayouts = getLayouts("customLayouts");
        JsArrayString users = getUsers();
        JsArrayString groups = getGroups();

        FolderPanel panel = new FolderPanel(restUrl, layouts, customLayouts,
                users, groups);
        StatusPanel status = new StatusPanel(restUrl, panel);
        status.setWidth("95%");

        topPanel.add(status, DockPanel.NORTH);
        topPanel.add(panel, DockPanel.CENTER);
        topPanel.setCellHeight(status, "50px");
        RootPanel.get().add(topPanel);

        ActionLoader loader = new ActionLoader(null, 3,
                "Loading Application...",
                "There was an error loading the application.\n"
                + "Please refresh the page to try again.",
                false, true);
        CurrentUserLoader.load(status, loader, restUrl);
        VenueServerLoader.load(loader, restUrl);
        FolderLoader.load(panel, loader, restUrl);
    }
}
