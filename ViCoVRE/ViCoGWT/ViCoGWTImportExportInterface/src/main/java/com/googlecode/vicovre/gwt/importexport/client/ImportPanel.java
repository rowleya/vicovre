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

package com.googlecode.vicovre.gwt.importexport.client;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.VenueLoader;
import com.googlecode.vicovre.gwt.client.VenuePanel;
import com.googlecode.vicovre.gwt.importexport.client.rest.CreateSessionSender;
import com.googlecode.vicovre.gwt.importexport.client.rest.RestVenueLoader;
import com.googlecode.vicovre.gwt.importexport.client.rest.SessionsLoader;

public class ImportPanel extends VerticalPanel implements ClickHandler,
        VenueLoader, SessionsHandler {

    private String url = null;

    private VerticalPanel createPanel = new VerticalPanel();

    private CheckBox liveCheckBox = new CheckBox("Live Session");

    private TextBox nameBox = new TextBox();

    private Button createButton = new Button("Create Import Session");

    private Button getSessionsButton = new Button("Get Sessions");

    private ListBox sessionsBox = new ListBox();

    private HashMap<String, Integer> sessionPosition =
        new HashMap<String, Integer>();

    private Button loadButton = new Button("Load Session");

    private VerticalPanel sessionPanel = new VerticalPanel();

    private Label idLabel = new Label();

    private FileUpload uploadField = new FileUpload();

    private Button uploadFileButton = new Button("Upload File");

    private VenuePanel venuePanel = new VenuePanel(this);

    private Button connectToVenueButton = new Button("Connect To Venue");

    private Button closeButton = new Button("Close Session");

    private Button switchButton = new Button("Switch Session");

    public ImportPanel(String url) {
        this.url = url;
        setWidth("100%");

        HorizontalPanel namePanel = new HorizontalPanel();
        Label nameLabel = new Label("Name:");
        namePanel.add(nameLabel);
        namePanel.add(nameBox);
        nameBox.setWidth("100%");
        namePanel.setWidth("100%");
        namePanel.setCellWidth(nameLabel, "50px");

        HorizontalPanel sessionsPanel = new HorizontalPanel();
        sessionsPanel.add(sessionsBox);
        sessionsPanel.add(getSessionsButton);
        sessionsPanel.setWidth("100%");
        sessionsBox.setWidth("100%");
        sessionsPanel.setCellWidth(getSessionsButton, "100px");

        createPanel.add(namePanel);
        createPanel.add(liveCheckBox);
        createPanel.add(createButton);
        createPanel.add(new HTML("<hr/>"));
        createPanel.add(new Label("Load existing session:"));
        createPanel.add(sessionsPanel);
        createPanel.add(loadButton);
        createPanel.setWidth("100%");
        add(createPanel);

        sessionPanel.setWidth("100%");
        sessionPanel.add(idLabel);
        sessionPanel.add(new HTML("<hr/>"));
        HorizontalPanel filePanel = new HorizontalPanel();
        filePanel.setWidth("100%");
        uploadField.setWidth("100%");
        filePanel.add(uploadField);
        filePanel.add(uploadFileButton);
        venuePanel.setWidth("100%");
        sessionPanel.add(filePanel);
        sessionPanel.add(new HTML("<hr/>"));
        sessionPanel.add(venuePanel);
        sessionPanel.add(connectToVenueButton);
        sessionPanel.add(new HTML("<hr/>"));
        sessionPanel.add(closeButton);
        sessionPanel.add(switchButton);

        createButton.addClickHandler(this);
        getSessionsButton.addClickHandler(this);
        uploadFileButton.addClickHandler(this);
        connectToVenueButton.addClickHandler(this);
        closeButton.addClickHandler(this);
        loadButton.addClickHandler(this);
        switchButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(createButton)) {
            CreateSessionSender.createSession(this, url,
                    liveCheckBox.getValue(), nameBox.getText());
        } else if (event.getSource().equals(getSessionsButton)) {
            SessionsLoader.loadSessions(this, url);
        } else if (event.getSource().equals(loadButton)) {
            int index = sessionsBox.getSelectedIndex();
            if (index != -1) {
                String session = sessionsBox.getValue(index);
                if ((session != null) && !session.equals("")) {
                    startSession(url + "import/" + session);
                }
            }
        } else if (event.getSource().equals(switchButton)) {
            remove(sessionPanel);
            add(createPanel);
        }

    }

    public void startSession(String sessionUri) {
        idLabel.setText(sessionUri);
        remove(createPanel);
        add(sessionPanel);
    }

    public void closeSession() {
        remove(sessionPanel);
        add(createPanel);
    }

    public void loadVenues(VenuePanel panel) {
        RestVenueLoader.loadVenues(panel, url);
    }

    public void setSessions(String[] sessionUrls) {
        sessionPosition.clear();
        sessionsBox.clear();
        if ((sessionUrls == null) || (sessionUrls.length == 0)) {
            sessionsBox.addItem("No Sessions!", "");
        } else {
            for (String sessionUrl : sessionUrls) {
                if (!sessionPosition.containsKey(sessionUrl)) {
                    sessionPosition.put(sessionUrl, sessionsBox.getItemCount());
                    sessionsBox.addItem(sessionUrl, sessionUrl);
                }
            }
        }
    }

}
