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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.client.StringDateTimeFormat;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.recorder.client.rest.ChangesAnnotator;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemDeleter;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemEditor;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemLayoutLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemStreamLoader;

public class PlayItem extends SimplePanel implements ClickHandler,
        MessageResponseHandler, Comparable<PlayItem> {

    private static final StringDateTimeFormat DATE_FORMAT =
        new StringDateTimeFormat("dd MMM yyyy 'at' HH:mm");

    private static final NumberFormat TIME_FORMAT =
        NumberFormat.getFormat("00");

    private final Image DELETE = new Image("images/delete.gif");

    private final Image EDIT = new Image("images/edit_meta.gif");

    private final Image PLAY_TO_VENUE = new Image("images/playToVenue.gif");

    private final Image LAYOUT = new Image("images/layout.gif");

    private final Image PLAY = new Image("images/play.gif");

    private final Image ANNOTATE = new Image("images/annotate.gif");

    private FolderPanel folderPanel = null;

    private String id = null;

    private Label name = new Label();

    private Label startDate = new Label();

    private Date start = null;

    private Label duration = new Label();

    private long durationValue = 0;

    private List<JSONStream> streams = null;

    private List<ReplayLayout> replayLayouts = null;

    private PushButton editButton = new PushButton(EDIT);

    private PushButton deleteButton = new PushButton(DELETE);

    private PushButton playToVenueButton = new PushButton(PLAY_TO_VENUE);

    private PushButton editLayoutButton = new PushButton(LAYOUT);

    private PushButton playButton = new PushButton(PLAY);

    private PushButton annotateButton = new PushButton(ANNOTATE);

    private MetadataPopup metadataPopup = null;

    private String url = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public PlayItem(String url, FolderPanel folderPanel, String id,
            MetadataPopup metadataPopup, Layout[] layouts,
            Layout[] customLayouts) {
        this.url = url;
        this.folderPanel = folderPanel;
        this.id = id;
        this.metadataPopup = metadataPopup;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
        metadataPopup.setHandler(this);
        name.setText(metadataPopup.getPrimaryValue());

        setWidth("100%");
        DOM.setStyleAttribute(getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(getElement(), "borderStyle", "solid");

        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(playButton);
        buttons.add(playToVenueButton);
        buttons.add(editButton);
        buttons.add(editLayoutButton);
        buttons.add(annotateButton);
        buttons.add(deleteButton);
        buttons.setWidth("120px");

        DockPanel topLine = new DockPanel();
        topLine.add(startDate, DockPanel.WEST);
        topLine.add(this.name, DockPanel.CENTER);
        topLine.add(buttons, DockPanel.EAST);
        topLine.add(duration, DockPanel.EAST);
        topLine.setWidth("100%");

        topLine.setCellWidth(duration, "100px");
        topLine.setCellWidth(buttons, "120px");
        topLine.setCellWidth(startDate, "160px");
        this.name.setWidth("100%");

        panel.add(topLine);
        add(panel);

        editButton.addClickHandler(this);
        deleteButton.addClickHandler(this);
        playToVenueButton.addClickHandler(this);
        editLayoutButton.addClickHandler(this);
        playButton.addClickHandler(this);
        annotateButton.addClickHandler(this);
    }

    public String getFolder() {
        return folderPanel.getCurrentFolder();
    }

    public String getId() {
        return id;
    }

    public Date getStartDate() {
        return start;
    }

    public void setStartDate(Date date) {
        this.start = date;
        this.startDate.setText(DATE_FORMAT.format(date));
    }

    public static String getTimeText(long duration) {
        long remainder = duration / 1000;
        long hours = remainder / 3600;
        remainder -= hours * 3600;
        long minutes = remainder / 60;
        remainder -= minutes * 60;
        long seconds = remainder;

        return TIME_FORMAT.format(hours) + ":"
                + TIME_FORMAT.format(minutes) + ":"
                + TIME_FORMAT.format(seconds);
    }

    public void setDuration(Long duration) {
        this.durationValue = duration;
        this.duration.setText(getTimeText(duration));
    }

    public long getDuration() {
        return durationValue;
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(editButton)) {
            metadataPopup.center();
        } else if (event.getSource().equals(deleteButton)) {
            PlayItemDeleter.deleteRecording(this, url);
        } else if (event.getSource().equals(playToVenueButton)) {
            PlayToVenuePopup playToVenuePopup = new PlayToVenuePopup(url, this);
            playToVenuePopup.center();
        } else if (event.getSource().equals(editLayoutButton)) {
            LayoutPopup layoutPopup = new LayoutPopup(this, layouts,
                    customLayouts, url);
            ActionLoader loader = new ActionLoader(layoutPopup, 2,
                    "Loading recording details...", null, true, false);
            PlayItemLayoutLoader.loadLayouts(this, loader, url);
            PlayItemStreamLoader.loadStreams(this, loader, url);
        } else if (event.getSource().equals(playButton)) {
            PlayToFlashPopup playPopup = new PlayToFlashPopup(this, layouts,
                    customLayouts);
            ActionLoader loader = new ActionLoader(playPopup, 1,
                    "Loading recording details...", null, true, false);
            PlayItemLayoutLoader.loadLayouts(this, loader, url);
        } else if (event.getSource().equals(annotateButton)) {
            ChangesAnnotator.annotate(url, this, name.getText());
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            name.setText(metadataPopup.getPrimaryValue());
            PlayItemEditor.editPlayItem(this, url);
        }
    }

    public Map<String, Object> getDetails() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("primaryKey", metadataPopup.getPrimaryKey());
        for (String key : metadataPopup.getKeys()) {
            String value = metadataPopup.getRealValue(key);
            if (!value.isEmpty()) {
                metadata.put(key, value);
                metadata.put(key + "Multiline", metadataPopup.isMultiline(key));
                metadata.put(key + "Visible", metadataPopup.isVisible());
                metadata.put(key + "Editable", metadataPopup.isEditable(key));
            }
        }
        return metadata;
    }

    public String getDetailsAsUrl() {
        String itemUrl = "metadataPrimaryKey=" + URL.encodeComponent(
                metadataPopup.getPrimaryKey());
        for (String key : metadataPopup.getKeys()) {
            String value = metadataPopup.getRealValue(key);
            if (!value.isEmpty()) {
                itemUrl += "&metadata" + key + "="
                    + URL.encodeComponent(value);
                itemUrl += "&metadata" + key + "Multiline="
                    + metadataPopup.isMultiline(key);
                itemUrl += "&metadata" + key + "Visible="
                    + metadataPopup.isVisible(key);
                itemUrl += "&metadata" + key + "Editable="
                    + metadataPopup.isEditable(key);
            }
        }
        return itemUrl;
    }

    public int compareTo(PlayItem item) {
        int startDateCompare = start.compareTo(item.start);
        int nameCompare = name.getText().compareTo(item.name.getText());
        if (startDateCompare == 0) {
            return nameCompare;
        }
        return startDateCompare;
    }

    public void setStreams(List<JSONStream> streams) {
        this.streams = streams;
    }

    public List<JSONStream> getStreams() {
        return streams;
    }

    public void setReplayLayouts(List<ReplayLayout> replayLayouts) {
        this.replayLayouts = replayLayouts;
    }

    public List<ReplayLayout> getReplayLayouts() {
        return replayLayouts;
    }

    public void setPlayable(boolean playable) {
        playToVenueButton.setEnabled(playable);
        playButton.setEnabled(playable);
    }

    public void setEditable(boolean editable) {
        editButton.setEnabled(editable);
        deleteButton.setEnabled(editable);
        editLayoutButton.setEnabled(editable);
        annotateButton.setEnabled(editable);
    }
}
