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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemCreator;

public class RecordPanel extends VerticalPanel implements ClickHandler {

    private Button createButton = new Button("Create New Recording");

    private VerticalPanel recordings = new VerticalPanel();

    private FolderPanel folderPanel = null;

    private PlayPanel playPanel = null;

    private String url = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public RecordPanel(FolderPanel folderPanel, PlayPanel playPanel,
            String url, Layout[] layouts, Layout[] customLayouts) {
        this.url = url;
        this.folderPanel = folderPanel;
        this.playPanel = playPanel;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
        setWidth("100%");
        setHeight("100%");
        setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

        ScrollPanel scroller = new ScrollPanel(recordings);
        recordings.setWidth("100%");
        scroller.setHeight("100%");
        Label label = new Label("Existing Recordings");
        add(createButton);
        add(label);
        add(scroller);
        setCellHeight(createButton, "20px");
        setCellWidth(scroller, "100%");
        setCellHeight(scroller, "100%");
        setCellHorizontalAlignment(createButton, ALIGN_CENTER);

        createButton.addClickHandler(this);
    }

    public void addItem(RecordingItem item) {
        this.recordings.add(item);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(createButton)) {
            RecordingItemCreator.createRecordingItem(folderPanel, playPanel,
                    this, url, layouts, customLayouts);
        }
    }

    public void clear() {
        recordings.clear();
    }
}
