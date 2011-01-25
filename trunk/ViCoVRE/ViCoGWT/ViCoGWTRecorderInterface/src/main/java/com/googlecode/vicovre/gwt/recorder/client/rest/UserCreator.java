package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window.Location;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.CreateUserPopup;
import com.googlecode.vicovre.gwt.recorder.client.PermissionExceptionPanel;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;

public class UserCreator extends AbstractVoidRestCall {

    private PermissionExceptionPanel panel = null;

    private CreateUserPopup popup = null;

    private String url = null;

    public static void create(CreateUserPopup popup,
            PermissionExceptionPanel panel, String url) {
        UserCreator creator = new UserCreator(popup, panel, url);
        creator.go();
    }

    public UserCreator(CreateUserPopup popup, PermissionExceptionPanel panel,
            String url) {
        this.popup = popup;
        this.panel = panel;
        this.url = url + "user/";
    }

    public void go() {
        displayWaitMessage("Adding user...", true);
        String goUrl = url + URL.encodePathSegment(popup.getUsername());
        goUrl += "?successUrl=" + URL.encodeQueryString(Location.getHref());
        GWT.log("Adding user with url " + goUrl);
        go(goUrl, Method.PUT);
    }

    protected void onSuccess() {
        popup.hide();
        if (!wasCancelled()) {
            panel.addUser(popup.getUsername());
        }
    }

    protected void onError(String message) {
        MessagePopup popup = new MessagePopup("Error creating user: " + message,
                null, MessagePopup.ERROR,
                MessageResponse.OK);
        popup.center();
    }

}
