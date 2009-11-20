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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.datepicker.client.DateBox;

public class RecordingItemPopup extends ModalPopup<Grid>
        implements ValueChangeHandler<Boolean>, ClickHandler {

    private static int lastId = 0;

    private final int id = lastId++;

    private TextBox name = new TextBox();

    private TextArea description = new TextArea();

    private VenuePanel venue = new VenuePanel();

    private RadioButton manualStart = null;

    private RadioButton autoStart = null;

    private HorizontalPanel startDatePanel = new HorizontalPanel();

    private DateBox startDate = new DateBox();

    private TimeBox startTime = new TimeBox(5, 5);

    private RadioButton manualStop = null;

    private RadioButton autoStop = null;

    private HorizontalPanel stopDatePanel = new HorizontalPanel();

    private DateBox stopDate = new DateBox();

    private TimeBox stopTime = new TimeBox(5, 5);

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private MessageResponseHandler handler = null;

    public RecordingItemPopup(MessageResponseHandler handler) {
        super(new Grid(6, 2));
        this.handler = handler;
        Grid grid = getWidget();

        manualStart = new RadioButton("start" + id);
        autoStart = new RadioButton("start" + id);
        manualStop = new RadioButton("stop" + id);
        autoStop = new RadioButton("stop" + id);

        manualStart.setText("Manually");
        manualStop.setText("Manually");
        autoStart.setText("Automatically");
        autoStop.setText("Automatically");
        startDate.setFormat(new DateFormat());
        stopDate.setFormat(new DateFormat());
        Date now = new Date();
        startDate.setValue(now);
        stopDate.setValue(now);
        startTime.setSecondsVisible(false);
        stopTime.setSecondsVisible(false);
        startTime.setHour(now.getHours());
        startTime.setMinute(now.getMinutes());
        stopTime.setHour(now.getHours() + 1);
        stopTime.setMinute(now.getMinutes());

        manualStart.addValueChangeHandler(this);
        manualStop.addValueChangeHandler(this);
        autoStart.addValueChangeHandler(this);
        autoStop.addValueChangeHandler(this);
        manualStart.setValue(true, true);
        manualStop.setValue(true, true);

        grid.setWidget(0, 0, new Label("Name:"));
        grid.setWidget(0, 1, name);
        grid.setWidget(1, 0, new Label("Description:"));
        grid.setWidget(1, 1, description);

        HorizontalPanel startPanel = new HorizontalPanel();
        startPanel.add(manualStart);
        startPanel.add(autoStart);
        startPanel.add(startDatePanel);
        startDatePanel.add(new Label(" on "));
        startDatePanel.add(startDate);
        startDatePanel.add(new Label(" at "));
        startDatePanel.add(startTime);
        grid.setWidget(2, 0, new Label("Start:"));
        grid.setWidget(2, 1, startPanel);

        HorizontalPanel stopPanel = new HorizontalPanel();
        stopPanel.add(manualStop);
        stopPanel.add(autoStop);
        stopPanel.add(stopDatePanel);
        stopDatePanel.add(new Label(" on "));
        stopDatePanel.add(stopDate);
        stopDatePanel.add(new Label(" at "));
        stopDatePanel.add(stopTime);
        grid.setWidget(3, 0, new Label("Stop:"));
        grid.setWidget(3, 1, stopPanel);

        grid.setWidget(4, 0, new Label("Virtual Venue:"));
        grid.setWidget(4, 1, venue);
        grid.setWidget(5, 0, cancel);
        grid.setWidget(5, 1, ok);

        grid.getCellFormatter().setHorizontalAlignment(5, 1,
                HorizontalPanel.ALIGN_RIGHT);
        grid.getColumnFormatter().setWidth(0, "150px");
        grid.getColumnFormatter().setWidth(1, "600px");
        grid.getCellFormatter().setVerticalAlignment(1, 0,
                VerticalPanel.ALIGN_TOP);
        grid.getCellFormatter().setVerticalAlignment(4, 0,
                VerticalPanel.ALIGN_TOP);
        name.setWidth("100%");
        description.setWidth("100%");
        venue.setWidth("100%");

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }

    public String getName() {
        return name.getText();
    }

    public String getDescription() {
        return description.getText();
    }

    public Date getStartDate() {
        if (autoStart.getValue()) {
            Date startDate = this.startDate.getValue();
            return new Date(startDate.getYear(), startDate.getMonth(),
                    startDate.getDate(), startTime.getHour(),
                    startTime.getMinute());
        }
        return null;
    }

    public Date getStopDate() {
        if (autoStop.getValue()) {
            Date stopDate = this.stopDate.getValue();
            return new Date(stopDate.getYear(), stopDate.getMonth(),
                    stopDate.getDate(), stopTime.getHour(),
                    stopTime.getMinute());
        }
        return null;
    }

    public String getVenueServer() {
        return venue.getVenueServer();
    }

    public String getVenue() {
        return venue.getVenue();
    }

    public String[] getAddresses() {
        return venue.getAddresses();
    }

    public void setName(String name) {
        this.name.setText(name);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public void setStartDate(Date startDate) {
        if (startDate != null) {
            this.startDate.setValue(startDate);
            this.startTime.setHour(startDate.getHours());
            this.startTime.setMinute(startDate.getMinutes());
            autoStart.setValue(true, true);
        } else {
            manualStart.setValue(true, true);
        }
    }

    public void setStopDate(Date stopDate) {
        if (stopDate != null) {
            this.stopDate.setValue(stopDate);
            this.stopTime.setHour(stopDate.getHours());
            this.stopTime.setMinute(stopDate.getMinutes());
            autoStop.setValue(true, true);
        } else {
            manualStop.setValue(true, true);
        }
    }

    public void setVenueServerUrl(String url) {
        this.venue.setVenueServer(url);
    }

    public void setVenueUrl(String venue) {
        this.venue.setVenue(venue);
    }

    public void setAddresses(String[] addresses) {
        this.venue.setAddresses(addresses);
    }

    public void setDescriptionEditable(boolean editable) {
        description.setEnabled(editable);
    }

    public void setRecording(boolean recording) {
        autoStart.setEnabled(!recording);
        manualStart.setEnabled(!recording);
        startDate.setEnabled(!recording);
        startTime.setEnabled(!recording);
        venue.setEnabled(!recording);
    }

    public void onValueChange(ValueChangeEvent<Boolean> event) {
        if (event.getValue() == true) {
            if (event.getSource().equals(manualStart)) {
                startDatePanel.setVisible(false);
            } else if (event.getSource().equals(manualStop)) {
                stopDatePanel.setVisible(false);
            } else if (event.getSource().equals(autoStart)) {
                startDatePanel.setVisible(true);
            } else if (event.getSource().equals(autoStop)) {
                stopDatePanel.setVisible(true);
            }
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(ok)) {
            hide();
            handler.handleResponse(new MessageResponse(MessageResponse.OK,
                    this));
        } else if (event.getSource().equals(cancel)) {
            hide();
            handler.handleResponse(new MessageResponse(MessageResponse.CANCEL,
                    this));
        }
    }

}
