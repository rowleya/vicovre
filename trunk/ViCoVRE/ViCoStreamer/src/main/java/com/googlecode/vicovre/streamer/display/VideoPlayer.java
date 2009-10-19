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

package com.googlecode.vicovre.streamer.display;

import java.awt.Component;
import java.awt.Rectangle;
import java.io.IOException;

import javax.media.Control;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Effect;
import javax.media.Format;
import javax.media.NoPlayerException;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.renderer.VideoRenderer;
import javax.swing.JLabel;

import com.googlecode.vicovre.media.renderer.RGBRenderer;

/**
 * Displays a video stream in a separate window
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha2
 */
class VideoPlayer implements ControllerListener {

    // The error to send when the controller has an error
    private static final String CONTROLLER_ERROR =
        "Controller would not configure";

    // True if the processor fails to realise
    private boolean processorFailed = false;

    // An object to allow locking
    private Integer stateLock = new Integer(0);

    // The processing player
    //private Processor player;

    // The renderer
    private RGBRenderer renderer;

    // The preview width
    private int previewwidth = 0;

    // The preview height
    private int previewheight = 0;

    /**
     * Creates a new VideoPlayer
     *
     * @param ds
     *            The datasource to play
     * @param previewwidth
     *            The width of the preview
     * @param previewheight
     *            The height of the preview
     * @throws IOException
     * @throws NoPlayerException
     */
    public VideoPlayer(DataSource ds, int previewwidth, int previewheight)
            throws IOException, NoPlayerException {
        this.previewwidth = previewwidth;
        this.previewheight = previewheight;

        PushBufferStream[] datastreams =
            ((PushBufferDataSource) ds).getStreams();
        renderer = new RGBRenderer(new Effect[0]);
        renderer.setDataSource(ds, 0);
        renderer.setInputFormat(datastreams[0].getFormat());

        // Configure the player
        /*player = javax.media.Manager.createProcessor(ds);
        player.addControllerListener(this);
        player.configure();
        processorFailed = false;
        while (!processorFailed && (player.getState() < Processor.Configured)) {
            synchronized (stateLock) {
                try {
                    stateLock.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }
        if (processorFailed) {
            throw new NoPlayerException(CONTROLLER_ERROR);
        }
        player.setContentDescriptor(null);

        // Set the format of the transmission
        TrackControl[] tracks = player.getTrackControls();
        for (int i = 0; i < tracks.length; i++) {
            Format f = tracks[i].getFormat();
            if (tracks[i].isEnabled() && (f instanceof VideoFormat)) {
                if (renderer != null) {
                    try {
                        tracks[i].setRenderer(renderer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Realise the processor
        player.realize();
        processorFailed = false;
        while (!processorFailed && (player.getState() < Processor.Realized)) {
            synchronized (stateLock) {
                try {
                    stateLock.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }
        if (processorFailed) {
            throw new NoPlayerException(CONTROLLER_ERROR);
        } */
        renderer.getComponent().setVisible(false);
    }

    /**
     * Gets the preview component of the player
     * @return The preview graphical component
     */
    public Component getPreviewComponent() {
        /*while (player.getState() < Processor.Realized) {
            synchronized (stateLock) {
                try {
                    stateLock.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        } */
        VideoRenderer preview = renderer.getPreviewRenderer();
        Component comp = null;
        if (preview != null) {
            preview.setBounds(new Rectangle(0, 0, previewwidth, previewheight));
            comp = preview.getComponent();
        } else {
            comp = new JLabel("<html><p>Preview Disabled in Code!</p></html>");
        }
        comp.setSize(previewwidth, previewheight);
        return comp;
    }

    /**
     * Returns a component where the video is displayed
     * @return the graphical component for the video
     */
    public Component getVisualComponent() {
        /*while (player.getState() < Processor.Realized) {
            synchronized (stateLock) {
                try {
                    stateLock.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        } */
        return renderer.getComponent();
    }

    /**
     * Returns a control for this player
     *
     * @param classname
     *            The name of the class of the control
     * @return the control
     */
    public Control getControl(String classname) {
        return (Control) renderer.getControl(classname);
    }

    /**
     * Starts the playback
     */
    public void start() {
        renderer.start();
    }

    /**
     * Stops the playback
     */
    public void stop() {
        renderer.stop();
    }

    /**
     * Waits for the first frame to become available
     */
    public void waitForFirstFrame() {
        renderer.waitForFirstFrame();
    }

    /**
     * @see javax.media.ControllerListener
     *     #controllerUpdate(javax.media.ControllerEvent)
     */
    public synchronized void controllerUpdate(ControllerEvent ce) {

        // If there was an error during configure or
        // realize, the processor will be closed
        if (ce instanceof ControllerClosedEvent) {
            processorFailed = true;
        }

        // All controller events, send a notification
        // to the waiting thread in waitForState method.
        synchronized (stateLock) {
            stateLock.notifyAll();
        }
    }
}
