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

package com.googlecode.vicovre.gwt.annotations.client;

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.Protocol;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.data.Status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.ModalPopup;

public class LoginPopup extends ModalPopup<Grid> implements ClickHandler {

    private Application application = null;

    private TextBox name = new TextBox();

    private TextBox email = new TextBox();

    private Button ok = new Button("OK");

    public LoginPopup(Application application) {
        super(new Grid(3, 2));
        this.application = application;

        Grid grid = getWidget();
        grid.setWidget(0, 0, new Label("Name:"));
        grid.setWidget(0, 1, name);
        grid.setWidget(1, 0, new Label("E-mail Address:"));
        grid.setWidget(1, 1, email);
        grid.setWidget(2, 1, ok);

        grid.getCellFormatter().setHorizontalAlignment(2, 1,
                HorizontalPanel.ALIGN_RIGHT);
        grid.getColumnFormatter().setWidth(0, "150px");
        grid.getColumnFormatter().setWidth(1, "450px");
        name.setWidth("100%");
        email.setWidth("100%");

        ok.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        String errorMessage = null;
        if (name.getText().trim().length() == 0) {
            errorMessage = "Name must not be blank!";
        } else if (email.getText().trim().length() == 0) {
            errorMessage = "E-mail Address must not be blank!";
        }

        if (errorMessage != null) {
            MessagePopup error = new MessagePopup(errorMessage,
                    null, MessagePopup.ERROR, MessageResponse.OK);
            error.center();
        } else {
            Client client = new Client(Protocol.HTTP);
            String url = application.getUrl();
            url += "login?";
            url += "name=" + URL.encodeComponent(name.getText().trim());
            url += "&email=" + URL.encodeComponent(email.getText().trim());
            GWT.log("URL = " + url, null);

            client.post(url, "", new Callback() {
                public void onEvent(Request request, Response response) {
                    String errorMessage = null;
                    if (!response.getStatus().equals(Status.SUCCESS_OK)) {
                        if (response.getStatus().equals(
                                Status.CLIENT_ERROR_CONFLICT)) {
                            errorMessage = "Name or E-mail Address in use.";
                        } else if (response.getStatus().equals(
                                Status.CLIENT_ERROR_UNAUTHORIZED)) {
                            errorMessage = "Invalid Name or E-mail Address";
                        } else {
                            errorMessage = "Unknown Error "
                                + response.getStatus().getCode() + ":"
                                + response.getStatus().getDescription();
                        }
                    }
                    if (errorMessage != null) {
                        MessagePopup error = new MessagePopup(errorMessage,
                                null, MessagePopup.ERROR, MessageResponse.OK);
                        error.center();
                    } else {
                        hide();
                        application.loginDone(name.getText().trim(),
                                email.getText().trim());
                    }
                }

            });
        }
    }

}
