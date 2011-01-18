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

package com.googlecode.vicovre.gwt.client.download;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.googlecode.vicovre.gwt.client.wizard.Wizard;
import com.googlecode.vicovre.gwt.client.wizard.WizardPage;
import com.googlecode.vicovre.gwt.utils.client.FormRadioButton;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;

public class FormatSelectionPage extends WizardPage implements ClickHandler {

    public static final int INDEX = 0;

    private String format = null;

    private FormRadioButton[] options = new FormRadioButton[]{
        new FormRadioButton("format", "Video File (wmv, flv, mp4)",
                "video", this),
        new FormRadioButton("format", "Audio File (mp3)",
                "audio/mp3", this),
        new FormRadioButton("format", "AG-VCR Recording",
                "application/x-agvcr", this)
    };

    public FormatSelectionPage() {
        add(new Label("Select the format which you would like to use"
                + " to download the recording:"));
        for (FormRadioButton option : options) {
            add(option);
        }
    }

    public void onClick(ClickEvent event) {
        Object source = event.getSource();
        if (source instanceof RadioButton) {
            RadioButton radioButton = (RadioButton) source;
            if (radioButton.getValue()) {
                format = radioButton.getFormValue();
            }
        }
    }

    public void show(Wizard wizard) {
        for (FormRadioButton option : options) {
            option.setValue(false);
        }
        format = null;
    }

    public int back(Wizard wizard) {
        return -1;
    }

    public boolean isFirst() {
        return true;
    }

    public boolean isLast() {
        return false;
    }

    public int next(Wizard wizard) {
        if (format == null) {
            MessagePopup error = new MessagePopup("Please select a format",
                    null, MessagePopup.ERROR, MessageResponse.OK);
            error.center();
            return -1;
        }
        wizard.setAttribute("format", format);

        if (format.startsWith("video")) {
            return LayoutSelectionPage.INDEX;
        }

        if (format.startsWith("audio")) {
            return AudioSelectionPage.INDEX;
        }

        return StreamsSelectionPage.INDEX;
    }

    public int getIndex() {
        return INDEX;
    }

}
