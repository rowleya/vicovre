package com.googlecode.vicovre.gwt.recorder.client;

import com.google.gwt.user.client.ui.Image;
import com.googlecode.vicovre.gwt.utils.client.TitledPushButton;

public class DragButton extends TitledPushButton {

    private PlayItem playItem = null;

    public DragButton(Image image, String title, PlayItem playItem) {
        super(image, title);
        this.playItem = playItem;
    }

    public PlayItem getPlayItem() {
        return playItem;
    }
}
