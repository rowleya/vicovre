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

package com.googlecode.vicovre.recorder;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.CannotRealizeException;
import javax.media.Effect;
import javax.media.GainControl;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.sound.sampled.FloatControl;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.xml.sax.SAXParseException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.renderer.RGBRenderer;
import com.googlecode.vicovre.media.ui.LocalStreamListener;
import com.googlecode.vicovre.media.ui.ProgressDialog;
import com.googlecode.vicovre.recorder.dialog.RecordingSourceDialog;
import com.googlecode.vicovre.recorder.dialog.UploadDialog;
import com.googlecode.vicovre.recorder.dialog.component.ArrowCanvas;
import com.googlecode.vicovre.recorder.dialog.component.TickCanvas;
import com.googlecode.vicovre.recorder.firstrunwizard.AnnotationServerPage;
import com.googlecode.vicovre.recorder.firstrunwizard.DataDirectoryPage;
import com.googlecode.vicovre.recorder.firstrunwizard.IntroPage;
import com.googlecode.vicovre.recorder.utils.VideoDragListener;
import com.googlecode.vicovre.recordings.RecordArchiveManager;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.InsecureRecordingDatabase;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.layout.impl.LayoutRepositoryXmlImpl;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.repositories.rtptype.impl.RtpTypeRepositoryXmlImpl;
import com.googlecode.vicovre.utils.Config;


/**
 * The main recorder class
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Recorder extends JFrame implements ActionListener, ChangeListener,
        LocalStreamListener, ItemListener {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The data directory config item
     */
    public static final String CONFIG_DATA_DIRECTORY = "DataDirectory";

    /**
     * The port config item
     */
    public static final String CONFIG_PORT = "Port";

    private static final int BORDER_SIZE = 5;

    // The prefix for the title of the application
    private static final String TITLE_PREFIX = "Recorder";

    // The width of the interface
    private static final int SIZE_WIDTH = 900;

    // The height of the interface
    private static final int SIZE_HEIGHT = 600;

    // The file separator
    private static final String SLASH = System.getProperty("file.separator");

    // The default data directory
    private static final String DEFAULT_DATA_DIRECTORY = "Recordings";

    // The default configuration file
    private static final String DEFAULT_CONFIG_FILE = ".recorder.xml";

    private static final String POSITION_PLACE = "<position>";

    // The text to display for each position
    private static final String POSITION_TEXT =
        "<html><p><center>Drag video of the "
            + POSITION_PLACE + " to here</center></p></html>";

    // The current configuration
    private Config configuration = null;

    // The configuration file
    private String configFile = null;

    // The recording database
    private RecordingDatabase recordingDatabase = null;

    // The RTP Type repository
    private RtpTypeRepository typeRepository = null;

    // The layout repository
    private LayoutRepository layoutRepository = null;

    // The data directory
    private String dataDirectory = null;

    // The dialog for selecting the source of the recorded streams
    private RecordingSourceDialog recordingSource = null;

    private JButton recordingSourceButton = new JButton(
            "<html><center>Select Recording Source</center></html>");

    private JButton recordButton = new JButton(
            "<html><center>Start Recording</center></html>");

    private JButton uploadButton = new JButton(
            "<html><center>Upload, erase or select Event</center></html>");

    private TickCanvas isSourceSelectedCanvas = new TickCanvas();

    private TickCanvas isRecordedCanvas = new TickCanvas();

    private TickCanvas isUploadedCanvas = new TickCanvas();

    private UploadDialog uploadDialog = null;

    private Vector<DataSource> dataSources = new Vector<DataSource>();

    private Vector<RGBRenderer> renderers = new Vector<RGBRenderer>();

    private JPanel previewPanel = new JPanel();

    private JPanel layoutPanel = new JPanel();

    private Layout currentLayout = null;

    private int currentLayoutWidth = 0;

    private int currentLayoutHeight = 0;

    private JComboBox layoutChooser = new JComboBox();

    private HashMap<String, JPanel> layoutPositionPanels =
        new HashMap<String, JPanel>();

    private HashMap<String, RGBRenderer> layoutPositionRenderers =
        new HashMap<String, RGBRenderer>();

    private HashMap<String, DataSource> layoutPositionSources =
        new HashMap<String, DataSource>();

    private HashMap<String, Long> layoutPositionSsrc =
        new HashMap<String, Long>();

    private RecordArchiveManager archiveManager = null;

    private JLabel recordStatus = new JLabel("Not Recording");

    private Vector<DataSource> audioDataSources = new Vector<DataSource>();

    private Vector<Player> audioPlayers = new Vector<Player>();

    private Vector<FloatControl> volumeControls = new Vector<FloatControl>();

    private Vector<JPanel> audioPanels = new Vector<JPanel>();

    private Vector<GainControl> playerGainControls = new Vector<GainControl>();

    private Vector<JSlider> audioSliders = new Vector<JSlider>();

    private Vector<JCheckBox> audioMutes = new Vector<JCheckBox>();

    private JCheckBox playerListen = new JCheckBox(
            "Listen to recorded audio");

    private JPanel volumePanel = new JPanel();

    private Server webServer = null;

    private com.googlecode.vicovre.annotations.live.Server liveAnnotationsServer
        = null;

    private File annotationDirectory = null;

    private File notRecordingAnnotationsDir = null;

    private boolean databaseLoaded = false;

    private boolean annotationServerStarted = false;

    private Integer threadsFinishedSync = new Integer(0);

    /**
     * Creates a new recorder
     * @param args The program arguments
     */
    public Recorder(String[] args) {

        // Set the title of the application
        super(TITLE_PREFIX);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(
                BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        add(content);

        // Load the configuration
        loadConfiguration(args);
        loadRepositories();

        // Load codecs
        try {
            Misc.configureCodecs("/knownCodecs.xml");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading codecs: " + e.getMessage(),
                    TITLE_PREFIX + " - Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // Load the database
        Thread databaseStarter = new Thread() {
            public void run() {
                loadDatabase();
                synchronized (threadsFinishedSync) {
                    databaseLoaded = true;
                    threadsFinishedSync.notifyAll();
                }
            }
        };
        databaseStarter.start();

        // Load the Annotation server
        Thread liveAnnotationServerStarter = new Thread() {
            public void run() {
                loadAnnotationServer();
                synchronized (threadsFinishedSync) {
                    annotationServerStarted = true;
                    threadsFinishedSync.notifyAll();
                }
            }
        };
        liveAnnotationServerStarter.start();

        // Set up the buttons
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new TableLayout(
                new double[]{140, TableLayout.FILL, 140, TableLayout.FILL, 140},
                new double[]{40, 20}));
        isSourceSelectedCanvas.setSize(new Dimension(20, 20));
        isRecordedCanvas.setSize(new Dimension(20, 20));
        isUploadedCanvas.setSize(new Dimension(20, 20));
        recordingSourceButton.addActionListener(this);
        recordButton.addActionListener(this);
        uploadButton.addActionListener(this);
        recordButton.setEnabled(false);
        toolPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        toolPanel.add(recordingSourceButton, "0, 0");
        toolPanel.add(new ArrowCanvas(), "1, 0");
        toolPanel.add(recordButton, "2, 0");
        toolPanel.add(new ArrowCanvas(), "3, 0");
        toolPanel.add(uploadButton, "4, 0");
        toolPanel.add(isSourceSelectedCanvas, "0, 1, c, c");
        toolPanel.add(isRecordedCanvas, "2, 1, c, c");
        toolPanel.add(isUploadedCanvas, "4, 1, c, c");
        content.add(toolPanel);

        JScrollPane volumeScroll = new JScrollPane(volumePanel);
        volumePanel.setLayout(new BoxLayout(volumePanel, BoxLayout.Y_AXIS));
        playerListen.addActionListener(this);

        layoutChooser.addItem("Select a Layout");
        for (Layout layout : layoutRepository.findLayouts()) {
            layoutChooser.addItem(layout.getName());
        }
        layoutChooser.addItemListener(this);

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new TableLayout(
                new double[]{20, 200, 5, TableLayout.FILL, 20},
                new double[]{20, 20, 5, TableLayout.FILL, 30, 5, 30, 5, 30}));
        layoutPanel.setBorder(BorderFactory.createEtchedBorder());
        layoutPanel.setLayout(null);
        previewPanel.setLayout(new PreviewLayout(180, 135));

        recordStatus.setFont(new Font(recordStatus.getFont().getName(),
                Font.BOLD, 18));
        JScrollPane previewScroll = new JScrollPane(previewPanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        previewScroll.setBorder(BorderFactory.createEtchedBorder());
        displayPanel.add(layoutChooser, "1, 1");
        displayPanel.add(layoutPanel, "3, 1, 3, 3");
        displayPanel.add(previewScroll, "1, 3, 1, 6");
        displayPanel.add(playerListen, "3, 4");
        displayPanel.add(volumeScroll, "3, 6");
        displayPanel.add(recordStatus, "1, 8, 3, 8, c, c");
        content.add(displayPanel);

        // Set up the UI
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }

            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        setSize(SIZE_WIDTH, SIZE_HEIGHT);
        setLocationRelativeTo(null);

        synchronized (threadsFinishedSync) {
            while (!databaseLoaded || !annotationServerStarted) {
                try {
                    threadsFinishedSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }

        recordingSource = new RecordingSourceDialog(this,
                configuration, typeRepository);
        recordingSource.init(typeRepository);
        loadUploadDialog();

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                drawCurrentLayout();
            }
        });

        setVisible(true);
    }

    private void loadRepositories() {
        try {
            typeRepository = new RtpTypeRepositoryXmlImpl("/rtptypes.xml");
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading RTP Types: " + e.getMessage(),
                    TITLE_PREFIX + " - Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            layoutRepository = new LayoutRepositoryXmlImpl("/layouts.xml");

        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading Layouts: " + e.getMessage(),
                    TITLE_PREFIX + " - Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void loadUploadDialog() {
        ProgressDialog progress = new ProgressDialog(TITLE_PREFIX, false, true,
                false);
        progress.setMessage("Loading Existing Recordings...");
        progress.setVisible(true);
        uploadDialog = new UploadDialog(this, recordingDatabase,
                annotationDirectory, configuration);
        progress.dispose();
    }

    // Start the annotation server
    private void loadAnnotationServer() {
        ProgressDialog progress = new ProgressDialog(TITLE_PREFIX, false, true,
                false);
        try {
            progress.setMessage("Starting Live Annotation Server...");
            progress.setVisible(true);
            webServer = new Server();
            Connector connector = new SelectChannelConnector();
            connector.setPort(configuration.getIntegerParameter(CONFIG_PORT,
                    8080));
            connector.setMaxIdleTime(60000);
            webServer.setConnectors(new Connector[]{connector});
            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/");
            webapp.setWar(getClass().getResource(
                   "/webapps/ViCoLiveAnnotations.war").toExternalForm());
            webapp.setDefaultsDescriptor(getClass().getResource(
                   "/org/mortbay/jetty/webapp/webdefault.xml").toExternalForm());
            webapp.setParentLoaderPriority(true);
            webServer.setHandler(webapp);
            webServer.start();
            liveAnnotationsServer =
                (com.googlecode.vicovre.annotations.live.Server)
                webapp.getServletContext().getAttribute("server");
            annotationDirectory = new File(dataDirectory, "annotations");
            annotationDirectory.mkdirs();
            notRecordingAnnotationsDir = new File(annotationDirectory,
                    "notrecorded");
            liveAnnotationsServer.setStoreDirectory(
                    notRecordingAnnotationsDir);
            progress.dispose();
        } catch (Throwable e) {
            progress.dispose();
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error Starting Annotation Server " + e.getMessage(),
                    TITLE_PREFIX + " - Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

    }

    private void loadDatabase() {
        try {
            recordingDatabase = new InsecureRecordingDatabase(dataDirectory,
                    typeRepository, layoutRepository, null, false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading database: " + e.getMessage(),
                    TITLE_PREFIX + " - Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void loadConfiguration(String[] args) {
        try {
            if (args.length > 0) {
                File xmlConfig = new File(args[0]);
                configuration = new Config(xmlConfig.getAbsolutePath());
                if (!xmlConfig.exists()) {
                    xmlConfig.createNewFile();
                }
                if (xmlConfig.canWrite()) {
                    configFile = xmlConfig.getAbsolutePath();
                }
            } else {
                File config = new File(System.getProperty("user.home"),
                        DEFAULT_CONFIG_FILE);
                if (config.canRead()) {
                    System.err.println("Reading existing config");
                    InputStream input = new FileInputStream(config);
                    try {
                        configuration = new Config(input);
                    } catch (SAXParseException e) {
                        e.printStackTrace();
                        config.delete();
                        configuration = null;
                    }
                }
                if (configuration == null) {
                    if (!config.exists()) {
                        config.createNewFile();
                    }
                    InputStream input = getClass().getResourceAsStream("/"
                            + DEFAULT_CONFIG_FILE);
                    if (input != null) {
                        configuration = new Config(input);
                    } else {
                        configuration = new Config();
                    }
                }
                if (config.canWrite()) {
                    configFile = config.getAbsolutePath();
                }
            }

            // Try to read config parameters
            boolean doConfig = false;
            String recordingDirectory = configuration.getParameter(
                    CONFIG_DATA_DIRECTORY, null);
            String portVal = configuration.getParameter(CONFIG_PORT, null);
            if (recordingDirectory == null) {
                configuration.setParameter(CONFIG_DATA_DIRECTORY,
                        System.getProperty("user.home") + SLASH
                        + DEFAULT_DATA_DIRECTORY);
                doConfig = true;
            }
            if (portVal == null) {
                configuration.setParameter(CONFIG_PORT, portVal);
                doConfig = true;
            }

            // If any parameters need to be set, prompt the user for the values
            if (doConfig) {
                if (!doConfig()) {
                    JOptionPane.showMessageDialog(null,
                        "The first-run wizard was cancelled."
                        + " This must be run before the recorder can be used."
                        + " This program will now exit.");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading configuration: " + e.getMessage(),
                    TITLE_PREFIX + " - Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        dataDirectory = configuration.getParameter(
                CONFIG_DATA_DIRECTORY, null);
    }

    private boolean doConfig() throws IOException {
        Wizard wizard = WizardPage.createWizard(new Class[]{
            IntroPage.class, DataDirectoryPage.class,
            AnnotationServerPage.class});
        Map<String, String> configMap = configuration.getConfigMap();
        Map<String, String> results = (Map<String, String>)
            WizardDisplayer.showWizard(wizard, null, null, configMap);
        if (results == null) {
            return false;
        }

        // Set the configuration values
        configuration.setParameter(CONFIG_DATA_DIRECTORY,
                results.get(DataDirectoryPage.DIRECTORY_KEY));
        if (configFile != null) {
            configuration.saveParameters(configFile);
        } else {
            JOptionPane.showMessageDialog(null,
                    "No location could be found to save the settings to!",
                    "Warning!", JOptionPane.WARNING_MESSAGE);
        }
        configuration.setParameter(CONFIG_PORT,
                results.get(AnnotationServerPage.PORT_KEY));
        configuration.saveParameters(configFile);
        return true;
    }

    private void selectRecordingSource() {
        recordingSource.setVisible(true);
        if (!recordingSource.wasCancelled()) {
            try {
                configuration.saveParameters(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            isSourceSelectedCanvas.setTick(true);
            isSourceSelectedCanvas.repaint();
            recordButton.setEnabled(true);
        }
    }

    /**
     *
     * @see javax.swing.event.ChangeListener#stateChanged(
     *     javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
        Object source = e.getSource();
        for (int i = 0; i < audioSliders.size(); i++) {
            JSlider slider = audioSliders.get(i);
            if (source.equals(slider)) {
                FloatControl volume = volumeControls.get(i);
                volume.setValue((slider.getValue() * volume.getMaximum())
                        / slider.getMaximum());
            }
        }
    }

    /**
     *
     * @see java.awt.event.ActionListener#actionPerformed(
     *     java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == recordingSourceButton) {
            selectRecordingSource();
        } else if (source == recordButton) {
            if (archiveManager == null) {
                startRecording();
            } else {
                stopRecording();
            }
        } else if (source == uploadButton) {
            uploadDialog.setVisible(true);
        } else if (source == playerListen) {
            for (int i = 0; i < playerGainControls.size(); i++) {
                GainControl control = playerGainControls.get(i);
                if (control != null) {
                    control.setMute(!playerListen.isSelected());
                }
            }
        } else {
            for (int i = 0; i < audioMutes.size(); i++) {
                JCheckBox mute = audioMutes.get(i);
                if (source.equals(mute)) {
                    FloatControl volume = volumeControls.get(i);
                    if (mute.isSelected()) {
                        volume.setValue(volume.getMinimum());
                    } else {
                        JSlider slider = audioSliders.get(i);
                        volume.setValue(
                                (slider.getValue() * volume.getMaximum())
                                / slider.getMaximum());
                    }
                }
            }
        }
    }

    private void startRecording() {
        archiveManager = new RecordArchiveManager(typeRepository,
                recordingDatabase.getTopLevelFolder(),
                RecordArchiveManager.generateId(new Date()));
        File annotationDir = new File(annotationDirectory,
                archiveManager.getRecording().getId());
        liveAnnotationsServer.setStoreDirectory(annotationDir);
        archiveManager.enableRecording();
        recordingSource.setArchiveManager(archiveManager);
        recordButton.setText("Stop Recording");
        recordStatus.setText("Recording");
        recordStatus.setForeground(Color.RED);
        uploadDialog.startRecording();
    }

    private void stopRecording() {
        try {
            archiveManager.disableRecording(false);
            archiveManager.terminate();
            liveAnnotationsServer.setStoreDirectory(
                    notRecordingAnnotationsDir);
            recordingSource.setArchiveManager(null);
            isRecordedCanvas.setTick(true);
            recordButton.setText("Start Recording");
            recordStatus.setText("Not Recording");
            recordStatus.setForeground(Color.BLACK);

            Recording recording = archiveManager.getRecording();

            if (currentLayout != null) {
                ReplayLayout layout = new ReplayLayout(layoutRepository);
                layout.setName(currentLayout.getName());
                layout.setTime(recording.getStartTime().getTime());

                for (LayoutPosition position
                        : currentLayout.getStreamPositions()) {
                    String name = position.getName();
                    if (layoutPositionSsrc.containsKey(name)) {
                        String ssrc =
                            String.valueOf(layoutPositionSsrc.get(name));
                        Stream selectedStream = null;
                        List<Stream> streams = recording.getStreams();
                        for (Stream stream : streams) {
                            if (stream.getSsrc().equals(ssrc)) {
                                selectedStream = stream;
                            }
                        }
                        layout.setStream(name, selectedStream);
                    }
                }

                Vector<ReplayLayout> layouts = new Vector<ReplayLayout>();
                layouts.add(layout);
                recording.setReplayLayouts(layouts);
            }

            uploadDialog.finishRecording(recording);
            recordingDatabase.addRecording(recording, null);
            archiveManager = null;
        } catch (Exception error) {
            error.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error stopping recording: " + error,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Quits the program
    private void quit() {
        recordingSource.resetAudioToOriginalValues();
        try {
            configuration.saveParameters(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dispose();
    }

    /**
     * Adds a video datasource to the panel
     * @param name The name of the data source
     * @param dataSource The data source to add
     * @param ssrc The ssrc of the video
     * @throws IOException
     */
    public void addLocalVideo(String name, DataSource dataSource, long ssrc)
            throws IOException {
        System.err.println("Adding local video");
        PushBufferStream[] datastreams =
            ((PushBufferDataSource) dataSource).getStreams();
        RGBRenderer previewRenderer = new RGBRenderer(new Effect[]{});
        previewRenderer.setDataSource(dataSource, 0);
        previewRenderer.setInputFormat(
                datastreams[0].getFormat());
        Component c = previewRenderer.getPreviewRenderer().getComponent();
        previewPanel.add(c);
        c.setVisible(true);
        VideoDragListener listener = new VideoDragListener(this,
                previewRenderer, dataSource, ssrc);
        c.addMouseMotionListener(listener);
        c.addMouseListener(listener);
        previewRenderer.start();
        dataSources.add(dataSource);
        renderers.add(previewRenderer);
        validate();
        repaint();
    }

    private void removeCurrentLayoutRenderer(RGBRenderer renderer) {
        String name = null;
        if (currentLayout != null) {
            for (LayoutPosition position : currentLayout.getStreamPositions()) {
                RGBRenderer r = layoutPositionRenderers.get(position.getName());
                if (renderer == r) {
                    name = position.getName();
                    break;
                }
            }
        }
        removeCurrentLayoutRenderer(name);
    }

    private void removeCurrentLayoutRenderer(DataSource dataSource) {
        String name = null;
        if (currentLayout != null) {
            for (LayoutPosition position : currentLayout.getStreamPositions()) {
                DataSource ds = layoutPositionSources.get(position.getName());
                if (dataSource == ds) {
                    name = position.getName();
                    break;
                }
            }
        }
        removeCurrentLayoutRenderer(name);
    }

    private void removeCurrentLayoutRenderer(String name) {

        if (name != null) {
            JPanel videoPanel = layoutPositionPanels.get(name);
            videoPanel.remove(layoutPositionRenderers.get(name).getComponent());
            layoutPositionRenderers.remove(name);
            videoPanel.validate();
            videoPanel.add(
                    new JLabel(POSITION_TEXT.replace(POSITION_PLACE, name)),
                    "0, 0, c, c");
            videoPanel.validate();
            videoPanel.repaint();
            layoutPositionSsrc.remove(name);
            layoutPositionSources.remove(name);
        }
    }

    /**
     * Removes the video from the recorder
     *
     * @param dataSource The datasource to remove
     */
    public void removeLocalVideo(DataSource dataSource) {
        int index = dataSources.indexOf(dataSource);
        if (index != -1) {
            RGBRenderer renderer = renderers.get(index);
            removeCurrentLayoutRenderer(dataSource);
            previewPanel.remove(renderer.getPreviewRenderer().getComponent());
            dataSources.remove(index);
            renderers.remove(index);
        }
    }

    /**
     * Adds a local audio datasource
     * @param name The name of the audio source
     * @param dataSource The datasource
     * @param volumeControl The volume control or null of none
     * @throws IOException
     * @throws CannotRealizeException
     * @throws NoPlayerException
     */
    public void addLocalAudio(String name, DataSource dataSource,
            FloatControl volumeControl, long ssrc)
            throws NoPlayerException, CannotRealizeException, IOException {
        Player player = Manager.createRealizedPlayer(dataSource);
        audioDataSources.add(dataSource);
        audioPlayers.add(player);
        volumeControls.add(volumeControl);

        GainControl gain = (GainControl) player.getControl(
                GainControl.class.getCanonicalName());
        playerGainControls.add(gain);
        if (gain != null) {
            gain.setLevel(0.5f);
            gain.setMute(!playerListen.isSelected());
        } else {
            System.err.println("No local playback gain control!");
        }
        JPanel audioPanel = new JPanel();
        audioPanel.setLayout(new BoxLayout(audioPanel, BoxLayout.X_AXIS));
        audioPanel.add(new JLabel(name));

        if (volumeControl != null) {
            JCheckBox muteBox = new JCheckBox("Mute");
            JSlider volumeSlider = new JSlider();
            volumeSlider.setValue((int) ((volumeControl.getValue()
                    * volumeSlider.getMaximum())
                    / volumeControl.getMaximum()));
            muteBox.addActionListener(this);
            volumeSlider.addChangeListener(this);
            audioPanel.add(Box.createHorizontalStrut(5));
            audioPanel.add(volumeSlider);
            audioPanel.add(muteBox);
            audioSliders.add(volumeSlider);
            audioMutes.add(muteBox);
        } else {
            System.err.println("No recording gain control for " + name);
            audioSliders.add(null);
            audioMutes.add(null);
        }
        audioPanels.add(audioPanel);
        volumePanel.add(audioPanel);

        player.start();
        validate();
    }

    /**
     * Removes the local audio playback
     * @param dataSource The datasource to remove
     */
    public void removeLocalAudio(DataSource dataSource) {
        int index = audioDataSources.indexOf(dataSource);
        if (index != -1) {
            System.err.println("Removing datasource");
            audioDataSources.remove(index);
            JPanel panel = audioPanels.get(index);
            if (panel != null) {
                System.err.println("Removing panel");
                volumePanel.remove(panel);
            }
            audioPanels.remove(index);
            Player player = audioPlayers.get(index);
            if (player != null) {
                player.stop();
            }
            audioPlayers.remove(index);
            volumeControls.remove(index);
            playerGainControls.remove(index);
            audioSliders.remove(index);
            audioMutes.remove(index);
        }
        validate();
    }

    /**
     * Moves a renderer to a layout position
     * @param renderer The renderer to move
     * @param ssrc The ssrc of the stream
     */
    public void moveVideoToPosition(String position, RGBRenderer renderer,
            DataSource dataSource, long ssrc) {
        RGBRenderer currentRenderer = layoutPositionRenderers.get(position);
        JPanel videoPanel = layoutPositionPanels.get(position);
        if (renderer != currentRenderer) {

            if (currentRenderer != null) {
                videoPanel.remove(currentRenderer.getComponent());
            }
            removeCurrentLayoutRenderer(renderer);

            Component c = renderer.getComponent();
            videoPanel.removeAll();
            videoPanel.add(c, "0, 0");
            Dimension size = new Dimension(200, 150);
            c.setSize(size);

            c.setMaximumSize(size);
            c.setPreferredSize(size);
            c.setMaximumSize(size);
            layoutPositionRenderers.put(position, renderer);
            layoutPositionSsrc.put(position, ssrc);
            layoutPositionSources.put(position, dataSource);
            validate();
        }
    }

    public Layout getCurrentLayout() {
        return currentLayout;
    }

    public JPanel getLayoutPanel(String position) {
        return layoutPositionPanels.get(position);
    }

    private void drawCurrentLayout() {
        if (currentLayout != null) {
            double scaleX = (double) layoutPanel.getWidth()
                / currentLayoutWidth;
            double scaleY = (double) layoutPanel.getHeight()
                / currentLayoutHeight;
            double scale = Math.min(scaleX, scaleY);
            layoutPanel.removeAll();
            for (LayoutPosition position
                    : currentLayout.getStreamPositions()) {
                String name = position.getName();
                JPanel panel = layoutPositionPanels.get(name);
                if (panel == null) {
                    panel = new JPanel();
                    panel.setBorder(BorderFactory.createEtchedBorder());
                    panel.setLayout(new TableLayout(
                            new double[]{TableLayout.FILL},
                            new double[]{TableLayout.FILL}));

                    if (position.isAssignable()) {
                        layoutPositionPanels.put(name, panel);
                        JLabel text = new JLabel(POSITION_TEXT.replace(
                                POSITION_PLACE, name));
                        panel.add(text, "0, 0, c, c");
                    } else {
                        panel.setBackground(Color.LIGHT_GRAY);
                        JLabel text = new JLabel("<html><center>" + name
                                + "</center></html>");
                        text.setForeground(Color.GRAY);
                        panel.add(text, "0, 0, c, c");
                    }
                }
                layoutPanel.add(panel);

                panel.setLocation((int) (position.getX() * scale),
                        (int) (position.getY() * scale));
                panel.setSize((int) (position.getWidth() * scale),
                        (int) (position.getHeight() * scale));
            }
            layoutPanel.validate();
            layoutPanel.repaint();
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource().equals(layoutChooser)
                && e.getStateChange() == ItemEvent.SELECTED) {
            currentLayout = layoutRepository.findLayout(
                    (String) layoutChooser.getSelectedItem());
            layoutPositionPanels.clear();
            layoutPositionRenderers.clear();
            layoutPositionSources.clear();
            layoutPositionSsrc.clear();
            if (currentLayout == null) {
                layoutPanel.removeAll();
                layoutPanel.validate();
                layoutPanel.repaint();
            } else {
                int minX = Integer.MAX_VALUE;
                int maxX = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = 0;
                for (LayoutPosition position
                        : currentLayout.getStreamPositions()) {
                    minX = Math.min(minX, position.getX());
                    minY = Math.min(minY, position.getY());
                    maxX = Math.max(maxX, position.getX()
                            + position.getWidth());
                    maxY = Math.max(maxY, position.getY()
                            + position.getHeight());
                }
                currentLayoutWidth = maxX + minX;
                currentLayoutHeight = maxY + minY;
                drawCurrentLayout();
            }
        }
    }

    /**
     * The main method
     * @param args The program arguments
     */
    public static void main(String[] args) {
        System.setSecurityManager(null);
        new Recorder(args);
    }
}
