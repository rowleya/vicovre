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

import java.util.HashMap;
import java.util.Vector;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PlayPanel extends HorizontalPanel
        implements SelectionHandler<TreeItem> {

    private VerticalPanel items = new VerticalPanel();

    private HashMap<String, Vector<PlayItem>> folderItems =
        new HashMap<String, Vector<PlayItem>>();

    private HashMap<String, TreeItem> folderTreeItems =
        new HashMap<String, TreeItem>();

    private Tree folders = new Tree();

    public PlayPanel() {
        setWidth("100%");
        setHeight("100%");
        setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

        ScrollPanel scroller = new ScrollPanel(items);
        items.setWidth("100%");
        scroller.setHeight("100%");

        VerticalPanel folderTree = new VerticalPanel();
        folderTree.setWidth("90%");
        DOM.setStyleAttribute(folderTree.getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(folderTree.getElement(), "borderStyle", "solid");
        DOM.setStyleAttribute(folderTree.getElement(), "borderColor", "black");
        HTML folderTitle = new HTML("<b>Categories</b>");
        DOM.setStyleAttribute(folderTitle.getElement(), "background",
                "DodgerBlue");
        ScrollPanel folderScroller = new ScrollPanel(folders);
        folderScroller.setHeight("100%");
        folderTree.add(folderTitle);
        folderTree.add(folderScroller);

        add(folderTree);
        add(scroller);
        setCellWidth(folderTree, "20%");
        setCellWidth(scroller, "80%");
        setCellHeight(scroller, "100%");

        TreeItem rootItem = new TreeItem("All");
        rootItem.setUserObject("/");
        Vector<PlayItem> rootItems = new Vector<PlayItem>();
        folderItems.put("/", rootItems);
        folderTreeItems.put("/", rootItem);
        folders.addItem(rootItem);
        folders.setSelectedItem(rootItem);
        folders.addSelectionHandler(this);
    }

    private boolean isInFolder(PlayItem item) {
        String currentFolder = (String)
            folders.getSelectedItem().getUserObject();
        return item.getFolder().startsWith(currentFolder);
    }

    public void addItem(PlayItem item) {
        String folderPath = "";
        TreeItem parent = null;
        for (String folder : item.getFolders()) {
            folderPath += "/" + folder;
            Vector<PlayItem> playItems = folderItems.get(folderPath);
            if (playItems == null) {
                playItems = new Vector<PlayItem>();
                TreeItem folderItem = new TreeItem(folder);
                folderItem.setUserObject(folderPath);
                if (parent == null) {
                    folders.addItem(folderItem);
                } else {
                    parent.addItem(folderItem);
                }
                folderTreeItems.put(folderPath, folderItem);
            }
            parent = folderTreeItems.get(folderPath);
            playItems.add(item);
            folderItems.put(folderPath, playItems);
        }
        if (isInFolder(item)) {
            items.add(item);
        }
    }

    public void onSelection(SelectionEvent<TreeItem> event) {
        String folder = (String) event.getSelectedItem().getUserObject();
        items.clear();
        Vector<PlayItem> playItems = folderItems.get(folder);
        for (PlayItem item : playItems) {
            items.add(item);
        }
    }
}
