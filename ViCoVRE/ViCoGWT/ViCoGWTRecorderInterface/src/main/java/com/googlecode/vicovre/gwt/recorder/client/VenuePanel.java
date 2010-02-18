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

import java.util.HashSet;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.VenueLoader;

public class VenuePanel extends VerticalPanel
        implements ValueChangeHandler<Boolean>, ClickHandler,
        MessageResponseHandler {

    private static final MultiWordSuggestOracle VENUE_SERVERS =
        new MultiWordSuggestOracle();

    private static final HashSet<String> VENUE_SERVER_URLS =
        new HashSet<String>();

    private RadioButton venueSelect = new RadioButton("source");

    private RadioButton manualSelect = new RadioButton("source");

    private Grid venueGrid = new Grid(2, 3);

    private HorizontalPanel manualPanel = new HorizontalPanel();

    private SuggestBox venueServer = new SuggestBox(VENUE_SERVERS);

    private ListBox venue = new ListBox();

    private ListBox addresses = new ListBox();

    private Button addButton = new Button("Add");

    private Button deleteButton = new Button("Delete");

    private Button editButton = new Button("Edit");

    private Button loadVenuesButton = new Button("Load Venues");

    private int editing = -1;

    private int deleting = -1;

    private String venueSelected = null;

    public static void addVenueServer(String uri) {
        if (!VENUE_SERVER_URLS.contains(uri)) {
            VENUE_SERVERS.add(uri);
            VENUE_SERVER_URLS.add(uri);
        }
    }

    public VenuePanel() {
        venueSelect.setText("Use venue server: ");
        manualSelect.setText("Manually enter addresses: ");

        venueGrid.setWidget(0, 0, new Label("Venue Server: "));
        venueGrid.setWidget(0, 1, venueServer);
        venueGrid.setWidget(0, 2, loadVenuesButton);
        venueGrid.setWidget(1, 0, new Label("Venue: "));
        venueGrid.setWidget(1, 1, venue);
        venueGrid.getColumnFormatter().setWidth(0, "120px");
        venueGrid.getColumnFormatter().setWidth(2, "110px");
        venueGrid.getCellFormatter().setHorizontalAlignment(0, 2, ALIGN_LEFT);
        venueGrid.setWidth("90%");
        venueServer.setWidth("100%");
        venue.setWidth("100%");
        loadVenuesButton.setWidth("110px");

        VerticalPanel buttons = new VerticalPanel();
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        manualPanel.add(addresses);
        manualPanel.add(buttons);
        addresses.setVisibleItemCount(4);
        manualPanel.setWidth("100%");
        addresses.setWidth("100%");
        buttons.setWidth("80px");
        addButton.setWidth("100%");
        deleteButton.setWidth("100%");
        editButton.setWidth("100%");
        manualPanel.setCellWidth(buttons, "80px");
        manualPanel.setCellWidth(addresses, "50%");

        VerticalPanel venueSelection = new VerticalPanel();
        venueSelection.setHorizontalAlignment(ALIGN_LEFT);
        venueSelection.add(venueSelect);
        venueSelection.add(venueGrid);
        venueSelection.setWidth("100%");

        VerticalPanel manualSelection = new VerticalPanel();
        manualSelection.setHorizontalAlignment(ALIGN_LEFT);
        manualSelection.add(manualSelect);
        manualSelection.add(manualPanel);
        manualSelection.setWidth("100%");

        add(venueSelection);
        add(manualSelection);

        manualSelect.addValueChangeHandler(this);
        venueSelect.addValueChangeHandler(this);
        venueSelect.setValue(true, true);

        loadVenuesButton.addClickHandler(this);
        addButton.addClickHandler(this);
        deleteButton.addClickHandler(this);
        editButton.addClickHandler(this);
    }

    public void allowManualAddresses(boolean allow) {
        if (allow) {
            manualSelect.setVisible(true);
        } else {
            manualSelect.setVisible(false);
            venueSelect.setValue(true, true);
        }
    }

    public void onValueChange(ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            if (event.getSource().equals(venueSelect)) {
                venueGrid.setVisible(true);
                manualPanel.setVisible(false);
            } else if (event.getSource().equals(manualSelect)) {
                venueGrid.setVisible(false);
                manualPanel.setVisible(true);
            }
        }
    }

    public void setVenues(String server, Venue[] venues) {
        addVenueServer(server);
        venueServer.setText(server);
        venue.clear();
        for (int i = 0; i < venues.length; i++) {
            venue.addItem(venues[i].getName(), venues[i].getUrl());
        }

        if (venueSelected != null) {
            setValue(venue, venueSelected);
        }
    }

    private void loadVenuesFromServer() {
        VenueLoader.loadVenues(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(loadVenuesButton)) {
            loadVenuesFromServer();
        } else if (event.getSource().equals(addButton)) {
            AddressInputPopup popup = new AddressInputPopup(this);
            popup.center();
        } else if (event.getSource().equals(editButton)) {
            int item = addresses.getSelectedIndex();
            if (item != -1) {
                String address = addresses.getItemText(item);
                String[] parts = address.split("/");
                AddressInputPopup popup = new AddressInputPopup(this);
                popup.setAddress(parts[0]);
                popup.setPort(parts[1]);
                popup.setTtl(parts[2]);
                editing = item;
                popup.center();
            }
        } else if (event.getSource().equals(deleteButton)) {
            int item = addresses.getSelectedIndex();
            if (item != -1) {
                MessagePopup popup = new MessagePopup(
                        "Are you sure you want to delete this address?", this,
                        MessagePopup.QUESTION,
                        MessageResponse.YES, MessageResponse.NO);
                deleting = item;
                popup.center();
            }
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getSource() instanceof MessagePopup) {
            if (response.getResponseCode() == MessageResponse.YES) {
                if (deleting != -1) {
                    addresses.removeItem(deleting);
                    deleting = -1;
                }
            }
        } else if (response.getSource() instanceof AddressInputPopup) {
            AddressInputPopup popup = (AddressInputPopup) response.getSource();
            if (response.getResponseCode() == MessageResponse.OK) {
                String addressString = popup.getAddress() + "/"
                    + popup.getPort() + "/" + popup.getTtl();
                boolean contains = false;
                for (int i = 0; i < addresses.getItemCount(); i++) {
                    String address = addresses.getItemText(i);
                    if (address.equals(addressString)) {
                        contains = true;
                        break;
                    }
                }
                if (editing != -1) {
                    if (!contains) {
                        addresses.setItemText(editing, addressString);
                    } else {
                        addresses.removeItem(editing);
                    }
                    editing = -1;
                } else if (!contains) {
                    addresses.addItem(addressString);
                }
            }
        }
    }

    public String getVenueServer() {
        if (venueSelect.getValue()) {
            return venueServer.getText();
        }
        return null;
    }

    public String getVenue() {
        if (venueSelect.getValue()) {
            int index = venue.getSelectedIndex();
            if (index == -1) {
                return null;
            }
            return venue.getValue(index);
        }
        return null;
    }

    public String[] getAddresses() {
        if (manualSelect.getValue()) {
            if (addresses.getItemCount() == 0) {
                return null;
            }
            String[] addrs = new String[addresses.getItemCount()];
            for (int i = 0; i < addrs.length; i++) {
                addrs[i] = addresses.getValue(i);
            }
            return addrs;
        }
        return null;
    }

    public void setVenueServer(String server) {
        this.venueServer.setText(server);
        loadVenuesFromServer();
        venueSelect.setValue(true, true);
    }

    private boolean setValue(ListBox box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getValue(i).equals(value)) {
                box.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    public void setVenue(String venue) {
        if (!setValue(this.venue, venue)) {
            this.venue.addItem(venue, venue);
        }
        venueSelected = venue;
        venueSelect.setValue(true, true);
    }

    public void setAddresses(String[] addresses) {
        this.addresses.clear();
        for (int i = 0; i < addresses.length; i++) {
            this.addresses.addItem(addresses[i]);
        }
        manualSelect.setValue(true, true);
    }

    public void setEnabled(boolean enabled) {
        venue.setEnabled(enabled);
        venueServer.getTextBox().setEnabled(enabled);
        addresses.setEnabled(enabled);
        addButton.setEnabled(enabled);
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        venueSelect.setEnabled(enabled);
        manualSelect.setEnabled(enabled);
        loadVenuesButton.setEnabled(enabled);
    }
}
