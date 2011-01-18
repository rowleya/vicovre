package com.googlecode.vicovre.gwt.client.download;

import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.wizard.Wizard;

public class DownloadWizard extends Wizard {

    private boolean inited = false;

    protected DownloadWizard(boolean cancelable) {
        super(cancelable);
    }

    public DownloadWizard(String url, String folder, String recordingId,
            JSONStream[] streams, Layout[] layouts, Layout[] customLayouts) {
        super(false);
        init(url, folder, recordingId, streams, layouts, customLayouts);
    }

    protected void init(String url, String folder, String recordingId,
            JSONStream[] streams, Layout[] layouts, Layout[] customLayouts) {
        if (!inited) {
            inited = true;
            addPage(new FormatSelectionPage());
            addPage(new LayoutSelectionPage(layouts, customLayouts, url));
            addPage(new VideoStreamSelectionPage(folder, recordingId, streams));
            addPage(new AudioSelectionPage(streams));
            addPage(new StreamsSelectionPage(streams, folder, recordingId));
            addPage(new DownloadAudioPage(folder, recordingId, streams));
            addPage(new DownloadVideoPage(folder, recordingId, streams));
            selectPage(FormatSelectionPage.INDEX);
        }
    }

}
