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

import java.util.List;
import java.util.Vector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.LayoutPosition;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;
import com.googlecode.vicovre.gwt.utils.client.Space;

public class LayoutCreatorPopup extends ModalPopup<LayoutPositionBox>
        implements MouseMoveHandler, MouseUpHandler, MouseDownHandler,
        ClickHandler, MessageResponseHandler {

    private static final int INITIAL_WIDTH = 640;

    private static final int INITIAL_HEIGHT = 480;

    private static final int MIN_BUTTON_WIDTH = 330;

    private static final int MIN_WIDTH = 110;

    private static final int MIN_HEIGHT = 110;

    private static final int CONTROL_PANEL_OFFSET = 25;

    private static final int CONFIRM_PANEL_OFFSET = 55;

    private static final int EXTRA_WIDTH = 10;

    private static final int EXTRA_HEIGHT = 85;

    private static final int DEFAULT_BORDER_SIZE = 0;

    private LayoutPositionBox mainPanel = new LayoutPositionBox();

    private LayoutPositionPanel currentlySelectedPanel = null;

    private Vector<LayoutPositionPanel> panels =
        new Vector<LayoutPositionPanel>();

    private int panelWidth = INITIAL_WIDTH + EXTRA_WIDTH;

    private int width = INITIAL_WIDTH;

    private int height = INITIAL_HEIGHT;

    private LayoutPositionHandle resizeHandle = new LayoutPositionHandle();

    private boolean dragging = false;

    private int startX = 0;

    private int startY = 0;

    private int resizeWidth = width;

    private int resizeHeight = height;

    private Label panelSize = new Label();

    private VerticalPanel controlPanel = new VerticalPanel();

    private HorizontalPanel confirmPanel = new HorizontalPanel();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private Button create = new Button("Create Position");

    private Button crop = new Button("Crop to Fit");

    private NumberBox borderSize = new NumberBox(DEFAULT_BORDER_SIZE);

    private Button borderSizeIncrease = new Button("+");

    private Button borderSizeDecrease = new Button("-");

    private MessageResponseHandler handler = null;

    private String url = null;

    private Layout layout = null;

    public LayoutCreatorPopup(String url, MessageResponseHandler handler) {
        super(new LayoutPositionBox());
        this.url = url;
        this.handler = handler;
        LayoutPositionBox panel = getWidget();

        panel.setWidth(panelWidth + "px");
        panel.setHeight((height + EXTRA_HEIGHT) + "px");

        panel.add(mainPanel, EXTRA_WIDTH / 2, EXTRA_WIDTH / 2);
        mainPanel.setWidth(width + "px");
        mainPanel.setHeight(height + "px");
        panelSize.setText(width + " x " + height);
        panelSize.setVisible(false);
        DOM.setStyleAttribute(mainPanel.getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(mainPanel.getElement(), "borderStyle", "solid");
        DOM.setStyleAttribute(mainPanel.getElement(), "borderColor", "black");

        resizeHandle.setSize("9px", "9px");
        panel.add(resizeHandle, width + 1, height + 1);
        DOM.setStyleAttribute(resizeHandle.getElement(), "backgroundColor",
                "black");
        DOM.setStyleAttribute(mainPanel.getElement(), "backgroundColor",
                "gray");

        resizeHandle.addMouseDownHandler(this);
        mainPanel.addMouseDownHandler(this);
        mainPanel.addMouseMoveHandler(this);
        mainPanel.addMouseUpHandler(this);
        panel.addMouseMoveHandler(this);
        panel.addMouseUpHandler(this);
        addDomHandler(this, MouseUpEvent.getType());
        addDomHandler(this, MouseMoveEvent.getType());

        HorizontalPanel buttonPanel = new HorizontalPanel();
        borderSize.setWidth("30px");
        controlPanel.setWidth("100%");
        controlPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        controlPanel.add(buttonPanel);
        buttonPanel.add(create);
        buttonPanel.add(Space.getHorizontalSpace(5));
        buttonPanel.add(crop);
        buttonPanel.add(Space.getHorizontalSpace(5));
        buttonPanel.add(new Label("Border Size: "));
        buttonPanel.add(borderSizeDecrease);
        buttonPanel.add(borderSize);
        buttonPanel.add(borderSizeIncrease);
        controlPanel.add(buttonPanel);
        panel.add(controlPanel, 0, height + CONTROL_PANEL_OFFSET);

        confirmPanel.setWidth(width + "px");
        confirmPanel.add(ok);
        confirmPanel.add(cancel);
        confirmPanel.setCellHorizontalAlignment(ok, HorizontalPanel.ALIGN_LEFT);
        confirmPanel.setCellHorizontalAlignment(cancel,
                HorizontalPanel.ALIGN_RIGHT);
        panel.add(confirmPanel, EXTRA_WIDTH / 2, height + CONFIRM_PANEL_OFFSET);

        create.addClickHandler(this);
        crop.addClickHandler(this);
        ok.addClickHandler(this);
        cancel.addClickHandler(this);
        borderSizeDecrease.addClickHandler(this);
        borderSizeIncrease.addClickHandler(this);
    }

    public LayoutPositionPanel addPosition(String name) {
        LayoutPositionPanel panel = new LayoutPositionPanel(this, mainPanel,
                name, 0, 0, width, height, panels.size(), true);
        panels.add(panel);
        mainPanel.add(panel, panel.getX(), panel.getY());
        panel.addMouseDownHandler(this);
        return panel;
    }

    private void setSize(int w, int h) {
        LayoutPositionBox panel = getWidget();
        panelWidth = w + EXTRA_WIDTH;
        if (panelWidth < MIN_BUTTON_WIDTH) {
            panelWidth = MIN_BUTTON_WIDTH;
            int mainPanelPos = (panelWidth - w) / 2;
            panel.setWidgetPosition(mainPanel, mainPanelPos, 5);
            panel.setWidgetPosition(resizeHandle, w + mainPanelPos - 3, h + 1);
        } else {
            panel.setWidgetPosition(mainPanel, 5, 5);
            panel.setWidgetPosition(resizeHandle, w + 1, h + 1);
        }
        panel.setWidth(panelWidth + "px");
        panel.setHeight((h + EXTRA_HEIGHT) + "px");
        mainPanel.setWidth(w + "px");
        mainPanel.setHeight(h + "px");
        panelSize.setText(w + " x " + h);
        panel.setWidgetPosition(controlPanel, 0, h + CONTROL_PANEL_OFFSET);
        panel.setWidgetPosition(confirmPanel, EXTRA_WIDTH / 2,
                h + CONFIRM_PANEL_OFFSET);
        confirmPanel.setWidth((panelWidth - EXTRA_WIDTH) + "px");

        if (panel.getWidgetIndex(panelSize) != -1) {
            int sizeX = panel.getWidgetLeft(resizeHandle) - 61;
            int sizeY = panel.getWidgetTop(resizeHandle) + 5;
            panel.setWidgetPosition(panelSize, sizeX, sizeY);
        }
    }

    private int checkWidth(int w) {
        int newWidth = w;
        if (newWidth > (Window.getClientWidth() - 20)) {
            newWidth = Window.getClientWidth() - 20;
        }
        if (newWidth < MIN_WIDTH) {
            newWidth = MIN_HEIGHT;
        }
        if (newWidth % 16 != 0) {
            newWidth += 16 - (newWidth % 16);
        }
        return newWidth;
    }

    private int checkHeight(int h) {
        int newHeight = h;
        if (newHeight > (Window.getClientHeight() - 90)) {
            newHeight = Window.getClientHeight() - 90;
        }
        if (newHeight < MIN_HEIGHT) {
            newHeight = MIN_HEIGHT;
        }
        if (newHeight % 16 != 0) {
            newHeight += 16 - (newHeight % 16);
        }
        return newHeight;
    }

    public void onMouseMove(MouseMoveEvent event) {
        if (!dragging) {
            if (currentlySelectedPanel != null) {
                currentlySelectedPanel.mouseMoved(event);
                event.stopPropagation();
            }
        } else {
            int diffX = event.getClientX() - startX;
            int diffY = event.getClientY() - startY;

            int newWidth = width + diffX + ((resizeWidth - width) / 2);
            int newHeight = height + diffY + ((resizeHeight - height) / 2);

            resizeWidth = checkWidth(newWidth);
            resizeHeight = checkHeight(newHeight);

            setSize(resizeWidth, resizeHeight);

            center();
        }
    }

    public void onMouseUp(MouseUpEvent event) {
        if (!dragging) {
            if (currentlySelectedPanel != null) {
                currentlySelectedPanel.mouseUp(event);
            }
        } else {
            dragging = false;
            panelSize.setVisible(false);
            getWidget().remove(panelSize);
            width = resizeWidth;
            height = resizeHeight;
            for (LayoutPositionPanel panel : panels) {
                panel.setMax(width, height);
            }
        }
    }

    public void onMouseDown(MouseDownEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPositionPanel) {
            LayoutPositionPanel panel = (LayoutPositionPanel) source;
            if (currentlySelectedPanel != null) {
                currentlySelectedPanel.setSelected(false);
            }
            currentlySelectedPanel = panel;
            panel.setSelected(true);
            panel.onMouseDown(event);
            event.stopPropagation();
        } else if (source instanceof LayoutPositionBox) {
             if (currentlySelectedPanel != null) {
                 currentlySelectedPanel.setSelected(false);
             }
        } else if (source == resizeHandle) {
            dragging = true;
            startX = event.getClientX();
            startY = event.getClientY();
            panelSize.setVisible(true);
            getWidget().add(panelSize, width - 61, height + 5);
        }
    }

    private void swapPanels(int index1, int index2) {
        LayoutPositionPanel panel1 = panels.get(index1);
        LayoutPositionPanel panel2 = panels.get(index2);
        panels.set(index2, panel1);
        panels.set(index1, panel2);
        panel1.setOrder(index2);
        panel2.setOrder(index1);
    }

    public void incrementOrder(int currentOrder) {
        if ((currentOrder + 1) < panels.size()) {
            swapPanels(currentOrder, currentOrder + 1);
        }
    }

    public void decrementOrder(int currentOrder) {
        if ((currentOrder - 1) >= 0) {
            swapPanels(currentOrder, currentOrder - 1);
        }
    }

    public void deletePosition(int currentOrder) {
        LayoutPositionPanel panel = panels.remove(currentOrder);
        mainPanel.remove(panel);
        for (int i = currentOrder; i < panels.size(); i++) {
            panels.get(i).setOrder(i);
        }
    }

    public boolean isNameOk(int currentOrder, String name) {
        for (int i = 0; i < panels.size(); i++) {
            if (i != currentOrder) {
                if (panels.get(i).getName().equals(name)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onClick(ClickEvent event) {
        Object source = event.getSource();
        if (source == create) {
            LayoutPositionNamePopup popup = new LayoutPositionNamePopup(this,
                    this, "", panels.size());
            popup.center();
        } else if (source == crop) {
            if (!panels.isEmpty()) {
                if (borderSize.getNumber() == null) {
                    borderSize.setNumber(0);
                }
                int border = borderSize.getNumber();
                int minX = Integer.MAX_VALUE;
                int maxX = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = 0;
                for (LayoutPositionPanel panel : panels) {
                    minX = Math.min(minX, panel.getX());
                    maxX = Math.max(maxX, panel.getX() + panel.getWidth());
                    minY = Math.min(minY, panel.getY());
                    maxY = Math.max(maxY, panel.getY() + panel.getHeight());
                }
                int xDiff = minX - border;
                int yDiff = minY - border;
                int newWidth = checkWidth(maxX - minX + (2 * border));
                int newHeight = checkHeight(maxY - minY + (2 * border));
                for (LayoutPositionPanel panel : panels) {
                    panel.setPosition(panel.getX() - xDiff,
                            panel.getY() - yDiff);
                }
                setSize(newWidth, newHeight);
                width = newWidth;
                height = newHeight;
                center();
            }
        } else if (source == ok) {
            if (panels.size() == 0) {
                MessagePopup error = new MessagePopup(
                        "You must create at least one position in the layout",
                        null, MessagePopup.ERROR, MessageResponse.OK);
                error.center();
            } else {
                List<LayoutPosition> positions = new Vector<LayoutPosition>();
                for (LayoutPositionPanel panel : panels) {
                    LayoutPosition position = new LayoutPosition(
                            panel.getName(), panel.getX(), panel.getY(),
                            panel.getWidth(), panel.getHeight(),
                            panel.isAssignable(), panel.hasChanges(),
                            panel.hasAudio());
                    positions.add(position);
                }
                layout = new Layout("", positions);

                LayoutNamePopup popup = new LayoutNamePopup(url,
                        layout, this, layout.getName());
                popup.center();
            }
        } else if (source == cancel) {
            handler.handleResponse(
                    new MessageResponse(MessageResponse.CANCEL, this));
            hide();
        } else if (source == borderSizeIncrease) {
            if (borderSize.getNumber() == null) {
                borderSize.setNumber(DEFAULT_BORDER_SIZE);
            } else if (borderSize.getNumber() < 100) {
                borderSize.setNumber(borderSize.getNumber().intValue() + 1);
            }
        } else if (source == borderSizeDecrease) {
            if (borderSize.getNumber() == null) {
                borderSize.setNumber(DEFAULT_BORDER_SIZE);
            } else if (borderSize.getNumber() > 0) {
                borderSize.setNumber(borderSize.getNumber().intValue() - 1);
            }
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() != MessageResponse.CANCEL) {
            ModalPopup<?> source = response.getSource();
            if (source instanceof LayoutPositionNamePopup) {
                LayoutPositionNamePopup popup =
                    (LayoutPositionNamePopup) source;
                String name = popup.getName();
                LayoutPositionPanel panel = addPosition(name);
                if (currentlySelectedPanel != null) {
                    currentlySelectedPanel.setSelected(false);
                }
                currentlySelectedPanel = panel;
                panel.setSelected(true);
            } else if (source instanceof LayoutNamePopup) {
                handler.handleResponse(new MessageResponse(MessageResponse.OK,
                        this));
                hide();
            }
        }
    }

    public Layout getLayout() {
        return layout;
    }

}
