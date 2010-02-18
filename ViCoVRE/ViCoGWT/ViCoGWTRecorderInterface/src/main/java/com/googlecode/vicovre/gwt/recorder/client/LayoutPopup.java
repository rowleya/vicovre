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
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.LayoutChanger;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.LayoutLoader;

public class LayoutPopup extends ModalPopup<VerticalPanel> implements
        ClickHandler, ChangeHandler {

    private PlayItem item = null;

    private ListBox layoutBox = new ListBox();

    private AbsolutePanel layout = new AbsolutePanel();

    private HashMap<String, ListBox> positions = new HashMap<String, ListBox>();

    private VerticalPanel audioStreams = new VerticalPanel();

    private Vector<CheckBox> audioBoxes = new Vector<CheckBox>();

    private TimeBox startTime = new TimeBox(1, 1);

    private TimeBox stopTime = new TimeBox(1, 1);

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    public LayoutPopup(PlayItem item) {
        super(new VerticalPanel());
        this.item = item;

        layoutBox.setVisibleItemCount(1);
        layout.setWidth("100%");
        layout.setHeight("20px");

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

        HorizontalPanel streamContainer = new HorizontalPanel();
        streamContainer.setWidth("100%");
        streamContainer.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        streamContainer.add(audioStreams);

        VerticalPanel panel = getWidget();
        panel.add(layoutBox);
        panel.add(layout);
        panel.add(timePanel);
        panel.add(streamContainer);
        panel.add(buttonPanel);

        layout.setWidth("0px");
        layout.setHeight("0px");
        DOM.setStyleAttribute(layout.getElement(), "border", "1px solid black");

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        layoutBox.addChangeHandler(this);
    }

    public void center() {
        layoutBox.clear();
        layoutBox.addItem("Select a layout", "");
        for (String layoutName : LayoutLoader.getLayouts().keySet()) {
            layoutBox.addItem(layoutName);
        }
        layoutBox.setSelectedIndex(0);
        List<ReplayLayout> layouts = item.getReplayLayouts();
        if (layouts.size() > 0) {
            setLayout(layouts.get(0));
        } else {
            setLayout(null);
        }
        super.center();
    }

    private String getStreamName(Stream stream) {
        String text = "";
        if (stream.getName() != null) {
            text = stream.getName();
            if (stream.getNote() != null) {
                text += " - " + stream.getNote();
            }
            text += " (" + stream.getSsrc() + ")";
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
            audioStreams.add(new Label("Select which audio streams are used:"));
            for (Stream stream : item.getStreams()) {
                if (stream.getMediaType().equalsIgnoreCase("Audio")) {
                    CheckBox box = new CheckBox(getStreamName(stream));
                    box.setFormValue(stream.getSsrc());
                    box.setValue(true);
                    audioBoxes.add(box);
                    audioStreams.add(box);
                }
            }
            Layout layoutSelected = LayoutLoader.getLayouts().get(value);
            List<LayoutPosition> positionList = layoutSelected.getPositions();
            int maxX = 0;
            int maxY = 0;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            for (LayoutPosition position : positionList) {
                if ((position.getX() + position.getWidth()) > maxX) {
                    maxX = position.getX() + position.getWidth();
                }
                if ((position.getY() + position.getHeight()) > maxY) {
                    maxY = position.getY() + position.getHeight();
                }
                if (position.getX() < minX) {
                    minX = position.getX();
                }
                if (position.getY() < minY) {
                    minY = position.getY();
                }
            }
            int width = maxX + minX;
            int height = maxY + minY + (40 * audioBoxes.size()) + 160;
            double scaleWidth = 1.0;
            double scaleHeight = 1.0;
            if (Window.getClientWidth() < width) {
                scaleWidth = (double) Window.getClientWidth() / width;
            }
            if (Window.getClientHeight() < height) {
                scaleHeight = (double) Window.getClientHeight() / height;
            }
            double scale = Math.min(scaleWidth, scaleHeight);

            for (LayoutPosition position : positionList) {
                Widget widget = null;
                if (position.isAssignable()) {
                    ListBox box = new ListBox();
                    for (Stream stream : item.getStreams()) {
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
            layout.setWidth((int) ((maxX + minX) * scale) + "px");
            layout.setHeight((int) ((maxY + minY) * scale) + "px");
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
            stopTime.setValue(item.getDuration());
        } else {
            setValue(layoutBox, replayLayout.getName());
            updateLayout();
            Layout layout = LayoutLoader.getLayouts().get(
                    replayLayout.getName());
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
            startTime.setValue(replayLayout.getTime());
            stopTime.setValue(replayLayout.getEndTime());
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            LayoutChanger.setLayout(this);
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
        Layout layout = LayoutLoader.getLayouts().get(name);
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

    public List<Map<String, Object>> getLayoutDetails() {
        List<Map<String, Object>> allDetails = new Vector<Map<String,Object>>();
        Map<String, Object> details = new HashMap<String, Object>();
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        long time = startTime.getValue();
        long endTime = stopTime.getValue();
        Layout layout = LayoutLoader.getLayouts().get(name);
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
