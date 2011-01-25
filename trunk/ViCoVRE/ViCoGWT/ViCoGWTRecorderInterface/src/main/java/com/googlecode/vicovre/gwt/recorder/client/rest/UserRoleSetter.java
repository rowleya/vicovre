package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.UserRolePopup;

public class UserRoleSetter extends AbstractVoidRestCall {

    private String url = null;

    private UserRolePopup popup = null;

    private String username = null;

    private String role = null;

    public static void set(String url, UserRolePopup popup, String username,
            String role) {
        UserRoleSetter setter = new UserRoleSetter(url, popup, username, role);
        setter.go();
    }

    public UserRoleSetter(String url, UserRolePopup popup, String username,
            String role) {
        this.url = url;
        this.popup = popup;
        this.username = username;
        this.role = role;
    }

    public void go() {
        displayWaitMessage("Setting Role...", true);
        go(url + "user/" + URL.encodePathSegment(username) + "/role?role="
                + URL.encodeQueryString(role), Method.PUT);
    }

    protected void onSuccess() {
        popup.hide();
    }

    protected void onError(String message) {
        displayError("Error setting role: " + message);
    }

}