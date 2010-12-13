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
import com.googlecode.vicovre.gwt.client.auth.LoginPopup;
import com.googlecode.vicovre.gwt.client.auth.Logout;
import com.googlecode.vicovre.gwt.client.json.JSONLayout;
import com.googlecode.vicovre.gwt.client.json.JSONLayouts;
import com.googlecode.vicovre.gwt.client.json.JSONStream;
import com.googlecode.vicovre.gwt.client.json.JSONStreams;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class Application implements EntryPoint, MessageResponseHandler {

    protected static final int FORMAT_SELECTION = 0;

    protected static final int LAYOUT_SELECTION = 1;

    protected static final int VIDEO_SELECTION = 2;

    protected static final int AUDIO_SELECTION = 3;

    protected static final int STREAM_SELECTION = 4;

    protected static final int DOWNLOAD_AUDIO = 5;

    protected static final int DOWNLOAD_VIDEO = 6;

    private Dictionary parameters = Dictionary.getDictionary("Parameters");

    protected String getUrl() {
        String url = GWT.getModuleBaseURL();
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
        if (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
        return folder;
    }

    protected Layout[] getLayouts(String parameter) {
        String layoutsJSON = parameters.get(parameter);
        if ((layoutsJSON != null) && !layoutsJSON.equals("")) {
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
        if ((streamsJSON != null) && !streamsJSON.equals("")) {
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

    protected String getRole() {
        return parameters.get("role");
    }

    protected boolean canPlay() {
        String canPlay = parameters.get("canPlay");
        return (canPlay != null) && canPlay.equals("true");
    }

    public void onModuleLoad() {

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        mainPanel.setHeight("100%");
        RootPanel.get().add(mainPanel);

        String role = getRole();
        boolean canPlay = canPlay();
        String url = getUrl();
        String recordingId = getRecordingId();
        String folder = getFolder();

        GWT.log("Base url = " + GWT.getModuleBaseURL());

        if (canPlay) {
            JSONStream[] streams = getStreams();

            Wizard wizard = new Wizard();
            wizard.addPage(new FormatSelectionPage(), FORMAT_SELECTION);
            wizard.addPage(new LayoutSelectionPage(getLayouts("layouts"),
                    getLayouts("customLayouts"), url), LAYOUT_SELECTION);
            wizard.addPage(new VideoStreamSelectionPage(folder,
                    recordingId, streams), VIDEO_SELECTION);
            wizard.addPage(new AudioSelectionPage(streams),
                    AUDIO_SELECTION);
            wizard.addPage(new StreamsSelectionPage(streams,
                    folder, recordingId), STREAM_SELECTION);
            wizard.addPage(new DownloadAudioPage(folder, recordingId,
                    streams), DOWNLOAD_AUDIO);
            wizard.addPage(new DownloadVideoPage(folder, recordingId,
                    streams), DOWNLOAD_VIDEO);
            wizard.selectPage(FORMAT_SELECTION);
            wizard.center();
        } else if (role.equals("User")) {
            LoginPopup popup = new LoginPopup(url);
            popup.center();
        } else {
            MessagePopup popup = new MessagePopup(
                    "Sorry, you do not have the right permissions to download "
                    + "this recording.",
                    this, MessagePopup.ERROR, MessageResponse.OK);
            popup.center();
        }
    }

    public void handleResponse(MessageResponse response) {
        Logout.logout(getUrl());
    }
}

