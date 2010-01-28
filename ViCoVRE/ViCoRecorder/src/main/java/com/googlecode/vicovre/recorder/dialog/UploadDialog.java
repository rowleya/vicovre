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

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import com.googlecode.vicovre.media.ui.OtherThread;
import com.googlecode.vicovre.media.ui.ProgressDialog;
import com.googlecode.vicovre.recorder.dialog.data.RecordingTableModel;
import com.googlecode.vicovre.recorder.dialog.data.StreamProgress;
import com.googlecode.vicovre.recorder.dialog.data.StreamsPart;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.utils.Config;

/**
 * A dialog for uploading recordings
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class UploadDialog extends JDialog implements ActionListener,
        ItemListener, StreamProgress {

    private static final long serialVersionUID = 1L;

    private static final String SERVER_CONFIG_PARAM = "UploadServer";

    private static final int DIALOG_WIDTH = 500;

    private static final int DIALOG_HEIGHT = 415;

    private static final String RECORDING_UPLOAD_URL = "recordingUpload.do?"
        + "folder=<folder>";

    private static final String STREAM_UPLOAD_URL = "streamUpload.do";

    private static final String ANNOTATION_UPLOAD_URL = "annotationUpload.do";

    private static final String LAYOUT_UPLOAD_URL = "replayLayout.do?"
        + "streamId=<streamId>&timestamp=<timestamp>&layoutName=<layoutName>"
        + "&positionName=<positionName>";

    private static final int AFTER_RECORDING_PROGRESS = 10;

    private static final int AFTER_STREAM_PROGRESS = 80;

    private static final int AFTER_LAYOUT_PROGRESS = 85;

    private static final int AFTER_ANNOTATIONS_PROGRESS = 100;

    private JComboBox server = new JComboBox();

    private List<String> servers = null;

    private RecordingTableModel recordingModel = new RecordingTableModel();

    private JTable recordingTable = new JTable(recordingModel);

    private JButton setRecordingEventButton =
        new JButton("<html><center>Edit Recording Metadata</center></html>");

    private JButton uploadButton =
        new JButton("<html><center>Upload Selected Recordings</center></html>");

    private JButton deleteButton =
        new JButton("<html><center>Delete Selected Recordings</center></html>");

    private JButton okButton = new JButton("Close");

    private boolean currentUrlInvalid = true;

    private ProgressDialog progress = null;

    private File annotationDirectory = null;

    private JTextField username = new JTextField();

    private JPasswordField password = new JPasswordField();

    private RecordingDatabase database = null;

    private Config configuration = null;

    /**
     *
     * Creates a new UploadDialog
     * @param parent The parent frame
     * @param recordingDao The recording DAO
     * @param mainEventDao The main event DAO
     * @param eventDao The event DAO
     * @param serverDao The server DAO
     * @param annotationDirectory The directory where the annotations are stored
     */
    public UploadDialog(JFrame parent, RecordingDatabase database,
            File annotationDirectory, Config configuration) {
        super(parent, "Upload Recordings", true);
        this.database = database;
        this.annotationDirectory = annotationDirectory;
        this.configuration = configuration;
        servers = new Vector<String>();
        for (String server : configuration.getParameters(SERVER_CONFIG_PARAM)) {
            servers.add(server);
        }

        List<Recording> recordings =
            database.getTopLevelFolder().getRecordings();
        for (Recording recording : recordings) {
            addRecording(recording);
        }

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
        setResizable(false);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(content);

        for (String url : servers) {
            server.addItem(url);
        }
        server.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        server.setAlignmentX(LEFT_ALIGNMENT);
        server.setEditable(true);
        server.addItemListener(this);
        if (!servers.isEmpty()) {
            server.setSelectedIndex(0);
            currentUrlInvalid = false;
        }
        JLabel serverLabel = new JLabel("Select server to upload to:");
        content.add(serverLabel);
        content.add(server);

        JPanel usernamePanel = new JPanel();
        usernamePanel.setAlignmentX(LEFT_ALIGNMENT);
        usernamePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        usernamePanel.setLayout(new BoxLayout(usernamePanel, BoxLayout.X_AXIS));
        usernamePanel.add(new JLabel("Username:"));
        usernamePanel.add(Box.createHorizontalStrut(5));
        usernamePanel.add(username);
        content.add(usernamePanel);

        JPanel passwordPanel = new JPanel();
        passwordPanel.setAlignmentX(LEFT_ALIGNMENT);
        passwordPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));
        passwordPanel.add(new JLabel("Password:"));
        passwordPanel.add(Box.createHorizontalStrut(5));
        passwordPanel.add(password);
        content.add(passwordPanel);

        content.add(Box.createVerticalStrut(5));

        JScrollPane scroller = new JScrollPane(recordingTable);
        Dimension scrollSize = new Dimension(DIALOG_WIDTH - 10, 210);
        scroller.setMinimumSize(scrollSize);
        scroller.setMaximumSize(scrollSize);
        scroller.setPreferredSize(scrollSize);
        scroller.setAlignmentX(LEFT_ALIGNMENT);
        JLabel recordingsLabel =
            new JLabel("Select the recordings to upload or delete:");
        recordingsLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(recordingsLabel);
        content.add(scroller);
        content.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new TableLayout(
                new double[]{TableLayout.FILL, 10, TableLayout.FILL, 10,
                        TableLayout.FILL, 10, TableLayout.FILL},
                new double[]{55}));
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        deleteButton.addActionListener(this);
        okButton.addActionListener(this);
        setRecordingEventButton.addActionListener(this);
        uploadButton.addActionListener(this);
        buttonPanel.add(setRecordingEventButton, "0, 0");
        buttonPanel.add(uploadButton, "2, 0");
        buttonPanel.add(deleteButton, "4, 0");
        buttonPanel.add(okButton, "6, 0");
        content.add(buttonPanel);

        recordingTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Point p = e.getPoint();
                    int row = recordingTable.rowAtPoint(p);
                    enterMetadataForRecording(row);
                }
            }
        });
    }

    /**
     * Adds a recording to the dialog
     * @param recording The recording to add
     */
    public void addRecording(Recording recording) {
        recordingModel.addRecording(recording);
    }

    /**
     * Indicates that recording has started
     */
    public void startRecording() {
        recordingModel.startRecording(new Date());
    }

    /**
     * Indicates that recording has stopped
     * @param recording The recording that has stopped and is to be added
     */
    public void finishRecording(Recording recording) {
        recordingModel.finishRecording(recording);
    }

    private void enterMetadataForRecording(int index) {
        RecordingMetadata metadata = null;
        Recording recording = null;
        if (index == (recordingModel.getRowCount() - 1)) {
            metadata = recordingModel.getCurrentRecordingMetadata();
        } else {
            recording = recordingModel.getRecording(index);
            metadata = recording.getMetadata();
        }
        MetadataDialog dialog = new MetadataDialog(this, metadata);
        dialog.setVisible(true);
        if (!dialog.wasCancelled()) {
            if (metadata == null) {
                metadata = new RecordingMetadata();
            }
            metadata.setName(dialog.getName());
            metadata.setDescription(dialog.getDescription());
            if (recording == null) {
                recordingModel.setCurrentRecordingMetadata(metadata);
            } else {
                recordingModel.indicateMetadataUpdate(recording);
            }
        }
    }

    /**
     *
     * @see java.awt.event.ActionListener#actionPerformed(
     *     java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            setVisible(false);
        } else if (e.getSource() == setRecordingEventButton) {
            int count = recordingTable.getSelectedRowCount();
            if (count > 1) {
                JOptionPane.showMessageDialog(this,
                    "Please ensure only one recording is selected for this"
                        + " operation",
                    "Error", JOptionPane.ERROR_MESSAGE);
            } else if (count < 1) {
                JOptionPane.showMessageDialog(this,
                        "Please select a recording",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                int index = recordingTable.getSelectedRow();
                enterMetadataForRecording(index);
            }
        } else if (e.getSource() == deleteButton) {
            int[] indices = recordingTable.getSelectedRows();
            if (indices.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "Please select one or more recordings to delete",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                Recording[] recordings = recordingModel.getRecordings(indices);
                for (Recording recording : recordings) {
                    if (recording == null) {
                        if (recordings.length == 1) {
                            JOptionPane.showMessageDialog(this,
                                "The current recording cannot be deleted "
                                    + "until it is finished!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                if (JOptionPane.showConfirmDialog(this,
                    "Deleted recordings cannot be recovered.\n"
                        + "Are you sure that you want to delete the selected"
                        + " recordings?",
                    "Error", JOptionPane.YES_NO_OPTION)
                            == JOptionPane.YES_OPTION) {
                    recordingModel.deleteRecordings(recordings);
                    for (Recording recording : recordings) {
                        if (recording != null) {
                            database.deleteRecording(recording);
                        }
                    }
                }
            }
        } else if (e.getSource() == uploadButton) {
            int[] indices = recordingTable.getSelectedRows();
            if (indices.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "Please select one or more recordings to upload",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                Recording[] recordings = recordingModel.getRecordings(indices);
                boolean missingMetadata = false;
                for (Recording recording : recordings) {
                    if (recording == null) {
                        if (recordings.length == 1) {
                            JOptionPane.showMessageDialog(this,
                                "The current recording cannot be uploaded "
                                    + "until it is finished!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else if (recording.getMetadata() == null) {
                        missingMetadata = true;
                    }
                }
                if (missingMetadata) {
                    JOptionPane.showMessageDialog(this,
                        "You must enter metadata for each recording you wish"
                            + "to upload.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    if (currentUrlInvalid) {
                        throw new MalformedURLException();
                    }
                    String urlString = (String) server.getSelectedItem();
                    if ((urlString == null) || urlString.equals("")) {
                        throw new MalformedURLException();
                    }
                    if (!urlString.endsWith("/")) {
                        urlString += "/";
                    }
                    final String url = urlString;
                    for (final Recording recording : recordings) {
                        if (recording != null) {
                            progress = new ProgressDialog(this,
                                    "Uploading Event", true, false, false);
                            OtherThread<Throwable> worker =
                                new OtherThread<Throwable>() {
                                    public Throwable doInBackground() {
                                        try {
                                            uploadRecording(recording, url);
                                            progress.setVisible(false);
                                            return null;
                                        } catch (Throwable e) {
                                            progress.setVisible(false);
                                            return e;
                                        }
                                    }
                                };
                            worker.execute();
                            progress.setVisible(true);

                            Throwable error = worker.get();
                            if (error != null) {
                                throw error;
                            }
                        }
                    }
                    JOptionPane.showMessageDialog(this,
                            "Recordings Successfully Uploaded",
                            "Upload Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (MalformedURLException error) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter the URL of a CREW server to"
                                + " upload to",
                            "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Throwable error) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Error uploading recording: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void uploadRecording(Recording recording, String urlString)
            throws IOException {
        progress.setMessage("Uploading Recording");
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username.getText(),
                        password.getPassword());
            }
        });

        progress.setMessage("Uploading Recording");
        String recUrl = RECORDING_UPLOAD_URL;
        recUrl = recUrl.replaceAll("<folder>",
           recording.getDirectory().getParentFile().getAbsolutePath().substring(
               database.getTopLevelFolder().getFile().getAbsolutePath().length()));
        recUrl = recUrl.replaceAll("<id>", recording.getId());
        URL recordingUpload = new URL(urlString + recUrl);
        URLConnection connection = recordingUpload.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
        String recordingUri = reader.readLine();
        progress.setProgress(AFTER_RECORDING_PROGRESS);

        progress.setMessage("Uploading Streams");
        String streamUrl = STREAM_UPLOAD_URL;
        URL streamUpload = new URL(urlString + streamUrl);
        HttpClient client = new HttpClient();
        client.getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username.getText(),
                        new String(password.getPassword())));
        PostMethod httppost = new PostMethod(streamUpload.toString());
        StringPart recUriParam = new StringPart("recordingUri", recordingUri);
        StreamsPart streamsParam = new StreamsPart(recording.getStreams(),
                recording.getDirectory());
        streamsParam.setStreamProgress(this);
        MultipartRequestEntity multipart = new MultipartRequestEntity(
                new Part[]{recUriParam, streamsParam}, httppost.getParams());
        httppost.setRequestEntity(multipart);
        httppost.setContentChunked(true);
        client.executeMethod(httppost);
        if (httppost.getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Error uploading streams: "
                    + httppost.getStatusText());
        }
        BufferedReader streamReader = new BufferedReader(
                new InputStreamReader(httppost.getResponseBodyAsStream()));
        for (int i = 0; i < recording.getStreams().size(); i++) {
            streamReader.readLine();
        }
        httppost.releaseConnection();
        progress.setProgress(AFTER_STREAM_PROGRESS);

        progress.setMessage("Uploading Layouts");
        for (ReplayLayout layout : recording.getReplayLayouts()) {
            for (ReplayLayoutPosition position : layout.getLayoutPositions()) {
                Stream stream = position.getStream();
                String layoutUrl = LAYOUT_UPLOAD_URL;
                layoutUrl = layoutUrl.replaceAll("<streamId>",
                        stream.getSsrc());
                layoutUrl = layoutUrl.replaceAll("<timestamp>",
                        String.valueOf(layout.getTime()));
                layoutUrl = layoutUrl.replaceAll("<layoutName>",
                        layout.getName());
                layoutUrl = layoutUrl.replaceAll("<positionName>",
                        position.getName());
                URL layoutUpload = new URL(urlString + layoutUrl);
                HttpURLConnection conn = (HttpURLConnection)
                    layoutUpload.openConnection();
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Error setting layout: "
                            + conn.getResponseMessage());
                }
            }
        }
        progress.setProgress(AFTER_LAYOUT_PROGRESS);

        File annotationFile = new File(annotationDirectory,
                recording.getId() + ".xml");
        if (annotationFile.exists()) {
            progress.setMessage("Uploading Annotations");
            String annotationUrl = ANNOTATION_UPLOAD_URL;
            URL annotationUpload = new URL(urlString + annotationUrl);
            client = new HttpClient();
            client.getState().setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username.getText(),
                            new String(password.getPassword())));
            httppost = new PostMethod(annotationUpload.toString());
            FilePart annotationsParam = new FilePart("annotations",
                    annotationFile);
            multipart = new MultipartRequestEntity(
                    new Part[]{recUriParam, annotationsParam},
                    httppost.getParams());
            httppost.setRequestEntity(multipart);
            httppost.setContentChunked(true);
            client.executeMethod(httppost);
            if (httppost.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Error uploading streams: "
                        + httppost.getStatusText());
            }
        }
        progress.setProgress(AFTER_ANNOTATIONS_PROGRESS);
    }

    /**
     *
     * @see java.awt.event.ItemListener#itemStateChanged(
     *     java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            String url = (String) server.getSelectedItem();
            try {
                URL u = new URL(url);
                if ((u.getHost() == null) || u.getHost().equals("")) {
                    throw new MalformedURLException();
                }
                currentUrlInvalid = false;
                if (!servers.contains(url)) {
                    servers.add(url);
                    configuration.addParameterValue(STREAM_UPLOAD_URL, url);
                }
            } catch (MalformedURLException error) {
                currentUrlInvalid = true;
                JOptionPane.showMessageDialog(this, "The URL is not valid",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     *
     * @see net.crew_vre.recorder.dialog.data.StreamProgress#updateProgress(
     *     long, long)
     */
    public void updateProgress(long totalBytes, long currentBytes) {
        float fraction = (float) currentBytes / (float) totalBytes;
        int extra = (int) ((AFTER_STREAM_PROGRESS - AFTER_RECORDING_PROGRESS)
            * fraction);
        progress.setProgress(AFTER_RECORDING_PROGRESS + extra);
    }
}
