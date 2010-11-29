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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.recorder.client.rest.HarvestItemDeleter;
import com.googlecode.vicovre.gwt.recorder.client.rest.HarvestItemEditor;
import com.googlecode.vicovre.gwt.recorder.client.rest.HarvestItemUpdater;

public class HarvestItem extends HorizontalPanel implements ClickHandler,
        MessageResponseHandler, Comparable<HarvestItem> {

    private final Image HARVEST = new Image("images/harvest.gif");

    private final Image EDIT = new Image("images/edit.gif");

    private final Image DELETE = new Image("images/delete.gif");

    private String id = null;

    private Label name = new Label("");

    private String url = null;

    private String format = null;

    private String updateFrequency = null;

    private int month = 0;

    private int dayOfMonth = 0;

    private int dayOfWeek = 0;

    private int hour = 0;

    private int minute = 0;

    private String venueServerUrl = null;

    private String venueUrl = null;

    private String[] addresses = null;

    private Label status = new Label("OK");

    private PushButton harvestButton = new PushButton(HARVEST);

    private PushButton editButton = new PushButton(EDIT);

    private PushButton deleteButton = new PushButton(DELETE);

    private FolderPanel folderPanel = null;

    private RecordPanel recordPanel = null;

    private PlayPanel playPanel = null;

    private String baseUrl = null;

    private HarvestItemPopup popup = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    private JsArrayString users = null;

    private JsArrayString groups = null;

    public HarvestItem(String baseUrl, FolderPanel folderPanel,
            RecordPanel recordPanel, PlayPanel playPanel, String id,
            String itemName, HarvestItemPopup popup, Layout[] layouts,
            Layout[] customLayouts, JsArrayString users, JsArrayString groups) {
        this.baseUrl = baseUrl;
        this.folderPanel = folderPanel;
        this.recordPanel = recordPanel;
        this.playPanel = playPanel;
        this.id = id;
        this.popup = popup;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
        this.users = users;
        this.groups = groups;
        name.setText(itemName);
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

    public String getFolder() {
        return folderPanel.getCurrentFolder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.getText();
    }

    public String getUrl() {
        return url;
    }

    public String getFormat() {
        return format;
    }

    public String getUpdateFrequency() {
        return updateFrequency;
    }

    public int getMonth() {
        return month;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setUpdateFrequency(String frequency) {
        this.updateFrequency = frequency;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setVenueServerUrl(String venueServerUrl) {
        this.venueServerUrl = venueServerUrl;
    }

    public void setVenueUrl(String venueUrl) {
        this.venueUrl = venueUrl;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(editButton)) {
            if (popup == null) {
                popup = new HarvestItemPopup(baseUrl, this);
            }
            popup.center();
        } else if (event.getSource().equals(harvestButton)) {
            HarvestItemUpdater.harvest(this, folderPanel, recordPanel,
                    playPanel,  baseUrl, layouts, customLayouts,
                    users, groups);
        } else if (event.getSource().equals(deleteButton)) {
            HarvestItemDeleter.deleteItem(this, baseUrl);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            HarvestItemPopup popup = (HarvestItemPopup) response.getSource();
            name.setText(popup.getName());
            url = popup.getUrl();
            format = popup.getFormat();
            updateFrequency = popup.getUpdateFrequency();
            month = popup.getMonth();
            dayOfMonth = popup.getDayOfMonth();
            dayOfWeek = popup.getDayOfWeek();
            hour = popup.getHour();
            minute = popup.getMinute();
            venueServerUrl = popup.getVenueServer();
            venueUrl = popup.getVenue();
            addresses = popup.getAddresses();
            HarvestItemEditor.updateItem(this, baseUrl);
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

    public String getDetailsAsUrl() {
        String itemUrl = "name=" + URL.encodeComponent(name.getText());
        itemUrl += "&url=" + URL.encodeComponent(url);
        itemUrl += "&format=" + URL.encodeComponent(format);
        itemUrl += "&updateFrequency=" + URL.encodeComponent(updateFrequency);
        itemUrl += "&hour=" + hour;
        itemUrl += "&minute=" + minute;
        if (updateFrequency.equals(HarvestItemPopup.UPDATE_ANUALLY)) {
            itemUrl += "&month=" + month;
            itemUrl += "&dayOfMonth=" + dayOfMonth;
        } else if (updateFrequency.equals(HarvestItemPopup.UPDATE_MONTHLY)) {
            itemUrl += "&dayOfMonth=" + dayOfMonth;
        } else if (updateFrequency.equals(HarvestItemPopup.UPDATE_WEEKLY)) {
            itemUrl += "&dayOfWeek=" + dayOfWeek;
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
        details.put("name", name.getText());
        details.put("url", url);
        details.put("format", format);
        details.put("updateFrequency", updateFrequency);
        if (updateFrequency.equals(HarvestItemPopup.UPDATE_ANUALLY)) {
            details.put("month", month);
            details.put("dayOfMonth", dayOfMonth);
        } else if (updateFrequency.equals(HarvestItemPopup.UPDATE_MONTHLY)) {
            details.put("dayOfMonth", dayOfMonth);
        } else if (updateFrequency.equals(HarvestItemPopup.UPDATE_WEEKLY)) {
            details.put("dayOfWeek", dayOfWeek);
        }
        details.put("hour", hour);
        details.put("minute", minute);
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

    public int compareTo(HarvestItem item) {
        return name.getText().compareTo(item.name.getText());
    }
}
