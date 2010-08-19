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

import java.util.Vector;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.json.JSONStream;


public class AudioSelectionPage extends WizardPage {

    private Vector<CheckBox> streamBoxes = new Vector<CheckBox>();

    private String baseUrl = null;

    public AudioSelectionPage(String baseUrl, JSONStream[] streams) {
        this.baseUrl = baseUrl;
        add(new Label(
            "Select the audio streams that you would like to include:"));
        for (JSONStream stream : streams) {
            if (stream.getMediaType().equalsIgnoreCase("Audio")) {
                String name = stream.getSsrc();
                if (stream.getName() != null) {
                    name = stream.getName();
                    if (stream.getNote() != null) {
                        name += " - " + stream.getNote();
                    }
                } else if (stream.getCname() != null) {
                    name = stream.getCname();
                }
                CheckBox checkBox = new CheckBox(name);
                checkBox.setValue(true);
                checkBox.setFormValue(stream.getSsrc());
                add(checkBox);
                streamBoxes.add(checkBox);
            }
        }
    }

    public int back(Wizard wizard) {
        String format = (String) wizard.getAttribute("format");
        if (format.startsWith("video")) {
            return Application.VIDEO_SELECTION;
        }
        return Application.FORMAT_SELECTION;
    }

    public boolean isFirst() {
        return false;
    }

    public boolean isLast() {
        return false;
    }

    public int next(Wizard wizard) {
        String format = (String) wizard.getAttribute("format");
        Vector<String> audioStreams = new Vector<String>();
        for (CheckBox box : streamBoxes) {
            if (box.getValue()) {
                audioStreams.add(box.getFormValue());
            }
        }
        if (audioStreams.isEmpty() && !format.startsWith("video")) {
            MessagePopup error = new MessagePopup(
                    "You must select at least one audio stream",
                    null, baseUrl + MessagePopup.ERROR, MessageResponse.OK);
            error.center();
            return -1;
        }
        wizard.setAttribute("audioStreams", audioStreams);
        if (format.startsWith("video")) {
            return Application.DOWNLOAD_VIDEO;
        }
        return Application.DOWNLOAD_AUDIO;
    }

    public void show(Wizard wizard) {
        for (CheckBox box : streamBoxes) {
            box.setValue(true);
        }
    }

}
