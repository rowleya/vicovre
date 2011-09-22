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

package com.googlecode.vicovre.gwt.client.download;

import java.util.Date;
import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.wizard.Wizard;
import com.googlecode.vicovre.gwt.client.wizard.WizardPage;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.StringDateTimeFormat;

public class DownloadAudioPage extends WizardPage implements ClickHandler {

    public static final int INDEX = 5;

    private static final StringDateTimeFormat DATE_FORMAT =
        new StringDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final NumberFormat NUMBER_FORMAT =
        NumberFormat.getFormat("00");

    private VerticalPanel playerPanel = new VerticalPanel();

    private Player player = null;

    private Button downloadButton = new Button("Download");

    private Button setStartButton = new Button(
            "Set Current Time as Start Time");

    private Button setEndButton = new Button(
            "Set Current Time as End Time");

    private Label startTimeLabel = new Label("00:00:00");

    private Label endTimeLabel = new Label("");

    private Button updatePreviewButton = new Button("Update Preview");

    private Button resetButton = new Button("Reset Start/End Times");

    private long previewStartTime = 0;

    private long startTime = 0;

    private long endTime = 0;

    private long duration = 0;

    private String folder = null;

    private String recordingId = null;

    private JSONStream[] streams = null;

    private Vector<String> streamIds = null;

    private ListBox format = new ListBox(false);

    public DownloadAudioPage(String folder,
            String recordingId, JSONStream[] streams) {
        this.folder = folder;
        this.recordingId = recordingId;
        this.streams = streams;

        setWidth("600px");
        setHeight("200px");

        playerPanel.setHorizontalAlignment(ALIGN_CENTER);
        playerPanel.setWidth("100%");
        add(playerPanel);

        FlexTable controlPanel = new FlexTable();
        controlPanel.setWidget(0, 0, new Label("Start Time:"));
        controlPanel.setWidget(0, 1, startTimeLabel);
        controlPanel.setWidget(0, 2, setStartButton);
        controlPanel.setWidget(1, 0, new Label("End Time:"));
        controlPanel.setWidget(1, 1, endTimeLabel);
        controlPanel.setWidget(1, 2, setEndButton);
        controlPanel.setWidget(0, 3, updatePreviewButton);
        controlPanel.setWidget(0, 4, resetButton);
        controlPanel.getFlexCellFormatter().setRowSpan(0, 3, 2);
        controlPanel.getFlexCellFormatter().setRowSpan(0, 4, 2);
        controlPanel.setWidget(2, 0, new Label("Format:"));
        controlPanel.setWidget(2, 1, format);
        controlPanel.setWidget(2, 2, downloadButton);
        setStartButton.addClickHandler(this);
        setEndButton.addClickHandler(this);
        updatePreviewButton.addClickHandler(this);
        resetButton.addClickHandler(this);
        downloadButton.addClickHandler(this);
        setStartButton.setWidth("250px");
        setEndButton.setWidth("250px");
        add(controlPanel);

        format.addItem("MP3 File", "audio/mpeg");
        format.addItem("WMA File", "audio/x-ms-wma");
        format.setSelectedIndex(0);
    }

    public int back(Wizard wizard) {
        return AudioSelectionPage.INDEX;
    }

    public boolean isFirst() {
        return false;
    }

    public boolean isLast() {
        return true;
    }

    public int next(Wizard wizard) {
        return -1;
    }

    private String getUrl(String format) {
        String url = GWT.getModuleBaseURL();
        if (!folder.equals("")) {
            url += folder + "/";
        }

        url += recordingId + "/downloadRecording.do?";
        if (format == null) {
            url += "format=" + URL.encodeComponent(this.format.getValue(
                this.format.getSelectedIndex()));
        } else {
            url += "format=" + URL.encodeComponent(format);
        }

        for (String stream : streamIds) {
            url += "&audio=" + stream;
        }
        return url;
    }

    private void setTimeLabel(long time, Label label) {
        long hours = time / (60 * 60);
        time -= hours * (60 * 60);
        long minutes = time / 60;
        time -= minutes * 60;
        long seconds = time;
        label.setText(NUMBER_FORMAT.format(hours) + ":"
                + NUMBER_FORMAT.format(minutes) + ":"
                + NUMBER_FORMAT.format(seconds));
    }

    public void show(Wizard wizard) {
        streamIds = (Vector<String>) wizard.getAttribute("audioStreams");

        long minStart = Long.MAX_VALUE;
        long maxEnd = 0;
        for (JSONStream stream : streams) {
            if (streamIds.contains(stream.getSsrc())) {
                Date startDate = DATE_FORMAT.parse(stream.getStartTime());
                Date endDate = DATE_FORMAT.parse(stream.getEndTime());
                minStart = Math.min(minStart, startDate.getTime());
                maxEnd = Math.max(maxEnd, endDate.getTime());
            }
        }

        duration = (maxEnd - minStart) / 1000;
        startTime = 0;
        endTime = duration;
        previewStartTime = 0;
        setTimeLabel(startTime, startTimeLabel);
        setTimeLabel(endTime, endTimeLabel);

        playerPanel.clear();
        String url = getUrl("video/x-flv") + "&genspeed=1.5";
        GWT.log("URL = " + url);
        player = new Player(600, 120, url, "offset", true, startTime,
                endTime - startTime, 0);
        playerPanel.add(player);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(setStartButton)) {
            long time = (long) player.getTime() + previewStartTime;
            if (time >= endTime) {
                MessagePopup error = new MessagePopup(
                        "The start time cannot be the same as or greater than"
                        + " the end time", null, MessagePopup.ERROR,
                        MessageResponse.OK);
                error.center();
            } else {
                startTime = time;
                setTimeLabel(startTime, startTimeLabel);
            }
        } else if (event.getSource().equals(setEndButton)) {
            long time = (long) player.getTime() + previewStartTime;
            if (time <= startTime) {
                MessagePopup error = new MessagePopup(
                        "The end time cannot be the same as or less than"
                        + " the start time", null, MessagePopup.ERROR,
                        MessageResponse.OK);
                error.center();
            } else {
                endTime = time;
                setTimeLabel(endTime, endTimeLabel);
            }
        } else if (event.getSource().equals(updatePreviewButton)) {
            player.setTimes(startTime, endTime - startTime);
            previewStartTime = startTime;
        } else if (event.getSource().equals(resetButton)) {
        	startTime = 0; // 00:00:00
        	setTimeLabel(startTime, startTimeLabel);
            endTime = duration;
            setTimeLabel(endTime, endTimeLabel);
            player.setTimes(startTime, endTime - startTime);
            previewStartTime = startTime;
        } else if (event.getSource().equals(downloadButton)) {
            player.stop();
            String url = getUrl(format.getValue(format.getSelectedIndex()));
            url += "&start=" + (startTime * 1000);
            url += "&duration=" + ((endTime - startTime) * 1000);
            url += "&genspeed=0";
            GWT.log("URL = " + url);
            Location.replace(url);
        }
    }

    public int getIndex() {
        return INDEX;
    }

}
