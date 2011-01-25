package com.googlecode.vicovre.gwt.recorder.client.rest;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.googlecode.vicovre.gwt.client.json.JSONUsers;
import com.googlecode.vicovre.gwt.client.rest.AbstractJSONRestCall;
import com.googlecode.vicovre.gwt.recorder.client.EditGroupPopup;

public class GroupLoader extends AbstractJSONRestCall {

    private String baseUrl = null;

    private String url = null;

    private JsArrayString users = null;

    private String group = null;

    public static void load(String url, String group, JsArrayString users) {
        GroupLoader loader = new GroupLoader(url, group, users);
        loader.go();
    }

    public GroupLoader(String url, String group, JsArrayString users) {
        super(false);
        this.baseUrl = url;
        this.users = users;
        this.group = group;
        this.url = url + "group/" + URL.encodePathSegment(group) + "/users";
    }

    public void go() {
        displayWaitMessage("Loading Group...", true);
        go(url);
    }

    protected void onSuccess(JSONObject object) {
        if (!wasCancelled()) {
            JsArrayString members = null;
            if (object != null) {
                JSONUsers memberList = JSONUsers.parse(object.toString());
                members = memberList.getUsers();
            }
            EditGroupPopup popup = new EditGroupPopup(group, baseUrl, users,
                    members);
            popup.center();
        }
    }

    protected void onError(String message) {
        displayError("Error loading group: " + message);
    }

}
