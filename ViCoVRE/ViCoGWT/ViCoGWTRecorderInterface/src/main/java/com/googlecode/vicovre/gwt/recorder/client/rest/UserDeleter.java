package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class UserDeleter extends AbstractVoidRestCall
        implements MessageResponseHandler {

    private String url = null;

    private String username = null;

    public static void delete(String url, String username) {
        UserDeleter deleter = new UserDeleter(url, username);
        deleter.go();
    }

    public UserDeleter(String url, String username) {
        this.url = url;
        this.username = username;
    }

    public void go() {
        MessagePopup popup = new MessagePopup(
                "Are you sure that you want to delete " + username + "?",
                this, MessagePopup.QUESTION, MessageResponse.OK,
                MessageResponse.CANCEL);
        popup.center();
    }

    protected void onSuccess() {
        // Do Nothing
    }

    protected void onError(String message) {
        displayError("Error deleting user: " + message);
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            displayWaitMessage("Deleting User...", true);
            go(url + "user/" + URL.encodePathSegment(username), Method.DELETE);
        }
    }

}
