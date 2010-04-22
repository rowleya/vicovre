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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Vector;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;
import ag3.interfaces.types.ConnectionDescription;
import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.rtp.AGController;
import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.media.rtp.UnsupportedEncryptionException;
import com.googlecode.vicovre.media.ui.FileDevice;
import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.repositories.rtptype.impl.RtpTypeRepositoryXmlImpl;
import com.googlecode.vicovre.streamer.display.DisplayPanel;
import com.googlecode.vicovre.utils.Config;

/**
 * The main class of the streamer
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Streamer extends JFrame implements ActionListener {

    private static final Capability VIDEO_CAPABILITY =
        new Capability(Capability.PRODUCER, Capability.VIDEO, "H261",
            Capability.VIDEO_RATE, 1);

    private static final Capability AUDIO_CAPABILITY =
        new Capability(Capability.PRODUCER, Capability.AUDIO, "L16",
            Capability.AUDIO_16KHZ, 1);

    private static final int[] VIDEO_TYPES = new int[]{77, 31, 96};

    private static final int[] AUDIO_TYPES = new int[]{84, 112};

    private DisplayPanel displayPanel = new DisplayPanel();

    private AGController agController = null;

    private ClientProfile clientProfile = new ClientProfile();

    private BridgeDescription bridge = new BridgeDescription();

    private JButton devicesButton = new JButton("Send...");

    private JButton fileButton = new JButton("Send File...");

    private JButton profileButton = new JButton("Profile...");

    private ProfileDialog profileDialog = new ProfileDialog(this);

    private LocalDeviceDialog localDeviceDialog = null;

    private String configFile = null;

    private Config config = null;

    private RtpTypeRepository types = null;

    private Vector<FileDevice> fileDevices = new Vector<FileDevice>();

    public Streamer() throws SAXException, IOException,
            ParserConfigurationException {
        super("Streamer");
        setSize(400, 600);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(displayPanel);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        bridge.setName("Multicast");
        bridge.setServerType("multicast");
        agController = new AGController(bridge,
                new Capability[]{AUDIO_CAPABILITY, VIDEO_CAPABILITY}, null,
                clientProfile, "ViCoStreamer");
        agController.setListener(displayPanel);

        types = new RtpTypeRepositoryXmlImpl("/rtptypes.xml");
        for (RTPType type : types.findRtpTypes()) {
            agController.mapFormat(type.getId(), type.getFormat());
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(devicesButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(fileButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(profileButton);
        add(buttonPanel);

        displayPanel.setAlignmentX(LEFT_ALIGNMENT);
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

        devicesButton.setEnabled(false);
        devicesButton.addActionListener(this);
        profileButton.addActionListener(this);
        fileButton.addActionListener(this);
    }

    public void init() {
        RTPType[] videoTypes = new RTPType[VIDEO_TYPES.length];
        for (int i = 0; i < VIDEO_TYPES.length; i++) {
            videoTypes[i] = types.findRtpType(VIDEO_TYPES[i]);
        }
        RTPType[] audioTypes = new RTPType[AUDIO_TYPES.length];
        for (int i = 0; i < AUDIO_TYPES.length; i++) {
            audioTypes[i] = types.findRtpType(AUDIO_TYPES[i]);
        }

        BridgedRTPConnector audioConnector =
            agController.getConnectorForCapability(AUDIO_CAPABILITY);
        BridgedRTPConnector videoConnector =
            agController.getConnectorForCapability(VIDEO_CAPABILITY);

        localDeviceDialog = new LocalDeviceDialog(this, videoTypes, audioTypes,
                true, videoConnector, audioConnector, displayPanel,
                clientProfile);
        if (config != null) {
            localDeviceDialog.setConfiguration(config);
        }
        agController.setJoinListener(localDeviceDialog);
        devicesButton.setEnabled(true);
    }

    public void setVenue(String venue) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, IOException,
            SAXException, ClassNotFoundException, InstantiationException,
            UnsupportedEncryptionException {
        ConnectionDescription connection = new ConnectionDescription();
        connection.setUri(venue);
        agController.joinVenue(connection);
    }

    public void setBridge(BridgeDescription bridge)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {
        agController.setBridge(bridge);
    }

    public void addAddress(String address, int port, int ttl)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException,
            UnsupportedEncryptionException {
        InetAddress addr = InetAddress.getByName(address);
        NetworkLocation location = null;
        if (addr.isMulticastAddress()) {
            MulticastNetworkLocation mcLocation =
                new MulticastNetworkLocation();
            mcLocation.setTtl(ttl);
            location = mcLocation;
        } else {
            location = new UnicastNetworkLocation();
        }

        location.setHost(address);
        location.setPort(port);

        agController.setLocationCapabilities(location,
                new Capability[]{VIDEO_CAPABILITY, AUDIO_CAPABILITY});
        agController.connectToLocation(location);
        System.err.println("Connecting to " + location);
    }

    private void editProfile() throws IOException {
        profileDialog.setVisible(true);
        if (!profileDialog.wasCancelled()) {
            clientProfile.setName(profileDialog.getName());
            clientProfile.setEmail(profileDialog.getEmail());
            clientProfile.setPhoneNumber(profileDialog.getPhone());
            clientProfile.setLocation(profileDialog.getLoc());
            profileDialog.storeConfiguration(config);
            config.saveParameters(configFile);
            agController.updateProfile(clientProfile);
            localDeviceDialog.changeClientProfile(clientProfile);
        }
    }

    public void setConfiguration(String configFile)
            throws SAXException, IOException {
        this.configFile = configFile;
        File file = new File(configFile);
        if (file.exists()) {
            config = new Config(configFile);
            profileDialog.init(config);
            clientProfile.setName(profileDialog.getName());
            clientProfile.setEmail(profileDialog.getEmail());
            clientProfile.setPhoneNumber(profileDialog.getPhone());
            clientProfile.setLocation(profileDialog.getLoc());
        } else {
            config = new Config();
            editProfile();
        }
    }

    public Config getConfig() {
        return config;
    }

    public void setSendOnly(boolean sendOnly) {
        displayPanel.setSendOnly(sendOnly);
    }

    public void close() {
        localDeviceDialog.stopDevices();
        for (FileDevice fileDevice : fileDevices) {
            fileDevice.stop();
        }
        System.exit(0);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(devicesButton)) {
            localDeviceDialog.setVisible(true);
        } else if (e.getSource().equals(profileButton)) {
            try {
                editProfile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource().equals(fileButton)) {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                FileDevice fileDevice = new FileDevice(file.getAbsolutePath(),
                        clientProfile, "ViCoStreamer");
                try {
                    BridgedRTPConnector audioConnector =
                        agController.getConnectorForCapability(
                                AUDIO_CAPABILITY);
                    BridgedRTPConnector videoConnector =
                        agController.getConnectorForCapability(
                                VIDEO_CAPABILITY);

                    RTPType videoRtpType = types.findRtpType(VIDEO_TYPES[0]);
                    RTPType audioRtpType = types.findRtpType(AUDIO_TYPES[0]);
                    fileDevice.prepare(videoConnector, audioConnector,
                            (VideoFormat) videoRtpType.getFormat(),
                            (AudioFormat) audioRtpType.getFormat(),
                            videoRtpType.getId(), audioRtpType.getId());
                    fileDevice.start(displayPanel);
                    fileDevices.add(fileDevice);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error streaming file: "
                            + e1.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public static void main(String[] args) {
        Streamer streamer = null;
        String configFile = System.getProperty("user.home")
            + File.separator + ".streamer.xml";
        try {
            Misc.configureCodecs("/knownCodecs.xml");

            streamer = new Streamer();
            streamer.setVisible(true);

            boolean venueChosen = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-v") || args[i].equals("--venue")) {
                    streamer.setVenue(args[i + 1]);
                    i += 1;
                    venueChosen = true;
                } else if (args[i].equals("-a")
                        || args[i].equals("--address")) {
                    String[] address = args[i + 1].split("/", 3);
                    i += 1;
                    String host = address[0];
                    int port = Integer.parseInt(address[1]);
                    int ttl = 127;
                    if (address.length > 2) {
                        ttl = Integer.parseInt(address[2]);
                    }
                    streamer.addAddress(host, port, ttl);
                    venueChosen = true;
                } else if (args[i].equals("--sendonly")) {
                    streamer.setSendOnly(true);
                }
            }

            streamer.setConfiguration(configFile);

            if (!venueChosen) {
                VenueDialog venue = new VenueDialog(streamer,
                        new Capability[]{VIDEO_CAPABILITY},
                        new Capability[]{AUDIO_CAPABILITY});
                Config config = streamer.getConfig();
                venue.setConfig(config);
                venue.setVisible(true);
                venue.storeConfig(config);
                config.saveParameters(configFile);
                if (venue.wasCancelled()) {
                    throw new Exception("No venue chosen");
                }
                streamer.setBridge(venue.getBridge());
                streamer.setVenue(venue.getVenue());
            }
            streamer.init();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(streamer,
                    "Error starting Streamer: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}
