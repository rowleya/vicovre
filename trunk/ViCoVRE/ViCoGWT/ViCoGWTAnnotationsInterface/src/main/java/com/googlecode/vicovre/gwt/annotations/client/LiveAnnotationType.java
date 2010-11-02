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

import org.restlet.gwt.data.Method;
import org.restlet.gwt.data.Response;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.annotations.client.json.JSONAnnotation;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.rest.AbstractRestCall;

public class LiveAnnotationType extends AbstractRestCall
        implements ClickHandler, KeyPressHandler {

    private Application application = null;

    private HorizontalPanel panel = new HorizontalPanel();

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private String tag = null;

    private TextArea message = new TextArea();

    private TextBox tags = new TextBox();

    private TextBox people = new TextBox();

    private String editingMessage = null;

    private String responseTo = null;

    private String timestamp = null;

    public LiveAnnotationType(Application application, String tag) {
        this.application = application;
        this.tag = tag;

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");

        FlexTable table = new FlexTable();
        table.setWidth("100%");
        table.setWidget(0, 0, new Label("Message:"));
        table.setWidget(0, 1, message);
        table.setWidget(1, 0, new Label("Tags:"));
        table.setWidget(1, 1, tags);
        table.setWidget(2, 0, new Label("People:"));
        table.setWidget(2, 1, people);
        table.getColumnFormatter().setWidth(0, "60px");
        table.getCellFormatter().setVerticalAlignment(0, 0,
                HorizontalPanel.ALIGN_TOP);
        message.setWidth("100%");
        tags.setWidth("100%");
        people.setWidth("100%");
        mainPanel.add(table);

        message.addKeyPressHandler(this);
        tags.addKeyPressHandler(this);
        people.addKeyPressHandler(this);

        tags.setTitle("Enter any tags here separated by a comma or space");
        people.setTitle("Enter any people referenced here separated by a comma"
                + " or a space");

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100px");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        table.setWidget(3, 1, buttonPanel);

        Image icon = new Image("images/annotations/" + tag + ".png");
        panel.add(icon);
        panel.add(mainPanel);

        panel.setWidth("100%");
        panel.setHeight("100%");
        panel.setCellWidth(icon, "100px");

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
    }

    public String getTag() {
        return tag;
    }

    public Panel getPanel() {
        return panel;
    }

    public void clearFields() {
        message.setText("");
        tags.setText("");
        people.setText("");
        editingMessage = null;
        responseTo = null;
    }

    public void edit(JSONAnnotation annotation) {
        editingMessage = annotation.getId();
        message.setText(annotation.getMessage());
        String tagString = "";
        JsArrayString annotationTags = annotation.getTags();
        for (int i = 0; i < annotationTags.length(); i++) {
            String tag = annotationTags.get(i);
            if (!tag.equals(this.tag)) {
                if (!tagString.equals("")) {
                    tagString += ", ";
                }
                tagString += tag;
            }
        }
        tags.setText(tagString);
        String peopleString = "";
        JsArrayString annotationPeople = annotation.getPeople();
        for (int i = 0; i < annotationPeople.length(); i++) {
            String person = annotationPeople.get(i);
            if (!peopleString.equals("")) {
                peopleString += ", ";
            }
            peopleString += person;
        }
        people.setText(peopleString);
        timestamp = String.valueOf(JSONAnnotation.TIMESTAMP_FORMAT.parse(
                annotation.getTimestamp()).getTime());
        responseTo = annotation.getResponseTo();
    }

    public void setResponseTo(String responseTo) {
        this.responseTo = responseTo;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(cancelButton)) {
            application.displayButtonPanel();
            clearFields();
        } else {
            String error = "";
            String post = "timestamp=" + timestamp;
            post += "&author=" + URL.encodeComponent(application.getAuthor());
            post += "&message=" + URL.encodeComponent(message.getText());
            post += "&tag=" + tag;
            if (!tags.getText().trim().equals("")) {
                for (String tag : tags.getText().split("[ \t,;:]+")) {
                    if (tag.startsWith("#")) {
                        tag = tag.substring(1);
                    }
                    if (!tag.matches("[A-Za-z0-9_-]+")) {
                        if (!error.equals("")) {
                            error += "; ";
                        }
                        error += "Tag " + tag
                            + " is invalid: tags must be alphanumeric";
                    }
                    if (!tag.equals(this.tag)) {
                        post += "&tag=" + URL.encodeComponent(tag);
                    }
                }
            }
            for (String person : people.getText().split("[ \t,;:]+")) {
                if (person.startsWith("@")) {
                    person = person.substring(1);
                }
                post += "&person=" + URL.encodeComponent(person);
            }
            if (responseTo != null) {
                post += "&responseTo=" + responseTo;
            }

            if (!error.equals("")) {
                MessagePopup errorPopup = new MessagePopup(error, null,
                        MessagePopup.ERROR, MessageResponse.OK);
                errorPopup.center();
            } else {
                application.clearPanel();
                String url = application.getUrl();
                if (editingMessage == null) {
                    url += "send";
                    GWT.log("Sending " + post);
                    go(url + "?" + post, Method.POST);
                } else {
                    url += "edit/" + editingMessage;
                    go(url + "?" + post, Method.PUT);
                }
            }
        }
    }

    protected void onError(String message) {
        application.displayAnnotationPanel(this);
        String errorMessage = "Error sending message: " + message;
        MessagePopup error = new MessagePopup(errorMessage,
                null, MessagePopup.ERROR, MessageResponse.OK);
        error.center();
    }

    protected void onSuccess(Response response) {
        application.displayButtonPanel();
        clearFields();
    }

    public void onKeyPress(KeyPressEvent event) {
        if ((event.getCharCode() == 13) || (event.getCharCode() == 10)) {
            okButton.click();
        }
    }

    public void focus() {
        message.setFocus(true);
    }
}
