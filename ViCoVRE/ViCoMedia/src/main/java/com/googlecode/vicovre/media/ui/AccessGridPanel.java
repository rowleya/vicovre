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

package com.googlecode.vicovre.media.ui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import javax.media.Effect;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.rtcp.SourceDescription;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.xml.sax.SAXException;

import com.googlecode.onevre.ag.agbridge.RegistryClient;
import com.googlecode.onevre.ag.types.BridgeDescription;
import com.googlecode.onevre.ag.types.Capability;
import com.googlecode.onevre.ag.types.ClientProfile;
import com.googlecode.onevre.ag.types.ConnectionDescription;
import com.googlecode.onevre.ag.types.server.VenueServer;
import com.googlecode.onevre.types.soap.exceptions.SoapException;
import com.googlecode.vicovre.media.renderer.RGBRenderer;
import com.googlecode.vicovre.media.rtp.AGController;
import com.googlecode.vicovre.media.rtp.RTCPPacketSink;
import com.googlecode.vicovre.media.rtp.RTPPacketSink;
import com.googlecode.vicovre.media.rtp.StreamListener;
import com.googlecode.vicovre.media.rtp.UnsupportedEncryptionException;
import com.googlecode.vicovre.utils.Config;

/**
 * A panel for selecting an access grid venue
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class AccessGridPanel extends JPanel implements ActionListener,
        ItemListener, StreamListener {

    private static final long serialVersionUID = 1L;

    private static final String LAST_AGSERVER = "lastAGServer";

    private static final String LAST_AGVENUE = "lastAGVenue";

    private static final String LAST_AGREGISTRY = "lastAGRegistry";

    private static final String LAST_AGBRIDGE = "lastAGBridge";

    private static final String VENUE_SERVERS = "venueServers";

    private static final String BRIDGE_REGISTRIES = "bridgeRegistries";

    // The default height of the preview window
    private static final int PREVIEW_HEIGHT = 85;

    // The default width of the preview window
    private static final int PREVIEW_WIDTH = 104;

    private static final BridgeDescription MULTICAST = new BridgeDescription();

    private ClientProfile clientProfile = null;

    private Capability[] videoCapabilities = null;

    private Capability[] capabilities = null;

    private HashMap<Integer, Format> mappedFormats =
        new HashMap<Integer, Format>();

    private JComboBox accessGridServer = new JComboBox();

    private String initialAccessGridServer = null;

    private JComboBox accessGridVenue = new JComboBox();

    private ConnectionDescription initialAccessGridVenue = null;

    private JComboBox accessGridBridgeRegistry = new JComboBox();

    private String initialAccessGridBridgeRegistry = null;

    private JComboBox accessGridBridge = new JComboBox();

    private BridgeDescription initialAccessGridBridge = new BridgeDescription();

    private JPanel agVideoStreamsPanel = new JPanel();

    private JPanel agPreviewPanel = new JPanel();

    private RGBRenderer previewRenderer = null;

    private DataSource previewDataSource = null;

    private AGController agController = null;

    private boolean agStreamReceived = false;

    private HashMap<Long, JPanel> agVideoStreamPanels =
        new HashMap<Long, JPanel>();

    private HashMap<Long, JLabel> agVideoStreamLabels =
        new HashMap<Long, JLabel>();

    private Vector<JButton> agPreview = new Vector<JButton>();

    private Vector<DataSource> agDataSource = new Vector<DataSource>();

    private Vector<Format> agFormat = new Vector<Format>();

    private JCheckBox encryption = new JCheckBox("Encryption key:");

    private boolean initialEncryption = false;

    private JTextArea encryptionKey = new JTextArea();

    private String initialEncryptionKey = "";

    private AGController currentVenue = null;

    private JDialog parent = null;

    private HashMap<Long, String> streamNames = new HashMap<Long, String>();

    private HashMap<Long, String> streamNotes = new HashMap<Long, String>();

    private HashMap<Long, String> streamCNames =
        new HashMap<Long, String>();

    private HashMap<String, ConnectionDescription[]> venueServers =
        new HashMap<String, ConnectionDescription[]>();

    private HashMap<String, BridgeDescription[]> bridgeServers =
        new HashMap<String, BridgeDescription[]>();

    private String tool = null;

    private class BridgeListRenderer extends BasicComboBoxRenderer {

        private static final long serialVersionUID = 1L;

        /**
         *
         * @see javax.swing.plaf.basic.BasicComboBoxRenderer#
         *     getListCellRendererComponent(javax.swing.JList, java.lang.Object,
         *     int, boolean, boolean)
         */
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof BridgeDescription) {
                BridgeDescription bridge = (BridgeDescription) value;
                return super.getListCellRendererComponent(list,
                        bridge.getName(), index, isSelected, cellHasFocus);
            }
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }
    }

    /**
     * Creates a new AccessGridPanel
     * @param parent The parent dialog
     * @param showPreview True if preview should be shown, false otherwise
     * @param videoCapabilities The video capabilities
     * @param audioCapabilities The audio capabilities
     * @param clientProfile The client profile
     */
    public AccessGridPanel(JDialog parent, boolean showPreview,
            Capability[] videoCapabilities,
            Capability[] audioCapabilities, ClientProfile clientProfile,
            String tool) {
        this.parent = parent;
        this.tool = tool;
        this.capabilities = new Capability[videoCapabilities.length
                                           + audioCapabilities.length];
        this.videoCapabilities = videoCapabilities;
        this.clientProfile = clientProfile;
        System.arraycopy(videoCapabilities, 0, capabilities, 0,
                videoCapabilities.length);
        System.arraycopy(audioCapabilities, 0, capabilities,
                videoCapabilities.length, audioCapabilities.length);

        MULTICAST.setName("Use Multicast");
        MULTICAST.setServerType("multicast");

        Dimension previewSize = new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        agVideoStreamsPanel.setLayout(new BoxLayout(agVideoStreamsPanel,
                BoxLayout.Y_AXIS));
        agVideoStreamsPanel.setBorder(BorderFactory.createEmptyBorder(
                0, 5, 0, 0));
        JScrollPane agVideoScroll = new JScrollPane(agVideoStreamsPanel);
        JPanel agVideoPanel = new JPanel();
        agVideoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        agVideoPanel.setLayout(new BoxLayout(agVideoPanel, BoxLayout.X_AXIS));
        agVideoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        agVideoPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 90));
        agPreviewPanel.setLayout(null);
        agPreviewPanel.setPreferredSize(previewSize);
        agPreviewPanel.setMaximumSize(previewSize);
        agPreviewPanel.setMinimumSize(previewSize);
        agPreviewPanel.setBackground(Color.BLACK);
        agVideoPanel.add(agVideoScroll);
        agVideoPanel.add(agPreviewPanel);

        JButton loadVenues = new JButton("Load Venues");
        JButton testVenue = new JButton("Test Connection");
        JButton loadBridges = new JButton("Load Bridges");
        loadVenues.addActionListener(this);
        testVenue.addActionListener(this);
        loadBridges.addActionListener(this);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        int previewHeight = 90;
        int previewSep = 5;
        if (!showPreview) {
            previewHeight = 0;
            previewSep = 0;
        }

        setLayout(new TableLayout(
                new double[]{120, TableLayout.FILL, 5, 130},
                new double[]{20, 5, 20, 5, 20, 5, 20, 5, 20,
                        previewSep, previewHeight}));
        add(new JLabel("AG3 Venue Server:"), "0, 0");
        add(accessGridServer, "1, 0");
        add(loadVenues, "3, 0");
        add(new JLabel("Virtual Venue:"), "0, 2");
        add(accessGridVenue, "1, 2");
        add(encryption, "0, 4");
        add(encryptionKey, "1, 4");
        add(new JLabel("AG3 Bridge Registry:"), "0, 6");
        add(accessGridBridgeRegistry, "1, 6");
        add(loadBridges, "3, 6");
        add(new JLabel("Bridge:"), "0, 8");
        add(accessGridBridge, "1, 8");

        if (showPreview) {
            add(testVenue, "3, 8");
            add(agVideoPanel, "0, 10, 3, 10");
        }

        accessGridServer.setEditable(true);
        accessGridBridgeRegistry.setEditable(true);
        accessGridServer.addItemListener(this);
        accessGridBridgeRegistry.addItemListener(this);
        accessGridBridge.addItemListener(this);
        accessGridBridge.addItem(MULTICAST);
        accessGridBridge.setRenderer(new BridgeListRenderer());
    }

    /**
     * Initializes the panel
     * @param configuration The configuration of the panel
     */
    public void init(Config configuration) {

        if (configuration != null) {
            String[] servers = configuration.getParameters(VENUE_SERVERS);
            for (String server : servers) {
                accessGridServer.addItem(server);
            }
            accessGridServer.setSelectedItem(null);
        }

        if (configuration != null) {
            String[] registries = configuration.getParameters(
                    BRIDGE_REGISTRIES);
            for (String registry : registries) {
                accessGridBridgeRegistry.addItem(registry);
            }
            accessGridBridgeRegistry.setSelectedItem(null);
        }

        String agServer = null;
        String agVenue = null;
        String agRegistry = null;
        String agBridge = null;

        if (configuration != null) {
            agServer = configuration.getParameter(LAST_AGSERVER, null);
            agVenue = configuration.getParameter(LAST_AGVENUE, null);
            agRegistry = configuration.getParameter(LAST_AGREGISTRY, null);
            agBridge = configuration.getParameter(LAST_AGBRIDGE, null);
        }

        accessGridServer.setSelectedItem(agServer);
        accessGridBridgeRegistry.setSelectedItem(agRegistry);

        for (int i = 0; (i < accessGridVenue.getItemCount())
                && (agVenue != null); i++) {
            if (((ConnectionDescription)
                    accessGridVenue.getItemAt(i)).getName().equals(agVenue)) {
                accessGridVenue.setSelectedIndex(i);
                agVenue = null;
            }
        }
        for (int i = 0; (i < accessGridBridge.getItemCount())
                && (agBridge != null); i++) {
            if (((BridgeDescription)
                    accessGridBridge.getItemAt(i)).getName().equals(agBridge)) {
                accessGridBridge.setSelectedIndex(i);
                agBridge = null;
            }
        }
    }

    /**
     * Maps an RTP Format identifier to a JMF format
     * @param rtpType The RTP type
     * @param format The format
     */
    public void mapFormat(int rtpType, Format format) {
        mappedFormats.put(rtpType, format);
        if (agController != null) {
            agController.mapFormat(rtpType, format);
        }
    }

    /**
     * Captures the current values for later resetting
     */
    public void captureInitialValues() {
        initialAccessGridServer = (String)
            accessGridServer.getSelectedItem();
        initialAccessGridVenue = (ConnectionDescription)
            accessGridVenue.getSelectedItem();
        initialAccessGridBridgeRegistry = (String)
            accessGridBridgeRegistry.getSelectedItem();
        initialAccessGridBridge = (BridgeDescription)
            accessGridBridge.getSelectedItem();
        initialEncryption = encryption.isSelected();
        initialEncryptionKey = encryptionKey.getText();
    }

    /**
     * Resets the selections to the values captured last with
     *     captureInitialValues
     */
    public void resetToInitialValues() {
        accessGridServer.setSelectedItem(initialAccessGridServer);
        accessGridVenue.setSelectedItem(initialAccessGridVenue);
        accessGridBridgeRegistry.setSelectedItem(
                initialAccessGridBridgeRegistry);
        accessGridBridge.setSelectedItem(initialAccessGridBridge);
        encryption.setSelected(initialEncryption);
        encryptionKey.setText(initialEncryptionKey);
    }

    /**
     * Stops the existing connection to the venue if any
     */
    public void stopConnection() {
        if (currentVenue != null) {
            currentVenue.leaveCurrentVenue();
        }
    }

    /**
     * Starts a connection to the venue, receiving streams
     * @param streamListener The stream listener to handle incoming streams
     * @throws UnsupportedEncryptionException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws SAXException
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws SoapException
     */
    public void startConnection(StreamListener streamListener)
            throws IllegalAccessException, IOException,
            ClassNotFoundException, InstantiationException,
            UnsupportedEncryptionException, SoapException {
        stopConnection();

        ConnectionDescription venueDescription =
            (ConnectionDescription) accessGridVenue.getSelectedItem();

        String encryptionKey = null;
        if (encryption.isSelected()) {
            encryptionKey = this.encryptionKey.getText();
        }
        currentVenue = new AGController(
                (BridgeDescription) accessGridBridge.getSelectedItem(),
                capabilities, encryptionKey, clientProfile, tool);
        for (int i : mappedFormats.keySet()) {
            Format format = mappedFormats.get(i);
            currentVenue.mapFormat(i, format);
        }
        currentVenue.setListener(streamListener);
        currentVenue.joinVenue(venueDescription);
    }

    /**
     * Stores the configuration
     * @param configuration The configuration to store in
     */
    public void storeConfiguration(Config configuration) {
        String agServer = (String) accessGridServer.getSelectedItem();
        if (agServer != null) {
            configuration.setParameter(LAST_AGSERVER, agServer);
        }
        ConnectionDescription agVenue = (ConnectionDescription)
            accessGridVenue.getSelectedItem();
        if (agVenue != null) {
            configuration.setParameter(LAST_AGVENUE, agVenue.getName());
        }
        String agRegistry = (String)
            accessGridBridgeRegistry.getSelectedItem();
        if (agRegistry != null) {
            configuration.setParameter(LAST_AGREGISTRY, agRegistry);
        }
        BridgeDescription agBridge = (BridgeDescription)
            accessGridBridge.getSelectedItem();
        if (agBridge != null) {
            configuration.setParameter(LAST_AGBRIDGE, agBridge.getName());
        }
    }

    /**
     * Verifies that a venue has been selected
     * @return true if the venue has been selected
     */
    public boolean verify() {
        if (accessGridServer.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "No venue server selected!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (accessGridVenue.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "No venue selected!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void stopCurrentPreview() {
        if (previewRenderer != null) {
            if (previewDataSource != null) {
                previewDataSource.disconnect();
            }
            agPreviewPanel.removeAll();
            previewRenderer.stop();
        }
        previewRenderer = null;
        previewDataSource = null;
    }

    /**
     * Stops the current preview
     */
    public void stopPreview() {
        stopCurrentPreview();
        if (agController != null) {
            agController.leaveCurrentVenue();
        }
        agVideoStreamsPanel.removeAll();
        agVideoStreamsPanel.validate();
        agVideoStreamsPanel.repaint();
    }

    /**
     *
     * @see java.awt.event.ActionListener#actionPerformed(
     *     java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Preview")) {
            stopCurrentPreview();
            int index = agPreview.indexOf(e.getSource());
            if (index != -1) {
                DataSource dataSource = agDataSource.get(index);
                Format format = agFormat.get(index);
                previewRenderer = new RGBRenderer(new Effect[]{});
                previewRenderer.setDataSource(dataSource, 0);
                previewRenderer.setInputFormat(format);
                Component c = previewRenderer.getComponent();
                agPreviewPanel.add(c);
                c.setSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
                c.setVisible(true);
                previewRenderer.start();
            }
        } else if (e.getActionCommand().equals("Load Venues")) {
            stopPreview();
            String server = (String) accessGridServer.getSelectedItem();
            if (server != null) {
                getVenues(server);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please enter the URL of an Access Grid Venue Server");
            }
        } else if (e.getActionCommand().equals("Test Connection")) {
            stopPreview();
            ConnectionDescription venueDescription =
                (ConnectionDescription) accessGridVenue.getSelectedItem();
            if (venueDescription != null) {
                joinVenue(venueDescription);
            } else {
                JOptionPane.showMessageDialog(this,
                     "Please select a virtual venue to test with");
            }
        } else if (e.getActionCommand().equals("Load Bridges")) {
            stopPreview();
            String bridgeRegistry = (String)
                accessGridBridgeRegistry.getSelectedItem();
            if (bridgeRegistry != null) {
                getBridges(bridgeRegistry);
            } else {
                JOptionPane.showMessageDialog(this,
                     "Please enter the URL of an Access Grid Bridge Registry");
            }
        }
    }

    /**
     *
     * @see java.awt.event.ItemListener#itemStateChanged(
     *     java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
        if ((e.getStateChange() == ItemEvent.SELECTED)
                && (e.getSource() == accessGridServer)) {
            String server = (String) accessGridServer.getSelectedItem();
            if (server != null) {
                getVenues(server);
            }
        } else if ((e.getStateChange() == ItemEvent.DESELECTED)
                && (e.getSource() == accessGridServer)) {
            stopPreview();
            accessGridVenue.removeAllItems();
        } else if ((e.getStateChange() == ItemEvent.SELECTED)
                && (e.getSource() == accessGridBridgeRegistry)) {
            String bridgeRegistry = (String)
                accessGridBridgeRegistry.getSelectedItem();
            getBridges(bridgeRegistry);
        } else if ((e.getStateChange() == ItemEvent.DESELECTED)
                && (e.getSource() == accessGridBridgeRegistry)) {
            stopPreview();
            accessGridBridge.removeAllItems();
        } else if ((e.getStateChange() == ItemEvent.DESELECTED)
                && (e.getSource() == accessGridBridge)) {
            stopPreview();
        }
    }

    private void getBridges(final String server) {
        final ProgressDialog progress = new ProgressDialog(parent,
                "Loading bridges", true, true, true);
        progress.setAlwaysOnTop(false);
        try {
            new URL(server);

            BridgeDescription[] bridges = bridgeServers.get(server);
            if (bridges == null) {
                OtherThread<Object> worker =
                        new OtherThread<Object>() {
                    public Object doInBackground() {
                        try {
                            RegistryClient client = new RegistryClient(
                                    server);
                            Vector<BridgeDescription> bridges =
                                client.lookupBridges();
                            progress.setVisible(false);
                            return bridges.toArray(new BridgeDescription[0]);
                        } catch (Throwable e) {
                            progress.setVisible(false);
                            return e;
                        }
                    }
                };

                worker.execute();
                progress.setMessage("Loading bridges");
                progress.setVisible(true);
                Object result = worker.get();
                if (result instanceof Throwable) {
                    throw (Throwable) result;
                }

                bridges = (BridgeDescription[]) result;
                bridgeServers.put(server, bridges);
            }
            Arrays.sort(bridges,
                    new BridgeDescription.BridgeNameComparator());
            accessGridBridge.removeAllItems();
            accessGridBridge.addItem(MULTICAST);
            for (int i = 0; i < bridges.length; i++) {
                accessGridBridge.addItem(bridges[i]);
            }

            boolean isInList = false;
            for (int i = 0; (i < accessGridBridgeRegistry.getItemCount())
                    && !isInList; i++) {
                if (accessGridBridgeRegistry.getItemAt(i).equals(server)) {
                    isInList = true;
                }
            }
            if (!isInList) {
                accessGridBridgeRegistry.addItem(server);
            }
        } catch (MalformedURLException error) {
            progress.setVisible(false);
            JOptionPane.showMessageDialog(this, "That is not a valid URL",
                    "URL Error", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable error) {
            if (!progress.wasCancelled()) {
                error.printStackTrace();
                progress.setVisible(false);
                JOptionPane.showMessageDialog(this,
                        "Error getting list of bridges: " + error.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void getVenues(String server) {
        final ProgressDialog progress = new ProgressDialog(parent,
                "Loading venues", true, true, true);
        progress.setAlwaysOnTop(false);
        try {
            URL url = null;
            try {
                url = new URL(server);
            } catch (MalformedURLException e) {
                url = new URL("https://" + server);
            }
            int port = url.getPort();
            if (port == -1) {
                port = 8000;
            }
            String protocol = url.getProtocol();
            if (protocol == null || protocol.equals("")) {
                protocol = "https";
            }
            final String venueServerUrl = protocol + "://"
                + url.getHost() + ":" + port + "/VenueServer";
            if (!venueServerUrl.equals(server)) {
                server = venueServerUrl;
                accessGridServer.setSelectedItem(venueServerUrl);
                return;
            }

            ConnectionDescription[] venues = venueServers.get(server);
            if (venues == null) {
                OtherThread<Object> worker =
                        new OtherThread<Object>() {
                    public Object doInBackground() {
                        try {
                            VenueServer venueServer = new VenueServer(
                                    venueServerUrl);
                            ConnectionDescription[] venues =
                                venueServer.getVenues(null);
                            progress.setVisible(false);
                            return venues;
                        } catch (Throwable e) {
                            progress.setVisible(false);
                            return e;
                        }
                    }
                };

                worker.execute();
                progress.setMessage("Loading venues");
                progress.setVisible(true);
                synchronized (worker) {
                    while (!worker.isFinished() && !progress.wasCancelled()) {
                        try {
                            worker.wait(1000);
                        } catch (InterruptedException e) {
                            // Do Nothing
                        }
                    }
                    if (!progress.wasCancelled()) {
                        Object result = worker.get();
                        if (result instanceof Throwable) {
                            throw (Throwable) result;
                        }

                        venues = (ConnectionDescription[]) result;
                        venueServers.put(server, venues);
                    }
                }
            }

            if (venues != null) {
                Arrays.sort(venues);
                for (int i = 0; i < venues.length; i++) {
                    accessGridVenue.addItem(venues[i]);
                }

                boolean isInList = false;
                for (int i = 0; (i < accessGridServer.getItemCount())
                        && !isInList; i++) {
                    if (accessGridServer.getItemAt(i).equals(server)) {
                        isInList = true;
                    }
                }
                if (!isInList) {
                    accessGridServer.addItem(server);
                }
            }
        } catch (MalformedURLException error) {
            progress.setVisible(false);
            JOptionPane.showMessageDialog(this, "That is not a valid URL",
                    "URL Error", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable error) {
            if (!progress.wasCancelled()) {
                error.printStackTrace();
                progress.setVisible(false);
                JOptionPane.showMessageDialog(this,
                        "Error getting list of venues: " + error.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void joinVenue(final ConnectionDescription venueDescription) {
        String encrKey = null;
        if (encryption.isSelected()) {
            encrKey = this.encryptionKey.getText();
        }
        agController = new AGController(
                (BridgeDescription) accessGridBridge.getSelectedItem(),
                videoCapabilities, encrKey, clientProfile, tool);
        agController.setListener(this);
        for (int i : mappedFormats.keySet()) {
            Format format = mappedFormats.get(i);
            agController.mapFormat(i, format);
        }
        agStreamReceived = false;
        agVideoStreamsPanel.add(new JLabel("Waiting for video streams..."));
        agVideoStreamsPanel.validate();
        agVideoStreamsPanel.repaint();
        final ProgressDialog progress = new ProgressDialog(parent,
                "Joining Venue", true, true, true);
        OtherThread<Throwable> worker =
                new OtherThread<Throwable>() {
            public Throwable doInBackground() {
                try {
                    agController.joinVenue(venueDescription);
                    progress.setVisible(false);
                    return null;
                } catch (Throwable e) {
                    progress.setVisible(false);
                    return e;
                }
            }
        };
        worker.execute();
        progress.setMessage("Joining Venue");
        progress.setVisible(true);

        try {
            Throwable e = worker.get();
            if (e != null) {
                throw e;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error joining venue: "
                    + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @see net.crew_vre.recorder.ag.StreamListener#addAudioStream(long,
     *     javax.media.protocol.DataSource, javax.media.format.AudioFormat)
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
        synchronized (agVideoStreamsPanel) {
            if (!agStreamReceived) {
                agStreamReceived = true;
                agVideoStreamsPanel.removeAll();
                agVideoStreamsPanel.validate();
                agVideoStreamsPanel.repaint();
            }

            if (!agVideoStreamLabels.containsKey(ssrc)) {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel,
                        BoxLayout.X_AXIS));
                JLabel name = new JLabel();
                name.setAlignmentX(Component.LEFT_ALIGNMENT);
                JButton button = new JButton("Preview");
                button.addActionListener(this);
                panel.add(name);
                panel.add(Box.createHorizontalGlue());
                panel.add(button);

                agVideoStreamLabels.put(ssrc, name);
                agVideoStreamPanels.put(ssrc, panel);
                agPreview.add(button);
                agDataSource.add(dataSource);
                agFormat.add(format);
                agVideoStreamsPanel.add(panel);

                agVideoStreamsPanel.validate();
                agVideoStreamsPanel.repaint();
            }
        }
    }

    /**
     * @see net.crew_vre.recorder.ag.StreamListener#removeAudioStream(long)
     */
    public void removeAudioStream(long ssrc) {
        // Does Nothing
    }

    /**
     * @see net.crew_vre.recorder.ag.StreamListener#removeVideoStream(long)
     */
    public void removeVideoStream(long ssrc) {
        JPanel panel = agVideoStreamPanels.get(ssrc);
        if (panel != null) {
            agVideoStreamsPanel.remove(panel);
            agVideoStreamsPanel.validate();
            agVideoStreamsPanel.repaint();
        }
    }

    public void setAudioStreamSDES(long ssrc, int item, String value) {
        // Does Nothing
    }

    public void setVideoStreamSDES(long ssrc, int item, String value) {
        JLabel label = agVideoStreamLabels.get(ssrc);
        if (item == SourceDescription.SOURCE_DESC_CNAME) {
            streamCNames.put(ssrc, value);
        } else if (item == SourceDescription.SOURCE_DESC_NAME) {
            streamNames.put(ssrc, value);
        } else if (item == SourceDescription.SOURCE_DESC_NOTE) {
            streamNotes.put(ssrc, value);
        }
        String cname = streamCNames.get(ssrc);
        String name = streamNames.get(ssrc);
        String description = streamNotes.get(ssrc);
        String text = "";
        String cnameText = " (from " + cname + ")";
        if (name != null) {
            text = name;
            if (description != null) {
                text += " - " + description;
            }
            text += cnameText;
        } else {
            text = label.getText();
            if (!text.endsWith(cnameText)) {
                text += cnameText;
            }
        }
        if (label != null) {
            label.setText(text);
        }
    }

    public String getVenue() {
        if (accessGridVenue.getSelectedIndex() >= 0) {
            return ((ConnectionDescription)
                    accessGridVenue.getSelectedItem()).getUri();
        }
        return null;
    }

    public BridgeDescription getBridge() {
        return (BridgeDescription) accessGridBridge.getSelectedItem();
    }

    public void addRtpSink(RTPPacketSink rtpSink) {
        if (agController != null) {
            agController.addRTPSink(rtpSink);
        }
    }

    public void addRtcpSink(RTCPPacketSink rtcpSink) {
        if (agController != null) {
            agController.addRTCPSink(rtcpSink);
        }
    }
}

