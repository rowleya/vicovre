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

package com.googlecode.vicovre.gwt.recorder.client;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.PlayItemChangeState;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.PlayItemSeek;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.PlayItemTimeUpdate;
import com.googlecode.vicovre.gwt.recorder.client.xmlrpc.PlayItemToVenue;

public class PlayToVenuePopup extends ModalPopup<VerticalPanel>
        implements SlideChangeHandler, ClickHandler {

    private static final int WIDTH = 800;

    private static final int HEIGHT = 200;

    private final Image PLAY = new Image("images/play.gif");

    private final Image PAUSE = new Image("images/pause.gif");

    private final Image STOP = new Image("images/stop.gif");

    private PlayItem item = null;

    private VenuePanel venue = new VenuePanel();

    private ToggleButton playButton = new ToggleButton(PLAY, PAUSE);

    private PushButton stopButton = new PushButton(STOP);

    private Label time = new Label("00:00:00");

    private SliderBar bar = new SliderBar();

    private boolean playing = false;

    private int id = 0;

    private PlayItemTimeUpdate timeUpdater =
        PlayItemTimeUpdate.getUpdater(this);

    public PlayToVenuePopup(PlayItem item) {
        super(new VerticalPanel());
        this.item = item;

        VerticalPanel panel = getWidget();
        panel.setWidth(WIDTH + "px");
        panel.setHeight(HEIGHT + "px");

        HorizontalPanel controls = new HorizontalPanel();
        controls.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        controls.add(playButton);
        controls.add(new Label(" "));
        controls.add(stopButton);
        controls.add(new Label(" "));
        controls.add(bar);
        controls.add(time);
        controls.add(new Label(" "));

        controls.setCellWidth(playButton, "30px");
        controls.setCellWidth(stopButton, "30px");
        controls.setCellWidth(time, "75px");
        controls.setCellWidth(bar, "100%");
        controls.setCellHorizontalAlignment(time, HorizontalPanel.ALIGN_RIGHT);
        playButton.setWidth("30px");
        stopButton.setWidth("30px");
        time.setWidth("80px");
        bar.setWidth("100%");
        bar.setHeight("30px");
        controls.setWidth("100%");
        venue.setWidth("100%");
        venue.allowManualAddresses(false);

        panel.add(venue);
        panel.add(controls);
        bar.addSlideChangeHandler(this);
        playButton.addClickHandler(this);
        stopButton.addClickHandler(this);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getItemFolder() {
        return item.getFolder();
    }

    public String getItemId() {
        return item.getId();
    }

    public String getVenueUrl() {
        return venue.getVenue();
    }

    private void setTimePosition(float position) {
        long value = (long) (item.getDuration() * position);
        time.setText(PlayItem.getTimeText(value));
    }

    public void slideValueChanged(float position) {
        setTimePosition(position);
        int value = (int) (item.getDuration() * position);
        if (!playing) {
            startPlay(value);
        } else {
            PlayItemSeek.seek(this, value);
            PlayItemChangeState.resume(this);
            timeUpdater.start();
        }
    }

    public void slideValueChanging(float position) {
        timeUpdater.stop();
        PlayItemChangeState.pause(this);
        setTimePosition(position);
    }

    public long getTime() {
        return (long) (bar.getPosition() * item.getDuration());
    }

    public void setTime(long time) {
        GWT.log("Setting time to " + time, null);
        float position = (float) time / item.getDuration();
        setTimePosition(position);
        bar.setPosition(position);
    }

    public void setPlaying() {
        playing = true;
        venue.setEnabled(false);
        playButton.setDown(true);
        timeUpdater.start();
    }

    public void setPaused() {
        playButton.setDown(false);
    }

    public void setStopped() {
        playButton.setDown(false);
        venue.setEnabled(true);
        playing = false;
        timeUpdater.stop();
    }

    private void startPlay(int seek) {
        if ((venue.getVenue() == null)
                && (venue.getAddresses() == null)) {
            MessagePopup error = new MessagePopup(
                    "You must specify a venue to play to", null,
                    MessagePopup.ERROR, MessageResponse.OK);
            error.center();
            playButton.setDown(false);
        } else {
            PlayItemToVenue.play(this, seek);
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(playButton)) {
            if (playButton.isDown()) {
                if (!playing) {
                    startPlay(0);
                } else {
                    PlayItemChangeState.resume(this);
                }
            } else {
                PlayItemChangeState.pause(this);
            }
        } else if (event.getSource().equals(stopButton)) {
            PlayItemChangeState.stop(this);
        }
    }
}
