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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
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
import com.googlecode.vicovre.media.rtp.UnsupportedEncryptionException;
import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.repositories.rtptype.impl.RtpTypeRepositoryXmlImpl;
import com.googlecode.vicovre.streamer.display.DisplayPanel;

/**
 * The main class of the streamer
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Streamer extends JFrame {

    private static final Capability VIDEO_CAPABILITY =
        new Capability(Capability.CONSUMER, Capability.VIDEO, "H261",
            Capability.VIDEO_RATE, 1);

    private static final Capability AUDIO_CAPABILITY =
        new Capability(Capability.CONSUMER, Capability.AUDIO, "L16",
            Capability.AUDIO_16KHZ, 1);

    private DisplayPanel displayPanel = new DisplayPanel();

    private AGController agController = null;

    private ClientProfile clientProfile = new ClientProfile();

    private BridgeDescription bridge = new BridgeDescription();

    public Streamer() throws SAXException, IOException,
            ParserConfigurationException {
        super("Streamer");
        setSize(320, 400);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JScrollPane scroller = new JScrollPane(displayPanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scroller);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        bridge.setName("Multicast");
        bridge.setServerType("multicast");
        agController = new AGController(bridge,
                new Capability[]{AUDIO_CAPABILITY, VIDEO_CAPABILITY}, null,
                clientProfile);
        agController.setListener(displayPanel);

        RtpTypeRepository types = new RtpTypeRepositoryXmlImpl("/rtptypes.xml");
        for (RTPType type : types.findRtpTypes()) {
            agController.mapFormat(type.getId(), type.getFormat());
        }
    }

    public void setVenue(String venue) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, IOException,
            SAXException, ClassNotFoundException, InstantiationException,
            UnsupportedEncryptionException {
        ConnectionDescription connection = new ConnectionDescription();
        connection.setUri(venue);
        agController.joinVenue(connection);
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

        agController.connectToLocation(location);
        System.err.println("Connecting to " + location);
    }

    public void close() {
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            Misc.configureCodecs("/knownCodecs.xml");

            Streamer streamer = new Streamer();

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-v") || args[i].equals("--venue")) {
                    streamer.setVenue(args[i + 1]);
                    i += 1;
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
                }
            }

            streamer.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error starting Streamer: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
