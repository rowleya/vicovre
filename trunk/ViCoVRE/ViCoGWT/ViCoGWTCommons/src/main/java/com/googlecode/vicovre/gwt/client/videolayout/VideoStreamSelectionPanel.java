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

package com.googlecode.vicovre.gwt.client.videolayout;

import java.util.HashMap;
import java.util.Vector;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.layout.LayoutPosition;
import com.googlecode.vicovre.gwt.utils.client.Space;

public class VideoStreamSelectionPanel extends VerticalPanel {

    private static final int MIN_WIDTH = 600;

    private VideoDragController dragController = null;

    private HorizontalPanel streamList = new HorizontalPanel();

    private Vector<VideoPreviewPanel> previewPanels =
        new Vector<VideoPreviewPanel>();

    private HashMap<String, VerticalPanel> positionPanel =
        new HashMap<String, VerticalPanel>();

    private HashMap<String, String> positionStream =
        new HashMap<String, String>();

    private Vector<VideoDropController> dropControllers =
        new Vector<VideoDropController>();

    private AbsolutePanel layoutPanel = new AbsolutePanel();

    private ScrollPanel streamScroller = new ScrollPanel(streamList);

    private Layout layout = null;

    private String recordingId = null;

    private String folder = null;

    public VideoStreamSelectionPanel(String folder,
            String recordingId, JSONStream[] streams) {
        this.folder = folder;
        this.recordingId = recordingId;

        AbsolutePanel mainPanel = new AbsolutePanel();
        add(mainPanel);
        VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.add(new Label(
            "Drag the video stream from the bottom"
            + " to the position you would like it to be in:"));
        contentPanel.setHorizontalAlignment(ALIGN_CENTER);
        mainPanel.add(contentPanel);
        dragController = new VideoDragController(mainPanel, false);
        dragController.setBehaviorDragProxy(true);

        streamList.add(Space.getHorizontalSpace(5));
        for (int i = 0; i < streams.length; i++) {
            JSONStream stream = streams[i];
            if (stream.getMediaType().equalsIgnoreCase("Video")) {
                VerticalPanel panel = new VerticalPanel();
                panel.setHorizontalAlignment(ALIGN_CENTER);
                VideoPreviewPanel previewPanel = new VideoPreviewPanel(
                        folder, recordingId, stream.getSsrc(), 160, 120);
                panel.add(previewPanel);
                String name = stream.getSsrc();
                if (stream.getName() != null) {
                    name = stream.getName();
                    if (stream.getNote() != null) {
                        name += "<br/>" + stream.getNote();
                    }
                } else if (stream.getCname() != null) {
                    name = stream.getCname();
                }
                panel.add(new HTML(name));
                streamList.add(panel);
                streamList.add(Space.getHorizontalSpace(5));
                previewPanels.add(previewPanel);

                dragController.makeDraggable(previewPanel);
            }
        }

        contentPanel.add(layoutPanel);
        contentPanel.add(Space.getVerticalSpace(5));
        contentPanel.add(streamScroller);
    }

    private void doLayout() {
        positionPanel.clear();
        positionStream.clear();
        layoutPanel.clear();

        if (layout != null) {

            int width = layout.getWidth();
            int height = layout.getHeight() + 200;
            double scaleWidth = 1.0;
            double scaleHeight = 1.0;
            if (Window.getClientWidth() < width) {
                scaleWidth = (double) Window.getClientWidth() / width;
            }
            if (Window.getClientHeight() < height) {
                scaleHeight = (double) Window.getClientHeight() / height;
            }
            double scale = Math.min(scaleWidth, scaleHeight);

            layoutPanel.setWidth((int) (layout.getWidth() * scale) + "px");
            layoutPanel.setHeight((int) (layout.getHeight() * scale) + "px");

            for (LayoutPosition position : layout.getPositions()) {
                if (position.isAssignable()) {
                    VerticalPanel panel = new VerticalPanel();
                    panel.setHorizontalAlignment(ALIGN_CENTER);
                    panel.setVerticalAlignment(ALIGN_MIDDLE);
                    panel.add(new Label(position.getName()));
                    int panelWidth = (int) (position.getWidth() * scale);
                    int panelHeight = (int) (position.getHeight() * scale);
                    panel.setWidth(panelWidth + "px");
                    panel.setHeight(panelHeight + "px");
                    DOM.setStyleAttribute(panel.getElement(), "border",
                            "1px solid blue");
                    layoutPanel.add(panel, (int) (position.getX() * scale),
                            (int) (position.getY() * scale));
                    positionPanel.put(position.getName(), panel);
                    VideoDropController dropController =
                        new VideoDropController(this, position.getName(), panel,
                                folder, recordingId,
                                panelWidth, panelHeight);
                    dropControllers.add(dropController);
                    dragController.registerDropController(dropController);
                }
            }

            int scrollerWidth = (int) (width * scale);
            if (width < MIN_WIDTH) {
                scrollerWidth = MIN_WIDTH;
                if (scrollerWidth > Window.getClientWidth()) {
                    scrollerWidth = Window.getClientWidth();
                }
            }
            streamScroller.setWidth(scrollerWidth + "px");
        }
    }

    private void undoLayout() {
        for (VideoDropController dropController : dropControllers) {
            dragController.unregisterDropController(dropController);
        }
        dropControllers.clear();
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
        if (isAttached()) {
            undoLayout();
            doLayout();
        }
    }

    protected void onLoad() {
        super.onLoad();
        doLayout();
    }

    protected void onUnload() {
        super.onUnload();
        undoLayout();
    }

    public void setPositon(String position, String stream) {
        positionStream.put(position, stream);
    }

    public String verify() {
        String error = null;
        for (String position : positionPanel.keySet()) {
            String stream = positionStream.get(position);
            if (stream == null) {
                error = "You must select a video stream"
                    + " for each position in the layout.";
            }
        }
        return error;
    }

    public HashMap<String, String> getPositionToStreamMap() {
        return positionStream;
    }

}
