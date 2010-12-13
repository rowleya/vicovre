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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.rest.RestVenueLoader;
import com.googlecode.vicovre.gwt.client.venue.VenueLoader;
import com.googlecode.vicovre.gwt.client.venue.VenuePanel;
import com.googlecode.vicovre.gwt.importexport.client.rest.StopStreamSender;
import com.googlecode.vicovre.gwt.importexport.client.rest.TransmitStreamSender;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class StreamPanel extends ModalPopup<VerticalPanel>
        implements VenueLoader, ClickHandler {

    private String sessionId = null;

    private String streamId = null;

    private Stream stream = null;

    private SubStream substream = null;

    private String url = null;

    private VenuePanel venuePanel = new VenuePanel(this);

    private Button transmitButton = new Button("Transmit");

    private Button stopButton = new Button("Stop");

    private Button closeButton = new Button("Close");

    public StreamPanel(String sessionId, Stream stream, SubStream substream,
            String url) {
        super(new VerticalPanel());
        this.sessionId = sessionId;
        this.stream = stream;
        this.substream = substream;
        this.url = url;

        VerticalPanel panel = getWidget();
        panel.setWidth("500px");
        panel.setHeight("200px");

        String title = "Send stream " + stream.getId();
        if (substream != null) {
            title += "(Substream " + substream.getIdString() + ")";
        }
        venuePanel.setWidth("100%");

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        buttonPanel.setWidth("100%");
        buttonPanel.add(transmitButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(closeButton);

        stopButton.setEnabled(false);

        panel.add(new Label(title));
        panel.add(venuePanel);
        panel.add(buttonPanel);

        transmitButton.addClickHandler(this);
        stopButton.addClickHandler(this);
        closeButton.addClickHandler(this);
    }

    public void loadVenues(VenuePanel venuePanel) {
        RestVenueLoader.loadVenues(venuePanel, url);
    }

    public void transmitFailed() {
        transmitButton.setEnabled(true);
    }

    public void transmitStarted(String streamId) {
        this.streamId = streamId;
        transmitButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    public void transmitStopped() {
        this.streamId = null;
        transmitButton.setEnabled(true);
    }

    public void transmitStoppedFailed() {
        stopButton.setEnabled(true);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(closeButton)) {
            hide();
        } else if (event.getSource().equals(transmitButton)) {
            String substreamId = null;
            if (substream != null) {
                substreamId = substream.getIdValue();
            }
            transmitButton.setEnabled(false);
            TransmitStreamSender.send(url, sessionId, stream.getId(),
                    substreamId, venuePanel, this);
        } else if (event.getSource().equals(stopButton)) {
            stopButton.setEnabled(false);
            StopStreamSender.send(url, sessionId, stream.getId(),
                    streamId, this);
        }
    }

}
