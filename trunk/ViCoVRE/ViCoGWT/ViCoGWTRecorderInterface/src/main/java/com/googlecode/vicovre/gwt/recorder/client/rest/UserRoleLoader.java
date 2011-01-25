package com.googlecode.vicovre.gwt.recorder.client.rest;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.rest.AbstractPlainRestCall;
import com.googlecode.vicovre.gwt.recorder.client.UserRolePopup;

public class UserRoleLoader extends AbstractPlainRestCall {

    private String url = null;

    private String username = null;

    public static void load(String url, String username) {
        UserRoleLoader loader = new UserRoleLoader(url, username);
        loader.go();
    }

    public UserRoleLoader(String url, String username) {
        this.url = url;
        this.username = username;
    }

    public void go() {
        displayWaitMessage("Loading User Role", true);
        go(url + "user/" + URL.encodePathSegment(username) + "/role");
    }

    protected void onSuccess(String role) {
        if (!wasCancelled()) {
            UserRolePopup popup = new UserRolePopup(username, role, url);
            popup.center();
        }
    }

    protected void onError(String message) {
        displayError("Error loading user role: " + message);
    }

}
