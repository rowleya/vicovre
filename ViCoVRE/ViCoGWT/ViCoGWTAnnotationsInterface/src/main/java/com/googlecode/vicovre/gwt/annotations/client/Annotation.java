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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TreeItem;
import com.googlecode.vicovre.gwt.annotations.client.json.JSONAnnotation;
import com.googlecode.vicovre.gwt.client.StringDateTimeFormat;

public class Annotation implements ClickHandler {

    private static final StringDateTimeFormat TIME_FORMAT =
        new StringDateTimeFormat("'['HH':'mm':'ss']'");

    private static final String URL_PATTERN = "(^|[ \t\r\n])((ftp|http|https|"
            + "gopher|mailto|news|nntp|telnet|wais|file|prospero|"
            + "aim|webcal):(([A-Za-z0-9$_.+!*(),;/?:@&~=-])|%"
            + "[A-Fa-f0-9]{2}){2,}(#([a-zA-Z0-9][a-zA-Z0-9$_.+!"
            + "*(),;/?:@&~=%-]*))?([A-Za-z0-9$_+!*();/?:~-]))";

    private static final int ICON_SIZE = 20;

    private Application application = null;

    private JSONAnnotation annotation = null;

    private HorizontalPanel panel = new HorizontalPanel();

    private TreeItem item = new TreeItem(panel);

    private PushButton editButton = null;

    private PushButton respondButton = null;

    private HTML htmlItem = new HTML();

    private LiveAnnotationType responseType = null;

    public Annotation(Application application, JSONAnnotation annotation) {
        this.application = application;
        this.annotation = annotation;

        responseType = new LiveAnnotationType(application, "Response");

        item.setState(true);

        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);

        Label timeLabel = new Label(TIME_FORMAT.format(
                JSONAnnotation.TIMESTAMP_FORMAT.parse(
                        annotation.getTimestamp())));
        panel.add(timeLabel);
        panel.setCellWidth(timeLabel, "80px");
        if (annotation.getAuthor().equals(application.getAuthor())) {
            Image editImage = new Image("images/annotations/edit.png");
            editImage.setWidth(ICON_SIZE + "px");
            editImage.setHeight(ICON_SIZE + "px");
            editButton = new PushButton(editImage);
            panel.add(editButton);
            panel.setCellWidth(editButton, (ICON_SIZE + 10) + "px");
            editButton.addClickHandler(this);
            editButton.setWidth(ICON_SIZE + "px");
            editButton.setHeight(ICON_SIZE + "px");
        }
        Image respondImage = new Image("images/annotations/Response.png");
        respondImage.setWidth(ICON_SIZE + "px");
        respondImage.setHeight(ICON_SIZE + "px");
        respondButton = new PushButton(respondImage);
        panel.add(respondButton);
        panel.setCellWidth(respondButton, (ICON_SIZE + 10) + "px");
        respondButton.addClickHandler(this);
        respondButton.setWidth(ICON_SIZE + "px");
        respondButton.setHeight(ICON_SIZE + "px");

        panel.add(htmlItem);
        setHtml();
    }

    private void setHtml() {
        User user = application.getUser(annotation.getAuthor());
        String htmlMsg = "<span style=\"color: " + user.getColour() + "\">";
        htmlMsg += user.getName() + ": ";
        htmlMsg += annotation.getMessage().replaceAll(URL_PATTERN,
                "$1<a href=\"$2\" target=\"_blank\">$2</a>");
        htmlMsg += "</span>";
        htmlItem.setHTML(htmlMsg);
    }

    public Panel getPanel() {
        return panel;
    }

    public TreeItem getItem() {
        return item;
    }

    public void setAnnotation(JSONAnnotation annotation) {
        this.annotation = annotation;
        setHtml();
    }

    public JSONAnnotation getAnnotation() {
        return annotation;
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(editButton)) {
            JsArrayString tags = annotation.getTags();
            LiveAnnotationType type = null;
            for (int i = 0; (i < tags.length()) && (type == null); i++) {
                type = application.getType(tags.get(i));
            }
            for (int i = 0; (i < tags.length()) && (type == null); i++) {
                if (tags.get(i).equals(responseType.getTag())) {
                    type = responseType;
                }
            }
            if (type == null) {
                type = application.getType(Application.DEFAULT_TAG);
            }
            type.edit(annotation);
            application.displayAnnotationPanel(type);
        } else if (event.getSource().equals(respondButton)) {
            responseType.setResponseTo(annotation.getId());
            TimeReceiver.getTime(application, responseType);
        }
    }

}
