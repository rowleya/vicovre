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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.HarvestItemDeleter;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.HarvestItemEditor;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.HarvestItemUpdater;

public class HarvestItem extends HorizontalPanel implements ClickHandler,
        MessageResponseHandler, Comparable<HarvestItem> {

    private final Image HARVEST = new Image("images/harvest.gif");

    private final Image EDIT = new Image("images/edit.gif");

    private final Image DELETE = new Image("images/delete.gif");

    private int id;

    private Label name = new Label("");

    private Label status = new Label("OK");

    private PushButton harvestButton = new PushButton(HARVEST);

    private PushButton editButton = new PushButton(EDIT);

    private PushButton deleteButton = new PushButton(DELETE);

    private HarvestItemPopup popup = new HarvestItemPopup(this);

    public HarvestItem(int id, String itemName) {
        popup = new HarvestItemPopup(this);
        popup.setName(itemName);
        init(id);
    }

    public HarvestItem(int id, HarvestItemPopup popup) {
        this.popup = popup;
        init(id);
    }

    public void init(int id) {
        this.id = id;
        name.setText(popup.getName());
        setWidth("100%");
        DOM.setStyleAttribute(getElement(), "borderColor", "black");
        DOM.setStyleAttribute(getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(getElement(), "borderStyle", "solid");

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(harvestButton);
        buttons.add(editButton);
        buttons.add(deleteButton);

        add(name);
        add(status);
        add(buttons);
        setCellWidth(status, "100px");
        setCellWidth(buttons, "100px");
        status.setWidth("100px");
        name.setWidth("100%");

        harvestButton.addClickHandler(this);
        editButton.addClickHandler(this);
        deleteButton.addClickHandler(this);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.popup.setUrl(url);
    }

    public void setUpdateFrequency(String frequency) {
        this.popup.setUpdateFrequency(frequency);
    }

    public void setMonth(int month) {
        this.popup.setMonth(month);
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.popup.setDayOfMonth(dayOfMonth);
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.popup.setDayOfWeek(dayOfWeek);
    }

    public void setVenueServerUrl(String venueServerUrl) {
        this.popup.setVenueServer(venueServerUrl);
    }

    public void setVenueUrl(String venueUrl) {
        this.popup.setVenue(venueUrl);
    }

    public void setAddresses(String[] addresses) {
        this.popup.setAddresses(addresses);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(editButton)) {
            popup.center();
        } else if (event.getSource().equals(harvestButton)) {
            HarvestItemUpdater.harvest(this);
        } else if (event.getSource().equals(deleteButton)) {
            HarvestItemDeleter.deleteItem(this);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            if (response.getSource().equals(popup)) {
                name.setText(popup.getName());
                HarvestItemEditor.updateItem(this);
            }
        }
    }

    public void setCreated(boolean created) {
        if (!created) {
            harvestButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
            harvestButton.setEnabled(true);
            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
        }
    }

    public void setFailedToCreate() {
        harvestButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(true);
    }

    public void setStatus(String status) {
        this.status.setText(status);
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("id", id);
        details.put("name", popup.getName());
        details.put("url", popup.getUrl());
        details.put("format", popup.getFormat());
        String frequency = popup.getUpdateFrequency();
        details.put("updateFrequency", frequency);
        if (frequency.equals(HarvestItemPopup.UPDATE_ANUALLY)) {
            details.put("month", popup.getMonth());
            details.put("dayOfMonth", popup.getDayOfMonth());
        } else if (frequency.equals(HarvestItemPopup.UPDATE_MONTHLY)) {
            details.put("dayOfMonth", popup.getDayOfMonth());
        } else if (frequency.equals(HarvestItemPopup.UPDATE_WEEKLY)) {
            details.put("dayOfWeek", popup.getDayOfWeek());
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

    public int compareTo(HarvestItem item) {
        return name.getText().compareTo(item.name.getText());
    }
}
