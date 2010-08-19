/**
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

package com.googlecode.vicovre.recorder.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.googlecode.vicovre.media.rtp.LocalRTPConnector;
import com.googlecode.vicovre.media.rtp.StreamListener;
import com.googlecode.vicovre.media.ui.AccessGridPanel;
import com.googlecode.vicovre.media.ui.LocalDevicePanel;
import com.googlecode.vicovre.recorder.Recorder;
import com.googlecode.vicovre.recordings.RecordArchiveManager;
import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.Config;

import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;

/**
 * A Dialog box that allows the user to choose the source of the recorded
 * streams i.e. local cameras or Access Grid venue
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordingSourceDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String LAST_SOURCE_LOCAL = "lastSourceLocal";

    private static final String LAST_SOURCE_AG = "lastSourceAG";

    // The width of the dialog
    private static final int DIALOG_WIDTH = 620;

    // The height of the dialog
    private static final int DIALOG_HEIGHT = 565;

    // The width of the border
    private static final int BORDER_WIDTH = 5;

    // The video rtp type
    private static final int[] VIDEO_RTP_TYPES = new int[]{77, 96, 31};

    private static final int[] AUDIO_RTP_TYPES = new int[]{84, 112};

    private final ClientProfile clientProfile = new ClientProfile();

    private static final Capability VIDEO_CAPABILITY =
        new Capability(Capability.CONSUMER, Capability.VIDEO, "H261",
            Capability.VIDEO_RATE, 1);

    private static final Capability AUDIO_CAPABILITY =
        new Capability(Capability.CONSUMER, Capability.AUDIO, "L16",
            Capability.AUDIO_16KHZ, 1);

    private static final BridgeDescription MULTICAST = new BridgeDescription();

    private LocalDevicePanel localDevicePanel = null;

    private JCheckBox localStreams = new JCheckBox(
            "Recording from Local Cameras");

    private boolean initialLocalStreams = false;

    private JCheckBox accessGrid = new JCheckBox(
            "Recording from Access Grid");

    private boolean initialAccessGrid = false;

    private AccessGridPanel accessGridPanel = null;

    private boolean cancelled = false;

    private Config configuration = null;

    private Recorder parent = null;

    private AGStreamListener streamListener = null;

    private LocalRTPConnector localConnector = new LocalRTPConnector();

    /**
     * Creates a new RecordingSourceDialog
     * @param parent The parent frame
     * @param configuration The configuration
     * @param typeRepository The RTP Type repository
     * @param deviceDao The known devices
     */
    public RecordingSourceDialog(Recorder parent, Config configuration,
            RtpTypeRepository typeRepository) {
        super(parent, true);
        this.parent = parent;

        accessGridPanel = new AccessGridPanel(this,
                true, new Capability[]{VIDEO_CAPABILITY},
                new Capability[]{AUDIO_CAPABILITY},
                clientProfile, "Recorder");
        accessGridPanel.init(configuration);
        RTPType[] audioRtpTypes = new RTPType[AUDIO_RTP_TYPES.length];
        for (int i = 0; i < audioRtpTypes.length; i++) {
            audioRtpTypes[i] = typeRepository.findRtpType(AUDIO_RTP_TYPES[i]);
        }
        RTPType[] videoRtpTypes = new RTPType[VIDEO_RTP_TYPES.length];
        for (int i = 0; i < videoRtpTypes.length; i++) {
            videoRtpTypes[i] = typeRepository.findRtpType(VIDEO_RTP_TYPES[i]);
        }
        localDevicePanel = new LocalDevicePanel(this,
                videoRtpTypes, audioRtpTypes, true,
                localConnector, localConnector,
                clientProfile, "Recorder");
        localDevicePanel.init(configuration);

        localDevicePanel.addLocalStreamListener(parent);

        this.configuration = configuration;
        String agEnabled = configuration.getParameter(LAST_SOURCE_AG,
                "false");
        String localEnabled = configuration.getParameter(LAST_SOURCE_LOCAL,
                "false");

        MULTICAST.setName("Use Multicast");
        MULTICAST.setServerType("multicast");

        setTitle("Select Recording Source");
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(null);
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
                BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH));
        add(mainPanel);

        localStreams.setAlignmentX(0.1f);
        accessGrid.setAlignmentX(0.1f);
        localStreams.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        accessGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.setAlignmentX(0.1f);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(localStreams);
        mainPanel.add(localDevicePanel);
        mainPanel.add(accessGrid);
        mainPanel.add(accessGridPanel);
        mainPanel.add(buttonPanel);

        /*ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(localStreams);
        buttonGroup.add(accessGrid);*/
        localStreams.addActionListener(this);
        accessGrid.addActionListener(this);

        localStreams.setSelected(localEnabled.equals("true"));
        accessGrid.setSelected(agEnabled.equals("true"));
        setPanelEnabled(localDevicePanel, localStreams.isSelected());
        setPanelEnabled(accessGridPanel, accessGrid.isSelected());
    }

    /**
     * Initializes the panel
     * @param typeRepository The repository of types
     */
    public void init(RtpTypeRepository typeRepository) {
        List<RTPType> rtpTypes = typeRepository.findRtpTypes();
        for (RTPType type : rtpTypes) {
            accessGridPanel.mapFormat(type.getId(), type.getFormat());
        }
    }

    /**
     *
     * @see java.awt.Dialog#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        stopPreview();

        if (visible) {
            initialAccessGrid = accessGrid.isSelected();
            accessGridPanel.captureInitialValues();
            localDevicePanel.captureInitialValues();
            initialLocalStreams = localStreams.isSelected();
            cancelled = true;
        } else if (cancelled) {
            accessGrid.setSelected(initialAccessGrid);
            setPanelEnabled(accessGridPanel, initialAccessGrid);
            accessGridPanel.resetToInitialValues();
            localDevicePanel.resetToInitialValues();
            localStreams.setSelected(initialLocalStreams);
            setPanelEnabled(localDevicePanel, initialLocalStreams);
        } else {
            storeConfiguration();
        }
        super.setVisible(visible);
    }

    private void storeConfiguration() {
        configuration.setParameter(LAST_SOURCE_LOCAL,
                String.valueOf(localStreams.isSelected()));
        configuration.setParameter(LAST_SOURCE_AG,
                String.valueOf(accessGrid.isSelected()));
        localDevicePanel.storeConfiguration(configuration);
        accessGridPanel.storeConfiguration(configuration);
    }

    private void setPanelEnabled(Container panel, boolean enabled) {
        panel.setEnabled(enabled);
        Component[] components = panel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof Container) {
                setPanelEnabled((Container) components[i], enabled);
            } else {
                components[i].setEnabled(enabled);
            }
        }
    }

    private void stopPreview() {
        accessGridPanel.stopPreview();
        localDevicePanel.stopPreview();
    }

    /**
     *
     * @see java.awt.event.ActionListener#actionPerformed(
     *     java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if ((e.getSource() == localStreams)
                || (e.getSource() == accessGrid)) {
            setPanelEnabled(localDevicePanel, localStreams.isSelected());
            setPanelEnabled(accessGridPanel, accessGrid.isSelected());
            if (e.getSource() == localStreams) {
                localDevicePanel.disablePreviewForRunningDevices();
            }
            stopPreview();
        } else if (e.getActionCommand().equals("OK")) {
            boolean ok = false;
            if (localStreams.isSelected()) {
                if (localDevicePanel.verify()) {
                    ok = true;
                    if (streamListener != null) {
                        streamListener.removeAllStreams();
                    }
                    cancelled = false;
                    localDevicePanel.changeDevices();
                    setVisible(false);
                }
            } else {
                localDevicePanel.stopDevices();
            }
            if (accessGrid.isSelected()) {
                if (accessGridPanel.verify()) {
                    ok = true;
                    accessGridPanel.stopConnection();
                    if (streamListener != null) {
                        streamListener.removeAllStreams();
                    }
                    streamListener = new AGStreamListener();
                    try {
                        accessGridPanel.startConnection(streamListener);
                        cancelled = false;
                        setVisible(false);
                    } catch (Exception error) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                            "Error connecting to venue: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                accessGridPanel.stopConnection();
            }
            if ((!accessGrid.isSelected() && !localStreams.isSelected())
                    || !ok) {
                JOptionPane.showMessageDialog(this,
                        "No source has been selected!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getActionCommand().equals("Cancel")) {
            cancelled = true;
            setVisible(false);
        }
    }


    /**
     * Determines if the dialog was cancelled
     * @return True if cancelled, false otherwise
     */
    public boolean wasCancelled() {
        return cancelled;
    }

    /**
     * Sets the manager to use to record
     * @param manager The manager
     */
    public void setArchiveManager(RecordArchiveManager manager) {
        localConnector.setRTCPSink(manager);
        localConnector.setRTPSink(manager);
        accessGridPanel.setRtcpSink(manager);
        accessGridPanel.setRtpSink(manager);
    }

    /**
     * Resets the audio to original values before the program started
     */
    public void resetAudioToOriginalValues() {
        localDevicePanel.resetAudioToOriginalValues(configuration);
    }

    private class AGStreamListener implements StreamListener {

        private HashMap<Long, DataSource> videoDataSources =
            new HashMap<Long, DataSource>();

        private void removeAllStreams() {
            for (DataSource d : videoDataSources.values()) {
                parent.removeLocalVideo(d);
            }
        }

        /**
         * @see net.crew_vre.recorder.ag.StreamListener#addAudioStream(
         *     long, javax.media.protocol.DataSource,
         *     javax.media.format.AudioFormat)
         */
        public void addAudioStream(long ssrc, DataSource dataSource,
                AudioFormat format) {
            // Does Nothing
        }

        /**
         * @see net.crew_vre.recorder.ag.StreamListener#addVideoStream(
         *     long, javax.media.protocol.DataSource,
         *     javax.media.format.VideoFormat)
         */
        public void addVideoStream(long ssrc, DataSource dataSource,
                VideoFormat format) {
            try {
                parent.addLocalVideo("", dataSource, ssrc);
                videoDataSources.put(ssrc, dataSource);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void removeAudioStream(long ssrc) {
            // Does Nothing
        }

        public void removeVideoStream(long ssrc) {
            parent.removeLocalVideo(videoDataSources.get(ssrc));
        }

        public void setAudioStreamSDES(long ssrc, int item, String value) {
            // Does Nothing
        }

        public void setVideoStreamSDES(long ssrc, int item, String value) {
            // Does Nothing
        }
    }
}
