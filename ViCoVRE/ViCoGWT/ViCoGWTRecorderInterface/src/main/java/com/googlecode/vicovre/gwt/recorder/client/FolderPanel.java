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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.json.JSONMetadata;
import com.googlecode.vicovre.gwt.recorder.client.rest.FolderCreator;
import com.googlecode.vicovre.gwt.recorder.client.rest.FolderEditor;
import com.googlecode.vicovre.gwt.recorder.client.rest.FolderMetadataLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.HarvestItemLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.RecordingItemLoader;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class FolderPanel extends HorizontalPanel
        implements SelectionHandler<TreeItem>, ClickHandler,
        MessageResponseHandler {

    private Button createButton = new Button("New Folder");

    private Button editMetadataButton = new Button("Edit Metadata");

    private TabPanel panel = new TabPanel();

    private PlayPanel playPanel = null;

    private RecordPanel recordPanel = null;

    private HarvestPanel harvestPanel = null;

    private HashMap<String, TreeItem> folderTreeItems =
        new HashMap<String, TreeItem>();

    private Tree folders = new Tree();

    private String url = null;

    private String currentPath = null;

    private MetadataPopup metadataPopup = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    private boolean userIsWriter = false;

    private JsArrayString users = null;

    private JsArrayString groups = null;

    public FolderPanel(String url, Layout[] layouts, Layout[] customLayouts,
            JsArrayString users, JsArrayString groups) {
        this.url = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
        this.users = users;
        this.groups = groups;

        playPanel = new PlayPanel(this, url, layouts, customLayouts);
        recordPanel = new RecordPanel(this, playPanel, url, layouts,
                customLayouts, users, groups);
        harvestPanel = new HarvestPanel(this, recordPanel, playPanel, url,
                layouts, customLayouts, users, groups);
        metadataPopup = new MetadataPopup("name");
        metadataPopup.setHandler(this);

        setWidth("95%");
        setHeight("100%");

        panel.setWidth("100%");
        panel.setHeight("95%");
        panel.getDeckPanel().setHeight("100%");
        panel.add(playPanel, "Play");
        panel.selectTab(0);
        panel.setAnimationEnabled(true);

        VerticalPanel folderTree = new VerticalPanel();
        folderTree.setWidth("100%");
        DOM.setStyleAttribute(folderTree.getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(folderTree.getElement(), "borderStyle", "solid");
        DOM.setStyleAttribute(folderTree.getElement(), "borderColor", "black");
        HTML folderTitle = new HTML("<b>Folders</b>");
        DOM.setStyleAttribute(folderTitle.getElement(), "background",
                "DodgerBlue");
        ScrollPanel folderScroller = new ScrollPanel(folders);
        folderScroller.setHeight("100%");
        folderTree.add(folderTitle);
        folderTree.add(createButton);
        folderTree.add(folderScroller);
        folderTree.add(editMetadataButton);

        add(folderTree);
        add(panel);
        setCellWidth(folderTree, "20%");
        setCellWidth(panel, "80%");
        setCellHeight(panel, "100%");

        TreeItem rootItem = new TreeItem("Root");
        rootItem.setUserObject("");
        folderTreeItems.put("", rootItem);
        folders.addItem(rootItem);
        folders.setSelectedItem(rootItem);

        folders.addSelectionHandler(this);
        createButton.addClickHandler(this);
        editMetadataButton.addClickHandler(this);
    }

    public void addFolder(String path) {
        GWT.log("Adding folder " + path, null);
        String[] foldersInPath = path.split("/");
        TreeItem folderItem = folderTreeItems.get(path);
        if (folderItem == null) {
            String folderPath = "";
            TreeItem parent = null;
            for (String folder : foldersInPath) {
                folderPath += folder;
                folderItem = folderTreeItems.get(folderPath);
                if (folderItem == null) {
                    folderItem = new TreeItem(folder);
                    folderItem.setUserObject(folderPath);
                    folderTreeItems.put(folderPath, folderItem);
                    if (parent == null) {
                        folders.addItem(folderItem);
                    } else {
                        parent.addItem(folderItem);
                    }
                }
                parent = folderTreeItems.get(folderPath);
                folderPath += "/";
            }
        }
    }

    public void setFolder(String path) {
        GWT.log("Current path = " + currentPath + " path = " + path);
        if (currentPath != null && currentPath.equals(path)) {
            return;
        }
        currentPath = path;

        ActionLoader loader = new ActionLoader(null, 4,
                "Loading Folder",
                "There was an error loading the folder.\n"
                + "Please try again.",
                true, true);

        playPanel.clear();
        recordPanel.clear();
        harvestPanel.clear();

        PlayItemLoader.loadPlayItems(path, this, playPanel, loader, url,
                layouts, customLayouts, users, groups);
        RecordingItemLoader.loadRecordingItems(path, this, playPanel,
                recordPanel, loader, url, layouts, customLayouts, users,
                groups);
        HarvestItemLoader.loadHarvestItems(path, this, recordPanel, playPanel,
                harvestPanel, loader, url, layouts, customLayouts, users,
                groups);
        FolderMetadataLoader.loadMetadata(path, this, loader, url);
        if (path.equals("") || path.equals("/") || !userIsWriter) {
            editMetadataButton.setEnabled(false);
        } else {
            editMetadataButton.setEnabled(true);
        }
    }

    public void setMetadata(JSONMetadata metadata) {
        metadataPopup.setMetadata(metadata);
    }

    public void reload() {
        currentPath = null;
        TreeItem folderItem = folders.getSelectedItem();
        if (folderItem != null) {
            setFolder((String) folderItem.getUserObject());
        }
    }

    public void onSelection(SelectionEvent<TreeItem> event) {
        TreeItem folderItem = event.getSelectedItem();
        setFolder((String) folderItem.getUserObject());
    }

    public String getCurrentFolder() {
        return (String) folders.getSelectedItem().getUserObject();
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(createButton)) {
            FolderCreator.createFolder(this, url);
        } else if (event.getSource().equals(editMetadataButton)) {
            metadataPopup.center();
        }
    }

    public void setUserIsAdministrator(boolean isAdministrator) {
        playPanel.setUserIsAdministrator(isAdministrator);
    }

    public void setUserIsWriter(boolean isWriter) {
        this.userIsWriter = isWriter;
        if (isWriter) {
            panel.add(recordPanel, "Record");
            panel.add(harvestPanel, "Harvest");
        } else {
            panel.remove(recordPanel);
            panel.remove(harvestPanel);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            FolderEditor.editFolder(metadataPopup, currentPath, url);
        }
    }

}
