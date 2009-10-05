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

package com.googlecode.vicovre.gwtinterface.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.PlayItemDeleter;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.PlayItemEditor;

public class PlayItem extends SimplePanel implements ClickHandler,
        MessageResponseHandler {

    private static final StringDateTimeFormat DATE_FORMAT =
        new StringDateTimeFormat("dd MMM yyyy 'at' HH:mm");

    private static final NumberFormat TIME_FORMAT =
        NumberFormat.getFormat("00");

    private final Image DELETE = new Image("images/delete.gif");

    private final Image EDIT = new Image("images/edit.gif");

    private final Image PLAY_TO_VENUE = new Image("images/playToVenue.gif");

    private String id = null;

    private Label name = new Label();

    private HTML description = new HTML();

    private Label startDate = new Label();

    private Label duration = new Label();

    private long durationValue = 0;

    private PushButton editButton = new PushButton(EDIT);

    private PushButton deleteButton = new PushButton(DELETE);

    private PushButton playToVenueButton = new PushButton(PLAY_TO_VENUE);

    private PlayItemEditPopup editPopup = new PlayItemEditPopup(this);

    private PlayToVenuePopup playToVenuePopup = null;

    public PlayItem(String id, String name) {

        this.id = id;
        this.name.setText(name);
        this.editPopup.setName(name);
        this.playToVenuePopup = new PlayToVenuePopup(this);

        setWidth("100%");
        DOM.setStyleAttribute(getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(getElement(), "borderStyle", "solid");

        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(playToVenueButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.setWidth("100px");

        DockPanel topLine = new DockPanel();
        topLine.add(startDate, DockPanel.WEST);
        topLine.add(this.name, DockPanel.CENTER);
        topLine.add(buttons, DockPanel.EAST);
        topLine.add(duration, DockPanel.EAST);
        topLine.setWidth("100%");

        topLine.setCellWidth(duration, "100px");
        topLine.setCellWidth(buttons, "100px");
        topLine.setCellWidth(startDate, "160px");
        this.name.setWidth("100%");

        DisclosurePanel descriptionPanel = new DisclosurePanel("Description");
        descriptionPanel.add(description);
        descriptionPanel.setWidth("100%");
        description.setWidth("100%");
        description.setHeight("50px");

        panel.add(topLine);
        panel.add(descriptionPanel);
        add(panel);

        editButton.addClickHandler(this);
        deleteButton.addClickHandler(this);
        playToVenueButton.addClickHandler(this);
    }

    public String getId() {
        return id;
    }

    public void setStartDate(Date date) {
        this.startDate.setText(DATE_FORMAT.format(date));
    }

    public static String getTimeText(long duration) {
        long remainder = duration / 1000;
        long hours = remainder / 3600;
        remainder -= hours * 3600;
        long minutes = remainder / 60;
        remainder -= minutes * 60;
        long seconds = remainder;

        return TIME_FORMAT.format(hours) + ":"
                + TIME_FORMAT.format(minutes) + ":"
                + TIME_FORMAT.format(seconds);
    }

    public void setDuration(Long duration) {
        this.durationValue = duration;
        this.duration.setText(getTimeText(duration));
    }

    public long getDuration() {
        return durationValue;
    }

    public void setDescription(String description) {
        this.description.setHTML(description.replaceAll("\n", "<br/>"));
        this.editPopup.setDescription(description);
    }

    public void setDescriptionIsEditable(boolean editable) {
        editPopup.setDescriptionIsEditable(editable);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(editButton)) {
            editPopup.center();
        } else if (event.getSource().equals(deleteButton)) {
            PlayItemDeleter.deleteRecording(this);
        } else if (event.getSource().equals(playToVenueButton)) {
            playToVenuePopup.center();
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            name.setText(editPopup.getName());
            description.setHTML(editPopup.getDescription().replaceAll(
                    "\n", "<br/>"));
            PlayItemEditor.editPlayItem(this);
        }
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("name", name.getText());
        details.put("description", description.getText());
        return details;
    }
}
