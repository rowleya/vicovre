package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.recorder.client.EditGroupPopup;

public class GroupUserEditor extends AbstractVoidRestCall {

    private EditGroupPopup popup = null;

    private String url = null;

    public static void edit(EditGroupPopup popup, String group, String url) {
        GroupUserEditor editor = new GroupUserEditor(popup, group, url);
        editor.go();
    }

    public GroupUserEditor(EditGroupPopup popup, String group, String url) {
        this.popup = popup;
        this.url = url + "group/" + URL.encodePathSegment(group) + "/users";
    }

    public void go() {
        displayWaitMessage("Editing group...", true);
        go(url, Method.PUT);
    }

    protected void onSuccess() {
        if (!wasCancelled()) {
            popup.hide();
        }
    }

    protected void onError(String message) {
        displayError("Error setting users: " + message);
    }



}
