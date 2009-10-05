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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

    // The player contained in the window
    private VideoPlayer player;

    // The panel to which the video belongs
    private VideoPanel panel;

    // The width offset of the panel
    private int woff = 0;

    // The height offset of the panel
    private int hoff = 0;

    // The preferred size of the video format
    private Dimension preferredSize =
        new Dimension(DEFAULT_WIDTH, DEFAULT_WIDTH);

    // The key listener
    private KeyListener keylistener = null;

    /**
     * Creates a new VideoWindow
     *
     * @param name
     *            The title of the window
     * @param p
     *            The player to play video with
     * @param panel
     *            The panel that owns the window
     */
    public VideoWindow(String name, VideoPlayer p, VideoPanel panel) {

        // Set the title of the window
        super(name);

        // Set the other variables
        player = p;
        this.panel = panel;

        // Set up listeners for the window closing and the keys being pressed
        addWindowListener(new FrameAdapter(this));
        keylistener = new KeyPress(this);

        // Show the window in its default state
        pack();

        // Get the default offsets for the window (title bar etc)
        Insets i = getInsets();
        woff = i.left + i.right;
        hoff = i.top + i.bottom;

        // Set the window preferred size
        preferredSize = player.getVisualComponent()
                .getPreferredSize();
        setPreferredSize(preferredSize);

        // Add the player to the window
        add(player.getVisualComponent());

        keyPressed('m');
        DesktopContainer.getInstance().add(this);
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
        player.getVisualComponent().setVisible(visible);
        if (!visible) {
            processWindowEvent(new WindowEvent(this,
                    WindowEvent.WINDOW_CLOSED));
            removeKeyListener(keylistener);
        } else {
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

    /**
     * Handles the pressing of a key
     *
     * @param key
     *            The key that was pressed
     */
    public void keyPressed(char key) {
        Dimension size = preferredSize;

        // Calculate the default height and width
        int width = DEFAULT_WIDTH;
        int height = (int) ((DEFAULT_WIDTH * size.getHeight()) / size
                .getWidth());

        // Resize the window depending on the key
        // Size of transmitting stream
        if ((key == 'D') || (key == 'd')) {
            setSize((int) size.getWidth() + woff, (int) size.getHeight()
                    + hoff);
        }

        // Small size
        if ((key == 'S') || (key == 's')) {
            setSize((width / SMALL_SIZE_DIVIDER) + woff,
                    (height / SMALL_SIZE_DIVIDER) + hoff);
        }

        // Medium size
        if ((key == 'M') || (key == 'm')) {
            setSize((width) + woff, (height) + hoff);
        }

        // Large size
        if ((key == 'L') || (key == 'l')) {
            setSize((width * LARGE_SIZE_MULTIPLIER) + woff,
                    (height * LARGE_SIZE_MULTIPLIER) + hoff);
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
            setSize(width + woff, height + hoff);
        }
    }

    /**
     *
     * @see java.awt.Component#setSize(int, int)
     */
    public void setSize(int width, int height) {
        super.setSize(width, height);
        player.getVisualComponent().setSize(width - woff, height - hoff);
    }

    /**
     *
     * @see java.awt.Component#setSize(java.awt.Dimension)
     */
    public void setSize(Dimension size) {
        setSize(size.width, size.height);
    }

    /**
     *
     * @see java.awt.Component#setBounds(int, int, int, int)
     */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (player != null) {
            player.getVisualComponent().setSize(width - woff, height - hoff);
        }
    }

    /**
     *
     * @see java.awt.Component#setBounds(java.awt.Rectangle)
     */
    public void setBounds(Rectangle bounds) {
        setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Restarts playback of the given stream
     *
     * @param p
     *            the player to use
     */
    public void restart(VideoPlayer p) {
        removeAll();
        add(p.getVisualComponent());
        pack();

        // Get the default offsets for the window (title bar etc)
        Insets i = getInsets();
        woff = i.left + i.right;
        hoff = i.top + i.bottom;

        // Set the window preferred size
        preferredSize = p.getVisualComponent().getPreferredSize();
        preferredSize.width += woff;
        preferredSize.height += hoff;
        setPreferredSize(preferredSize);
        setSize(getSize());
        player = p;
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