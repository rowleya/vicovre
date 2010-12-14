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
import java.util.Vector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONRecording;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.layout.LayoutPosition;
import com.googlecode.vicovre.gwt.client.videolayout.VideoStreamSelectionPanel;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemLayoutChanger;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class LayoutPopup extends ModalPopup<VerticalPanel> implements
        ClickHandler, MessageResponseHandler {

    private PlayItem item = null;

    private Layout layout = null;

    private VideoStreamSelectionPanel videoSelection = null;

    private VerticalPanel audioStreams = new VerticalPanel();

    private Vector<CheckBox> audioBoxes = new Vector<CheckBox>();

    private TimeBox startTime = new TimeBox(1, 1);

    private long originalStartTime = -1;

    private TimeBox stopTime = new TimeBox(1, 1);

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private Button layoutButton = new Button("Change Layout");

    private HashMap<String, Layout> layouts = new HashMap<String, Layout>();

    private HashMap<String, Layout> customLayouts =
        new HashMap<String, Layout>();

    private String url = null;

    public LayoutPopup(PlayItem item, Layout[] layouts,
            Layout[] customLayouts, String url) {
        super(new VerticalPanel());
        this.item = item;
        this.url = url;

        for (Layout inputLayout : layouts) {
            this.layouts.put(inputLayout.getName(), inputLayout);
        }
        for (Layout inputLayout : customLayouts) {
            this.customLayouts.put(inputLayout.getName(), inputLayout);
        }

        videoSelection = new VideoStreamSelectionPanel(item.getFolder(),
                item.getId(), new JSONStream[0]);

        HorizontalPanel startPanel = new HorizontalPanel();
        startPanel.add(new Label("Start at:"));
        startPanel.add(startTime);
        HorizontalPanel endPanel = new HorizontalPanel();
        endPanel.add(new Label("End at:"));
        endPanel.add(stopTime);
        HorizontalPanel timePanel = new HorizontalPanel();
        timePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        timePanel.setWidth("100%");
        timePanel.add(startPanel);
        timePanel.add(endPanel);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.setCellHorizontalAlignment(okButton,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(cancelButton,
                HorizontalPanel.ALIGN_RIGHT);
        okButton.setWidth("75px");
        cancelButton.setWidth("75px");
        buttonPanel.setWidth("100%");
        buttonPanel.setHeight("20px");

        audioStreams.setWidth("100%");
        audioStreams.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        ScrollPanel audioScroll = new ScrollPanel(audioStreams);
        audioScroll.setHeight("100px");

        VerticalPanel panel = getWidget();
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        panel.add(layoutButton);
        panel.add(videoSelection);
        panel.add(timePanel);
        panel.add(new Label("Select which audio streams are used:"));
        panel.add(audioScroll);
        panel.add(buttonPanel);

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        layoutButton.addClickHandler(this);
    }

    public void center() {
        videoSelection.setStreams(item.getStreams().toArray(new JSONStream[0]));
        audioStreams.clear();
        audioBoxes.clear();
        for (JSONStream stream : item.getStreams()) {
            if (stream.getMediaType().equalsIgnoreCase("Audio")) {
                CheckBox box = new CheckBox(getStreamName(stream));
                box.setFormValue(stream.getSsrc());
                box.setValue(true);
                audioBoxes.add(box);
                audioStreams.add(box);
            }
        }

        List<ReplayLayout> replayLayouts = item.getReplayLayouts();
        if (replayLayouts.size() > 0) {
            setLayout(replayLayouts.get(0));
        } else {
            setLayout(null);
        }
        super.center();
        if (layout == null) {
            LayoutSelectionPopup popup = new LayoutSelectionPopup(
                    layouts.values().toArray(new Layout[0]),
                    customLayouts.values().toArray(new Layout[0]), url, this);
            popup.center();
        }
    }

    private String getTimeString(long value) {
        long remainder = value / 1000;
        long hours = remainder / 3600;
        remainder -= (hours * 3600);
        long minutes = remainder / 60;
        remainder -= (minutes * 60);
        long seconds = remainder;

        String hourString = hours + ":";
        if (hours < 10) {
            hourString = "0" + hourString;
        }
        String minuteString = minutes + ":";
        if (minutes < 10) {
            minuteString = "0" + minuteString;
        }
        String secondString = seconds + "";
        if (seconds < 10) {
            secondString = "0" + secondString;
        }
        return hourString + minuteString + secondString;
    }

    private String getStreamName(JSONStream stream) {
        String text = "";
        if (stream.getName() != null) {
            text = stream.getName();
            if (stream.getNote() != null) {
                text += " - " + stream.getNote();
            }

            Date start = JSONRecording.DATE_FORMAT.parse(stream.getStartTime());
            Date end = JSONRecording.DATE_FORMAT.parse(stream.getEndTime());
            long duration = end.getTime() - start.getTime();
            long startOffset = start.getTime() - item.getStartDate().getTime();
            text += " (" + getTimeString(duration);
            if (startOffset > 0) {
                text += " starting at " + getTimeString(startOffset);
            }
            text += ")";
        } else {
            text = stream.getCname() + " ("
                + stream.getSsrc() + ")";
        }
        return text;
    }

    private void updateLayout() {
        if (layout != null) {
            int maxWidth = layout.getWidth();
            int maxHeight = layout.getHeight() + 480;
            if (maxWidth > Window.getClientWidth()) {
                maxWidth = Window.getClientWidth();
            }
            if (maxHeight > Window.getClientHeight()) {
                maxHeight = Window.getClientHeight();
            }
            videoSelection.setWidth(maxWidth);
            videoSelection.setHeight(maxHeight - 300);
        }
        videoSelection.setLayout(layout);
        if (isShowing()) {
            super.center();
        }
    }

    private void setLayout(ReplayLayout replayLayout) {
        if (replayLayout == null) {
            layout = null;
            updateLayout();
            startTime.setValue(0);
            stopTime.setValue(item.getDuration());
            originalStartTime = -1;
        } else {
            layout = layouts.get(replayLayout.getName());
            if (layout == null) {
                layout = customLayouts.get(replayLayout.getName());
            }
            updateLayout();
            for (LayoutPosition position : layout.getPositions()) {
                String stream = replayLayout.getStream(position.getName());
                if (position.isAssignable()) {
                    videoSelection.setPositon(position.getName(), stream);
                }
            }
            List<String> layoutAudioStreams = replayLayout.getAudioStreams();
            for (CheckBox box : audioBoxes) {
                box.setValue(layoutAudioStreams.contains(box.getFormValue()));
            }
            originalStartTime = replayLayout.getTime();
            startTime.setValue(originalStartTime);
            stopTime.setValue(replayLayout.getEndTime());
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            PlayItemLayoutChanger.setLayout(this, url);
        } else if (event.getSource().equals(cancelButton)) {
            hide();
        } else if (event.getSource().equals(layoutButton)) {
            LayoutSelectionPopup popup = new LayoutSelectionPopup(
                    layouts.values().toArray(new Layout[0]),
                    customLayouts.values().toArray(new Layout[0]), url, this);
            popup.setLayout(layout);
            popup.center();
        }
    }

    public String getId() {
        return item.getId();
    }

    public ReplayLayout getLayout() {
        if (layout == null) {
            return null;
        }
        long time = startTime.getValue();
        long endTime = stopTime.getValue();
        HashMap<String, String> positionMap =
            videoSelection.getPositionToStreamMap();
        Vector<String> layoutAudioStreams = new Vector<String>();
        for (CheckBox box : audioBoxes) {
            if (box.getValue()) {
                layoutAudioStreams.add(box.getFormValue());
            }
        }

        return new ReplayLayout(layout.getName(), time, endTime, positionMap,
                layoutAudioStreams);
    }

    public long getLayoutTime() {
        return startTime.getValue();
    }

    public long getOriginalLayoutTime() {
        return originalStartTime;
    }

    public String getLayoutDetailsAsUrl() {
        if (layout == null) {
            return null;
        }
        String itemUrl = "name=" + layout.getName();
        itemUrl += "&endTime=" + stopTime.getValue();

        HashMap<String, String> positions =
            videoSelection.getPositionToStreamMap();
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                String stream = positions.get(position.getName());
                itemUrl += "&" + position.getName() + "=" + stream;
            }
        }
        for (CheckBox box : audioBoxes) {
            if (box.getValue()) {
                itemUrl += "&audioStream=" + box.getFormValue();
            }
        }
        return itemUrl;
    }

    public String getFolder() {
        return item.getFolder();
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            LayoutSelectionPopup popup =
                (LayoutSelectionPopup) response.getSource();
            layout = popup.getLayout();
            updateLayout();
        } else if (layout == null) {
            hide();
        }
    }
}
