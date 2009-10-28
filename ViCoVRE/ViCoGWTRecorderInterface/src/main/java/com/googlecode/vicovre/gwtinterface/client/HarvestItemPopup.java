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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class HarvestItemPopup extends ModalPopup<Grid>
        implements ClickHandler, ChangeHandler {

    /**
     * Updates the source manually.
     */
    public static final String UPDATE_MANUALLY = "Manual";

    /**
     * Updates the source annually.
     */
    public static final String UPDATE_ANUALLY = "Annual";

    /**
     * Updates the source monthly.
     */
    public static final String UPDATE_MONTHLY = "Monthly";

    /**
     * Updates the source weekly.
     */
    public static final String UPDATE_WEEKLY = "Weekly";

    private TextBox name = new TextBox();

    private TextBox url = new TextBox();

    private ListBox format = new ListBox();

    private ListBox updateFrequency = new ListBox();

    private ListBox month = new ListBox();

    private ListBox day = new ListBox();

    private ListBox weekDay = new ListBox();

    private HorizontalPanel timePanel = new HorizontalPanel();

    private ListBox hour = new ListBox();

    private ListBox minute = new ListBox();

    private VenuePanel venue = new VenuePanel();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private MessageResponseHandler handler = null;

    @SuppressWarnings("deprecation")
    public HarvestItemPopup(MessageResponseHandler handler) {
        super(new Grid(6, 2));
        this.handler = handler;
        Grid grid = getWidget();
        grid.setWidget(0, 0, new Label("Name:"));
        grid.setWidget(0, 1, name);
        grid.setWidget(1, 0, new Label("URL:"));
        grid.setWidget(1, 1, url);
        grid.setWidget(2, 0, new Label("Format:"));
        grid.setWidget(2, 1, format);
        grid.setWidget(3, 0, new Label("Update Frequency:"));

        HorizontalPanel updatePanel = new HorizontalPanel();
        updatePanel.add(updateFrequency);
        updatePanel.add(day);
        updatePanel.add(month);
        updatePanel.add(weekDay);
        updatePanel.add(timePanel);

        timePanel.add(new Label(" at "));
        timePanel.add(hour);
        timePanel.add(new Label(":"));
        timePanel.add(minute);
        grid.setWidget(3, 1, updatePanel);

        grid.setWidget(4, 0, new Label("Virtual Venue:"));
        grid.setWidget(4, 1, venue);

        grid.setWidget(5, 0, cancel);
        grid.setWidget(5, 1, ok);

        grid.getCellFormatter().setHorizontalAlignment(5, 1,
                HorizontalPanel.ALIGN_RIGHT);
        grid.getColumnFormatter().setWidth(0, "150px");
        grid.getColumnFormatter().setWidth(1, "600px");
        grid.getCellFormatter().setVerticalAlignment(4, 0,
                VerticalPanel.ALIGN_TOP);
        name.setWidth("100%");
        url.setWidth("100%");
        venue.setWidth("100%");

        format.addItem("MAGIC");

        updateFrequency.addItem("Harvest Manually", UPDATE_MANUALLY);
        updateFrequency.addItem("Harvest Anually", UPDATE_ANUALLY);
        updateFrequency.addItem("Harvest Monthly", UPDATE_MONTHLY);
        updateFrequency.addItem("Harvest Weekly", UPDATE_WEEKLY);

        StringDateTimeFormat monthFormat = new StringDateTimeFormat("MMMM");
        StringDateTimeFormat dayFormat = new StringDateTimeFormat("d");
        StringDateTimeFormat weekdayFormat = new StringDateTimeFormat("EEEE");

        for (int i = 0; i < 12; i++) {
            String value = monthFormat.format(new Date(2009, i, 1));
            month.addItem("of " + value, value);
        }
        for (int i = 1; i <= 31; i++) {
            String ordinal = "th";
            String value = dayFormat.format(new Date(2009, 0, i));
            if (i < 10 || i > 20) {
                if ((i % 10) == 1) {
                    ordinal = "st";
                } else if ((i % 10) == 2) {
                    ordinal = "nd";
                } else if ((i % 10) == 3) {
                    ordinal = "rd";
                }
            }
            day.addItem("on the " + value + ordinal, value);
        }
        for (int i = 0; i < 7; i++) {
            String value = weekdayFormat.format(new Date(2009, 8, 20 + i));
            weekDay.addItem("on " + value, value);
        }

        for (int i = 0; i < 23; i++) {
            String item = String.valueOf(i);
            if (i < 10) {
                item = "0" + i;
            }
            hour.addItem(item);
        }

        for (int i = 0; i < 60; i += 5) {
            String item = String.valueOf(i);
            if (i < 10) {
                item = "0" + i;
            }
            minute.addItem(item);
        }

        month.setVisible(false);
        day.setVisible(false);
        weekDay.setVisible(false);
        timePanel.setVisible(false);
        updateFrequency.addChangeHandler(this);
        updateFrequency.setSelectedIndex(0);

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
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

    private void updateFrequency() {
        int index = updateFrequency.getSelectedIndex();
        String value = updateFrequency.getValue(index);
        if (value.equals("Manual")) {
            month.setVisible(false);
            day.setVisible(false);
            weekDay.setVisible(false);
            timePanel.setVisible(false);
        } else if (value.equals("Annual")) {
            month.setVisible(true);
            day.setVisible(true);
            weekDay.setVisible(false);
            timePanel.setVisible(true);
        } else if (value.equals("Monthly")) {
            month.setVisible(false);
            day.setVisible(true);
            weekDay.setVisible(false);
            timePanel.setVisible(true);
        } else if (value.equals("Weekly")) {
            month.setVisible(false);
            day.setVisible(false);
            weekDay.setVisible(true);
            timePanel.setVisible(true);
        }
    }

    public void onChange(ChangeEvent event) {
        if (event.getSource().equals(updateFrequency)) {
            updateFrequency();
        }
    }

    public String getName() {
        return name.getText();
    }

    public String getUrl() {
        return url.getText();
    }

    public String getFormat() {
        return format.getValue(format.getSelectedIndex());
    }

    public String getUpdateFrequency() {
        return updateFrequency.getValue(updateFrequency.getSelectedIndex());
    }

    public int getMonth() {
        return month.getSelectedIndex();
    }

    public int getDayOfMonth() {
        return day.getSelectedIndex();
    }

    public int getDayOfWeek() {
        return weekDay.getSelectedIndex();
    }

    public int getHour() {
        return hour.getSelectedIndex();
    }

    public int getMinute() {
        return minute.getSelectedIndex() * 5;
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

    public void setUrl(String url) {
        this.url.setText(url);
    }

    private void setValue(ListBox box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getValue(i).equals(value)) {
                box.setSelectedIndex(i);
                break;
            }
        }
    }

    public void setFormat(String format) {
        setValue(this.format, format);
    }

    public void setUpdateFrequency(String frequency) {
        setValue(this.updateFrequency, frequency);
        updateFrequency();
    }

    public void setMonth(int month) {
        this.month.setSelectedIndex(month);
    }

    public void setDayOfMonth(int day) {
        this.day.setSelectedIndex(day);
    }

    public void setDayOfWeek(int weekDay) {
        this.weekDay.setSelectedIndex(weekDay);
    }

    public void setVenueServer(String server) {
        venue.setVenueServer(server);
    }

    public void setVenue(String venue) {
        this.venue.setVenue(venue);
    }

    public void setAddresses(String[] addresses) {
        this.venue.setAddresses(addresses);
    }
}
