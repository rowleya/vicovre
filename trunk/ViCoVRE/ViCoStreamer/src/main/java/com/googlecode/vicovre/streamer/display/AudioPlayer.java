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

import java.io.IOException;

import javax.media.Control;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.NoPlayerException;
import javax.media.Processor;
import javax.media.protocol.DataSource;

/**
 * Displays a video stream in a separate window
 *
 * @author Andrew G D Rowley
 */
class AudioPlayer implements ControllerListener {

    private static final String CONTROLLER_ERROR =
        "Controller would not configure";

    // True if the processor fails to realise
    private boolean processorFailed = false;

    // An object to allow locking
    private Integer stateLock = new Integer(0);

    // The processing player
    private Processor player;

    /**
     * Creates a new AudioPlayer
     *
     * @param ds
     *            The datasource to play
     * @param parent
     *            The ReceviePanel to report to
     *
     * @throws IOException
     * @throws NoPlayerException
     */
    public AudioPlayer(DataSource ds) throws IOException,
            NoPlayerException {

        // Configure the player
        player = javax.media.Manager.createProcessor(ds);
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
        }
    }

    /**
     * Returns a control for this player
     *
     * @param classname
     *            The name of the class of the control
     * @return The control
     */
    public Control getControl(String classname) {
        return player.getControl(classname);
    }

    /**
     * Returns the state of the player
     * @return the state
     */
    public int getState() {
        return player.getState();
    }

    /**
     * Starts the playback
     */
    public void start() {
        player.start();
    }

    /**
     * Stops the playback
     */
    public void stop() {
        player.stop();
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