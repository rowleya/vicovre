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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.LayoutPosition;
import com.googlecode.vicovre.gwt.client.json.JSONRecording;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemLayoutChanger;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class LayoutPopup extends ModalPopup<VerticalPanel> implements
        ClickHandler, ChangeHandler {

    private PlayItem item = null;

    private ListBox layoutBox = new ListBox();

    private AbsolutePanel layout = new AbsolutePanel();

    private HashMap<String, ListBox> positions = new HashMap<String, ListBox>();

    private VerticalPanel audioStreams = new VerticalPanel();

    private Vector<CheckBox> audioBoxes = new Vector<CheckBox>();

    private TimeBox startTime = new TimeBox(1, 1);

    private long originalStartTime = -1;

    private TimeBox stopTime = new TimeBox(1, 1);

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private HashMap<String, Layout> layouts = new HashMap<String, Layout>();

    private String url = null;

    public LayoutPopup(PlayItem item, Layout[] layouts,
            Layout[] customLayouts, String url) {
        super(new VerticalPanel());
        this.item = item;
        this.url = url;

        HorizontalPanel layoutSelectPanel = new HorizontalPanel();
        layoutSelectPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        layoutSelectPanel.setWidth("100%");
        layoutBox.setVisibleItemCount(1);
        layout.setWidth("100%");
        layout.setHeight("20px");
        layoutBox.addItem("", "");
        for (Layout layout : layouts) {
            layoutBox.addItem(layout.getName());
            this.layouts.put(layout.getName(), layout);
        }
        SelectElement select = layoutBox.getElement().cast();
        OptGroupElement customGroup = Document.get().createOptGroupElement();
        customGroup.setLabel("Custom Layouts");
        for (Layout layout : customLayouts) {
            OptionElement option = Document.get().createOptionElement();
            option.setInnerText(layout.getName());
            customGroup.appendChild(option);
            this.layouts.put(layout.getName(), layout);
        }
        select.appendChild(customGroup);
        layoutBox.setSelectedIndex(0);
        Label layoutTitle = new Label("Select a layout: ");
        layoutSelectPanel.add(layoutTitle);
        layoutSelectPanel.setCellWidth(layoutTitle, "100px");
        layoutSelectPanel.add(layoutBox);

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
        panel.add(layoutSelectPanel);
        panel.add(layout);
        panel.add(timePanel);
        panel.add(new Label("Select which audio streams are used:"));
        panel.add(audioScroll);
        panel.add(buttonPanel);

        layout.setWidth("0px");
        layout.setHeight("0px");
        DOM.setStyleAttribute(layout.getElement(), "border", "1px solid black");

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        layoutBox.addChangeHandler(this);
    }

    public void center() {
        List<ReplayLayout> layouts = item.getReplayLayouts();
        if (layouts.size() > 0) {
            setLayout(layouts.get(0));
        } else {
            setLayout(null);
        }
        super.center();
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
        layout.clear();
        positions.clear();
        audioStreams.clear();
        audioBoxes.clear();
        String value = layoutBox.getValue(layoutBox.getSelectedIndex());
        if (value.equals("")) {
            layout.setWidth("0px");
            layout.setHeight("0px");
        } else {
            for (JSONStream stream : item.getStreams()) {
                if (stream.getMediaType().equalsIgnoreCase("Audio")) {
                    CheckBox box = new CheckBox(getStreamName(stream));
                    box.setFormValue(stream.getSsrc());
                    box.setValue(true);
                    audioBoxes.add(box);
                    audioStreams.add(box);
                }
            }
            Layout layoutSelected = layouts.get(value);
            List<LayoutPosition> positionList = layoutSelected.getPositions();

            int layoutWidth = layoutSelected.getWidth();
            int layoutHeight = layoutSelected.getHeight();
            double width = layoutWidth + 10;
            double height = layoutHeight + 400;
            double scaleWidth = 1.0;
            double scaleHeight = 1.0;
            if (Window.getClientWidth() < width) {
                scaleWidth = Window.getClientWidth() / width;
            }
            if (Window.getClientHeight() < height) {
                scaleHeight = Window.getClientHeight() / height;
            }
            double scale = Math.min(scaleWidth, scaleHeight);

            for (LayoutPosition position : positionList) {
                Widget widget = null;
                if (position.isAssignable()) {
                    ListBox box = new ListBox();
                    for (JSONStream stream : item.getStreams()) {
                        if (stream.getMediaType().equalsIgnoreCase("Video")) {
                            box.addItem(getStreamName(stream),
                                    stream.getSsrc());
                        }
                    }

                    positions.put(position.getName(), box);
                    box.setWidth((int) (position.getWidth() * scale) + "px");
                    widget = box;
                } else {
                    Label label = new Label(position.getName());
                    widget = label;
                }
                HorizontalPanel panel = new HorizontalPanel();
                panel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
                panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
                panel.add(widget);
                panel.setWidth((int) (position.getWidth() * scale) + "px");
                panel.setHeight((int) (position.getHeight() * scale) + "px");
                DOM.setStyleAttribute(panel.getElement(), "border",
                        "1px solid blue");
                layout.add(panel, (int) (position.getX() * scale),
                        (int) (position.getY() * scale));

            }
            layout.setWidth((int) (layoutWidth * scale) + "px");
            layout.setHeight((int) (layoutHeight * scale) + "px");
        }
        if (isShowing()) {
            super.center();
        }
    }

    private void setValue(ListBox box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getValue(i).equals(value)) {
                box.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setLayout(ReplayLayout replayLayout) {
        if (replayLayout == null) {
            setValue(layoutBox, "");
            updateLayout();
            startTime.setValue(0);
            stopTime.setValue(item.getDuration());
            originalStartTime = -1;
        } else {
            setValue(layoutBox, replayLayout.getName());
            updateLayout();
            Layout layout = layouts.get(replayLayout.getName());
            for (LayoutPosition position : layout.getPositions()) {
                String stream = replayLayout.getStream(position.getName());
                if (position.isAssignable()) {
                    setValue(positions.get(position.getName()), stream);
                }
            }
            List<String> audioStreams = replayLayout.getAudioStreams();
            for (CheckBox box : audioBoxes) {
                box.setValue(audioStreams.contains(box.getFormValue()));
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
        }
    }

    public void onChange(ChangeEvent event) {
        updateLayout();
    }

    public String getId() {
        return item.getId();
    }

    public ReplayLayout getLayout() {
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        long time = startTime.getValue();
        long endTime = stopTime.getValue();
        Layout layout = layouts.get(name);
        HashMap<String, String> positionMap = new HashMap<String, String>();
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                positionMap.put(position.getName(),
                        box.getValue(box.getSelectedIndex()));
            }
        }
        Vector<String> audioStreams = new Vector<String>();
        for (CheckBox box : audioBoxes) {
            if (box.getValue()) {
                audioStreams.add(box.getFormValue());
            }
        }

        return new ReplayLayout(name, time, endTime, positionMap, audioStreams);
    }

    public long getLayoutTime() {
        return startTime.getValue();
    }

    public long getOriginalLayoutTime() {
        return originalStartTime;
    }

    public String getLayoutDetailsAsUrl() {
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        String itemUrl = "name=" + name;
        itemUrl += "&endTime=" + stopTime.getValue();

        Layout layout = layouts.get(name);
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                String stream = box.getValue(box.getSelectedIndex());
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

    public List<Map<String, Object>> getLayoutDetails() {
        List<Map<String, Object>> allDetails = new Vector<Map<String,Object>>();
        Map<String, Object> details = new HashMap<String, Object>();
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        long time = startTime.getValue();
        long endTime = stopTime.getValue();
        Layout layout = layouts.get(name);
        HashMap<String, String> positionMap = new HashMap<String, String>();
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                positionMap.put(position.getName(),
                        box.getValue(box.getSelectedIndex()));
            }
        }
        Vector<String> audioStreams = new Vector<String>();
        for (CheckBox box : audioBoxes) {
            if (box.getValue()) {
                audioStreams.add(box.getFormValue());
            }
        }
        details.put("name", name);
        details.put("time", new Long(time).intValue());
        details.put("endTime", new Long(endTime).intValue());
        details.put("positions", positionMap);
        details.put("audioStreams", audioStreams);

        allDetails.add(details);

        return allDetails;
    }

    public String getFolder() {
        return item.getFolder();
    }
}
