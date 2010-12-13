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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONMetadata;
import com.googlecode.vicovre.gwt.client.json.JSONMetadataElement;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class MetadataPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler, MessageResponseHandler {

    private static final int WIDTH = 750;

    private Grid grid = new Grid(0, 2);

    private HashMap<String, TextBoxBase> items =
        new HashMap<String, TextBoxBase>();

    private HashMap<String, Label> labels =
        new HashMap<String, Label>();

    private HashMap<String, Boolean> isEditable =
        new HashMap<String, Boolean>();

    private HashMap<String, Boolean> isVisible =
        new HashMap<String, Boolean>();

    private HashMap<String, Boolean> isMultiline =
        new HashMap<String, Boolean>();

    private Vector<String> keys = new Vector<String>();

    private int gridRows = 0;

    private String primaryKey = null;

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private Button addItem = new Button("Add Simple Item");

    private Button addMultilineItem = new Button("Add Multiline Item");

    private MessageResponseHandler handler = null;

    private int maxFieldWidth = 0;

    public MetadataPopup(String primaryKey) {
        super(new VerticalPanel());
        this.primaryKey = primaryKey;

        addItem(primaryKey, "", false, true, true);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(cancelButton);
        buttonPanel.add(addItem);
        buttonPanel.add(addMultilineItem);
        buttonPanel.add(okButton);
        buttonPanel.setCellHorizontalAlignment(cancelButton,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(addItem,
                HorizontalPanel.ALIGN_CENTER);
        buttonPanel.setCellHorizontalAlignment(addMultilineItem,
                HorizontalPanel.ALIGN_CENTER);
        buttonPanel.setCellHorizontalAlignment(okButton,
                HorizontalPanel.ALIGN_RIGHT);

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        addItem.addClickHandler(this);
        addMultilineItem.addClickHandler(this);

        VerticalPanel panel = getWidget();
        panel.add(grid);
        panel.add(buttonPanel);
    }

    public void setHandler(MessageResponseHandler handler) {
        this.handler = handler;
    }

    private void addItem(String key, String value, boolean multiline,
            boolean visible, boolean editable) {
        TextBoxBase itemBox = null;
        if (multiline) {
            itemBox = new TextArea();
        } else {
            itemBox = new TextBox();
        }
        itemBox.setValue(value);
        itemBox.setWidth("100%");
        items.put(key, itemBox);
        if (editable) {
            String displayName = getDisplayName(key);
            Label itemLabel = new Label(displayName + ":");
            labels.put(key, itemLabel);
            grid.resizeRows(gridRows + 1);
            grid.setWidget(gridRows, 0, itemLabel);
            grid.setWidget(gridRows, 1, itemBox);
            grid.getRowFormatter().setVerticalAlign(gridRows,
                    VerticalPanel.ALIGN_TOP);
            gridRows += 1;

            if (isShowing()) {
                maxFieldWidth = Math.max(maxFieldWidth,
                        itemLabel.getOffsetWidth());
                grid.getColumnFormatter().setWidth(0, maxFieldWidth + "px");
                grid.getColumnFormatter().setWidth(1, (WIDTH - maxFieldWidth)
                        + "px");
            }

        }

        keys.add(key);
        isEditable.put(key, editable);
        isVisible.put(key, visible);
        isMultiline.put(key, multiline);
    }

    public void center() {
        super.center();
        maxFieldWidth = 0;
        for (Label label : labels.values()) {
            maxFieldWidth = Math.max(maxFieldWidth, label.getOffsetWidth());
        }
        for (int i = 0; i < gridRows; i++) {
            grid.getColumnFormatter().setWidth(0, maxFieldWidth + "px");
            grid.getColumnFormatter().setWidth(1, (WIDTH - maxFieldWidth)
                    + "px");
        }
    }

    public void setMetadata(Map<String, Object> metadata) {
        GWT.log(metadata.toString());
        items.clear();
        keys.clear();
        gridRows = 0;
        grid.clear();
        grid.resize(0, 2);
        primaryKey = (String) metadata.get("primaryKey");
        for (String key : metadata.keySet()) {
            if (!key.equals("primaryKey") && !key.endsWith("Visible")
                    && !key.endsWith("Editable")
                    && !key.endsWith("Multiline")) {
                boolean visible = true;
                boolean editable = true;
                boolean multiline = false;
                if (metadata.containsKey(key + "Visible")) {
                    visible = metadata.get(key + "Visible").equals("true");
                }
                if (metadata.containsKey(key + "Editable")) {
                    editable = metadata.get(key + "Editable").equals("true");
                }
                if (metadata.containsKey(key + "Multiline")) {
                    multiline = metadata.get(key + "Multiline").equals("true");
                }
                addItem(key, (String) metadata.get(key),
                        multiline, visible, editable);
            }
        }
    }

    public void setMetadata(JSONMetadata metadata) {
        items.clear();
        keys.clear();
        gridRows = 0;
        grid.clear();
        grid.resize(0, 2);
        primaryKey = metadata.getPrimaryKey();
        JsArray<JSONMetadataElement> keys = metadata.getKeys();
        for (int i = 0; i < keys.length(); i++) {
            JSONMetadataElement element = keys.get(i);
            addItem(element.getName(), element.getValue(),
                    element.isMultiline(), element.isVisible(),
                    element.isEditable());
        }
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getPrimaryValue() {
        return items.get(primaryKey).getText();
    }

    public List<String> getKeys() {
        return keys;
    }

    public String getRealValue(String key) {
        return items.get(key).getText();
    }

    public String getValue(String key) {
        String value = items.get(key).getText();
        if (value != null) {
            for (String otherKey : items.keySet()) {
                if (!key.equals(otherKey)) {
                    String otherValue = items.get(key).getText();
                    if (otherValue != null) {
                        value.replace("${" + otherKey + "}", otherValue);
                    }
                }
            }
        }
        return value;
    }

    public boolean isMultiline(String key) {
        return isMultiline.get(key);
    }

    public boolean isEditable(String key) {
        return isEditable.get(key);
    }

    public boolean isVisible(String key) {
        return isVisible.get(key);
    }

    public void setValue(String key, String value, boolean multiline) {
        if (!items.containsKey(key)) {
            addItem(key, value, multiline, true, true);
        } else {
            items.get(key).setText(value);
            isMultiline.put(key, multiline);
        }
    }

    public static String getDisplayName(String key) {
        String displayName = "";
        displayName += Character.toUpperCase(key.charAt(0));
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c)) {
                displayName += " ";
                displayName += c;
            } else {
                displayName += c;
            }
        }
        return displayName;
    }

    public static String getKey(String displayName) {
        String key = "";
        key += Character.toLowerCase(displayName.charAt(0));
        for (int i = 1; i < displayName.length(); i++) {
            char c = displayName.charAt(i);
            if (Character.isSpace(c)) {
                i++;
                key += Character.toUpperCase(displayName.charAt(i));
            } else {
                key += c;
            }
        }
        return key;
    }

    public void onClick(ClickEvent event) {
        Object source = event.getSource();
        if (handler != null) {
            if (source == okButton) {
                String error = null;
                if (getPrimaryValue().isEmpty()) {
                    error = "You must specify a value for "
                        + getDisplayName(getPrimaryKey());
                }
                if (error == null) {
                    hide();
                    handler.handleResponse(new MessageResponse(
                            MessageResponse.OK, this));
                } else {
                    MessagePopup errorPopup = new MessagePopup(error, null,
                            MessagePopup.ERROR, MessageResponse.OK);
                    errorPopup.center();
                }
            } else if (source == cancelButton) {
                hide();
                handler.handleResponse(new MessageResponse(
                        MessageResponse.CANCEL, this));
            } else if (source == addItem) {
                EditMetadataNamePopup popup =
                    new EditMetadataNamePopup(false, this);
                popup.center();
            } else if (source == addMultilineItem) {
                EditMetadataNamePopup popup =
                    new EditMetadataNamePopup(true, this);
                popup.center();
            }
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            EditMetadataNamePopup popup =
                (EditMetadataNamePopup) response.getSource();
            String name = popup.getName();
            if (name.equals("")) {
                MessagePopup errorPopup = new MessagePopup(
                        "The name of the item cannot be blank", null,
                        MessagePopup.ERROR, MessageResponse.OK);
                errorPopup.center();
            } else {
                addItem(getKey(name), "", popup.isMultiline(), true, true);
            }
        }
    }

    public String getDetailsAsUrl() {
        String itemUrl = "metadataPrimaryKey=" + URL.encodeComponent(
                primaryKey);
        for (String key : keys) {
            String value = getRealValue(key);
            if (!value.isEmpty()) {
                itemUrl += "&metadata" + key + "="
                    + URL.encodeComponent(value);
                itemUrl += "&metadata" + key + "Multiline="
                    + isMultiline(key);
                itemUrl += "&metadata" + key + "Visible="
                    + isVisible(key);
                itemUrl += "&metadata" + key + "Editable="
                    + isEditable(key);
            }
        }
        return itemUrl;
    }
}
