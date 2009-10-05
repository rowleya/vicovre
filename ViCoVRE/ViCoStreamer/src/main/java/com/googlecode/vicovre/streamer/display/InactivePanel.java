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
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A panel for inactive participants (so you know who is listening in!)
 *
 * @author Andrew G D Rowley
 */
class InactivePanel extends JPanel {

    // The height of the panel
    private static final int PANEL_HEIGHT = 20;

    // The number of items in the panel
    private static final int NO_ITEMS = 2;

    // The string to display when the name is not known
    private static final String UNKNOWN_NAME_STRING = "Unknown";

    // The name of the participant
    private JLabel name = new JLabel(UNKNOWN_NAME_STRING);

    // The CNAME of the participant
    private String cname = new String(UNKNOWN_NAME_STRING);

    /**
     * Creates a new InactivePanel
     *
     * @param cname
     *            The CNAME of the panel
     */
    public InactivePanel(String cname) {
        this.cname = cname;

        // Setup the panel
        setLayout(new GridLayout(1, NO_ITEMS, 0, 0));
        JLabel inactive = new JLabel("Inactive:");
        add(inactive);
        add(name);

        // Set the text formatting
        name.setHorizontalTextPosition(JLabel.LEFT);
        name.setVerticalTextPosition(JLabel.TOP);
        name.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_NORM));
        inactive.setHorizontalTextPosition(JLabel.LEFT);
        inactive.setVerticalTextPosition(JLabel.TOP);
        inactive.setFont(new Font(Constants.FONT, Constants.FONT_STYLE,
                Constants.FONT_SIZE_NORM));

        // Setup the panel some more
        Dimension d = VideoPanel.getDefaultSize();
        d.setSize(d.getWidth(), PANEL_HEIGHT);
        setPreferredSize(d);
        setBorder(BorderFactory.createEtchedBorder());
    }

    /**
     * Sets the name of the participant
     */
    public void setName(String newName) {
        name.setText(newName);
    }

    /**
     * Finishes with the panel
     */
    public void end() {
        // Does Nothing
    }

    /**
     * Gets the CNAME of the particpant
     *
     * @return The CNAME of the participant
     */
    public String getCNAME() {
        return cname;
    }
}