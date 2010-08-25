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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.text.NumberFormat;

import javax.media.Effect;
import javax.media.NoPlayerException;
import javax.media.control.BitRateControl;
import javax.media.control.FrameRateControl;
import javax.media.protocol.DataSource;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A panel for receiving video streams
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha2
 */
public class VideoPanel extends JPanel implements MouseListener, ItemListener,
        Comparable<VideoPanel>, UpdateablePanel {

    // The modifier for Giga
    private static final String GIGA = "G";

    // The modifier for Mega
    private static final String MEGA = "M";

    // The modifier for Kilo
    private static final String KILO = "k";

    // The number of bytes in a kilobyte
    private static final int BYTES_PER_KB = 1024;

    // The width of the information panel
    private static final int INFO_PANEL_WIDTH = 165;

    // The number of spaces in the rate panel
    private static final int RATE_SPACES = 3;

    // A Space
    private static final String SPACE = " ";

    // The text to display when the window is not displayed
    private static final String NOT_DISPLAYED_TEXT = "Not Displayed";

    // The text to represent frames per second
    private static final String FRAMES_PER_SECOND = "fps";

    // The text to represent bytes per second
    private static final String B_PS = "b/s";

    // The text when a name is unknown
    private static final String UNKNOWN_TEXT = "Unknown";

    // The total height taken up by borders
    private static final int BORDER_HEIGHT = 10;

    // The width of the status panel
    private static final int STATUS_WIDTH = 180;

    // The height of a medium window
    private static final double MEDIUM_HEIGHT = 288.0;

    // The width of a medium window
    private static final double MEDIUM_WIDTH = 352.0;

    // The x position of the preview
    private static final int PREVIEW_X = 5;

    // The y position of the preview
    private static final int PREVIEW_Y = 5;

    // The spacing between components
    private static final int SPACING = 5;

    // The default height of the preview window
    private static final int PREVIEW_HEIGHT = 85;

    // The default width of the preview window
    private static final int PREVIEW_WIDTH =
        (int) (PREVIEW_HEIGHT * (MEDIUM_WIDTH / MEDIUM_HEIGHT));

    // The width of the panel
    private static final int PANEL_WIDTH = PREVIEW_WIDTH + STATUS_WIDTH;

    // The height of the panel
    private static final int PANEL_HEIGHT = PREVIEW_HEIGHT + BORDER_HEIGHT;

    // The video player
    private VideoPlayer player;

    // True if the video is displayed
    private boolean displayed = false;

    // The panel for the preview
    private JPanel preview = new JPanel();

    // The name of the participant
    private JLabel format = new JLabel(UNKNOWN_TEXT);

    // The participant description
    private JLabel name = new JLabel(UNKNOWN_TEXT);

    // The bitrate of the stream
    private JLabel bitrate = new JLabel(0 + SPACE + KILO + B_PS);

    // The framerate of the stream
    private JLabel framerate = new JLabel(0 + SPACE + FRAMES_PER_SECOND);

    // A label to indicate if the window is displayed
    private JLabel display = new JLabel(NOT_DISPLAYED_TEXT);

    // The large window with the video in it
    private VideoWindow video;

    // The cname of the participant
    private String cname = new String(UNKNOWN_TEXT);

    // Stops the video when checked
    private JCheckBox mute = new JCheckBox("Mute", false);

    // True if the client is visible
    private boolean isVisible = true;

    // True if the video window is always on top
    private boolean isAlwaysOnTop = false;

    // The preview component
    private Component previewComp = null;

    /**
     * Creates a new VideoPanel
     *
     * @param ds
     *            The datasource to play with the panel
     * @param format The format of the data
     * @param muteNow True if the source should be muted
     * @throws IOException
     * @throws NoPlayerException
     */
    public VideoPanel(DataSource ds, String format, boolean muteNow) {
        this(ds, format, muteNow, new Effect[0]);
    }

    public VideoPanel(DataSource ds, String format, boolean muteNow,
            Effect[] effects) {

        // Create and start the player
        String error = null;
        try {
            player = new VideoPlayer(ds, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    effects);

            if (!muteNow) {
                player.start();
                mute.setSelected(false);
            } else {
                mute.setSelected(true);
            }
        } catch (Exception e) {
            error = e.getMessage();
            e.printStackTrace();
        }


        // Set the text
        this.format.setText(format);

        // Set up the panel
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(null);

        // Set up the rate display
        JPanel ratepanel = new JPanel();
        ratepanel.setLayout(new GridLayout(1, RATE_SPACES, 0, 0));
        ratepanel.add(bitrate);
        ratepanel.add(framerate);
        bitrate.setHorizontalTextPosition(JLabel.LEFT);
        bitrate.setVerticalTextPosition(JLabel.TOP);
        bitrate.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_SMALL));
        framerate.setHorizontalTextPosition(JLabel.LEFT);
        framerate.setVerticalTextPosition(JLabel.TOP);
        framerate.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_SMALL));
        ratepanel.setAlignmentX(0f);

        // Set up the display selection and mute
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.X_AXIS));
        displayPanel.add(display);
        displayPanel.add(Box.createHorizontalGlue());
        displayPanel.add(mute);
        display.setHorizontalTextPosition(JLabel.LEFT);
        display.setVerticalTextPosition(JLabel.TOP);
        display.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_SMALL));
        mute.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_SMALL));
        mute.addItemListener(this);
        displayPanel.setAlignmentX(0f);

        // Add all the information to a separate panel
        JPanel infopanel = new JPanel();
        infopanel.setLayout(new BoxLayout(infopanel, BoxLayout.Y_AXIS));
        infopanel.add(name);
        infopanel.add(Box.createVerticalGlue());
        infopanel.add(this.format);
        infopanel.add(Box.createVerticalGlue());
        infopanel.add(ratepanel);
        infopanel.add(Box.createVerticalGlue());
        infopanel.add(displayPanel);
        name.setHorizontalTextPosition(JLabel.LEFT);
        name.setVerticalTextPosition(JLabel.TOP);
        name.setFont(new Font(Constants.FONT,
                Constants.FONT_STYLE, Constants.FONT_SIZE_SMALL));
        name.setAlignmentX(0f);
        this.format.setHorizontalTextPosition(JLabel.LEFT);
        this.format.setVerticalTextPosition(JLabel.TOP);
        this.format.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_SMALL));
        this.format.setAlignmentX(0f);

        // Add the items to this panel
        add(preview);
        add(infopanel);

        // Set sizes and locations
        infopanel.setSize(INFO_PANEL_WIDTH, PREVIEW_HEIGHT);
        preview.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        preview.setLayout(null);
        preview.setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        infopanel.setLocation(PREVIEW_WIDTH + PREVIEW_X + SPACING, SPACING);
        preview.setLocation(PREVIEW_X, PREVIEW_Y);

        if (error == null) {
            previewComp = player.getPreviewComponent();
            preview.add(previewComp);
            previewComp.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
            previewComp.setLocation(0, 0);

            video = createVideoWindow(name.getText(), this);
            video.setVisible(false);

            // Make the preview clickable
            preview.addMouseListener(this);
            previewComp.addMouseListener(this);

            Thread playerAddThread = new Thread() {
                public void run() {
                    player.waitForFirstFrame();
                    video.setPlayer(player);
                }
            };
            playerAddThread.start();
        } else {
            JLabel errorLabel = new JLabel("<html><p>Error decoding stream: "
                    + error + "</p></html>");
            errorLabel.setFont(new Font(Constants.FONT,
                    Constants.FONT_STYLE, Constants.FONT_SIZE_SMALL));
            errorLabel.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
            preview.add(errorLabel);
        }
    }

    /**
     * Handles a change in the mute button
     *
     * @see java.awt.event.ItemListener
     *      #itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == mute) {
            boolean set = e.getStateChange() == ItemEvent.DESELECTED;
            if (!set) {
                player.stop();
            } else {
                player.start();
            }
        }
    }

    /**
     * Returns the CNAME of the participant
     * @return the CNAME
     */
    public String getCNAME() {
        return cname;
    }

    /**
     * Returns the format of the video
     * @return the format name
     */
    public String getFormat() {
        return this.format.getText();
    }

    /**
     * Returns the default size of the video window for the layout
     * @return the size
     */
    public static Dimension getDefaultSize() {
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }

    /**
     * Stops the playback of the video
     */
    public void stopPlaying() {
        video.setVisible(false);
        displayed = false;
        display.setText(NOT_DISPLAYED_TEXT);
    }

    /**
     * Handles the user clicking on the preview window
     *
     * @see java.awt.event.MouseListener
     *      #mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {

        if (!displayed) {

            // If the window is not displayed, display it
            video.setVisible(true);
            displayed = true;
            display.setText("Displayed");
        } else if (SwingUtilities.isRightMouseButton(e)) {

            // If the right mouse button was clicked hide the window
            video.setVisible(false);
            displayed = false;
            display.setText(NOT_DISPLAYED_TEXT);
        } else if (SwingUtilities.isLeftMouseButton(e)) {

            // If the left mouse button was clicked flash the window
            video.highlight();
        }
    }

    /**
     * @see java.awt.event.MouseListener
     *      #mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
        // Do Nothing
    }

    /**
     * @see java.awt.event.MouseListener #mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
        // Do Nothing
    }

    /**
     * @see java.awt.event.MouseListener
     *      #mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        // Do Nothing
    }

    /**
     * @see java.awt.event.MouseListener
     *      #mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
        // Do Nothing
    }

    /**
     * Stops the playback of the video
     */
    public void end() {
        player.stop();
        if (displayed) {
            video.closing();
            displayed = false;
        }
        DesktopContainer.getInstance().remove(video);
    }

    /**
     * Sets the CNAME of the video panel
     *
     * @param newCNAME
     *            the new CNAME to set
     */
    public void setCNAME(String newCNAME) {
        cname = newCNAME;
    }

    /**
     * Sets the name of the participant
     *
     * @param newName
     *            the new name to set
     */
    public void setName(String newName) {
        name.setText(newName);
        if (video != null) {
            video.setTitle(newName);
        }
    }

    /**
     * Refreshes the statistics and preview of the video
     */
    public void doRefresh() {

        // If this panel is invisible, don't do anything
        if (!isShowing()) {
            return;
        }
        if (previewComp != null) {
            previewComp.setSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }

        if (player == null) {
            return;
        }

        // Format and display the bitrate
        NumberFormat format = NumberFormat.getInstance();
        BitRateControl brc = (BitRateControl) player
                .getControl("javax.media.control.BitRateControl");
        if (brc != null) {
            String modifier = B_PS;
            format.setMaximumFractionDigits(0);
            double rate = brc.getBitRate();
            if (rate > BYTES_PER_KB) {
                rate = rate / BYTES_PER_KB;
                modifier = KILO + B_PS;
                format.setMaximumFractionDigits(1);
            }
            if (rate > BYTES_PER_KB) {
                rate = rate / BYTES_PER_KB;
                modifier = MEGA + B_PS;
            }
            if (rate > BYTES_PER_KB) {
                rate = rate / BYTES_PER_KB;
                modifier = GIGA + B_PS;
            }
            bitrate.setText(format.format(rate) + SPACE + modifier);
        }

        // Format and display the frame rate
        format.setMaximumFractionDigits(1);
        FrameRateControl frc = (FrameRateControl) player
                .getControl("javax.media.control.FrameRateControl");
        if (frc != null) {
            framerate.setText(format.format(frc.getFrameRate()) + SPACE
                    + FRAMES_PER_SECOND);
        }
    }

    /**
     * Compares the CNAMEs of two panels
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(VideoPanel panel) {
        return getCNAME().compareTo(panel.getCNAME());
    }

    protected VideoWindow createVideoWindow(String name, VideoPanel panel) {
        return new VideoWindow(name, panel);
    }

    /**
     * Returns the current video window
     * @return the video window
     */
    public VideoWindow getVideoWindow() {
        return video;
    }

    /**
     *
     * @see java.awt.Component#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            isVisible = false;
            end();
        } else if (!isVisible) {
            isVisible = true;
            player.start();
            video.setAlwaysOnTop(isAlwaysOnTop);
        }
    }

    /**
     * Mutes the video stream
     */
    public void mute() {
        player.stop();
        mute.setSelected(true);
    }

    /**
     * Unmutes the video stream
     */
    public void unmute() {
        player.start();
        mute.setSelected(false);
    }

    /**
     * Sets the always on top state of the video window
     * @param alwaysOnTop True if the window is to be always on top
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        isAlwaysOnTop = alwaysOnTop;
        video.setAlwaysOnTop(alwaysOnTop);
    }
}