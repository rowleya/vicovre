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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.RecordingItemDeleter;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.RecordingItemEditor;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.RecordingItemPauser;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.RecordingItemResumer;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.RecordingItemStarter;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.RecordingItemStopper;

public class RecordingItem extends SimplePanel implements ClickHandler,
        MessageResponseHandler {

    private static final String STOPPED = "Stopped";

    private static final String RECORDING = "Recording";

    private static final String PAUSED = "Paused";

    private static final String COMPLETED = "Completed";

    private static final String ERROR = "Error: ";

    private final Image RECORD = new Image("images/record.gif");

    private final Image STOP = new Image("images/stop.gif");

    private final Image PAUSE = new Image("images/pause.gif");

    private final Image DELETE = new Image("images/delete.gif");

    private final Image EDIT = new Image("images/edit.gif");

    private Label name = new Label();

    private HTML description = new HTML();

    private Label status = new Label(STOPPED);

    private ToggleButton recordButton = new ToggleButton(RECORD, PAUSE);

    private PushButton stopButton = new PushButton(STOP);

    private PushButton deleteButton = new PushButton(DELETE);

    private PushButton editButton = new PushButton(EDIT);

    private RecordingItemPopup popup = new RecordingItemPopup(this);

    private int id = 0;

    public RecordingItem(int id, String itemName) {
        popup = new RecordingItemPopup(this);
        popup.setName(itemName);
        init(id);
    }

    public RecordingItem(int id, RecordingItemPopup popup) {
        this.popup = popup;
        init(id);
    }

    private void init(int id) {
        this.id = id;
        name.setText(popup.getName());
        setWidth("100%");
        DOM.setStyleAttribute(getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(getElement(), "borderStyle", "solid");

        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(recordButton);
        buttons.add(stopButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.setWidth("100px");

        DockPanel topLine = new DockPanel();
        topLine.add(name, DockPanel.WEST);
        topLine.add(status, DockPanel.CENTER);
        topLine.add(buttons, DockPanel.EAST);
        topLine.setWidth("100%");

        topLine.setCellWidth(status, "100px");
        topLine.setCellWidth(buttons, "100px");
        name.setWidth("100%");

        DisclosurePanel descriptionPanel = new DisclosurePanel("Description");
        descriptionPanel.add(description);
        descriptionPanel.setWidth("100%");
        description.setWidth("100%");
        description.setHeight("50px");

        panel.add(topLine);
        panel.add(descriptionPanel);
        add(panel);

        recordButton.addClickHandler(this);
        stopButton.addClickHandler(this);
        editButton.addClickHandler(this);
        deleteButton.addClickHandler(this);

        stopButton.setEnabled(false);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status.getText();
    }

    public void setDescription(String description) {
        this.description.setHTML(description.replaceAll("\n", "<br/>"));
        popup.setDescription(description);
    }

    public void setDescriptionIsEditable(boolean editable) {
        popup.setDescriptionEditable(editable);
    }

    public void setStartDate(Date startDate) {
        popup.setStartDate(startDate);
    }

    public void setStopDate(Date stopDate) {
        popup.setStopDate(stopDate);
    }

    public void setVenueServerUrl(String url) {
        popup.setVenueServerUrl(url);
    }

    public void setVenueUrl(String venue) {
        popup.setVenueUrl(venue);
    }

    public void setAddresses(String[] addresses) {
        popup.setAddresses(addresses);
    }

    public void setStatus(String status) {
        this.status.setText(status);
        if (status.equals(RECORDING)) {
            DOM.setStyleAttribute(this.status.getElement(), "color", "red");
            stopButton.setEnabled(true);
            recordButton.setDown(true);
            popup.setRecording(true);
        } else if (status.startsWith(ERROR)) {
            DOM.setStyleAttribute(this.status.getElement(), "color", "red");
        } else {
            DOM.setStyleAttribute(this.status.getElement(), "color", "black");
            if (status.equals(PAUSED)) {
                popup.setRecording(true);
                stopButton.setEnabled(true);
                recordButton.setDown(false);
            } else if (status.equals(STOPPED)) {
                stopButton.setEnabled(false);
                recordButton.setDown(false);
                popup.setRecording(false);
            } else if (status.equals(COMPLETED)) {
                recordButton.setEnabled(false);
                stopButton.setEnabled(false);
                deleteButton.setEnabled(false);
                editButton.setEnabled(false);
            }
        }

    }

    public void setCreated(boolean created) {
        if (!created) {
            recordButton.setEnabled(false);
            stopButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
            recordButton.setEnabled(true);
            stopButton.setEnabled(false);
            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
        }
    }

    public void setFailedToCreate() {
        recordButton.setEnabled(false);
        stopButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(true);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(recordButton)) {
            if (recordButton.isDown()) {
                if (status.getText().equals(STOPPED)) {
                    RecordingItemStarter.start(this);
                } else {
                    RecordingItemResumer.resume(this);
                }
            } else {
                RecordingItemPauser.pause(this);
            }
        } else if (event.getSource().equals(stopButton)) {
            RecordingItemStopper.stop(this);
        } else if (event.getSource().equals(editButton)) {
            popup.center();
        } else if (event.getSource().equals(deleteButton)) {
            RecordingItemDeleter.deleteRecording(this);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            name.setText(popup.getName());
            description.setHTML(popup.getDescription().replaceAll(
                    "\n", "<br/>"));
            RecordingItemEditor.updateRecording(this);
        }
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("id", id);

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("name", popup.getName());
        metadata.put("description", popup.getDescription());
        details.put("metadata", metadata);

        if (popup.getStartDate() != null) {
            details.put("startDate", popup.getStartDate());
        }
        if (popup.getStopDate() != null) {
            details.put("stopDate", popup.getStopDate());
        }

        String venueServerUrl = popup.getVenueServer();
        if (venueServerUrl != null) {
            details.put("ag3VenueServer", venueServerUrl);
            details.put("ag3VenueUrl", popup.getVenue());
        } else {
            String[] addrs = popup.getAddresses();
            Map<String, Object>[] addresses = new Map[addrs.length];
            for (int i = 0; i < addresses.length; i++) {
                addresses[i] = new HashMap<String, Object>();
                String[] parts = addrs[i].split("/");
                addresses[i].put("host", parts[0]);
                addresses[i].put("port", Integer.valueOf(parts[1]));
                addresses[i].put("ttl", Integer.valueOf(parts[2]));
            }
            details.put("addresses", addresses);
        }
        return details;
    }
}
