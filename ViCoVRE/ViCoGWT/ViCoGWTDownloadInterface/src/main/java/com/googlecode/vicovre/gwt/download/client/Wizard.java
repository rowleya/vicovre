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

import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.client.Space;

public class Wizard extends ModalPopup<VerticalPanel> implements ClickHandler {

    private Button nextButton = new Button("Next");

    private Button backButton = new Button("Back");

    private DeckPanel wizardPanel = new DeckPanel();

    private String baseUrl = null;

    private HashMap<String, Object> attributes = new HashMap<String, Object>();

    public Wizard(String baseUrl) {
        super(new VerticalPanel());
        this.baseUrl = baseUrl;

        VerticalPanel panel = getWidget();

        panel.add(wizardPanel);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);
        buttonPanel.setCellHorizontalAlignment(backButton,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(nextButton,
                HorizontalPanel.ALIGN_RIGHT);

        panel.add(Space.getVerticalSpace(10));
        panel.add(buttonPanel);

        backButton.addClickHandler(this);
        nextButton.addClickHandler(this);
    }

    public void addPage(WizardPage page, int index) {
        wizardPanel.insert(page, index);
    }

    private void selectPage(int index, boolean back) {
        WizardPage page = (WizardPage) wizardPanel.getWidget(index);
        backButton.setEnabled(!page.isFirst());
        nextButton.setEnabled(!page.isLast());
        if (!back) {
            page.show(this);
        }
        wizardPanel.showWidget(index);
        center();
    }

    public void selectPage(int index) {
        selectPage(index, false);
    }

    public void onClick(ClickEvent event) {
        WizardPage page = (WizardPage) wizardPanel.getWidget(
                wizardPanel.getVisibleWidget());

        int index = -1;
        boolean back = false;
        if (event.getSource().equals(nextButton)) {
            index = page.next(this);
        } else if (event.getSource().equals(backButton)) {
            index = page.back(this);
            back = true;
        }

        if (index >= 0) {
            selectPage(index, back);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }
}
