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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.media.CannotRealizeException;
import javax.media.Format;
import javax.media.NoPlayerException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.rtcp.SourceDescription;
import javax.sound.sampled.FloatControl;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.googlecode.onevre.ag.types.ClientProfile;
import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.media.rtp.JoinListener;
import com.googlecode.vicovre.media.rtp.SendOnlyRTPSocketAdapter;
import com.googlecode.vicovre.media.ui.LocalDevicePanel;
import com.googlecode.vicovre.media.ui.LocalStreamListener;
import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.utils.Config;

public class LocalDeviceDialog extends JDialog implements ActionListener,
        LocalStreamListener, JoinListener {

    private LocalDevicePanel panel = null;

    private JButton okButton = new JButton("OK");

    private JButton cancelButton = new JButton("Cancel");

    private boolean cancelled = true;

    private BridgedRTPConnector videoConnector = null;

    private BridgedRTPConnector audioConnector = null;

    private JTextField webPort = new JTextField();

    private String initialWebPort = "";

    public LocalDeviceDialog(Frame parent,
            RTPType[] videoTypes, RTPType[] audioTypes,
            boolean allowTypeSelection,
            BridgedRTPConnector videoConnector,
            BridgedRTPConnector audioConnector,
            LocalStreamListener listener,
            ClientProfile profile) {
        super(parent, "Select Devices to Transmit", true);
        setSize(600, 320);
        setLocationRelativeTo(parent);
        JPanel content = new JPanel();
        add(content);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.videoConnector = videoConnector;
        this.audioConnector = audioConnector;

        panel = new LocalDevicePanel(this, videoTypes,
                audioTypes, allowTypeSelection,
                new SendOnlyRTPSocketAdapter(videoConnector),
                new SendOnlyRTPSocketAdapter(audioConnector),
                profile, "ViCoStreamer",
                new VideoFormat[]{
                    new YUVFormat(YUVFormat.YUV_420),
                    new RGBFormat(null, -1, Format.intArray, -1, 32, -1, -1,
                            -1, -1, -1, -1, -1),
                    new RGBFormat(null, -1, Format.byteArray, -1, 24, -1, -1,
                            -1, -1, -1, -1, -1)
                });
        content.add(panel);

        JPanel webPanel = new JPanel();
        content.add(webPanel);
        webPanel.setLayout(new BoxLayout(webPanel, BoxLayout.X_AXIS));
        webPanel.add(new JLabel("Web Server Port:"));
        webPanel.add(Box.createHorizontalStrut(5));
        webPanel.add(webPort);
        webPort.setMaximumSize(new Dimension(75, 20));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(okButton);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        content.add(buttons);

        panel.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        webPanel.setAlignmentX(LEFT_ALIGNMENT);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        panel.addLocalStreamListener(this);
        panel.addLocalStreamListener(listener);
    }

    public void setConfiguration(Config configuration) {
        this.webPort.setText(configuration.getParameter("webPort", "7890"));
        panel.init(configuration);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            cancelled = true;
            initialWebPort = webPort.getText();
            panel.captureInitialValues();
        }
        super.setVisible(visible);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            cancelled = false;
            panel.changeDevices();
            setVisible(false);
        } else if (e.getSource().equals(cancelButton)) {
            cancelled = true;
            webPort.setText(initialWebPort);
            panel.resetToInitialValues();
            setVisible(false);
        }
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public void addLocalAudio(String name, DataSource dataSource,
            FloatControl volumeControl, long ssrc) throws NoPlayerException,
            CannotRealizeException, IOException {
        audioConnector.addStream(ssrc);
    }

    public void addLocalVideo(String name, DataSource dataSource, long ssrc)
            throws IOException {
        videoConnector.addStream(ssrc);
    }

    public void removeLocalAudio(DataSource dataSource) {
        // Do Nothing
    }

    public void removeLocalVideo(DataSource dataSource) {
        // Do Nothing
    }

    public void changeClientProfile(ClientProfile profile) {
        panel.changeClientProfile(profile);
    }

    public void stopDevices() {
        panel.stopDevices();
    }

    public void participantJoined(SourceDescription[] sourceDescription) {
        System.err.println("New participant");
        panel.forceKeyFrame();
    }

    public void setWebPort(int webPort) {
        this.webPort.setText(String.valueOf(webPort));
    }

    public int getWebPort() {
        return Integer.parseInt(this.webPort.getText());
    }

}
