package com.googlecode.vicovre.gwt.recorder.client.rest;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONObject;
import com.googlecode.vicovre.gwt.client.json.JSONUsers;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.EditUsersPopup;

public class UserLoader extends AbstractJSONRestCall {

    private String url = null;

    public static void load(String url) {
        UserLoader loader = new UserLoader(url);
        loader.go();
    }

    public UserLoader(String url) {
        super(false);
        this.url = url;
    }

    public void go() {
        displayWaitMessage("Loading users...", true);
        go(url + "user");
    }

    protected void onSuccess(JSONObject object) {
        if (!wasCancelled()) {
            JsArrayString users = null;
            if (object != null) {
                JSONUsers userObject = JSONUsers.parse(object.toString());
                users = userObject.getUsers();
            }
            EditUsersPopup popup = new EditUsersPopup(url, users);
            popup.center();
        }
    }

    protected void onError(String message) {
        displayError("Error loading users: " + message);
    }

}
