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
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.NumberFormat;

import javax.media.GainControl;
import javax.media.NoPlayerException;
import javax.media.control.BitRateControl;
import javax.media.protocol.DataSource;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Used to display audio streams.
 *
 * Presents a slider and mute control for each stream
 *
 * @author Andrew G D Rowley
 */
class AudioPanel extends JPanel implements ChangeListener, ItemListener,
        UpdateablePanel {

    // The number of bytes in a kilobyte
    private static final int BYTES_PER_KB = 1024;

    // The class of the Gain control
    private static final String GAIN_CONTROL_CLASS = "javax.media.GainControl";

    // The gain slider default value
    private static final int GAIN_SLIDER_DEFAULT = 50;

    // The gain slider max value
    private static final int GAIN_SLIDER_MAX = 100;

    // The gain slider min value
    private static final int GAIN_SLIDER_MIN = 0;

    // The string to put if the name is unknown
    private static final String UNKNOWN_NAME_STRING = "Unknown";

    // The width of the components
    private static final int SLIDER_WIDTH = 240;

    // The height of the components
    private static final int SLIDER_HEIGHT = 15;

    // The height of the panel
    private static final int PANEL_HEIGHT = 50;

    // The name of the sender
    private JLabel name = new JLabel(UNKNOWN_NAME_STRING);

    // The bitrate of the stream
    private JLabel bitrate = new JLabel("0 kb/s");

    // The player of the stream
    private AudioPlayer player;

    // The cname of the sender
    private String cname = new String(UNKNOWN_NAME_STRING);

    // The gain control
    private GainControl gain;

    // A slider to allow gain adjustments
    private JSlider slider = new JSlider(GAIN_SLIDER_MIN, GAIN_SLIDER_MAX,
            GAIN_SLIDER_DEFAULT);

    // A mute checkbox
    private JCheckBox mute = new JCheckBox("Mute", false);

    /**
     * Creates a new AudioPanel.
     *
     * @param ds The datasource to play
     * @param muteNow True if the panel should be muted
     * @throws IOException
     * @throws NoPlayerException
     */
    public AudioPanel(DataSource ds, boolean muteNow) {

        String error = null;
        try {

            // Create and start a player for the stream
            player = new AudioPlayer(ds);
            player.start();
        } catch (Exception e) {
            error = e.getMessage();
            e.printStackTrace();
        }

        // Setup the display
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
        textPanel.add(name);
        textPanel.add(Box.createHorizontalGlue());
        textPanel.add(bitrate);
        add(textPanel);

        // Setup the text displays
        name.setHorizontalTextPosition(JLabel.LEFT);
        name.setVerticalTextPosition(JLabel.TOP);
        name.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_NORM));
        bitrate.setHorizontalTextPosition(JLabel.LEFT);
        bitrate.setVerticalTextPosition(JLabel.TOP);
        bitrate.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_NORM));

        if (error == null) {

            // Add a gain slider
            gain = (GainControl) player.getControl(GAIN_CONTROL_CLASS);
            gain.setMute(muteNow);
            slider.setValue((int) (gain.getLevel()
                    * (GAIN_SLIDER_MAX - GAIN_SLIDER_MIN)));
            slider.addChangeListener(this);
            slider.setPreferredSize(new Dimension(SLIDER_WIDTH, SLIDER_HEIGHT));

            // Add the mute checkbox
            mute.setSelected(gain.getMute());
            mute.addItemListener(this);
            mute.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                    Constants.FONT_SIZE_NORM));

            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
            controlPanel.add(slider);
            controlPanel.add(Box.createRigidArea(new Dimension(
                    Constants.SPACING, 0)));
            controlPanel.add(mute);
            add(controlPanel);
        } else {
            add(new JLabel("Error: " + error));
        }

        // Set the size and outline
        setPreferredSize(new Dimension(VideoPanel.getDefaultSize().width,
                PANEL_HEIGHT));
        setBorder(BorderFactory.createEtchedBorder());

    }

    /**
     * Handles changes in the mute checkbox
     *
     * @see java.awt.event.ItemListener#itemStateChanged
     *      (java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == mute) {
            boolean set = e.getStateChange() == ItemEvent.SELECTED;
            gain.setMute(set);
        }
    }

    /**
     * Handles changes in the gain slider
     *
     * @see javax.swing.event.ChangeListener#stateChanged
     *      (javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == slider) {
            int value = slider.getValue();
            gain.setLevel((float) value / (GAIN_SLIDER_MAX - GAIN_SLIDER_MIN));
        }
    }

    /**
     * Stops the audio process
     */
    public void end() {
        if (player != null) {
            player.stop();
        }
    }

    /**
     * Sets the name to be displayed
     *
     * @param newName
     *            the name to be set
     */
    public void setName(String newName) {
        name.setText(newName);
    }

    /**
     * Sets the CNAME of this stream
     *
     * @param newCNAME
     *            The CNAME to set
     */
    public void setCNAME(String newCNAME) {
        cname = newCNAME;
    }

    /**
     * Retreives the CNAME of the stream
     *
     * @return The CNAME of the stream
     */
    public String getCNAME() {
        return cname;
    }

    /**
     * Updates the stream data
     */
    public void doRefresh() {

        // Get the rate
        if (player != null) {
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(0);
            BitRateControl brc = (BitRateControl) player
                    .getControl("javax.media.control.BitRateControl");
            if (brc != null) {
                bitrate.setText(format.format((double) brc.getBitRate()
                        / BYTES_PER_KB) + " kb/s");
            }
        }
    }

    /**
     *
     * @see java.awt.Component#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            end();
        } else {
            if (player != null) {
                player.start();
            }
        }
    }
}