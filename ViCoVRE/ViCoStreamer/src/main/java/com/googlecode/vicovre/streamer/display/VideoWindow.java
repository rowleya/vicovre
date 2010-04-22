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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;

/**
 * A window containing a video player
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha2
 */
class VideoWindow extends Frame {

    // The multiplier for the large size
    private static final int LARGE_SIZE_MULTIPLIER = 2;

    // The divider for the small size
    private static final int SMALL_SIZE_DIVIDER = 2;

    // The time in ms to wait between flashes
    private static final int FLASH_WAIT_TIME = 100;

    // The number of times to flash the window
    private static final int TIMES_TO_FLASH = 2;

    // The default width of the video
    private static final int DEFAULT_WIDTH = 352;

    // The offset of the key pressed
    private static final int KEY_OFFSET = 5;

    // The width to remove for key '4'
    private static final int NEGATIVE_MULTIPLIER_WIDTH = 59;

    // The width to add for key '6'
    private static final int POSITIVE_MULTIPLIER_WIDTH = 118;

    // The height to remove for key '4'
    private static final int NEGATIVE_MULTIPLIER_HEIGHT = 48;

    // The width to add for key '6'
    private static final int POSITIVE_MULTIPLIER_HEIGHT = 96;

    // The player visual component
    private Component playerComponent;

    // The default size of the video
    private Dimension videoSize;

    // The panel to which the video belongs
    private VideoPanel panel;

    // The key listener
    private KeyListener keylistener = null;

    // True until the player has been visible at least once
    private boolean firstVisible = true;

    /**
     * Creates a new VideoWindow
     *
     * @param name
     *            The title of the window
     * @param panel
     *            The panel that owns the window
     */
    public VideoWindow(String name, VideoPanel panel) {

        // Set the title of the window
        super(name);

        // Set the other variables
        this.panel = panel;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Set up listeners for the window closing and the keys being pressed
        addWindowListener(new FrameAdapter(this));
        keylistener = new KeyPress(this);

        DesktopContainer.getInstance().add(this);
    }

    protected void setPlayer(VideoPlayer player) {
        playerComponent = player.getVisualComponent();
        videoSize = playerComponent.getPreferredSize();
        add(playerComponent, 0);
        setPreferredSize(getDefaultSize());
        keyPressed('m');
    }

    /**
     * Handles the window closing
     */
    public void closing() {
        panel.stopPlaying();
    }

    /**
     *
     * @see java.awt.Component#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (playerComponent != null) {
            playerComponent.setVisible(visible);
        }
        if (!visible) {
            processWindowEvent(new WindowEvent(this,
                    WindowEvent.WINDOW_CLOSED));
            removeKeyListener(keylistener);
        } else {
            if (firstVisible) {
                keyPressed('m');
                firstVisible = false;
            }
            processWindowEvent(new WindowEvent(this,
                    WindowEvent.WINDOW_OPENED));
            addKeyListener(keylistener);
            validate();
        }
    }

    /**
     * Causes the window to flash
     */
    public void highlight() {
        System.err.println("Size = " + getSize());
        try {

            // Make the window visible and invisible repeatedly
            for (int i = 0; i < TIMES_TO_FLASH; i++) {
                toFront();
                Thread.sleep(FLASH_WAIT_TIME);
                toBack();
                Thread.sleep(FLASH_WAIT_TIME);
            }
            toFront();
        } catch (InterruptedException e) {
            // Do Nothing
        }
    }

    public Dimension getDefaultSize() {
        if (playerComponent == null) {
            return new Dimension(DEFAULT_WIDTH, DEFAULT_WIDTH);
        }
        return videoSize;
    }

    /**
     * Handles the pressing of a key
     *
     * @param key
     *            The key that was pressed
     */
    public void keyPressed(char key) {
        Dimension size = getDefaultSize();

        // Calculate the default height and width
        int width = DEFAULT_WIDTH;
        int height = (int) ((DEFAULT_WIDTH * size.getHeight()) / size
                .getWidth());

        // Resize the window depending on the key
        // Size of transmitting stream
        if ((key == 'D') || (key == 'd')) {
            setPlayerSize(size.width, size.height);
        }

        // Small size
        if ((key == 'S') || (key == 's')) {
            setPlayerSize((width / SMALL_SIZE_DIVIDER),
                    (height / SMALL_SIZE_DIVIDER));
        }

        // Medium size
        if ((key == 'M') || (key == 'm')) {
            setPlayerSize(width, height);
        }

        // Large size
        if ((key == 'L') || (key == 'l')) {
            setPlayerSize((width * LARGE_SIZE_MULTIPLIER),
                    (height * LARGE_SIZE_MULTIPLIER));
        }

        // Size between very small and very large (2 = small, 5 = medium, 8 =
        // large)
        if ((key >= '1') && (key <= '9')) {
            int value = (key - '0') - KEY_OFFSET;
            int wdiff = value < 0 ? NEGATIVE_MULTIPLIER_WIDTH
                    : POSITIVE_MULTIPLIER_WIDTH;
            int hdiff = value < 0 ? NEGATIVE_MULTIPLIER_HEIGHT
                    : POSITIVE_MULTIPLIER_HEIGHT;
            width = (width + (wdiff * value));
            height = (height + (hdiff * value));
            setPlayerSize(width, height);
        }
    }

    private void setPlayerSize(int width, int height) {
        if (playerComponent != null) {
            playerComponent.setPreferredSize(new Dimension(width, height));
        }
        Insets i = getInsets();
        int woff = i.left + i.right;
        int hoff = i.top + i.bottom;
        setSize(width + woff, height + hoff);
    }

    /**
     * Returns the CNAME of the video
     * @return the CNAME
     */
    public String getCNAME() {
        return panel.getCNAME();
    }

    /**
     * Handles the closing of the window
     */
    class FrameAdapter extends WindowAdapter {

        // The window to close
        private VideoWindow v;

        /**
         * Creates a new FrameAdapter
         *
         * @param v
         *            The window to close
         */
        public FrameAdapter(VideoWindow v) {
            this.v = v;
        }

        /**
         *
         * @see java.awt.event.WindowListener
         *     #windowClosing(java.awt.event.WindowEvent)
         */
        public void windowClosing(WindowEvent e) {
            v.closing();
        }
    }

    /**
     * Handles the pressing of keys
     */
    class KeyPress extends KeyAdapter {

        // The window to notify
        private VideoWindow v;

        /**
         * Creates a new KeyPress
         *
         * @param v
         *            The window to notify
         */
        public KeyPress(VideoWindow v) {
            this.v = v;
        }

        /**
         *
         * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
         */
        public void keyTyped(KeyEvent e) {
            v.keyPressed(e.getKeyChar());
        }
    }
}