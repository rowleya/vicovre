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

import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.Space;


public class StreamsSelectionPage extends WizardPage implements ClickHandler {

    private Vector<CheckBox> streamBoxes = new Vector<CheckBox>();

    private Button download = new Button("Download");

    private Button selectAll = new Button("Select All");

    private Button clearSelection = new Button("Clear Selection");

    private String folder = null;

    private String recordingId = null;

    private String getStreamName(JSONStream stream, String separator) {
        String name = stream.getSsrc();
        if (stream.getName() != null) {
            name = stream.getName();
            if (stream.getNote() != null) {
                name += separator + stream.getNote();
            }
        } else if (stream.getCname() != null) {
            name = stream.getCname();
        }
        return name;
    }

    public StreamsSelectionPage(JSONStream[] streams,
            String folder, String recordingId) {
        this.folder = folder;
        this.recordingId = recordingId;
        add(new Label(
            "Select the streams that you would like to include:"));
        add(new Label("Audio Streams:"));
        VerticalPanel audioPanels = new VerticalPanel();
        ScrollPanel audioScroll = new ScrollPanel(audioPanels);
        DOM.setStyleAttribute(audioScroll.getElement(), "border",
                "1px solid black");
        for (JSONStream stream : streams) {
            if (stream.getMediaType().equalsIgnoreCase("Audio")) {
                CheckBox checkBox = new CheckBox(getStreamName(stream, " - "));
                checkBox.setValue(true);
                checkBox.setFormValue(stream.getSsrc());
                audioPanels.add(checkBox);
                streamBoxes.add(checkBox);
            }
        }
        add(audioScroll);

        add(Space.getVerticalSpace(5));
        add(new Label("Video Streams:"));
        VerticalPanel videoPanels = new VerticalPanel();
        videoPanels.add(Space.getVerticalSpace(5));
        ScrollPanel videoScroll = new ScrollPanel(videoPanels);
        DOM.setStyleAttribute(videoScroll.getElement(), "border",
                "1px solid black");
        HorizontalPanel videoPanel = null;
        String lastCname = "";
        int maxVideoCount = 0;
        int videoCount = 0;
        for (JSONStream stream : streams) {
            if (stream.getMediaType().equalsIgnoreCase("Video")) {
                if (!stream.getCname().equals(lastCname)) {
                    if (videoPanel != null) {
                        videoPanels.add(videoPanel);
                        videoPanels.add(Space.getVerticalSpace(5));
                        maxVideoCount = Math.max(videoCount, maxVideoCount);
                        videoCount = 0;
                    }
                    videoPanel = new HorizontalPanel();
                    videoPanel.add(Space.getHorizontalSpace(5));
                }
                lastCname = stream.getCname();
                videoCount++;
                VideoPreviewPanel preview = new VideoPreviewPanel(
                        folder, recordingId, stream.getSsrc(), 160, 120);
                VerticalPanel panel = new VerticalPanel();
                panel.setHorizontalAlignment(ALIGN_CENTER);
                panel.add(preview);
                CheckBox checkBox = new CheckBox(getStreamName(stream, "<br/>"),
                        true);
                checkBox.setValue(true);
                checkBox.setFormValue(stream.getSsrc());
                panel.add(checkBox);
                streamBoxes.add(checkBox);
                videoPanel.add(panel);
                videoPanel.add(Space.getHorizontalSpace(5));
            }
        }
        if (videoPanel != null) {
            videoPanels.add(videoPanel);
            maxVideoCount = Math.max(videoCount, maxVideoCount);
        }

        int width = 25 + (maxVideoCount * 165);
        audioScroll.setWidth(width + "px");
        audioScroll.setHeight(100 + "px");
        videoScroll.setWidth(width + "px");
        videoScroll.setHeight(300 + "px");
        add(videoScroll);
        add(Space.getVerticalSpace(5));

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.setHorizontalAlignment(ALIGN_CENTER);
        buttonPanel.add(selectAll);
        buttonPanel.add(clearSelection);
        buttonPanel.add(download);
        add(buttonPanel);
        download.addClickHandler(this);
        selectAll.addClickHandler(this);
        clearSelection.addClickHandler(this);
    }

    public int back(Wizard wizard) {
        return Application.FORMAT_SELECTION;
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

    public void show(Wizard wizard) {
        for (CheckBox box : streamBoxes) {
            box.setValue(true);
        }
    }

    public void onClick(ClickEvent event) {
        Object source = event.getSource();
        if (source == selectAll) {
            for (CheckBox box : streamBoxes) {
                box.setValue(true);
            }
        } else if (source == clearSelection) {
            for (CheckBox box : streamBoxes) {
                box.setValue(false);
            }
        } else if (source == download) {
            boolean isStream = false;
            for (CheckBox box : streamBoxes) {
                if (box.getValue()) {
                    isStream = true;
                    break;
                }
            }
            if (!isStream) {
                MessagePopup error = new MessagePopup(
                        "You must select at least one stream.", null,
                        MessagePopup.ERROR, MessageResponse.OK);
                error.center();
            } else {
                String url = GWT.getModuleBaseURL() + folder + "/"
                    + recordingId + "/downloadRecording.do?";
                url += "format=" + URL.encodeComponent("application/x-agvcr");
                for (CheckBox box : streamBoxes) {
                    if (box.getValue()) {
                        url += "&stream=" + box.getFormValue();
                    }
                }
                GWT.log("Getting agvcr download from " + url);
                Window.open(url, "_blank",
                    "status=0,toolbar=0,menubar=0,location=0,resizable=0");
            }
        }
    }

}
