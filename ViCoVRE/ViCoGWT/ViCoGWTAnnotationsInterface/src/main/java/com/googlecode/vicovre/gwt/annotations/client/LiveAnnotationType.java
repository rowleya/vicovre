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

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.restlet.gwt.Callback;
import org.restlet.gwt.Client;
import org.restlet.gwt.data.Protocol;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;
import org.restlet.gwt.data.Status;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;

public class LiveAnnotationType extends Callback implements ClickHandler {

    private Application application = null;

    private String name = null;

    private String image = null;

    private String visible = null;

    private HorizontalPanel panel = new HorizontalPanel();

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private HashMap<String, HashMap<String, String>> fieldAttributes =
        new HashMap<String, HashMap<String, String>>();

    private HashMap<String, TextBoxBase> fieldWidgets =
        new HashMap<String, TextBoxBase>();

    private String editingMessage = null;

    private String relatesTo = null;

    private String timestamp = null;

    public LiveAnnotationType(Application application, String name,
            String image, String visible,
            HashMap<String, HashMap<String, String>> fieldAttributes) {
        this.application = application;
        this.name = name;
        this.image = image;
        this.visible = visible;
        this.fieldAttributes = fieldAttributes;

        if (this.image.startsWith("/")) {
            this.image = this.image.substring(1);
        }

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        for (String field : fieldAttributes.keySet()) {
            HashMap<String, String> attributes = fieldAttributes.get(field);
            String type = attributes.get("type");
            String display = attributes.get("displayname");
            if (!type.equals("messageId")) {
                VerticalPanel fieldPanel = new VerticalPanel();
                fieldPanel.setWidth("100%");
                Label label = new Label(display + ":");
                TextBoxBase textBox = null;
                if (type.equals("text")) {
                    textBox = new TextBox();
                } else if (type.equals("textarea")) {
                    textBox = new TextArea();
                    textBox.setHeight("100px");
                }
                textBox.setName(field);
                fieldPanel.add(label);
                fieldPanel.add(textBox);
                textBox.setWidth("100%");
                mainPanel.add(fieldPanel);
                fieldWidgets.put(field, textBox);
            }
        }

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("200px");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel);

        Image icon = new Image(application.getUrl() + this.image);
        panel.add(icon);
        panel.add(mainPanel);

        panel.setWidth("100%");
        panel.setHeight("100%");
        panel.setCellWidth(icon, "100px");

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getVisible() {
        return visible;
    }

    public Panel getPanel() {
        return panel;
    }

    public void clearFields() {
        for (TextBoxBase field : fieldWidgets.values()) {
            field.setText("");
        }
        editingMessage = null;
        relatesTo = null;
    }

    public void edit(Annotation annotation) {
        editingMessage = annotation.getId();
        for (TextBoxBase field : fieldWidgets.values()) {
            field.setText(annotation.getBodyItem(field.getName()));
        }
        timestamp = annotation.getTimestamp();
    }

    public List<String> getFields() {
        return new Vector<String>(fieldWidgets.keySet());
    }

    public String getFieldAttribute(String field, String attribute) {
        return fieldAttributes.get(field).get(attribute);
    }

    public void setRelatesTo(String relatesTo) {
        this.relatesTo = relatesTo;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(cancelButton)) {
            application.displayButtonPanel();
            clearFields();
        } else {
            application.clearPanel();
            String url = application.getUrl();
            url += "annotations/send";

            String post = "type=" + URL.encodeComponent(name);
            post += "&author=" + URL.encodeComponent(application.getAuthor());
            if (editingMessage != null) {
                post += "&id="
                    + URL.encodeComponent(editingMessage);
            }
            post += "&timestamp=" + timestamp;
            for (String field : fieldAttributes.keySet()) {
                TextBoxBase text = fieldWidgets.get(field);
                if (text != null) {
                    post += "&" + field + "="
                        + URL.encodeComponent(text.getText());
                } else {
                    String type = getFieldAttribute(field, "type");
                    if (type.equals("messageId")) {
                        post += "&" + field + "=" + relatesTo;
                    }
                }
            }
            Client client = new Client(Protocol.HTTP);
            client.post(url + "?" + post, "", this);
        }
    }

    public void onEvent(Request request, Response response) {
        if (response.getStatus().equals(Status.SUCCESS_OK)) {
            application.displayButtonPanel();
            clearFields();
        } else {
            application.displayAnnotationPanel(this);
            String errorMessage = "Error sending message "
                + response.getStatus().getCode() + ": "
                + response.getStatus().getDescription();
            MessagePopup error = new MessagePopup(errorMessage,
                    null, MessagePopup.ERROR, MessageResponse.OK);
            error.center();
        }
    }
}
