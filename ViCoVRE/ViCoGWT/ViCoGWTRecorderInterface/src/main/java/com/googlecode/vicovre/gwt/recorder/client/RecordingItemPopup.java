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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.datepicker.client.DateBox;
import com.googlecode.vicovre.gwt.client.rest.RestVenueLoader;
import com.googlecode.vicovre.gwt.client.venue.VenueLoader;
import com.googlecode.vicovre.gwt.client.venue.VenuePanel;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;
import com.googlecode.vicovre.gwt.utils.client.NumberBox;
import com.googlecode.vicovre.gwt.utils.client.Space;
import com.googlecode.vicovre.gwt.utils.client.StringDateTimeFormat;

public class RecordingItemPopup extends ModalPopup<FlexTable>
        implements ValueChangeHandler<Boolean>, ClickHandler, VenueLoader,
        ChangeHandler {

    public static final String NO_REPEAT = "None";

    public static final String REPEAT_DAILY = "Daily";

    public static final String REPEAT_WEEKLY = "Weekly";

    public static final String REPEAT_MONTHLY = "Monthly";

    public static final String REPEAT_ANNUALLY = "Annually";

    private static final String NOT_REPEATED = "Not Repeated";

    private static final String REPEATED_DAILY = "Daily";

    private static final String REPEATED_WORK_DAYS = "Work Days";

    private static final String REPEATED_WEEKLY = "Weekly";

    private static final String REPEATED_MONTHLY = "Monthly (specific date)";

    private static final String REPEATED_MONTHLY_WEEK =
        "Monthly (specific week)";

    private static final String REPEATED_ANNUALLY = "Annually (specific date)";

    private static final String REPEATED_ANNUALLY_WEEK =
        "Annually (specific week)";

    private static int lastId = 0;

    private final int id = lastId++;

    private TextBox name = new TextBox();

    private VenuePanel venue = new VenuePanel(this);

    private ListBox repeatFrequency = new ListBox();

    private RadioButton manualStart = null;

    private RadioButton autoStart = null;

    private HorizontalPanel startDatePanel = new HorizontalPanel();

    private HorizontalPanel startTimePanel = new HorizontalPanel();

    private DateBox startDate = new DateBox();

    private TimeBox startTime = new TimeBox(5, 5);

    private RadioButton manualStop = null;

    private RadioButton autoStop = null;

    private HorizontalPanel stopDatePanel = new HorizontalPanel();

    private HorizontalPanel stopTimePanel = new HorizontalPanel();

    private DateBox stopDate = new DateBox();

    private TimeBox stopTime = new TimeBox(5, 5);

    private HorizontalPanel repeatTimePanel = new HorizontalPanel();

    private HorizontalPanel repeatDetailPanel = new HorizontalPanel();

    private TimeBox repeatStart = new TimeBox(5, 5);

    private TimeBox repeatDuration = new TimeBox(5, 5);

    private HorizontalPanel repeatItemFrequencyPanel = new HorizontalPanel();

    private NumberBox repeatItemFrequency = new NumberBox(1);

    private Label repeatItem = new Label();

    private ListBox repeatDayOfWeek = new ListBox();

    private ListBox repeatDayOfMonth = new ListBox();

    private ListBox repeatWeekNumber = new ListBox();

    private ListBox repeatMonth = new ListBox();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private RecordingItem item = null;

    private MessageResponseHandler handler = null;

    private String url = null;

    private MetadataPopup metadataPopup = null;

    public RecordingItemPopup(RecordingItem item, String url,
            MetadataPopup metadataPopup) {
        super(new FlexTable());
        this.item = item;
        this.handler = item;
        this.url = url;
        this.metadataPopup = metadataPopup;
        init();
    }

    public RecordingItemPopup(MessageResponseHandler handler, String url,
            MetadataPopup metadataPopup) {
        super(new FlexTable());
        this.handler = handler;
        this.url = url;
        this.metadataPopup = metadataPopup;
        init();
    }

    public void setRecordingItem(RecordingItem item) {
        this.item = item;
        this.handler = item;
    }

    private void init() {
        FlexTable grid = getWidget();

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

        repeatFrequency.addItem(NOT_REPEATED, NO_REPEAT);
        repeatFrequency.addItem(REPEATED_DAILY, REPEAT_DAILY);
        repeatFrequency.addItem(REPEATED_WORK_DAYS, REPEAT_DAILY);
        repeatFrequency.addItem(REPEATED_WEEKLY, REPEAT_WEEKLY);
        repeatFrequency.addItem(REPEATED_MONTHLY, REPEAT_MONTHLY);
        repeatFrequency.addItem(REPEATED_MONTHLY_WEEK, REPEAT_MONTHLY);
        repeatFrequency.addItem(REPEATED_ANNUALLY, REPEAT_ANNUALLY);
        repeatFrequency.addItem(REPEATED_ANNUALLY_WEEK, REPEAT_ANNUALLY);
        repeatFrequency.setSelectedIndex(0);
        repeatFrequency.addChangeHandler(this);
        repeatDetailPanel.setVisible(false);
        repeatTimePanel.setVisible(false);

        repeatTimePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        repeatTimePanel.add(new Label("Starting at "));
        repeatTimePanel.add(repeatStart);
        repeatTimePanel.add(Space.getHorizontalSpace(5));
        repeatTimePanel.add(new Label("Duration: "));
        repeatTimePanel.add(repeatDuration);
        repeatStart.setSecondsVisible(false);
        repeatDuration.setSecondsVisible(false);
        repeatStart.setHour(now.getHours());
        repeatStart.setMinute(now.getMinutes());
        repeatDuration.setHour(1);

        repeatItemFrequencyPanel.setVerticalAlignment(
                HorizontalPanel.ALIGN_MIDDLE);
        repeatItemFrequencyPanel.add(new Label("Every"));
        repeatItemFrequencyPanel.add(Space.getHorizontalSpace(5));
        repeatItemFrequencyPanel.add(repeatItemFrequency);
        repeatItemFrequencyPanel.add(Space.getHorizontalSpace(5));
        repeatItemFrequencyPanel.add(repeatItem);
        repeatItemFrequency.setWidth("33px");

        manualStart.addValueChangeHandler(this);
        manualStop.addValueChangeHandler(this);
        autoStart.addValueChangeHandler(this);
        autoStop.addValueChangeHandler(this);
        manualStart.setValue(true, true);
        manualStop.setValue(true, true);

        StringDateTimeFormat monthFormat = new StringDateTimeFormat("MMMM");
        StringDateTimeFormat dayFormat = new StringDateTimeFormat("d");
        StringDateTimeFormat weekdayFormat = new StringDateTimeFormat("EEEE");
        for (int i = 0; i < 12; i++) {
            String value = monthFormat.format(new Date(2009, i, 1));
            repeatMonth.addItem("of " + value, value);
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
            repeatDayOfMonth.addItem("on the " + value + ordinal, value);
        }
        for (int i = 0; i < 7; i++) {
            String value = weekdayFormat.format(new Date(2009, 8, 18 + i));
            repeatDayOfWeek.addItem("on " + value, value);
        }
        repeatWeekNumber.addItem("in the First Week", "1");
        repeatWeekNumber.addItem("in the Second Week", "2");
        repeatWeekNumber.addItem("in the Third Week", "3");
        repeatWeekNumber.addItem("in the Forth Week", "4");
        repeatWeekNumber.addItem("in the Last Week", "0");

        repeatDetailPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        repeatDetailPanel.add(repeatItemFrequencyPanel);
        repeatDetailPanel.add(repeatDayOfMonth);
        repeatDetailPanel.add(repeatDayOfWeek);
        repeatDetailPanel.add(repeatWeekNumber);
        repeatDetailPanel.add(repeatMonth);

        grid.setWidget(0, 0, new Label(MetadataPopup.getDisplayName(
                metadataPopup.getPrimaryKey())));
        grid.setWidget(0, 1, name);

        VerticalPanel repeatPanel = new VerticalPanel();
        HorizontalPanel repeatFirstLine = new HorizontalPanel();
        repeatFirstLine.add(repeatFrequency);
        repeatFirstLine.add(repeatTimePanel);
        repeatPanel.add(repeatFirstLine);
        repeatPanel.add(repeatDetailPanel);
        grid.setWidget(1, 0, new Label("Repetition:"));
        grid.setWidget(1, 1, repeatPanel);

        HorizontalPanel startPanel = new HorizontalPanel();
        startPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        startDatePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        startTimePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        startPanel.add(manualStart);
        startPanel.add(autoStart);
        startPanel.add(Space.getHorizontalSpace(5));
        startPanel.add(startDatePanel);
        startPanel.add(Space.getHorizontalSpace(5));
        startPanel.add(startTimePanel);
        startDatePanel.add(new Label("on"));
        startDatePanel.add(startDate);
        startTimePanel.add(new Label("at"));
        startTimePanel.add(startTime);
        grid.setWidget(2, 0, new Label("Start:"));
        grid.setWidget(2, 1, startPanel);

        HorizontalPanel stopPanel = new HorizontalPanel();
        stopPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        stopDatePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        stopTimePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        stopPanel.add(manualStop);
        stopPanel.add(autoStop);
        stopPanel.add(Space.getHorizontalSpace(5));
        stopPanel.add(stopDatePanel);
        stopPanel.add(Space.getHorizontalSpace(5));
        stopPanel.add(stopTimePanel);
        stopDatePanel.add(new Label("on"));
        stopDatePanel.add(stopDate);
        stopTimePanel.add(new Label("at"));
        stopTimePanel.add(stopTime);
        grid.setWidget(3, 0, new Label("Stop:"));
        grid.setWidget(3, 1, stopPanel);

        grid.setWidget(4, 0, new Label("Virtual Venue:"));
        grid.setWidget(4, 1, venue);
        grid.setWidget(5, 0, cancel);
        grid.setWidget(5, 1, ok);

        grid.getColumnFormatter().setWidth(0, "150px");
        grid.getColumnFormatter().setWidth(1, "600px");
        grid.getCellFormatter().setVerticalAlignment(1, 0,
                VerticalPanel.ALIGN_TOP);
        grid.getCellFormatter().setVerticalAlignment(4, 0,
                VerticalPanel.ALIGN_TOP);
        grid.getFlexCellFormatter().setHorizontalAlignment(5, 1,
                HorizontalPanel.ALIGN_RIGHT);
        name.setWidth("100%");
        venue.setWidth("100%");

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }

    private void setValue(ListBox box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getValue(i).equals(value)) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    private void setText(ListBox box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemText(i).equals(value)) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    public void center() {
        if (item != null) {
            this.name.setText(metadataPopup.getPrimaryValue());

            String frequency = item.getRepeatFrequency();
            GWT.log("Frequency = " + frequency);
            if (frequency != null) {
                if (frequency.equals(NO_REPEAT)) {
                    setText(repeatFrequency, NOT_REPEATED);
                } else {
                    repeatItemFrequency.setText(String.valueOf(
                            item.getRepeatItemFrequency()));
                    repeatStart.setHour(item.getRepeatStartHour());
                    repeatStart.setMinute(item.getRepeatStartMinute());
                    repeatDuration.setHour(
                            item.getRepeatDurationMinutes() / 60);
                    repeatDuration.setMinute(item.getRepeatDurationMinutes()
                            - (repeatDuration.getHour() * 60));
                    if (frequency.equals(REPEAT_DAILY)) {
                        if (item.isIgnoreWeekends()) {
                            setText(repeatFrequency, REPEATED_WORK_DAYS);
                            repeatItemFrequency.setNumber(1);
                        } else {
                            setText(repeatFrequency, REPEATED_DAILY);
                        }
                    } else if (frequency.equals(REPEAT_WEEKLY)) {
                        setText(repeatFrequency, REPEATED_WEEKLY);
                        repeatDayOfWeek.setSelectedIndex(
                                item.getRepeatDayOfWeek());
                    } else if (frequency.equals(REPEAT_MONTHLY)) {
                        if (item.getRepeatDayOfMonth() > 0) {
                            setText(repeatFrequency, REPEATED_MONTHLY);
                            repeatDayOfMonth.setSelectedIndex(
                                    item.getRepeatDayOfMonth() - 1);
                        } else {
                            setText(repeatFrequency, REPEATED_MONTHLY_WEEK);
                            repeatDayOfWeek.setSelectedIndex(
                                    item.getRepeatDayOfWeek());
                            setValue(repeatWeekNumber, String.valueOf(
                                    item.getRepeatWeekNumber()));
                        }
                    } else if (frequency.equals(REPEAT_ANNUALLY)) {
                        repeatMonth.setSelectedIndex(item.getRepeatMonth());
                        if (item.getRepeatDayOfMonth() > 0) {
                            setText(repeatFrequency, REPEATED_ANNUALLY);
                            repeatDayOfMonth.setSelectedIndex(
                                    item.getRepeatDayOfMonth() - 1);
                        } else {
                            setText(repeatFrequency, REPEATED_ANNUALLY_WEEK);
                            repeatDayOfWeek.setSelectedIndex(
                                    item.getRepeatDayOfWeek());
                            setValue(repeatWeekNumber, String.valueOf(
                                    item.getRepeatWeekNumber()));
                        }
                    }
                }
            }

            Date startDate = item.getStartDate();
            if (startDate != null) {
                this.startDate.setValue(startDate);
                this.startTime.setHour(startDate.getHours());
                this.startTime.setMinute(startDate.getMinutes());
                autoStart.setValue(true, true);
            } else {
                manualStart.setValue(true, true);
            }
            Date stopDate = item.getStopDate();
            if (stopDate != null) {
                this.stopDate.setValue(stopDate);
                this.stopTime.setHour(stopDate.getHours());
                this.stopTime.setMinute(stopDate.getMinutes());
                autoStop.setValue(true, true);
            } else {
                manualStop.setValue(true, true);
            }
            if (item.getVenueServerUrl() != null) {
                venue.setVenueServer(item.getVenueServerUrl());
                venue.setVenue(item.getVenueUrl());
            } else {
                venue.setAddresses(item.getAddresses());
            }

            setupRepetition();
        }

        super.center();
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

    public String getRepeatFrequency() {
        return repeatFrequency.getValue(repeatFrequency.getSelectedIndex());
    }

    public int getRepeatStartHour() {
        return repeatStart.getHour();
    }

    public int getRepeatStartMinute() {
        return repeatStart.getMinute();
    }

    public int getRepeatDurationMinutes() {
        return (repeatDuration.getHour() * 60) + repeatDuration.getMinute();
    }

    public boolean isIgnoreWeekends() {
        return repeatFrequency.getItemText(
                repeatFrequency.getSelectedIndex()).equals(REPEATED_WORK_DAYS);
    }

    public int getRepeatItemFrequency() {
        return repeatItemFrequency.getNumber();
    }

    public int getRepeatDayOfWeek() {
        return repeatDayOfWeek.getSelectedIndex();
    }

    public int getRepeatDayOfMonth() {
        int index = repeatFrequency.getSelectedIndex();
        String frequency = repeatFrequency.getItemText(index);
        if (frequency.equals(REPEATED_ANNUALLY)
                || frequency.equals(REPEATED_MONTHLY)) {
            return repeatDayOfMonth.getSelectedIndex() + 1;
        }
        return 0;
    }

    public int getRepeatWeekNumber() {
        return Integer.parseInt(repeatWeekNumber.getValue(
                repeatWeekNumber.getSelectedIndex()));
    }

    public int getRepeatMonth() {
        return repeatMonth.getSelectedIndex();
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
            String frequency = repeatFrequency.getValue(
                    repeatFrequency.getSelectedIndex());
            if (event.getSource().equals(manualStart)) {
                startDatePanel.setVisible(false);
                if (frequency.equals(NO_REPEAT)) {
                    startTimePanel.setVisible(false);
                }
            } else if (event.getSource().equals(manualStop)) {
                stopDatePanel.setVisible(false);
                if (frequency.equals(NO_REPEAT)) {
                    stopTimePanel.setVisible(false);
                }
            } else if (event.getSource().equals(autoStart)) {
                startDatePanel.setVisible(true);
                if (frequency.equals(NO_REPEAT)) {
                    startTimePanel.setVisible(true);
                }
            } else if (event.getSource().equals(autoStop)) {
                stopDatePanel.setVisible(true);
                if (frequency.equals(NO_REPEAT)) {
                    stopTimePanel.setVisible(true);
                }
            }
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(ok)) {
            String error = null;
            if (name.getText().isEmpty()) {
                error = "Please enter a name for the recording";
            } else if (venue.getVenueServer() != null) {
                if (venue.getVenueServer().equals("")) {
                    error = "Please enter the name of a venue server";
                } else if (venue.getVenue() == null) {
                    error = "Please select a venue";
                }
            } else if (venue.getAddresses() != null) {
                if (venue.getAddresses().length < 1) {
                    error = "Please enter at least one address";
                }
            } else if (repeatFrequency.getValue(
                    repeatFrequency.getSelectedIndex()).equals(NO_REPEAT)) {
                if (repeatItemFrequency.getNumber() <= 0) {
                    error = "The frequency must be more than 0";
                }
            }
            if (error == null) {
                metadataPopup.setValue(metadataPopup.getPrimaryKey(),
                        name.getText(), false);
                hide();
                handler.handleResponse(new MessageResponse(MessageResponse.OK,
                        this));
            } else {
                MessagePopup errorPopup = new MessagePopup(error, null,
                        MessagePopup.ERROR, MessageResponse.OK);
                errorPopup.center();
            }
        } else if (event.getSource().equals(cancel)) {
            hide();
            handler.handleResponse(new MessageResponse(MessageResponse.CANCEL,
                    this));
        }
    }

    public void loadVenues(VenuePanel panel) {
        RestVenueLoader.loadVenues(panel, url);
    }

    private void setupRepetition() {
        int index = repeatFrequency.getSelectedIndex();
        String frequency = repeatFrequency.getItemText(index);
        String frequencyValue = repeatFrequency.getValue(index);

        boolean repeated = !frequency.equals(NOT_REPEATED);
        repeatTimePanel.setVisible(repeated);
        repeatDetailPanel.setVisible(repeated);
        manualStart.setVisible(!repeated);
        autoStart.setVisible(!repeated);
        manualStart.setValue(!repeated);
        autoStart.setValue(repeated);
        manualStop.setValue(true);
        autoStop.setValue(false);
        startDatePanel.setVisible(repeated);
        stopDatePanel.setVisible(false);
        startTimePanel.setVisible(false);
        stopTimePanel.setVisible(false);

        Date now = new Date();
        startDate.setValue(now);
        stopDate.setValue(now);

        if (!repeated) {
            manualStop.setText("Manually");
            startTime.setHour(now.getHours());
            startTime.setMinute(now.getMinutes());
            stopTime.setHour(now.getHours() + 1);
            stopTime.setMinute(now.getMinutes());
        } else {
            manualStop.setText("Never");
            startTime.setHour(0);
            startTime.setMinute(0);
            stopTime.setHour(0);
            stopTime.setMinute(0);
            repeatItemFrequency.setNumber(1);
            if (!frequency.equals(REPEATED_WORK_DAYS)) {
                repeatItemFrequencyPanel.setVisible(true);
                if (frequency.equals(REPEATED_DAILY)) {
                    repeatItem.setText("Days");
                } else if (frequencyValue.equals(REPEAT_WEEKLY)) {
                    repeatItem.setText("Weeks");
                } else if (frequencyValue.equals(REPEAT_MONTHLY)) {
                    repeatItem.setText("Months");
                } else if (frequencyValue.equals(REPEAT_ANNUALLY)) {
                    repeatItem.setText("Years");
                }
            } else {
                repeatItemFrequencyPanel.setVisible(false);
            }

            if (frequencyValue.equals(REPEAT_DAILY)) {
                repeatDayOfMonth.setVisible(false);
                repeatWeekNumber.setVisible(false);
                repeatDayOfWeek.setVisible(false);
                repeatMonth.setVisible(false);
            } else if (frequencyValue.equals(REPEAT_WEEKLY)) {
                repeatDayOfMonth.setVisible(false);
                repeatWeekNumber.setVisible(false);
                repeatDayOfWeek.setVisible(true);
                repeatMonth.setVisible(false);
            } else if (frequency.equals(REPEATED_MONTHLY)) {
                repeatDayOfMonth.setVisible(true);
                repeatWeekNumber.setVisible(false);
                repeatDayOfWeek.setVisible(false);
                repeatMonth.setVisible(false);
            } else if (frequency.equals(REPEATED_MONTHLY_WEEK)) {
                repeatDayOfMonth.setVisible(false);
                repeatWeekNumber.setVisible(true);
                repeatDayOfWeek.setVisible(true);
                repeatMonth.setVisible(false);
            } else if (frequency.equals(REPEATED_ANNUALLY)) {
                repeatDayOfMonth.setVisible(true);
                repeatWeekNumber.setVisible(false);
                repeatDayOfWeek.setVisible(false);
                repeatMonth.setVisible(true);
            } else if (frequency.equals(REPEATED_ANNUALLY_WEEK)) {
                repeatDayOfMonth.setVisible(false);
                repeatWeekNumber.setVisible(true);
                repeatDayOfWeek.setVisible(true);
                repeatMonth.setVisible(true);
            }
        }
    }

    public void onChange(ChangeEvent event) {
        if (event.getSource().equals(repeatFrequency)) {
            setupRepetition();
        }
    }

}
