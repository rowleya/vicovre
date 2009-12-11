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

import java.util.HashSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;

public class SliderBar extends AbsolutePanel implements MouseMoveHandler,
        MouseDownHandler, MouseUpHandler {

    private static final int SLIDER_WIDTH = 11;

    private FlexTable table = new FlexTable();

    private Label bar = new Label();

    private Label slider = new Label();

    private boolean dragging = false;

    private HashSet<SlideChangeHandler> changeHandlers =
        new HashSet<SlideChangeHandler>();

    public SliderBar() {
        int space = SLIDER_WIDTH / 2;
        slider.setWidth(SLIDER_WIDTH + "px");
        slider.setHeight("100%");

        table.setWidth("100%");
        table.setHeight("100%");
        bar.setWidth("100%");
        bar.setHeight("100%");
        table.setWidget(1, 1, bar);
        table.getFlexCellFormatter().setColSpan(0, 0, 3);
        table.getCellFormatter().setHeight(0, 0, "2px");
        table.getFlexCellFormatter().setColSpan(2, 0, 3);
        table.getCellFormatter().setHeight(2, 0, "2px");
        table.getCellFormatter().setWidth(1, 0, space + "px");
        table.getCellFormatter().setWidth(1, 2, space + "px");
        table.setCellPadding(0);
        table.setCellSpacing(0);

        add(table, 0, 0);
        add(slider, 0, 0);

        DOM.setStyleAttribute(slider.getElement(), "opacity", "0.80");
        DOM.setStyleAttribute(slider.getElement(), "filter",
                " alpha(opacity=80)");
        DOM.setStyleAttribute(slider.getElement(), "backgroundColor", "blue");
        DOM.setStyleAttribute(bar.getElement(), "backgroundColor", "black");

        bar.addMouseMoveHandler(this);
        bar.addMouseUpHandler(this);
        slider.addMouseDownHandler(this);
    }

    public void addSlideChangeHandler(SlideChangeHandler handler) {
        changeHandlers.add(handler);
    }

    public void removeSlideChangeHandler(SlideChangeHandler handler) {
        changeHandlers.remove(handler);
    }

    private void setSliderPosition(int x) {
        x -= (SLIDER_WIDTH / 2);
        int leftMost = getWidgetLeft(table);
        int rightMost = leftMost + bar.getOffsetWidth();
        if (x < leftMost) {
            x = leftMost;
        } else if (x > rightMost) {
            x = rightMost;
        }
        setWidgetPosition(slider, x, getWidgetTop(slider));
    }

    private float getSliderPosition(int x) {
        x -= (SLIDER_WIDTH / 2);
        int leftMost = getWidgetLeft(table);
        int rightMost = leftMost + bar.getOffsetWidth();
        if (x < leftMost) {
            x = leftMost;
        } else if (x > rightMost) {
            x = rightMost;
        }
        return (float) (x - leftMost) / (rightMost - leftMost);
    }

    public void onMouseMove(MouseMoveEvent event) {
        if (dragging) {
            int x = event.getRelativeX(getElement());
            setSliderPosition(x);
            float position = getSliderPosition(x);
            for (SlideChangeHandler handler : changeHandlers) {
                handler.slideValueChanging(position);
            }
        }
    }

    public void onMouseDown(MouseDownEvent event) {
        dragging = true;
        DOM.setCapture(bar.getElement());
        DOM.setStyleAttribute(slider.getElement(), "opacity", "0.50");
        DOM.setStyleAttribute(slider.getElement(), "filter",
                " alpha(opacity=50)");
    }

    public void onMouseUp(MouseUpEvent event) {
        int x = event.getRelativeX(getElement());
        if (dragging) {
            dragging = false;
            DOM.releaseCapture(bar.getElement());
            DOM.setStyleAttribute(slider.getElement(), "opacity", "0.80");
            DOM.setStyleAttribute(slider.getElement(), "filter",
                    " alpha(opacity=80)");
        } else {
            setSliderPosition(x);
        }
        float position = getSliderPosition(x);
        for (SlideChangeHandler handler : changeHandlers) {
            handler.slideValueChanged(position);
        }
    }

    public void setPosition(float position) {
        int leftMost = getWidgetLeft(table);
        int rightMost = leftMost + bar.getOffsetWidth();
        int x = (int) ((position * (rightMost - leftMost)) - leftMost);
        setSliderPosition(x);
    }

    public float getPosition() {
        int x = bar.getAbsoluteLeft() - getAbsoluteLeft();
        return getSliderPosition(x);
    }
}
