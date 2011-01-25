package com.googlecode.vicovre.gwt.recorder.client.rest;

import org.restlet.client.data.Method;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.googlecode.vicovre.gwt.client.rest.AbstractVoidRestCall;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;

public class PasswordResetter extends AbstractVoidRestCall {

    private String url = null;

    private String user = null;

    public static void reset(String url, String user) {
        PasswordResetter resetter = new PasswordResetter(url, user);
        resetter.go();
    }

    public PasswordResetter(String url, String user) {
        this.url = url;
        this.user = user;
    }

    public void go() {
        displayWaitMessage("Resetting password...", true);
        go(url + "user/" + URL.encodePathSegment(user)
                + "/resetPassword?successUrl="
                + URL.encodeQueryString(Location.getHref()),
                Method.PUT);
    }


    protected void onSuccess() {
        if (!wasCancelled()) {
            MessagePopup popup = new MessagePopup(
                "A request to reset your password has been sent.\n"
                + "Please follow the instructions that have been e-mailed to you.",
                null, MessagePopup.INFO, MessageResponse.OK);
            popup.center();
        }
    }

    protected void onError(String message) {
        displayError("Error resetting password: " + message);
    }

}
