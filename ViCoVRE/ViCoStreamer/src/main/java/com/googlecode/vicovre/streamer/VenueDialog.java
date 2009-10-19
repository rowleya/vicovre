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

package com.googlecode.vicovre.streamer;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;

import com.googlecode.vicovre.media.config.Config;
import com.googlecode.vicovre.media.ui.AccessGridPanel;

public class VenueDialog extends JDialog implements ActionListener {

    private final ClientProfile clientProfile = new ClientProfile();

    private AccessGridPanel venue = null;

    private JButton okButton = new JButton("OK");

    private JButton cancelButton = new JButton("Cancel");

    private boolean cancelled = true;

    public VenueDialog(Frame parent, Capability[] videoCapabilities,
            Capability[] audioCapabilities) {
        super(parent, "Select a venue", true);

        venue = new AccessGridPanel(this, false,
                videoCapabilities, audioCapabilities, clientProfile,
                "ViCoStreamer");

        JPanel content = new JPanel();
        add(content);
        setSize(600, 200);
        setLocationRelativeTo(null);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(cancelButton);
        venue.setAlignmentX(CENTER_ALIGNMENT);

        content.add(venue);
        content.add(Box.createVerticalGlue());
        content.add(buttonPanel);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            cancelled = true;
            venue.captureInitialValues();
        }
        super.setVisible(visible);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            if (venue.getVenue() == null) {
                JOptionPane.showMessageDialog(this, "No venue chosen!", "Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                cancelled = false;
                setVisible(false);
            }
        } else {
            cancelled = true;
            venue.resetToInitialValues();
            setVisible(false);
        }
    }

    public String getVenue() {
        return venue.getVenue();
    }

    public BridgeDescription getBridge() {
        return venue.getBridge();
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public void setConfig(Config config) {
        venue.init(config);
    }

    public void storeConfig(Config config) {
        venue.storeConfiguration(config);
    }

}
