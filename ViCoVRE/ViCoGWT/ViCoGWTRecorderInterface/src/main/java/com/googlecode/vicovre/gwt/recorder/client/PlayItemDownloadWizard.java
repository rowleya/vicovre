package com.googlecode.vicovre.gwt.recorder.client;

import com.googlecode.vicovre.gwt.client.download.DownloadWizard;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.layout.Layout;

public class PlayItemDownloadWizard extends DownloadWizard {

    private PlayItem item = null;

    private String url = null;

    private Layout[] layouts = null;

    private Layout[] customLayouts = null;

    private boolean inited = false;

    public PlayItemDownloadWizard(PlayItem item, String url,
            Layout[] layouts, Layout[] customLayouts) {
        super(true);
        this.item = item;
        this.url = url;
        this.layouts = layouts;
        this.customLayouts = customLayouts;
    }

    public void center() {
        if (!inited) {
            inited = true;
            init(url, item.getFolder(), item.getId(),
                    item.getStreams().toArray(new JSONStream[0]),
                    layouts, customLayouts);
        }
        super.center();
    }



}
