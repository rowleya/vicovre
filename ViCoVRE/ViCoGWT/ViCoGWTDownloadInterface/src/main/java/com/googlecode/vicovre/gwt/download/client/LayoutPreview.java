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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.LayoutPosition;

public class LayoutPreview extends AbsolutePanel implements HasClickHandlers,
        HasMouseOverHandlers, HasMouseOutHandlers {

    private static final int EXTRA_WIDTH = 10;

    private static final int EXTRA_HEIGHT = 25;

    private AbsolutePanel highlight = new AbsolutePanel();

    private AbsolutePanel select = new AbsolutePanel();

    private Layout layout = null;

    public LayoutPreview(Layout layout, int widthIn) {
        this.layout = layout;

        int width = widthIn - EXTRA_WIDTH;
        int layoutWidth = layout.getWidth();
        int layoutHeight = layout.getHeight();
        double scale = (double) width / layoutWidth;
        GWT.log("Scale = " + scale);
        int height = (int) (layoutHeight * scale);

        select.setWidth((width + EXTRA_WIDTH) + "px");
        select.setHeight((height + EXTRA_HEIGHT) + "px");
        DOM.setStyleAttribute(select.getElement(), "backgroundColor", "orange");
        select.setVisible(false);
        add(select, 0, 0);

        VerticalPanel content = new VerticalPanel();
        add(content, 5, 5);

        AbsolutePanel layoutPanel = new AbsolutePanel();
        layoutPanel.setWidth(width + "px");
        layoutPanel.setHeight(height + "px");
        DOM.setStyleAttribute(layoutPanel.getElement(),
                "backgroundColor", "grey");

        for (LayoutPosition position : layout.getPositions()) {
            if (position.isAssignable()) {
                AbsolutePanel panel = new AbsolutePanel();
                panel.setWidth((int) (position.getWidth() * scale) + "px");
                panel.setHeight((int) (position.getHeight() * scale) + "px");
                DOM.setStyleAttribute(panel.getElement(), "backgroundColor",
                        "black");
                layoutPanel.add(panel, (int) (position.getX() * scale),
                        (int) (position.getY() * scale));
            }
        }
        content.add(layoutPanel);

        Label label = new Label(layout.getName());
        label.setWidth(width + "px");
        label.setHorizontalAlignment(Label.ALIGN_CENTER);
        content.add(label);
        setWidth((width + EXTRA_WIDTH) + "px");
        setHeight((height + EXTRA_HEIGHT) + "px");

        highlight.setWidth((width + EXTRA_WIDTH) + "px");
        highlight.setHeight((height + EXTRA_HEIGHT) + "px");
        DOM.setStyleAttribute(highlight.getElement(), "backgroundColor",
                "yellow");
        DOM.setStyleAttribute(highlight.getElement(), "opacity", "0.70");
        DOM.setStyleAttribute(highlight.getElement(), "filter",
                " alpha(opacity=70)");
        highlight.setVisible(false);
        add(highlight, 0, 0);
    }

    public void setHighlight(boolean highlight) {
        this.highlight.setVisible(highlight);
    }

    public boolean isHighlighted() {
        return highlight.isVisible();
    }

    public void setSelected(boolean selected) {
        this.select.setVisible(selected);
    }

    public boolean isSelected() {
        return select.isVisible();
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
        return addDomHandler(handler, MouseOverEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return addDomHandler(handler, MouseOutEvent.getType());
    }

    public Layout getLayout() {
        return layout;
    }
}
