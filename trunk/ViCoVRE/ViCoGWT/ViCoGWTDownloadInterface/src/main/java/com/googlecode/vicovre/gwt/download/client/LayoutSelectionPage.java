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

import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.layout.LayoutSelectionPanel;
import com.googlecode.vicovre.gwt.utils.client.MessagePopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;

public class LayoutSelectionPage extends WizardPage {

    private LayoutSelectionPanel panel = null;

    public LayoutSelectionPage(Layout[] predefinedLayouts,
            Layout[] customLayouts, String url) {
        panel = new LayoutSelectionPanel(predefinedLayouts, customLayouts, url);
        add(panel);
    }

    public int back(Wizard wizard) {
        return Application.FORMAT_SELECTION;
    }

    public boolean isFirst() {
        return false;
    }

    public boolean isLast() {
        return false;
    }

    public int next(Wizard wizard) {
        Layout selection = panel.getSelection();
        if (selection == null) {
            MessagePopup error = new MessagePopup("Please select a layout",
                    null, MessagePopup.ERROR,
                    MessageResponse.OK);
            error.center();
            return -1;
        }
        wizard.setAttribute("layout", selection);

        return Application.VIDEO_SELECTION;
    }

    public void show(Wizard wizard) {
        panel.setLayout(null);
    }

}
