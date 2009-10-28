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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.LayoutChanger;

public class LayoutPopup extends ModalPopup<VerticalPanel> implements
        ClickHandler, ChangeHandler {

    private PlayItem item = null;

    private HashMap<String, Layout> layouts = null;

    private List<Stream> streams = null;

    private ListBox layoutBox = new ListBox();

    private AbsolutePanel layout = new AbsolutePanel();

    private HashMap<String, ListBox> positions = new HashMap<String, ListBox>();

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    public LayoutPopup(HashMap<String, Layout> layouts, PlayItem item) {
        super(new VerticalPanel());
        this.item = item;
        this.layouts = layouts;

        layoutBox.setVisibleItemCount(1);
        layoutBox.addItem("Select a layout", "");
        for (String layoutName : layouts.keySet()) {
            layoutBox.addItem(layoutName);
        }
        layoutBox.setSelectedIndex(0);
        layout.setWidth("100%");
        layout.setHeight("20px");

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

        VerticalPanel panel = getWidget();
        panel.add(layoutBox);
        panel.add(layout);
        panel.add(buttonPanel);

        layout.setWidth("0px");
        layout.setHeight("0px");
        DOM.setStyleAttribute(layout.getElement(), "border", "1px solid black");

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        layoutBox.addChangeHandler(this);
    }

    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    private void updateLayout() {
        layout.clear();
        positions.clear();
        String value = layoutBox.getValue(layoutBox.getSelectedIndex());
        if (value.equals("")) {
            layout.setWidth("0px");
            layout.setHeight("0px");
        } else {
            Layout layoutSelected = layouts.get(value);
            List<LayoutPosition> positionList = layoutSelected.getPositions();
            int maxX = 0;
            int maxY = 0;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            for (LayoutPosition position : positionList) {
                Widget widget = null;
                if (position.isAssignable()) {
                    ListBox box = new ListBox();
                    for (Stream stream : streams) {
                        if (stream.getMediaType().equalsIgnoreCase("Video")) {
                            String text = "";
                            if (stream.getName() != null) {
                                text = stream.getName();
                                if (stream.getNote() != null) {
                                    text += " - " + stream.getNote();
                                }
                            } else {
                                text = stream.getCname() + "("
                                    + stream.getSsrc() + ")";
                            }
                            box.addItem(text, stream.getSsrc());
                        }
                    }
                    positions.put(position.getName(), box);
                    box.setWidth(position.getWidth() + "px");
                    widget = box;
                } else {
                    Label label = new Label(position.getName());
                    widget = label;
                }
                HorizontalPanel panel = new HorizontalPanel();
                panel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
                panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
                panel.add(widget);
                panel.setWidth(position.getWidth() + "px");
                panel.setHeight(position.getHeight() + "px");
                DOM.setStyleAttribute(panel.getElement(), "border",
                        "1px solid blue");
                layout.add(panel, position.getX(), position.getY());
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
            layout.setWidth((maxX + minX) + "px");
            layout.setHeight((maxY + minY) + "px");
        }
        if (isShowing()) {
            center();
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

    public void setLayout(ReplayLayout replayLayout) {
        if (replayLayout == null) {
            setValue(layoutBox, "");
            updateLayout();
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
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            item.setLayout(getLayout());
            LayoutChanger.setLayout(this);
        } else if (event.getSource().equals(cancelButton)) {
            setLayout(item.getLayout());
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
        long time = 0;
        Layout layout = layouts.get(name);
        HashMap<String, String> positionMap = new HashMap<String, String>();
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                positionMap.put(position.getName(),
                        box.getValue(box.getSelectedIndex()));
            }
        }

        return new ReplayLayout(name, time, positionMap);
    }

    public List<Map<String, Object>> getLayoutDetails() {
        List<Map<String, Object>> allDetails = new Vector<Map<String,Object>>();
        Map<String, Object> details = new HashMap<String, Object>();
        String name = layoutBox.getItemText(layoutBox.getSelectedIndex());
        if ((name == null) || name.equals("")) {
            return null;
        }
        long time = 0;
        Layout layout = layouts.get(name);
        HashMap<String, String> positionMap = new HashMap<String, String>();
        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                ListBox box = positions.get(position.getName());
                positionMap.put(position.getName(),
                        box.getValue(box.getSelectedIndex()));
            }
        }
        details.put("name", name);
        details.put("time", new Long(time).intValue());
        details.put("positions", positionMap);

        allDetails.add(details);

        return allDetails;
    }
}
