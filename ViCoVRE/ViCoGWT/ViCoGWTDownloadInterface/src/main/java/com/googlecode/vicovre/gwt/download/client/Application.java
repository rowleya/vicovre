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

import java.util.Arrays;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.Layout;
import com.googlecode.vicovre.gwt.client.json.JSONLayout;
import com.googlecode.vicovre.gwt.client.json.JSONLayouts;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.json.JSONStreams;

public class Application implements EntryPoint {

    protected static final int FORMAT_SELECTION = 0;

    protected static final int LAYOUT_SELECTION = 1;

    protected static final int VIDEO_SELECTION = 2;

    protected static final int AUDIO_SELECTION = 3;

    protected static final int STREAM_SELECTION = 4;

    protected static final int DOWNLOAD_VIDEO = 5;

    protected static final int DOWNLOAD_AUDIO = 6;

    private Dictionary parameters = Dictionary.getDictionary("Parameters");

    protected String getBaseUrl() {
        String url = GWT.getHostPageBaseURL();
        String recording = getFolder() + "/" + getRecordingId() + "/";
        if (url.endsWith(recording)) {
            url = url.substring(0, url.length() - recording.length() + 1);
        }
        return url;
    }

    protected String getUrl() {
        String url = getBaseUrl();
        String paramUrl = parameters.get("url");
        if (paramUrl.startsWith("/")) {
            paramUrl = paramUrl.substring(1);
        }
        if (!paramUrl.endsWith("/")) {
            paramUrl += "/";
        }
        return url + paramUrl;
    }

    protected String getRecordingId() {
        String id = parameters.get("recording");
        return id;
    }

    protected String getFolder() {
        String folder = parameters.get("folder");
        return folder;
    }

    protected Layout[] getLayouts(String parameter) {
        String layoutsJSON = parameters.get(parameter);
        if (layoutsJSON != null) {
            JSONLayouts jsonLayouts = JSONLayouts.parse(layoutsJSON);
            JsArray<JSONLayout> layoutArray = jsonLayouts.getLayouts();
            Layout[] layouts = new Layout[layoutArray.length()];
            for (int i = 0; i < layoutArray.length(); i++) {
                layouts[i] = new Layout(layoutArray.get(i));
            }
            return layouts;
        }
        return new Layout[0];
    }

    protected JSONStream[] getStreams() {
        String streamsJSON = parameters.get("streams");
        if (streamsJSON != null) {
            JSONStreams jsonStreams = JSONStreams.parse(streamsJSON);
            JsArray<JSONStream> streamArray = jsonStreams.getStreams();
            JSONStream[] streams = new JSONStream[streamArray.length()];
            for (int i = 0; i < streamArray.length(); i++) {
                streams[i] = streamArray.get(i);
            }
            Arrays.sort(streams, new StreamComparator());
            return streams;
        }
        return new JSONStream[0];
    }

    public void onModuleLoad() {
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        mainPanel.setHeight("100%");
        RootPanel.get().add(mainPanel);

        String url = getUrl();
        String baseUrl = getBaseUrl();
        String recordingId = getRecordingId();
        String folder = getFolder();
        JSONStream[] streams = getStreams();

        Wizard wizard = new Wizard(getBaseUrl());
        wizard.addPage(new FormatSelectionPage(), FORMAT_SELECTION);
        wizard.addPage(new LayoutSelectionPage(getLayouts("layouts"),
                getLayouts("customLayouts"), url), LAYOUT_SELECTION);
        wizard.addPage(new VideoStreamSelectionPage(baseUrl, folder,
                recordingId, streams), VIDEO_SELECTION);
        wizard.addPage(new AudioSelectionPage(streams), AUDIO_SELECTION);
        wizard.addPage(new StreamsSelectionPage(streams,
                baseUrl, folder, recordingId), STREAM_SELECTION);
        wizard.selectPage(FORMAT_SELECTION);
        wizard.center();
    }
}

