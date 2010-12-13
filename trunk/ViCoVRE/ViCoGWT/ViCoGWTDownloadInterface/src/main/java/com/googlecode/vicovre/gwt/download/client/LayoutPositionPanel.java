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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.Space;

public class LayoutPositionPanel extends AbsolutePanel
        implements MouseDownHandler, HasClickHandlers, HasMouseDownHandlers,
        Comparable<LayoutPositionPanel>, ClickHandler, MessageResponseHandler,
        ChangeHandler {

    public static final int HALF_BOX_WIDTH = 5;

    public static final int HALF_BOX_HEIGHT = 5;

    private static final int BOX_WIDTH = 9;

    private static final int BOX_HEIGHT = 9;

    private static final int MIN_WIDTH = 176;

    private static final int MIN_HEIGHT = 144;

    private static final double ASPECT_CIF = 11.0 / 9.0;

    private static final double ASPECT_43 = 4.0 / 3.0;

    private static final double ASPECT_WIDE = 16.0 / 9.0;

    private LayoutPositionHandle resizeBox = new LayoutPositionHandle();

    private AbsolutePanel panel = new AbsolutePanel();

    private Label name = new Label();

    private Label size = new Label();

    private Label position = new Label();

    private Label order = new Label();

    private boolean selected = false;

    private int startX = 0;

    private int startY = 0;

    private Object dragObject = null;

    private AbsolutePanel parent = null;

    private int width = MIN_WIDTH;

    private int height = MIN_HEIGHT;

    private int x = 0;

    private int y = 0;

    private int maxX = 0;

    private int maxY = 0;

    private int resizeWidth = 0;

    private int resizeHeight = 0;

    private int moveX = 0;

    private int moveY = 0;

    private int currentOrder = 0;

    private Button increaseOrderButton = new Button("+");

    private Button decreaseOrderButton = new Button("-");

    private Button editNameButton = new Button("Edit Name");

    private Button deleteButton = new Button("Delete");

    private LayoutCreatorPopup popup = null;

    private ListBox aspectRatio = new ListBox(false);

    private RadioButton hasAudio = new RadioButton("hasAudio", "Main Speaker");

    private RadioButton hasChanges = new RadioButton("hasChanges",
            "Presentation or Screen");

    private boolean lastHasAudio = false;

    private boolean lastHasChanges = false;

    private boolean assignable = true;

    private VerticalPanel centerPanel = new VerticalPanel();

    private FlexTable controlPanel = new FlexTable();

    public LayoutPositionPanel(LayoutCreatorPopup popup, AbsolutePanel parent,
            String name, int x, int y, int maxX,
            int maxY, int order, boolean assignable) {
        this.popup = popup;
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.maxX = maxX;
        this.maxY = maxY;
        this.resizeWidth = width;
        this.resizeHeight = height;
        this.moveX = x;
        this.moveY = y;
        this.currentOrder = order;
        this.assignable = assignable;
        setSize((width + HALF_BOX_WIDTH) + "px",
                (height + HALF_BOX_HEIGHT) + "px");
        DOM.setStyleAttribute(getElement(), "zIndex", "" + order);

        panel.setSize(width + "px", height + "px");
        DOM.setStyleAttribute(panel.getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(panel.getElement(), "borderStyle", "solid");
        DOM.setStyleAttribute(panel.getElement(), "borderColor", "darkGray");
        DOM.setStyleAttribute(panel.getElement(), "backgroundColor", "black");
        add(panel, 0, 0);

        DOM.setStyleAttribute(controlPanel.getElement(), "color", "white");
        controlPanel.setCellPadding(0);
        controlPanel.setCellSpacing(0);
        add(centerPanel, 0, 0);
        centerPanel.setWidth("100%");
        centerPanel.setHeight("100%");
        centerPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        centerPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);

        this.name.setText(name);
        this.name.setWidth("100%");
        this.name.setHorizontalAlignment(Label.ALIGN_CENTER);
        DOM.setStyleAttribute(this.name.getElement(), "color", "white");
        centerPanel.add(this.name);

        Widget space = Space.getHorizontalSpace(5);
        HorizontalPanel editPanel = new HorizontalPanel();
        editPanel.setWidth("100%");
        editPanel.add(editNameButton);
        editPanel.add(space);
        editPanel.add(deleteButton);
        editPanel.setCellWidth(space, "5px");
        editPanel.setCellHorizontalAlignment(editNameButton,
                HorizontalPanel.ALIGN_RIGHT);
        editPanel.setCellHorizontalAlignment(deleteButton,
                HorizontalPanel.ALIGN_LEFT);
        DOM.setStyleAttribute(editNameButton.getElement(), "padding", "0");
        DOM.setStyleAttribute(deleteButton.getElement(), "padding", "0");
        editNameButton.setHeight("17px");
        deleteButton.setHeight("17px");
        editNameButton.setWidth("67px");
        deleteButton.setWidth("67px");
        editNameButton.addClickHandler(this);
        deleteButton.addClickHandler(this);
        controlPanel.setWidget(0, 0, editPanel);
        controlPanel.getFlexCellFormatter().setColSpan(0, 0, 2);

        position.setText(x + " x " + y);
        DOM.setStyleAttribute(position.getElement(), "color", "white");
        controlPanel.setText(1, 0, "Position: ");
        controlPanel.setWidget(1, 1, position);

        size.setText(width + " x " + height);
        DOM.setStyleAttribute(size.getElement(), "color", "white");
        controlPanel.setText(2, 0, "Size: ");
        controlPanel.setWidget(2, 1, size);

        HorizontalPanel orderPanel = new HorizontalPanel();
        orderPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        orderPanel.add(decreaseOrderButton);
        orderPanel.add(this.order);
        orderPanel.add(increaseOrderButton);
        orderPanel.setCellWidth(this.order, "25px");
        orderPanel.setCellHorizontalAlignment(this.order,
                HorizontalPanel.ALIGN_CENTER);
        this.order.setText("" + order);
        DOM.setStyleAttribute(this.order.getElement(), "color", "white");
        decreaseOrderButton.setWidth("10px");
        increaseOrderButton.setWidth("10px");
        decreaseOrderButton.setHeight("17px");
        increaseOrderButton.setHeight("17px");
        decreaseOrderButton.addClickHandler(this);
        increaseOrderButton.addClickHandler(this);
        DOM.setStyleAttribute(decreaseOrderButton.getElement(), "padding", "0");
        DOM.setStyleAttribute(increaseOrderButton.getElement(), "padding", "0");
        controlPanel.setText(3, 0, "Order: ");
        controlPanel.setWidget(3, 1, orderPanel);

        aspectRatio.addItem("None", "0");
        aspectRatio.addItem("Normal", String.valueOf(ASPECT_CIF));
        aspectRatio.addItem("TV (4:3)", String.valueOf(ASPECT_43));
        aspectRatio.addItem("Wide (16:9)", String.valueOf(ASPECT_WIDE));
        aspectRatio.setSelectedIndex(1);
        controlPanel.setText(4, 0, "Ratio: ");
        controlPanel.setWidget(4, 1, aspectRatio);

        DOM.setStyleAttribute(hasChanges.getElement(), "color", "white");
        DOM.setStyleAttribute(hasAudio.getElement(), "color", "white");
        controlPanel.setWidget(5, 0, hasChanges);
        controlPanel.setWidget(6, 0, hasAudio);
        controlPanel.getFlexCellFormatter().setColSpan(5, 0, 2);
        controlPanel.getFlexCellFormatter().setColSpan(6, 0, 2);

        resizeBox.setSize(BOX_WIDTH + "px", BOX_HEIGHT + "px");
        DOM.setStyleAttribute(resizeBox.getElement(),
                "backgroundColor", "white");
        add(resizeBox, 0, 0);
        DOM.setStyleAttribute(resizeBox.getElement(), "left", "");
        DOM.setStyleAttribute(resizeBox.getElement(), "right", "0");
        DOM.setStyleAttribute(resizeBox.getElement(), "top", "");
        DOM.setStyleAttribute(resizeBox.getElement(), "bottom", "0");

        resizeBox.setVisible(false);

        resizeBox.addMouseDownHandler(this);
        aspectRatio.addChangeHandler(this);
        hasAudio.addClickHandler(this);
        hasChanges.addClickHandler(this);
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
        return addDomHandler(handler, MouseDownEvent.getType());
    }

    public void setSelected(boolean selected) {
        resizeBox.setVisible(selected);
        if (selected != this.selected) {
            String borderColor = null;
            if (selected) {
                borderColor = "white";
                centerPanel.add(controlPanel);
                centerPanel.remove(name);
            } else {
                borderColor = "darkGray";
                centerPanel.add(name);
                centerPanel.remove(controlPanel);
            }
            DOM.setStyleAttribute(panel.getElement(), "borderColor",
                    borderColor);
        }

        this.selected = selected;
    }

    public void onMouseDown(MouseDownEvent event) {
        if (selected && (dragObject == null)) {
            startX = event.getClientX();
            startY = event.getClientY();
            dragObject = event.getSource();
        }
    }

    private void resize(int diffWidth, int diffHeight) {
        int newWidth = width + diffWidth;
        int newHeight = height + diffHeight;

        if (newWidth < MIN_WIDTH) {
            newWidth = MIN_WIDTH;
        }
        if (newHeight < MIN_HEIGHT) {
            newHeight = MIN_HEIGHT;
        }

        if ((x + newWidth) > maxX) {
            newWidth = maxX - x;
        }
        if ((y + newHeight) > maxY) {
            newHeight = maxY - y;
        }
        if ((newWidth % 16) != 0) {
            newWidth += 16 - (newWidth % 16);
        }
        if ((newHeight % 16) != 0) {
            newHeight += 16 - (newHeight % 16);
        }

        double ratio = Double.parseDouble(aspectRatio.getValue(
                aspectRatio.getSelectedIndex()));
        if (ratio != 0) {
            newWidth = (int) (newHeight * ratio);
        }

        resizeWidth = newWidth;
        resizeHeight = newHeight;

        setSize((resizeWidth + HALF_BOX_WIDTH) + "px",
                (resizeHeight + HALF_BOX_HEIGHT) + "px");

        size.setText(resizeWidth + " x " + resizeHeight);
        panel.setSize(resizeWidth + "px", resizeHeight + "px");
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        position.setText(x + " x " + y);
        parent.setWidgetPosition(this, x, y);
    }

    private void move(int diffX, int diffY) {
        int newX = x + diffX;
        int newY = y + diffY;

        if (newX < 0) {
            newX = 0;
        }
        if (newY < 0) {
            newY = 0;
        }

        if ((newX + width) > maxX) {
            newX = maxX - width;
        }
        if ((newY + height) > maxY) {
            newY = maxY - height;
        }

        moveX = newX;
        moveY = newY;

        position.setText(moveX + " x " + moveY);
        parent.setWidgetPosition(this, moveX, moveY);

    }

    public void mouseMoved(MouseMoveEvent event) {
        if (selected) {
            int diffX = event.getClientX() - startX;
            int diffY = event.getClientY() - startY;

            if (dragObject == resizeBox) {
                resize(diffX, diffY);
            } else if (dragObject == this) {
                move(diffX, diffY);
            }
        }
    }

    public void mouseUp(MouseUpEvent event) {
        if (selected) {
            if (dragObject != null) {
                width = resizeWidth;
                height = resizeHeight;
                x = moveX;
                y = moveY;
            }
            dragObject = null;
        }
    }

    public int compareTo(LayoutPositionPanel panel) {
        return currentOrder - panel.currentOrder;
    }

    public void setOrder(int order) {
        DOM.setStyleAttribute(getElement(), "zIndex", "" + order);
        this.currentOrder = order;
        this.order.setText("" + order);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == increaseOrderButton) {
            popup.incrementOrder(currentOrder);
        } else if (event.getSource() == decreaseOrderButton) {
            popup.decrementOrder(currentOrder);
        } else if (event.getSource() == deleteButton) {
            popup.deletePosition(currentOrder);
        } else if (event.getSource() == editNameButton) {
            LayoutPositionNamePopup namePopup =
                new LayoutPositionNamePopup(popup, this, name.getText(),
                        currentOrder);
            namePopup.center();
        } else if (event.getSource() == hasChanges) {
            if (hasChanges.getValue() && lastHasChanges) {
                hasChanges.setValue(false);
            }
            lastHasChanges = hasChanges.getValue();
        } else if (event.getSource() == hasAudio) {
            if (hasAudio.getValue() && lastHasAudio) {
                hasAudio.setValue(false);
            }
            lastHasAudio = hasAudio.getValue();
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() != MessageResponse.CANCEL) {
            LayoutPositionNamePopup namePopup = (LayoutPositionNamePopup)
                response.getSource();
            String nameEntered = namePopup.getName();
            name.setText(nameEntered);
        }
    }

    public void setMax(int maxX, int maxY) {
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public String getName() {
        return name.getText();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void onChange(ChangeEvent event) {
        resize(0, 0);
        width = resizeWidth;
        height = resizeHeight;
    }

    public boolean isAssignable() {
        return assignable;
    }

    public boolean hasChanges() {
        return hasChanges.getValue();
    }

    public boolean hasAudio() {
        return hasAudio.getValue();
    }
}
