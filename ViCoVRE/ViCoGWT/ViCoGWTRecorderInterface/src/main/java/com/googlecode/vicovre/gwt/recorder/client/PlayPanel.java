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
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.recorder.client.rest.DefaultLayoutLoader;

public class PlayPanel extends VerticalPanel implements ClickHandler {

    private VerticalPanel items = new VerticalPanel();

    private Button layoutButton = new Button(
            "Set default layout for all items in this folder");

    private String url = null;

    private FolderPanel folderPanel = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    public PlayPanel(FolderPanel folderPanel, String url, Layout[] layouts,
            Layout[] customLayouts) {
        this.folderPanel = folderPanel;
        this.url = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;

        setWidth("100%");
        setHeight("100%");
        setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

        ScrollPanel scroller = new ScrollPanel(items);
        items.setWidth("100%");
        scroller.setHeight("100%");

        add(layoutButton);
        add(scroller);
        setCellWidth(scroller, "100%");
        setCellHeight(scroller, "100%");

        layoutButton.addClickHandler(this);
        layoutButton.setVisible(false);
    }

    public void addItem(PlayItem item) {
        this.items.add(item);
    }

    public void clear() {
        items.clear();
    }

    public void onClick(ClickEvent event) {
        DefaultLayoutPopup popup =
            new DefaultLayoutPopup(layouts, customLayouts, url,
                    folderPanel.getCurrentFolder());
        DefaultLayoutLoader.loadLayouts(folderPanel.getCurrentFolder(),
                popup, url);
    }

    public void setUserIsAdministrator(boolean isAdministrator) {
        layoutButton.setVisible(isAdministrator);
    }
}
