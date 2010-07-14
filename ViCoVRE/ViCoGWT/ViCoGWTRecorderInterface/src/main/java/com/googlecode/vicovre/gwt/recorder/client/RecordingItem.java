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

package com.googlecode.vicovre.gwt.recorder.client;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
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
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemDeleter;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemEditor;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemPauser;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemResumer;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemStarter;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemStopper;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.Recording;

public class RecordingItem extends SimplePanel implements ClickHandler,
        MessageResponseHandler, Comparable<RecordingItem> {

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

    private boolean descriptionEditable = true;

    private Date startDate = null;

    private Date stopDate = null;

    private String venueServerUrl = null;

    private String venueUrl = null;

    private String[] addresses = null;

    private Label status = new Label(STOPPED);

    private ToggleButton recordButton = new ToggleButton(RECORD, PAUSE);

    private PushButton stopButton = new PushButton(STOP);

    private PushButton deleteButton = new PushButton(DELETE);

    private PushButton editButton = new PushButton(EDIT);

    private String id = null;

    private String url = null;

    private FolderPanel folderPanel = null;

    public RecordingItem(FolderPanel folderPanel, String id, String itemName,
            String url) {
        this.folderPanel = folderPanel;
        this.id = id;
        this.url = url;
        name.setText(itemName);
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

    public String getId() {
        return id;
    }

    public String getFolder() {
        return folderPanel.getCurrentFolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status.getText();
    }

    public void setDescription(String description) {
        this.description.setHTML(description.replaceAll("\n", "<br/>"));
    }

    public void setDescriptionIsEditable(boolean editable) {
        this.descriptionEditable = editable;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    public void setVenueServerUrl(String url) {
        this.venueServerUrl = url;
    }

    public void setVenueUrl(String venue) {
        this.venueUrl = venue;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public String getName() {
        return name.getText();
    }

    public String getDescription() {
        return description.getHTML().replaceAll("<br/>", "\n");
    }

    public boolean getDescriptionIsEditable() {
        return descriptionEditable;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public String getVenueServerUrl() {
        return venueServerUrl;
    }

    public String getVenueUrl() {
        return venueUrl;
    }

    public String[] getAddresses() {
        return addresses;
    }

    public void setStatus(String status) {
        this.status.setText(status);
        if (status.startsWith(RECORDING)) {
            DOM.setStyleAttribute(this.status.getElement(), "color", "red");
            stopButton.setEnabled(true);
            recordButton.setDown(true);
        } else if (status.startsWith(ERROR)) {
            DOM.setStyleAttribute(this.status.getElement(), "color", "red");
        } else {
            DOM.setStyleAttribute(this.status.getElement(), "color", "black");
            if (status.startsWith(PAUSED)) {
                stopButton.setEnabled(true);
                recordButton.setDown(false);
            } else if (status.startsWith(STOPPED)) {
                stopButton.setEnabled(false);
                recordButton.setDown(false);
            } else if (status.startsWith(COMPLETED)) {
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
                if (status.getText().startsWith(STOPPED)) {
                    RecordingItemStarter.start(this, url);
                } else {
                    RecordingItemResumer.resume(this, url);
                }
            } else {
                RecordingItemPauser.pause(this, url);
            }
        } else if (event.getSource().equals(stopButton)) {
            RecordingItemStopper.stop(this, url);
        } else if (event.getSource().equals(editButton)) {
            RecordingItemPopup popup = new RecordingItemPopup(this, url);
            popup.center();
        } else if (event.getSource().equals(deleteButton)) {
            RecordingItemDeleter.deleteRecording(this, url);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            RecordingItemPopup popup = (RecordingItemPopup)
                response.getSource();
            name.setText(popup.getName());
            description.setHTML(popup.getDescription().replaceAll(
                    "\n", "<br/>"));
            startDate = popup.getStartDate();
            stopDate = popup.getStopDate();
            venueServerUrl = popup.getVenueServer();
            venueUrl = popup.getVenue();
            addresses = popup.getAddresses();
            if (id != null) {
                RecordingItemEditor.updateRecording(this, url);
            }
        }
    }

    public String getDetailsAsUrl() {
        String itemUrl = "metadata_name=" + URL.encodeComponent(getName());

        String description = getDescription();
        if (description != null) {
            itemUrl += "&metadata_description="
                + URL.encodeComponent(description);
        }

        Date startDate = getStartDate();
        if (startDate != null) {
            itemUrl += "&startDate="
                + URL.encodeComponent(Recording.DATE_FORMAT.format(startDate));
        }
        Date stopDate = getStopDate();
        if (stopDate != null) {
            itemUrl += "&stopDate="
                + URL.encodeComponent(Recording.DATE_FORMAT.format(stopDate));
        }

        String ag3VenueServer = getVenueServerUrl();
        if (ag3VenueServer != null) {
            itemUrl += "&ag3VenueServer="
                + URL.encodeComponent(ag3VenueServer);
            itemUrl += "&ag3VenueUrl="
                + URL.encodeComponent(getVenueUrl());
        } else {
            String[] addresses = getAddresses();
            for (int i = 0; i < addresses.length; i++) {
                String[] parts = addresses[i].split("/");
                itemUrl += "&host=" + parts[0];
                itemUrl += "&port=" + parts[1];
                itemUrl += "&ttl=" + parts[2];
            }
        }
        return itemUrl;
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("id", id);

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("name", getName());
        metadata.put("description", getDescription());
        details.put("metadata", metadata);

        if (startDate != null) {
            details.put("startDate", startDate);
        }
        if (stopDate != null) {
            details.put("stopDate", stopDate);
        }

        if (venueServerUrl != null) {
            details.put("ag3VenueServer", venueServerUrl);
            details.put("ag3VenueUrl", venueUrl);
        } else {
            List<Map<String, Object>> addrs =
                new Vector<Map<String, Object>>();
            for (int i = 0; i < addresses.length; i++) {
                Map<String, Object> address = new HashMap<String, Object>();
                String[] parts = addresses[i].split("/");
                address.put("host", parts[0]);
                address.put("port", Integer.valueOf(parts[1]));
                address.put("ttl", Integer.valueOf(parts[2]));
                addrs.add(address);
            }
            details.put("addresses", addrs);
        }
        return details;
    }

    public int compareTo(RecordingItem item) {
        int startDateCompare = 0;
        int stopDateCompare = 0;

        if (startDate != null && item.startDate != null) {
            startDateCompare = startDate.compareTo(item.startDate);
        } else if (startDate != null) {
            startDateCompare = 1;
        } else if (item.startDate != null) {
            startDateCompare = -1;
        }

        if (stopDate != null && item.stopDate != null) {
            stopDateCompare = stopDate.compareTo(item.stopDate);
        } else if (stopDate != null) {
            stopDateCompare = 1;
        } else if (item.stopDate != null) {
            stopDateCompare = -1;
        }

        int nameCompare = name.getText().compareTo(item.name.getText());
        if (startDateCompare == 0) {
            if (stopDateCompare == 0) {
                return nameCompare;
            }
            return stopDateCompare;
        }
        return startDateCompare;
    }
}
