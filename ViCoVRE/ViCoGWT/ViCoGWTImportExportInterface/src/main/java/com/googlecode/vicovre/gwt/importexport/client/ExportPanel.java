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
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.importexport.client.rest.SessionLoader;
import com.googlecode.vicovre.gwt.importexport.client.rest.SessionsLoader;

public class ExportPanel extends VerticalPanel implements ClickHandler,
        DoubleClickHandler {

    private Button getSessionsButton = new Button("Get Sessions");

    private ListBox sessionsBox = new ListBox();

    private HashMap<String, Integer> sessionPosition =
        new HashMap<String, Integer>();

    private Button getStreamsButton = new Button("Get Streams");

    private Tree streamsTree = new Tree();

    private HashMap<String, Stream> streams =
        new HashMap<String, Stream>();

    private String url = null;

    public ExportPanel(String url) {
        this.url = url;

        ScrollPanel scroller = new ScrollPanel(streamsTree);
        scroller.setHeight("200px");
        DOM.setStyleAttribute(scroller.getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(scroller.getElement(), "borderStyle", "solid");
        DOM.setStyleAttribute(scroller.getElement(), "borderColor", "black");

        sessionsBox.setWidth("100%");

        add(getSessionsButton);
        add(sessionsBox);
        add(getStreamsButton);
        add(scroller);

        getSessionsButton.addClickHandler(this);
        getStreamsButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(getSessionsButton)) {
            SessionsLoader.loadSessions(this, url);
        } else if (event.getSource().equals(getStreamsButton)) {
            int index = sessionsBox.getSelectedIndex();
            if (index != -1) {
                String session = sessionsBox.getValue(index);
                if ((session != null) && !session.equals("")) {
                    SessionLoader.loadSession(this, url, session);
                }
            }
        }
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

    public void setStreams(Stream[] streams) {
        streamsTree.clear();
        this.streams.clear();
        if ((streams == null) || (streams.length == 0)) {
            streamsTree.add(new Label("No Streams!"));
        } else {
            int index = sessionsBox.getSelectedIndex();
            if (index != -1) {
                String session = sessionsBox.getValue(index);
                for (Stream stream : streams) {
                    if (!this.streams.containsKey(stream.getId())) {
                        StreamItem item = new StreamItem(session, stream, url);
                        item.addDoubleClickHandler(this);
                        TreeItem streamItem = new TreeItem(item);
                        streamItem.setUserObject(stream);
                        for (SubStream substream : stream.getSubStreams()) {
                            StreamItem subItem = new StreamItem(session,
                                    substream, url);
                            subItem.addDoubleClickHandler(this);
                            TreeItem substreamItem = new TreeItem(subItem);
                            substreamItem.setUserObject(substream);
                            streamItem.addItem(substreamItem);

                        }
                        streamsTree.addItem(streamItem);
                        this.streams.put(stream.getId(), stream);
                    }
                }
            }
        }
    }

    public void onDoubleClick(DoubleClickEvent event) {
        Object source = event.getSource();
        if (source instanceof StreamItem) {
            StreamItem item = (StreamItem) source;
            StreamPanel panel = item.getPanel();
            panel.center();
        }
    }

}
