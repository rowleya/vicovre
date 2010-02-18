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

import java.util.Date;
import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TreeItem;
import com.googlecode.vicovre.gwt.client.StringDateTimeFormat;

public class Annotation implements ClickHandler {

    private static final StringDateTimeFormat DATE_FORMAT =
        new StringDateTimeFormat("'['HH:mm:ss']'");

    private static final int ICON_SIZE = 20;

    private Application application = null;

    private String id = null;

    private String type = null;

    private String timestamp = null;

    private HashMap<String, String> body = new HashMap<String, String>();

    private HorizontalPanel panel = new HorizontalPanel();

    private TreeItem item = new TreeItem(panel);

    private PushButton editButton = null;

    private HashMap<PushButton, LiveAnnotationType> inlineButtons =
        new HashMap<PushButton, LiveAnnotationType>();

    public Annotation(Application application, String id, String type,
            String author, String timestamp, HashMap<String, String> body,
            String html) {
        this.application = application;
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.body = body;

        item.setState(true);

        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);

        Label timeLabel = new Label(DATE_FORMAT.format(
                new Date(Long.parseLong(timestamp))));
        panel.add(timeLabel);
        panel.setCellWidth(timeLabel, "80px");
        if (author.equals(application.getAuthor())) {
            Image editImage = new Image("images/edit.png");
            editImage.setWidth(ICON_SIZE + "px");
            editImage.setHeight(ICON_SIZE + "px");
            editButton = new PushButton(editImage);
            panel.add(editButton);
            panel.setCellWidth(editButton, (ICON_SIZE + 10) + "px");
            editButton.addClickHandler(this);
            editButton.setWidth(ICON_SIZE + "px");
            editButton.setHeight(ICON_SIZE + "px");
        }
        for (LiveAnnotationType laType : application.getInlineButtonTypes()) {
            String imageUrl = laType.getImage();
            if (imageUrl.startsWith("/")) {
                imageUrl = imageUrl.substring(1);
            }
            Image buttonImage = new Image(application.getUrl() + imageUrl);
            buttonImage.setWidth(ICON_SIZE + "px");
            buttonImage.setHeight(ICON_SIZE + "px");
            PushButton button = new PushButton(buttonImage);
            inlineButtons.put(button, laType);
            panel.add(button);
            panel.setCellWidth(button, (ICON_SIZE + 10) + "px");
            button.addClickHandler(this);
            button.setWidth(ICON_SIZE + "px");
            button.setHeight(ICON_SIZE + "px");
        }

        LiveAnnotationType laType = application.getType(type);
        User user = application.getUser(author);
        String htmlMsg = "<span style=\"color: " + user.getColour() + "\">";
        htmlMsg += user.getName() + ": ";
        String imageUrl = laType.getImage();
        if (imageUrl.startsWith("/")) {
            imageUrl = imageUrl.substring(1);
        }
        htmlMsg += "<img src=\"" + application.getUrl() + imageUrl
            + "\"/ width=\"" + ICON_SIZE
            + "\" height=\"" + ICON_SIZE + "\">";
        htmlMsg += html;
        htmlMsg += "</span>";
        HTML htmlItem = new HTML(htmlMsg);
        panel.add(htmlItem);
    }

    public Panel getPanel() {
        return panel;
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getBodyItem(String item) {
        return body.get(item);
    }

    public TreeItem getItem() {
        return item;
    }

    public void setItem(TreeItem item) {
        this.item = item;
        item.setWidget(panel);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(editButton)) {
            LiveAnnotationType laType = application.getType(type);
            laType.edit(this);
            application.displayAnnotationPanel(laType);
        } else {
            LiveAnnotationType laType = inlineButtons.get(event.getSource());
            if (laType != null) {
                laType.setRelatesTo(id);
                TimeReceiver.getTime(application, laType);
            }
        }
    }

}
