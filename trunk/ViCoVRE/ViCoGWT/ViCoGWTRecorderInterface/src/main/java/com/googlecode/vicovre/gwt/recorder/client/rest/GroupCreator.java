package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window.Location;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.CreateGroupPopup;
import com.googlecode.vicovre.gwt.recorder.client.PermissionExceptionPanel;

public class GroupCreator extends AbstractVoidRestCall {

    private PermissionExceptionPanel panel = null;

    private CreateGroupPopup popup = null;

    private String url = null;

    public static void create(CreateGroupPopup popup,
            PermissionExceptionPanel panel, String url) {
        GroupCreator creator = new GroupCreator(popup, panel, url);
        creator.go();
    }

    public GroupCreator(CreateGroupPopup popup, PermissionExceptionPanel panel,
            String url) {
        this.popup = popup;
        this.panel = panel;
        this.url = url + "group/";
    }

    public void go() {
        displayWaitMessage("Adding group...", true);
        String goUrl = url + URL.encodePathSegment(popup.getGroup());
        goUrl += "?successUrl=" + URL.encodeQueryString(Location.getHref());
        GWT.log("Adding group with url " + goUrl);
        go(goUrl, Method.PUT);
    }

    protected void onSuccess() {
        popup.hide();
        if (!wasCancelled()) {
            panel.addGroup(popup.getGroup());
        }
    }

    protected void onError(String message) {
        displayError("Error creating user: " + message);
    }

}
