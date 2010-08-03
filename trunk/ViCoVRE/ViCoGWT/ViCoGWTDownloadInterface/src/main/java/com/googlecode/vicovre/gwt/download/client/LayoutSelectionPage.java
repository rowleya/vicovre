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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.MessageResponseHandler;

public class LayoutSelectionPage extends WizardPage implements ClickHandler,
        MouseOverHandler, MouseOutHandler, MessageResponseHandler {

    private static final int LAYOUT_WIDTH = 150;

    private LayoutPreview selection = null;

    private HorizontalPanel currentCustomLayoutPanel = null;

    private VerticalPanel customLayoutPanel = new VerticalPanel();

    private int customLayoutCount = 0;

    private int noLayoutsPerWidth = 0;

    private String width = null;

    private String url = null;

    private String baseUrl = null;

    public LayoutSelectionPage(Layout[] predefinedLayouts,
            Layout[] customLayouts, String url) {
        this.url = url;
        this.customLayoutCount = customLayouts.length;

        noLayoutsPerWidth = Window.getClientWidth() / LAYOUT_WIDTH;
        int maxLayouts = Math.max(predefinedLayouts.length,
                customLayouts.length);
        if (noLayoutsPerWidth > maxLayouts) {
            noLayoutsPerWidth = maxLayouts;
        }
        width = (noLayoutsPerWidth * LAYOUT_WIDTH) + "px";

        Label predefLabel = new Label("Predefined Layouts");
        DOM.setStyleAttribute(predefLabel.getElement(), "fontWeight", "bold");
        add(predefLabel);
        setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        addLayouts(predefinedLayouts, this, width, noLayoutsPerWidth);

        customLayoutPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        DisclosurePanel customLayoutDisclosurePanel =
            new DisclosurePanel("Custom Layouts");
        DOM.setStyleAttribute(
                customLayoutDisclosurePanel.getHeader().getElement(),
                "fontWeight", "bold");
        customLayoutDisclosurePanel.setContent(customLayoutPanel);
        add(customLayoutDisclosurePanel);
        currentCustomLayoutPanel = addLayouts(customLayouts, customLayoutPanel,
                width, noLayoutsPerWidth);

        Button addNewButton = new Button("Create New Layout");
        add(addNewButton);
        addNewButton.addClickHandler(this);
    }

    private HorizontalPanel createNextPanel(String width) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        return panel;
    }

    private LayoutPreview createPreview(Layout layout) {
        LayoutPreview preview = new LayoutPreview(layout, LAYOUT_WIDTH - 5);
        preview.addClickHandler(this);
        preview.addMouseOverHandler(this);
        preview.addMouseOutHandler(this);
        return preview;
    }

    private HorizontalPanel addLayouts(Layout[] layouts, VerticalPanel panel,
            String width, int noLayoutsPerWidth) {
        HorizontalPanel layoutPanel = null;
        int count = 0;
        for (Layout layout : layouts) {
            if ((count % noLayoutsPerWidth) == 0) {
                layoutPanel = createNextPanel(width);
                panel.add(layoutPanel);
            }
            LayoutPreview preview = createPreview(layout);
            layoutPanel.add(preview);
            count += 1;
        }
        return layoutPanel;
    }

    public LayoutPreview addCustomLayout(Layout layout) {
        if ((customLayoutCount % noLayoutsPerWidth) == 0) {
            currentCustomLayoutPanel = createNextPanel(width);
            customLayoutPanel.add(currentCustomLayoutPanel);
        }
        LayoutPreview preview = createPreview(layout);
        currentCustomLayoutPanel.add(preview);
        customLayoutCount += 1;
        return preview;
    }

    public int back(Wizard wizard) {
        return Application.FORMAT_SELECTION;
    }

    public boolean isFirst() {
        return false;
    }

    public boolean isLast() {
        return false;
    }

    public int next(Wizard wizard) {
        if (selection == null) {
            MessagePopup error = new MessagePopup("Please select a layout",
                    null, wizard.getBaseUrl() + MessagePopup.ERROR,
                    MessageResponse.OK);
            error.center();
            return -1;
        }
        wizard.setAttribute("layout", selection.getLayout());
        return Application.AUDIO_SELECTION;
    }

    public void show(Wizard wizard) {
        if (selection != null) {
            selection.setSelected(false);
            selection = null;
        }
        this.baseUrl = wizard.getBaseUrl();
    }

    public void onClick(ClickEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPreview) {
            LayoutPreview preview = (LayoutPreview) source;
            if (selection != null) {
                selection.setSelected(false);
            }
            selection = preview;
            selection.setSelected(true);
        } else {
            LayoutCreatorPopup popup = new LayoutCreatorPopup(url, baseUrl,
                    this);
            popup.center();
        }
    }

    public void onMouseOver(MouseOverEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPreview) {
            LayoutPreview preview = (LayoutPreview) source;
            preview.setHighlight(true);
        }
    }

    public void onMouseOut(MouseOutEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPreview) {
            LayoutPreview preview = (LayoutPreview) source;
            preview.setHighlight(false);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            LayoutCreatorPopup popup = (LayoutCreatorPopup)
                response.getSource();
            Layout layout = popup.getLayout();
            LayoutPreview preview = addCustomLayout(layout);
            if (selection != null) {
                selection.setSelected(false);
            }
            selection = preview;
            selection.setSelected(true);
        }
    }

}
