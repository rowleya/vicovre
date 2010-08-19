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

package com.googlecode.vicovre.gwt.download.client;

import java.util.Date;
import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Space;
import com.googlecode.vicovre.gwt.client.StringDateTimeFormat;
import com.googlecode.vicovre.gwt.client.json.JSONStream;

import pl.rmalinowski.gwt2swf.client.ui.SWFWidget;

public class DownloadAudioPage extends WizardPage {

    private static final StringDateTimeFormat DATE_FORMAT =
        new StringDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final NumberFormat NUMBER_FORMAT =
        NumberFormat.getFormat("00");

    private VerticalPanel playerPanel = new VerticalPanel();

    private SWFWidget player = null;

    private Button downloadButton = new Button("Download");

    private Button setStartButton = new Button("Set Start Time");

    private Button setEndButton = new Button("Set End Time");

    private Label startTimeLabel = new Label("00:00:00");

    private Label endTimeLabel = new Label("");

    private long startTime = 0;

    private long endTime = 0;

    private String baseUrl = null;

    private String folder = null;

    private String recordingId = null;

    private JSONStream[] streams = null;

    private Vector<String> streamIds = null;

    private ListBox format = new ListBox(false);

    public DownloadAudioPage(String baseUrl, String folder,
            String recordingId, JSONStream[] streams) {
        this.baseUrl = baseUrl;
        this.folder = folder;
        this.recordingId = recordingId;
        this.streams = streams;

        setWidth("300px");
        setHeight("200px");

        playerPanel.setHorizontalAlignment(ALIGN_CENTER);
        add(playerPanel);

        HorizontalPanel controlPanel = new HorizontalPanel();

        format.addItem("MP3 File", "audio/mpeg");
        format.addItem("WMA File", "audio/x-ms-wma");
        format.setSelectedIndex(0);

        HorizontalPanel downloadPanel = new HorizontalPanel();
        downloadPanel.setHorizontalAlignment(ALIGN_CENTER);
        downloadPanel.add(new Label("Format:"));
        downloadPanel.add(format);
        downloadPanel.add(Space.getHorizontalSpace(5));
        downloadPanel.add(downloadButton);
        add(downloadPanel);
    }

    public int back(Wizard wizard) {
        return Application.AUDIO_SELECTION;
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

        String url = baseUrl;
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

        long duration = (maxEnd - minStart) / 1000;
        startTime = 0;
        endTime = duration;
        setTimeLabel(startTime, startTimeLabel);
        setTimeLabel(endTime, endTimeLabel);

        playerPanel.clear();
        player = new SWFWidget(baseUrl + "jwplayer.swf", 290, 100);
        player.addFlashVar("provider", "sound");
        String url = getUrl("audio/mpeg") + "&genSpeed=1.5";
        player.addFlashVar("file", URL.encodeComponent(url));
        GWT.log("URL = " + url);
        playerPanel.add(player);
    }

}
