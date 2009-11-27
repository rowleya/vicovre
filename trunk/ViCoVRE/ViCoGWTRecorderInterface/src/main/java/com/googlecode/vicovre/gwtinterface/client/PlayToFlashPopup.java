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

package com.googlecode.vicovre.gwtinterface.client;

import java.util.List;

import pl.rmalinowski.gwt2swf.client.ui.SWFWidget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwtinterface.client.xmlrpc.LayoutLoader;

public class PlayToFlashPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private Button closeButton = new Button("Close");

    private SWFWidget player = null;

    private PlayItem item = null;

    public PlayToFlashPopup(PlayItem item) {
        super(new VerticalPanel());
        this.item = item;
        closeButton.addClickHandler(this);
    }

    public void center() {
        if (player == null) {
            player = new SWFWidget("CrewPlayer.swf");
            player.addFlashVar("uri", Application.getParam("playUrl")
                    + "?recordingId=" + item.getId() + "%26startTime=0");
            VerticalPanel panel = getWidget();
            panel.add(player);
            panel.add(closeButton);
        }
        List<ReplayLayout> replayLayouts = item.getReplayLayouts();
        if ((replayLayouts != null) && (replayLayouts.size() > 0)) {
            int minX = Integer.MAX_VALUE;
            int maxX = 0;
            int minY = Integer.MAX_VALUE;
            int maxY = 0;
            for (ReplayLayout replayLayout : replayLayouts) {
                Layout layout = LayoutLoader.getLayouts().get(
                        replayLayout.getName());
                for (LayoutPosition position : layout.getPositions()) {
                    if ((position.getX() + position.getWidth()) > maxX) {
                        maxX = position.getX() + position.getWidth();
                    }
                    if ((position.getY() + position.getHeight()) > maxY) {
                        maxY = position.getY() + position.getHeight();
                    }
                    if (position.getX() < minX) {
                        minX = position.getX();
                    }
                    if (position.getY() < minY) {
                        minY = position.getY();
                    }
                }
            }
            player.setSize(maxX + "px", maxY + "px");
            super.center();
        } else {
            MessagePopup errorPopup = new MessagePopup(
                "No layout has been selected."
                + "\nPlease edit the layout to play the recording using flash.",
                null, MessagePopup.WARNING, MessageResponse.OK);
            errorPopup.center();
        }
    }

    public void onClick(ClickEvent event) {
        hide();
    }
}
