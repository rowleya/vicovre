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
import java.util.Vector;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
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
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.recorder.client.rest.FolderLayoutChanger;
import com.googlecode.vicovre.gwt.recorder.client.rest.json.JSONStreamMetadata;

public class DefaultLayoutPopup extends ModalPopup<VerticalPanel> implements
        ClickHandler, ChangeHandler {

    private ListBox layoutBox = new ListBox();

    private AbsolutePanel layout = new AbsolutePanel();

    private HashMap<String, ListBox> positions = new HashMap<String, ListBox>();

    private VerticalPanel audioStreams = new VerticalPanel();

    private Vector<CheckBox> audioBoxes = new Vector<CheckBox>();

    private TimeBox startTime = new TimeBox(1, 1);

    private TimeBox stopTime = new TimeBox(1, 1);

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private HashMap<String, Layout> layouts = new HashMap<String, Layout>();

    private JsArray<JSONStreamMetadata> streams = null;

    private String url = null;

    private String folder = null;

    public DefaultLayoutPopup(Layout[] layouts,
            Layout[] customLayouts, String url, String folder) {
        super(new VerticalPanel());
        this.url = url;
        this.folder = folder;

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

        layoutBox.setSelectedIndex(0);
        updateLayout();
        startTime.setValue(0);
        stopTime.setValue(0);
    }

    public void setStreams(JsArray<JSONStreamMetadata> streams) {
        this.streams = streams;
    }

    public void center() {
        super.center();
    }

    private String getStreamName(JSONStreamMetadata stream) {
        String text = "";
        if (stream.getName() != null) {
            text = stream.getName();
            if (stream.getNote() != null) {
                text += " - " + stream.getNote();
            }
        } else {
            return null;
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
            for (int i = 0; i < streams.length(); i++) {
                JSONStreamMetadata stream = streams.get(i);
                if (stream.getMediaType().equalsIgnoreCase("Audio")) {
                    CheckBox box = new CheckBox(getStreamName(stream));
                    box.setFormValue(String.valueOf(i));
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
                    for (int i = 0; i < streams.length(); i++) {
                        JSONStreamMetadata stream = streams.get(i);
                        if (stream.getMediaType().equalsIgnoreCase("Video")) {
                            box.addItem(getStreamName(stream),
                                    String.valueOf(i));
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

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            FolderLayoutChanger.setLayout(this, url);
        } else if (event.getSource().equals(cancelButton)) {
            hide();
        }
    }

    public void onChange(ChangeEvent event) {
        updateLayout();
    }

    public DefaultLayout getLayout() {
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        long time = startTime.getValue();
        long endTime = stopTime.getValue();
        Layout layout = layouts.get(name);
        HashMap<String, JSONStreamMetadata> positionMap =
            new HashMap<String, JSONStreamMetadata>();
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                int index = Integer.parseInt(box.getValue(
                        box.getSelectedIndex()));
                positionMap.put(position.getName(), streams.get(index));
            }
        }
        Vector<JSONStreamMetadata> audioStreams =
            new Vector<JSONStreamMetadata>();
        for (CheckBox box : audioBoxes) {
            if (box.getValue()) {
                int index = Integer.parseInt(box.getFormValue());
                audioStreams.add(streams.get(index));
            }
        }

        return new DefaultLayout(name, time, endTime, positionMap,
                audioStreams);
    }

    public String getLayoutDetailsAsUrl() {
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        String itemUrl = "name=" + URL.encodeComponent(name);
        itemUrl += "&startTime=" + startTime.getValue();
        itemUrl += "&endTime=" + stopTime.getValue();

        Layout layout = layouts.get(name);
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                int index = Integer.parseInt(box.getValue(
                        box.getSelectedIndex()));
                JSONStreamMetadata stream = streams.get(index);
                itemUrl += "&" + position.getName() + "Name="
                    + URL.encodeComponent(stream.getName());
                if (stream.getNote() != null) {
                    itemUrl += "&" + position.getName() + "Note="
                        + URL.encodeComponent(stream.getNote());
                }
            }
        }
        for (CheckBox box : audioBoxes) {
            if (box.getValue()) {
                int index = Integer.parseInt(box.getFormValue());
                itemUrl += "&audioName="
                    + URL.encodeComponent(streams.get(index).getName());
            }
        }
        return itemUrl;
    }

    public String getFolder() {
        return folder;
    }
}
