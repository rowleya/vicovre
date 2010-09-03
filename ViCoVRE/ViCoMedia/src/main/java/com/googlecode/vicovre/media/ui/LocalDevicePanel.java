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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.media.CannotRealizeException;
import javax.media.Effect;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPConnector;
import javax.sound.sampled.FloatControl;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ag3.interfaces.types.ClientProfile;

import com.googlecode.vicovre.media.processor.ClosestFormatComparator;
import com.googlecode.vicovre.media.protocol.sound.JavaSoundStream;
import com.googlecode.vicovre.media.renderer.RGBRenderer;
import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.utils.Config;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;

/**
 * A panel for selecting local devices
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class LocalDevicePanel extends JPanel implements ActionListener,
        ItemListener, LocalStreamListener {

    private static final long serialVersionUID = 1L;

    private static final String LAST_DEVICES = "lastDevices";

    private static final String LAST_AUDIO_DEVICES = "lastAudioDevices";

    private static final String LAST_AUDIO_SELECTED = "lastAudioSelect";

    private static final String LAST_AUDIO_VOLUME = "lastAudioVolume";

    private static final String DEVICES = "devices";

    // The default height of the preview window
    private static final int PREVIEW_HEIGHT = 85;

    // The default width of the preview window
    private static final int PREVIEW_WIDTH = 104;

    // The height of the device listing box
    private static final int DEVICE_BOX_HEIGHT = 90;

    // The height of an audio device line
    private static final int DEVICE_HEIGHT = 20;

    private JPanel videoDeviceBox = new JPanel();

    private JPanel previewPanel = new JPanel();

    private VideoFormat[] preferredCaptureFormats = null;

    private HashSet<VideoDevice> videoDevices = new HashSet<VideoDevice>();

    private HashMap<VideoDevice, JCheckBox> videoSelected =
        new HashMap<VideoDevice, JCheckBox>();

    private HashMap<VideoDevice, Boolean> videoInitiallySelected =
        new HashMap<VideoDevice, Boolean>();

    private HashMap<VideoDevice, JButton> videoPreview =
        new HashMap<VideoDevice, JButton>();

    private HashMap<VideoDevice, JComboBox> videoFormat =
        new HashMap<VideoDevice, JComboBox>();

    private HashMap<VideoDevice, JComboBox> videoInput =
        new HashMap<VideoDevice, JComboBox>();

    private HashMap<VideoDevice, JComboBox> videoCaptureFormat =
        new HashMap<VideoDevice, JComboBox>();

    private HashMap<VideoDevice, CaptureFormat[][]> videoCaptureFormats =
        new HashMap<VideoDevice, CaptureFormat[][]>();

    private JPanel audioDeviceBox = new JPanel();

    private HashSet<AudioDevice> audioDevices = new HashSet<AudioDevice>();

    private HashMap<AudioDevice, JCheckBox> audioSelected =
        new HashMap<AudioDevice, JCheckBox>();

    private HashMap<AudioDevice, Boolean> audioInitiallySelected =
        new HashMap<AudioDevice, Boolean>();

    private HashMap<AudioDevice, JComboBox> audioInputSelected =
        new HashMap<AudioDevice, JComboBox>();

    private HashMap<AudioDevice, Integer> audioInputInitiallySelected =
        new HashMap<AudioDevice, Integer>();

    private HashMap<AudioDevice, JComboBox> audioFormat =
        new HashMap<AudioDevice, JComboBox>();

    private RTPConnector audioConnector = null;

    private RTPConnector videoConnector = null;

    private RGBRenderer previewRenderer = null;

    private DataSource previewDataSource = null;

    private VideoDevice previewDevice = null;

    private JDialog parent = null;

    private JButton redetectDevices = new JButton("Re-Detect");

    private Vector<String> knownDevices = new Vector<String>();

    private HashSet<LocalStreamListener> listeners =
        new HashSet<LocalStreamListener>();

    private RTPType[] videoTypes = null;

    private RTPType[] audioTypes = null;

    private boolean allowTypeSelection = false;

    private ClientProfile profile = null;

    private String tool = null;

    /**
     * Creates a new LocalDevicePanel
     * @param parent The parent
     */
    public LocalDevicePanel(JDialog parent,
            RTPType[] videoTypes, RTPType[] audioTypes,
            boolean allowTypeSelection,
            RTPConnector videoConnector, RTPConnector audioConnector,
            ClientProfile profile, String tool,
            VideoFormat[] preferredCaptureFormats) {
        this.parent = parent;
        this.videoConnector = videoConnector;
        this.audioConnector = audioConnector;
        this.videoTypes = videoTypes;
        this.audioTypes = audioTypes;
        this.allowTypeSelection = allowTypeSelection;
        this.profile = profile;
        this.tool = tool;
        this.preferredCaptureFormats = preferredCaptureFormats;

        detectDevices();

        setAlignmentX(Component.LEFT_ALIGNMENT);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        videoDeviceBox.setLayout(new BoxLayout(videoDeviceBox,
                BoxLayout.Y_AXIS));
        JScrollPane videoScroll = new JScrollPane(videoDeviceBox);
        JPanel videoPanel = new JPanel();
        videoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.X_AXIS));
        videoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                DEVICE_BOX_HEIGHT));
        videoPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE,
                DEVICE_BOX_HEIGHT));
        previewPanel.setLayout(null);
        Dimension previewSize = new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        previewPanel.setPreferredSize(previewSize);
        previewPanel.setMaximumSize(previewSize);
        previewPanel.setMinimumSize(previewSize);
        previewPanel.setBackground(Color.BLACK);
        videoPanel.add(videoScroll);
        videoPanel.add(previewPanel);
        add(new JLabel("Select video devices to use:"));
        add(videoPanel);

        audioDeviceBox.setLayout(new BoxLayout(audioDeviceBox,
                BoxLayout.Y_AXIS));
        JScrollPane audioScroll = new JScrollPane(audioDeviceBox);
        audioScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        audioScroll.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, DEVICE_BOX_HEIGHT));
        audioScroll.setPreferredSize(new Dimension(
                Integer.MAX_VALUE, DEVICE_BOX_HEIGHT));
        add(new JLabel("Select audio devices to use:"));
        add(audioScroll);

        redetectDevices.addActionListener(this);
        add(redetectDevices);
    }

    public void changeClientProfile(ClientProfile profile) {
        this.profile = profile;
    }

    public void init(Config configuration) {
        if (configuration != null) {

            String[] knownDevs = configuration.getParameters(DEVICES);
            for (String device : knownDevs) {
                knownDevices.add(device);
            }

            List<String> selectedCaptureDevices = Arrays.asList(
                configuration.getParameters(LAST_DEVICES));
            for (VideoDevice device : videoDevices) {
                videoSelected.get(device).setSelected(
                        selectedCaptureDevices.contains(
                                device.getDevice().getName()));
            }

            List<String> selectedAudioDevices = Arrays.asList(
                configuration.getParameters(LAST_AUDIO_DEVICES));
            List<String> selectedAudioInputs = Arrays.asList(
                    configuration.getParameters(LAST_AUDIO_SELECTED));
            for (AudioDevice device : audioDevices) {
                audioSelected.get(device).setSelected(
                        selectedAudioDevices.contains(device.getName()));
                JComboBox inputBox = audioInputSelected.get(device);
                for (String line : device.getLines()) {
                    if (selectedAudioInputs.contains(
                            device.getName() + ":" + line)) {
                        inputBox.setSelectedItem(line);
                    }
                    String lastVolume = configuration.getParameter(
                        LAST_AUDIO_VOLUME + ":" + device.getName() + ":" + line,
                        null);
                    if (lastVolume != null) {
                        device.setLineVolume(line, Float.parseFloat(
                                lastVolume));
                    }
                }
            }
        }
    }

    public void addLocalStreamListener(LocalStreamListener listener) {
        listeners.add(listener);
    }

    public void removeLocalStreamListener(LocalStreamListener listener) {
        listeners.remove(listener);
    }

    private CaptureFormat[] getCaptureFormats(Format[] formats) {
        Vector<CaptureFormat> captureFormats = new Vector<CaptureFormat>();
        for (int k = 0; k < formats.length; k++) {
            if (formats[k] instanceof VideoFormat) {
                CaptureFormat format =
                    new CaptureFormat((VideoFormat) formats[k]);
                int index = captureFormats.indexOf(format);
                if (index == -1) {
                    captureFormats.add(format);
                } else {
                    CaptureFormat currentFormat = captureFormats.get(index);

                    for (int i = 0; i < preferredCaptureFormats.length; i++) {
                        ClosestFormatComparator comparator =
                            new ClosestFormatComparator(
                                preferredCaptureFormats[i]);
                        int value = comparator.compare(formats[k],
                                currentFormat.getFormat());
                        if (value > 0) {
                            break;
                        } else if (value < 0) {
                            captureFormats.set(index, format);
                            break;
                        }
                    }
                }
            }
        }
        Collections.sort(captureFormats);
        return captureFormats.toArray(new CaptureFormat[0]);
    }

    private void detectDevices() {
        ProgressDialog progress = new ProgressDialog(parent,
                "Detecting Devices", false, true, false);
        progress.setMessage("Detecting Devices...");
        progress.setVisible(true);

        detectVideoDevices();
        detectAudioDevices();

        videoDeviceBox.removeAll();
        VideoDevice[] videoDevs = videoDevices.toArray(new VideoDevice[0]);
        Arrays.sort(videoDevs);

        for (int i = 0; i < videoDevs.length; i++) {
            try {
                JComboBox inputBox = videoInput.get(videoDevs[i]);
                inputBox.setRenderer(new InputBoxCellRenderer(
                        inputBox.getRenderer()));
                String[] inputs = videoDevs[i].getDevice().getInputs();
                CaptureFormat[][] captureFormats = videoCaptureFormats.get(
                        videoDevs[i]);

                if (inputs.length > 0) {
                    boolean successfulInput = false;
                    for (int j = 0; j < inputs.length; j++) {
                        try {
                            Format[] formats = videoDevs[i].getFormats(j);
                            inputBox.addItem(inputs[j]);
                            captureFormats[j] = getCaptureFormats(formats);
                            successfulInput = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            JLabel errorLabel =
                                new JLabel("Error starting input " + inputs[j]);
                            errorLabel.setToolTipText(
                                    "Error: " + e.getMessage());
                            errorLabel.setForeground(Color.RED);
                            inputBox.addItem(errorLabel);
                            captureFormats[j] = new CaptureFormat[0];
                        }
                    }
                    if (!successfulInput) {
                        throw new IOException("Could not find a working input");
                    }
                } else {
                    Format[] formats = videoDevs[i].getFormats(0);
                    captureFormats[0] = getCaptureFormats(formats);
                    inputBox.addItem("");
                }
                inputBox.addItemListener(this);
                inputBox.setSelectedIndex(0);
                inputBox.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
                changeCaptureFormats(videoDevs[i], inputBox);

                JComboBox captureFormatBox = videoCaptureFormat.get(
                        videoDevs[i]);
                captureFormatBox.setMaximumSize(
                        new Dimension(100, Integer.MAX_VALUE));

                JCheckBox checkBox = videoSelected.get(videoDevs[i]);
                checkBox.setAlignmentX(LEFT_ALIGNMENT);
                JPanel devicePanel = new JPanel();
                devicePanel.setLayout(new BoxLayout(devicePanel,
                        BoxLayout.X_AXIS));
                JComboBox formatBox = videoFormat.get(videoDevs[i]);
                formatBox.setEditable(false);
                formatBox.setSelectedIndex(0);
                formatBox.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
                JButton preview = videoPreview.get(videoDevs[i]);
                preview.addActionListener(this);
                devicePanel.add(checkBox);
                devicePanel.add(Box.createHorizontalGlue());
                devicePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                        DEVICE_HEIGHT));
                if (inputBox.getItemCount() > 1) {
                    devicePanel.add(inputBox);
                }
                devicePanel.add(captureFormatBox);
                if (allowTypeSelection) {
                    devicePanel.add(formatBox);
                }
                devicePanel.add(preview);
                videoDeviceBox.add(devicePanel);
            } catch (Exception e) {
                e.printStackTrace();
                JPanel devicePanel = new JPanel();
                devicePanel.setLayout(new BoxLayout(devicePanel,
                        BoxLayout.X_AXIS));
                JLabel errorLabel = new JLabel(" Error starting device "
                        + videoDevs[i].getDevice().getName());
                errorLabel.setToolTipText("Error: " + e.getMessage());
                errorLabel.setForeground(Color.RED);
                errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                devicePanel.add(errorLabel);
                devicePanel.add(Box.createHorizontalGlue());
                videoDeviceBox.add(devicePanel);
            }
        }
        if (videoDevs.length == 0) {
            videoDeviceBox.add(new JLabel(" No Video Devices Detected"));
        }

        audioDeviceBox.removeAll();
        for (AudioDevice device : audioDevices) {
            try {
                device.test();
                JComboBox inputBox = audioInputSelected.get(device);
                for (String line : device.getLines()) {
                    inputBox.addItem(line);
                }
                inputBox.addItemListener(this);
                inputBox.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
                inputBox.setMinimumSize(new Dimension(100, Integer.MAX_VALUE));

                JCheckBox checkBox = audioSelected.get(device);
                JComboBox formatBox = audioFormat.get(device);
                formatBox.setEditable(false);
                formatBox.setSelectedIndex(0);
                formatBox.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
                formatBox.setMinimumSize(new Dimension(100, Integer.MAX_VALUE));

                JPanel devicePanel = new JPanel();
                devicePanel.setLayout(new BoxLayout(devicePanel,
                        BoxLayout.X_AXIS));
                devicePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                        DEVICE_HEIGHT));
                devicePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                devicePanel.add(checkBox);
                devicePanel.add(Box.createHorizontalGlue());
                if (allowTypeSelection) {
                    devicePanel.add(formatBox);
                }
                devicePanel.add(inputBox);
                audioDeviceBox.add(devicePanel);

            } catch (Exception e) {
                JPanel devicePanel = new JPanel();
                devicePanel.setLayout(new BoxLayout(devicePanel,
                        BoxLayout.X_AXIS));
                devicePanel.setMaximumSize(new Dimension(481,
                        DEVICE_HEIGHT));
                devicePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel errorLabel = new JLabel(" Error starting device "
                        + device.getName());
                errorLabel.setToolTipText("Error: " + e.getMessage());
                errorLabel.setForeground(Color.RED);
                errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                devicePanel.add(errorLabel);
                devicePanel.add(Box.createHorizontalGlue());
                audioDeviceBox.add(devicePanel);
            }
        }

        progress.setVisible(false);
    }


    /**
     * Resets the audio settings to the values they were set to when the program
     * was started
     * @param configuration The configuration to store values in
     */
    public void resetAudioToOriginalValues(Config configuration) {
        for (AudioDevice device : audioDevices) {
            for (String line : device.getLines()) {
                float volume = device.getLineVolume(line);
                if (volume != -1) {
                    configuration.setParameter(
                        LAST_AUDIO_VOLUME + ":" + device.getName() + ":" + line,
                        String.valueOf(volume));
                }
            }

            device.resetToOriginalVolumes();
        }
    }

    /**
     * Stores the initial values in case the dialog is cancelled
     */
    public void captureInitialValues() {
        for (VideoDevice device : videoDevices) {
            JCheckBox checkBox = videoSelected.get(device);
            JButton preview = videoPreview.get(device);
            videoInitiallySelected.put(device, checkBox.isSelected());
            preview.setEnabled(!device.isStarted());
        }
        for (AudioDevice device : audioDevices) {
            JCheckBox checkBox = audioSelected.get(device);
            audioInitiallySelected.put(device, checkBox.isSelected());
        }
    }

    /**
     * Resets the panel to how it was when captureInitialValues was last called
     */
    public void resetToInitialValues() {
        for (VideoDevice device : videoDevices) {
            JCheckBox checkBox = videoSelected.get(device);
            checkBox.setSelected(videoInitiallySelected.get(device));
        }
        for (AudioDevice device : audioDevices) {
            JCheckBox checkBox = audioSelected.get(device);
            checkBox.setSelected(audioInitiallySelected.get(device));
        }
    }

    /**
     * Stores the current configuration
     * @param configuration The config to store using
     */
    public void storeConfiguration(Config configuration) {
        configuration.setParameters(DEVICES, knownDevices);
        Vector<String> lastDevices = new Vector<String>();
        for (VideoDevice device : videoDevices) {
            JCheckBox checkBox = videoSelected.get(device);
            if (checkBox.isSelected()) {
                lastDevices.add(device.getDevice().getName());
            }
        }
        Vector<String> lastAudioDevices = new Vector<String>();
        Vector<String> lastAudioInputs = new Vector<String>();
        for (AudioDevice device : audioDevices) {
            JCheckBox checkBox = audioSelected.get(device);
            if (checkBox.isSelected()) {
                lastAudioDevices.add(device.getName());
            }
            JComboBox inputBox = audioInputSelected.get(device);
            lastAudioInputs.add(device.getName() + ":"
                    + (String) inputBox.getSelectedItem());
        }
        configuration.setParameters(LAST_DEVICES, lastDevices);
        configuration.setParameters(LAST_AUDIO_DEVICES, lastAudioDevices);
        configuration.setParameters(LAST_AUDIO_SELECTED, lastAudioInputs);
    }

    /**
     * Sets up the selected and deselected devices
     */
    public void changeDevices() {
        for (VideoDevice device : videoDevices) {
            JCheckBox checkBox = videoSelected.get(device);
            if (checkBox.isSelected()) {
                startVideoDevice(device);
            } else {
                stopVideoDevice(device);
            }
        }
        for (AudioDevice device : audioDevices) {
            JCheckBox checkBox = audioSelected.get(device);
            if (checkBox.isSelected()) {
                startAudioDevice(device);
            } else {
                stopAudioDevice(device);
            }
        }
    }

    /**
     * Stops all the devices
     */
    public void stopDevices() {
        for (VideoDevice device : videoDevices) {
            stopVideoDevice(device);
        }
        for (AudioDevice device : audioDevices) {
            stopAudioDevice(device);
        }
    }


    private void startVideoDevice(VideoDevice device) {

        try {
            JComboBox inputBox = videoInput.get(device);
            JComboBox formatBox = videoFormat.get(device);
            RTPType type = (RTPType) formatBox.getSelectedItem();
            JComboBox captureFormatBox = videoCaptureFormat.get(device);
            CaptureFormat format = (CaptureFormat)
                captureFormatBox.getSelectedItem();
            device.prepare(inputBox.getSelectedIndex(), videoConnector,
                    (VideoFormat) type.getFormat(), type.getId(),
                    format.getFormat());
            device.start(this);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "There was an error starting the device "
                    + device.getDevice().getName(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopVideoDevice(VideoDevice device) {
        device.stop();
        device.finish();
    }

    private void startAudioDevice(AudioDevice device) {
        try {
            JComboBox inputBox = audioInputSelected.get(device);
            String line = (String) inputBox.getSelectedItem();
            JComboBox formatBox = audioFormat.get(device);
            RTPType type = (RTPType) formatBox.getSelectedItem();
            device.prepare(audioConnector,
                    (AudioFormat) type.getFormat(), type.getId());
            device.start(this, line);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "There was an error starting the audio device "
                    + device.getName(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopAudioDevice(AudioDevice device) {
        device.stop(this);
    }

    private void detectAudioDevices() {
        Vector<String> devices = JavaSoundStream.getCompatibleMixers();
        for (String device : devices) {
            AudioDevice dev = new AudioDevice(device, profile, tool);
            if (!audioDevices.contains(dev)) {
                AudioDevice audioDev = new AudioDevice(device, profile, tool);
                audioDevices.add(audioDev);
                audioSelected.put(audioDev, new JCheckBox(device));
                audioFormat.put(audioDev, new JComboBox(audioTypes));
                audioInitiallySelected.put(audioDev, false);
                audioInputSelected.put(audioDev, new JComboBox());
                audioInputInitiallySelected.put(audioDev, 0);
            }
        }
    }

    private void detectVideoDevices() {

        Vector<VideoCaptureDevice> devices = new Vector<VideoCaptureDevice>();

        // Screen devices
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int i = 0; i < gs.length; i++) {
            VideoCaptureDevice device = new VideoCaptureDevice(
                "Local Screen - Monitor " + String.valueOf(i + 1),
                com.googlecode.vicovre.media.protocol.screen.DataSource.class,
                new MediaLocator("screen://fullscreen:" + i));
            devices.add(device);
        }

        // Civil devices
        CaptureSystemFactory factory =
            DefaultCaptureSystemFactorySingleton.instance();
        try {
            CaptureSystem system = factory.createCaptureSystem();
            system.init();
            List<com.lti.civil.CaptureDeviceInfo> list =
                system.getCaptureDeviceInfoList();
            for (int i = 0; i < list.size(); ++i) {
                com.lti.civil.CaptureDeviceInfo civilInfo = list.get(i);
                String name = civilInfo.getDescription();
                String[] outputNames = civilInfo.getOutputNames();
                if (outputNames.length > 0) {
                    for (int j = 0; j < outputNames.length; j++) {
                        String[] inputNames = civilInfo.getInputNames(j);
                        MediaLocator[] inputLocators =
                            new MediaLocator[inputNames.length];
                        for (int k = 0; k < inputNames.length; k++) {
                            inputLocators[k] = new MediaLocator("civil:"
                                        + civilInfo.getDeviceID()
                                        + "?output=" + j
                                        + "&input=" + k);
                        }
                        VideoCaptureDevice device = new VideoCaptureDevice(
                                name + " - " + outputNames[j],
                            com.googlecode.vicovre.media.protocol.civil.DataSource.class,
                            civilInfo.getInputNames(j), inputLocators);
                        devices.add(device);
                    }
                } else {
                    VideoCaptureDevice device = new VideoCaptureDevice(name,
                        com.googlecode.vicovre.media.protocol.civil.DataSource.class,
                        new MediaLocator("civil:" + civilInfo.getDeviceID()));
                    devices.add(device);
                }
            }
        } catch (CaptureException e) {
            e.printStackTrace();
        }

        HashSet<String> names = new HashSet<String>();
        HashMap<String, String> idMap = new HashMap<String, String>();
        for (String device : knownDevices) {
            String[] parts = device.split(":");
            names.add(parts[0]);
            idMap.put(parts[1], parts[0]);
        }

        for (VideoCaptureDevice device : devices) {
            VideoDevice dev = new VideoDevice(device, profile, tool);
            VideoDevice vidDev = null;
            if (!videoDevices.contains(dev)) {
                String knownDev = idMap.get(device.getLocator().getRemainder());
                if (knownDev != null) {
                    device.setName(knownDev);
                } else {
                    String name = device.getName();
                    int count = 1;
                    while (names.contains(name)) {
                        count += 1;
                        name = device.getName() + " #" + count;
                    }
                    device.setName(name);
                    knownDevices.add(name + ":"
                            + device.getLocator().getRemainder());
                    names.add(name);
                }
                vidDev = new VideoDevice(device, profile, tool);
                videoDevices.add(vidDev);
                videoSelected.put(vidDev, new JCheckBox(
                        vidDev.getDevice().getName()));
                videoFormat.put(vidDev, new JComboBox(videoTypes));
                videoInitiallySelected.put(vidDev, false);
                videoPreview.put(vidDev, new JButton("Preview"));
                videoInput.put(vidDev, new JComboBox());
                if (device.getInputs().length > 0) {
                    videoCaptureFormats.put(vidDev,
                        new CaptureFormat[device.getInputs().length][0]);
                } else {
                    videoCaptureFormats.put(vidDev,
                        new CaptureFormat[1][0]);
                }
                videoCaptureFormat.put(vidDev, new JComboBox());
            }
        }
    }

    /**
     * Stops any preview
     */
    public void stopPreview() {
        if (previewRenderer != null) {
            if (previewDataSource != null) {
                previewDataSource.disconnect();
            }
            previewPanel.removeAll();
            previewRenderer.stop();
            previewDevice.stop();
        }
        previewRenderer = null;
        previewDataSource = null;
        previewDevice = null;
    }

    /**
     * Verifies that the panel is OK
     * @return true if the correct devices are selected
     */
    public boolean verify() {
        boolean deviceSelected = false;
        for (VideoDevice device : videoDevices) {
            JCheckBox checkBox = videoSelected.get(device);
            if (checkBox.isSelected()) {
                deviceSelected = true;
            }
        }
        for (AudioDevice device : audioDevices) {
            JCheckBox checkBox = audioSelected.get(device);
            if (checkBox.isSelected()) {
                deviceSelected = true;
            }
        }
        if (!deviceSelected) {
            JOptionPane.showMessageDialog(this,
                    "No devices have been selected to record!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Disables the preview of any devices currently started
     */
    public void disablePreviewForRunningDevices() {
        for (VideoDevice device : videoDevices) {
            if (device.isStarted()) {
                JButton preview = videoPreview.get(device);
                preview.setEnabled(false);
            }
        }
    }

    /**
     *
     * @see java.awt.event.ActionListener#actionPerformed(
     *     java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Preview")) {
            stopPreview();
            for (VideoDevice device : videoDevices) {
                if (e.getSource() == videoPreview.get(device)) {
                    try {
                        JComboBox inputBox = videoInput.get(device);
                        DataSource dataSource = device.getDataSource(
                                inputBox.getSelectedIndex());
                        dataSource.connect();
                        PushBufferStream[] datastreams =
                            ((PushBufferDataSource) dataSource).getStreams();
                        previewRenderer = new RGBRenderer(new Effect[]{});
                        previewRenderer.setDataSource(dataSource, 0);
                        previewRenderer.setInputFormat(
                                datastreams[0].getFormat());
                        previewDataSource = dataSource;
                        previewDevice = device;
                        Component c = previewRenderer.getComponent();
                        previewPanel.add(c);
                        c.setSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
                        c.setVisible(true);
                        previewRenderer.start();
                    } catch (Exception error) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                            "Error displaying preview: " + error.getMessage(),
                            "Preview Error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                }
            }
        } else if (e.getSource().equals(redetectDevices)) {
            stopPreview();
            detectDevices();
        }
    }

    private void changeCaptureFormats(VideoDevice device, JComboBox inputBox) {
        JComboBox captureFormat = videoCaptureFormat.get(device);
        captureFormat.removeAllItems();
        CaptureFormat[][] formats = videoCaptureFormats.get(device);
        int index = inputBox.getSelectedIndex();
        for (CaptureFormat f : formats[index]) {
            captureFormat.addItem(f);
        }
        if (captureFormat.getItemCount() > 1) {
            captureFormat.setVisible(true);
        } else {
            captureFormat.setVisible(false);
        }
    }

    /**
     *
     * @see java.awt.event.ItemListener#itemStateChanged(
     *     java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            for (AudioDevice device : audioDevices) {
                JComboBox inputBox = audioInputSelected.get(device);
                if (e.getSource() == inputBox) {
                    String line = (String) inputBox.getSelectedItem();
                    device.selectLine(line);
                    return;
                }
            }
            for (VideoDevice device : videoDevices) {
                JComboBox inputBox = videoInput.get(device);
                if (e.getSource() == inputBox) {
                    changeCaptureFormats(device, inputBox);
                }
            }
        }
    }

    public void addLocalAudio(String name, DataSource dataSource,
            FloatControl volumeControl, long ssrc) throws NoPlayerException,
            CannotRealizeException, IOException {
        for (LocalStreamListener listener : listeners) {
            listener.addLocalAudio(name, dataSource, volumeControl, ssrc);
        }
    }

    public void addLocalVideo(String name, DataSource dataSource, long ssrc)
            throws IOException {
        for (LocalStreamListener listener : listeners) {
            listener.addLocalVideo(name, dataSource, ssrc);
        }
    }

    public void removeLocalAudio(DataSource dataSource) {
        for (LocalStreamListener listener : listeners) {
            listener.removeLocalAudio(dataSource);
        }
    }

    public void removeLocalVideo(DataSource dataSource) {
        for (LocalStreamListener listener : listeners) {
            listener.removeLocalVideo(dataSource);
        }
    }

    public void forceKeyFrame() {
        for (VideoDevice device : videoDevices) {
            JCheckBox checkBox = videoSelected.get(device);
            if (checkBox.isSelected()) {
                device.doKeyFrame();
            }
        }
    }
}
