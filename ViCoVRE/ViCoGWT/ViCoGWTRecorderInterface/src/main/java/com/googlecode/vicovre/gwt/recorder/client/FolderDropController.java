package com.googlecode.vicovre.gwt.recorder.client;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.drop.SimpleDropController;
import com.google.gwt.user.client.ui.TreeItem;
import com.googlecode.vicovre.gwt.recorder.client.rest.PlayItemMover;

public class FolderDropController extends SimpleDropController {

    private TreeItem item = null;

    private String url = null;

    public FolderDropController(TreeItem folderItem, String url) {
        super(folderItem.getWidget());
        this.item = folderItem;
        this.url = url;
    }

    public void onDrop(DragContext context) {
        DragButton button = (DragButton) context.draggable;
        PlayItem playItem = button.getPlayItem();
        String destinationFolder = (String) item.getUserObject();
        PlayItemMover.moveRecording(playItem, url, destinationFolder);
    }
}
