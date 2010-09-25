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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.media.ui.OtherThread;
import com.googlecode.vicovre.media.ui.ProgressDialog;
import com.googlecode.vicovre.recorder.dialog.data.RecordingTableModel;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.InsecureRecording;
import com.googlecode.vicovre.utils.Config;

/**
 * A dialog for uploading recordings
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class UploadDialog extends JDialog implements ActionListener,
        ItemListener{

    private static final long ZIP_HEADER_LENGTH = 22;

    private static final long ZIP_FILE_HEADER_LENGTH = 30;

    private static final long ZIP_CENTRAL_HEADER_LENGTH = 46;

    private static final long serialVersionUID = 1L;

    private static final String SERVER_CONFIG_PARAM = "UploadServer";

    private static final int DIALOG_WIDTH = 500;

    private static final int DIALOG_HEIGHT = 415;

    private static final String LOGIN_URL =
        "rest/auth/form?username=<username>&password=<password>";

    private static final String LOGOUT_URL =
        "rest/auth/logout";

    private static final String RECORDING_URL = "rest/recording/<folder>";

    private static final String RECORDING_UPLOAD_URL =
        RECORDING_URL + "?startDate=<startDate>";

    private static final String METADATA_UPLOAD_URL =
        "rest/recording/<folder>/<id>";

    private static final String LAYOUT_UPLOAD_URL =
        "rest/recording/<folder>/<id>/layout/<time>";

    private static final int AFTER_LOGIN_PROGRESS = 1;

    private static final int AFTER_RECORDING_PROGRESS = 90;

    private static final int AFTER_METADATA_PROGRESS = 95;

    private static final int AFTER_LAYOUT_PROGRESS = 99;

    private static final int AFTER_LOGOUT_PROGRESS = 100;

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
            Config configuration) {
        super(parent, "Upload Recordings", true);
        this.database = database;
        this.configuration = configuration;
        servers = new Vector<String>();
        for (String server : configuration.getParameters(SERVER_CONFIG_PARAM)) {
            servers.add(server);
        }

        List<Recording> recordings = database.getRecordings("");
        for (Recording recording : recordings) {
            addRecording((InsecureRecording) recording);
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
    public void addRecording(InsecureRecording recording) {
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
    public void finishRecording(InsecureRecording recording) {
        recordingModel.finishRecording(recording);
    }

    private void enterMetadataForRecording(int index) {
        RecordingMetadata metadata = null;
        InsecureRecording recording = null;
        if (index == (recordingModel.getRowCount() - 1)) {
            metadata = recordingModel.getCurrentRecordingMetadata();
        } else {
            recording = recordingModel.getRecording(index);
            metadata = recording.getMetadata();
        }
        MetadataDialog dialog = new MetadataDialog(this, metadata);
        dialog.setVisible(true);
        if (!dialog.wasCancelled()) {
            metadata = dialog.getMetadata();
            if (recording == null) {
                recordingModel.setCurrentRecordingMetadata(metadata);
            } else {
                recording.setMetadata(metadata);
                recordingModel.indicateMetadataUpdate(recording);
                try {
                    database.updateRecordingMetadata(recording);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Error updating database: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
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
                InsecureRecording[] recordings =
                    recordingModel.getRecordings(indices);
                for (InsecureRecording recording : recordings) {
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
                    for (InsecureRecording recording : recordings) {
                        if (recording != null) {
                            try {
                                database.deleteRecording(recording);
                            } catch (IOException error) {
                                error.printStackTrace();
                            }
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
                InsecureRecording[] recordings =
                    recordingModel.getRecordings(indices);
                boolean missingMetadata = false;
                for (InsecureRecording recording : recordings) {
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
                    for (final InsecureRecording recording : recordings) {
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

    private String login(String urlString) throws IOException {
        progress.setMessage("Logging in");
        String loginUrl = LOGIN_URL;
        loginUrl = loginUrl.replace("<username>",
                URLEncoder.encode(username.getText(), "UTF-8"));
        loginUrl = loginUrl.replace("<password>",
                URLEncoder.encode(new String(password.getPassword()), "UTF-8"));
        URL login = new URL(urlString + loginUrl);
        HttpURLConnection authConnection =
            (HttpURLConnection) login.openConnection();
        authConnection.setRequestMethod("POST");
        if (authConnection.getResponseCode() != 200) {
            throw new IOException(
                    "Your login credentials have not been recognized: "
                    + authConnection.getResponseCode() + ": "
                    + authConnection.getResponseMessage());
        }
        Map<String, List<String>> headers = authConnection.getHeaderFields();
        String sessionId = null;
        for (String header : headers.keySet()) {
            if ((header != null) && header.equals("Set-Cookie")) {
                for (String setCookie : headers.get(header)) {
                    int index = setCookie.indexOf(';');
                    if (index == -1) {
                        index = setCookie.length();
                    }
                    String cookie = setCookie.substring(0, index);
                    if (cookie.startsWith("JSESSIONID=")) {
                        sessionId = cookie.substring("JSESSIONID=".length());
                    }
                }
            }
        }
        if (sessionId == null) {
            throw new IOException("Missing JSESSIONID cookie in response!");
        }
        progress.setProgress(AFTER_LOGIN_PROGRESS);
        return sessionId;
    }

    private String uploadRecording(InsecureRecording recording, String folder,
            String urlString, String sessionId) throws IOException {
        progress.setMessage("Uploading recording");
        long totalBytes = ZIP_HEADER_LENGTH;
        Vector<File> files = new Vector<File>();
        Vector<Long> crcs = new Vector<Long>();
        String[] exts = new String[]{"", RecordingConstants.STREAM_INDEX,
                RecordingConstants.STREAM_METADATA};
        for (Stream stream : recording.getStreams()) {
            for (String ext : exts) {
                File file = new File(recording.getDirectory(),
                        stream.getSsrc() + ext);
                if (file.exists()) {
                    String name = file.getName();
                    files.add(file);
                    totalBytes += file.length();
                    totalBytes += ZIP_FILE_HEADER_LENGTH + name.length();
                    totalBytes += ZIP_CENTRAL_HEADER_LENGTH + name.length();

                    CRC32 crc = new CRC32();
                    FileInputStream input = new FileInputStream(file);
                    byte[] buffer = new byte[8096];
                    int bytesRead = input.read(buffer);
                    while (bytesRead != -1) {
                        crc.update(buffer, 0, bytesRead);
                        bytesRead = input.read(buffer);
                    }
                    input.close();
                    crcs.add(crc.getValue());
                }
            }
        }

        String url = RECORDING_UPLOAD_URL;
        url = url.replace("<folder>", folder);
        url = url.replace("<startDate>", recording.getStartTimeString());

        URL upload = new URL(urlString + url);
        HttpURLConnection connection =
            (HttpURLConnection) upload.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/zip");
        connection.setRequestProperty("Content-Length",
                String.valueOf(totalBytes));
        connection.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);

        ZipOutputStream output = new ZipOutputStream(
                connection.getOutputStream());
        output.setLevel(ZipOutputStream.STORED);
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            ZipEntry entry = new ZipEntry(file.getName());
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(file.length());
            entry.setCompressedSize(file.length());
            entry.setCrc(crcs.get(i));
            entry.setTime(file.lastModified());
            entry.setComment("");
            entry.setExtra(new byte[0]);
            output.putNextEntry(entry);

            FileInputStream input = new FileInputStream(file);
            byte[] buffer = new byte[8096];
            int bytesRead = input.read(buffer);
            int totalBytesRead = 0;
            while (bytesRead != -1) {
                totalBytesRead += bytesRead;
                output.write(buffer, 0, bytesRead);
                updateProgress(totalBytes, totalBytesRead);
                bytesRead = input.read(buffer);
            }
            input.close();
            output.closeEntry();
        }
        output.close();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Error uploading recording: "
                    + connection.getResponseCode() + ": "
                    + connection.getResponseMessage());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
        String id = reader.readLine();
        String recordingUrl = urlString + RECORDING_URL;
        recordingUrl = recordingUrl.replace("<folder>", folder);
        progress.setProgress(AFTER_RECORDING_PROGRESS);
        return id.substring(recordingUrl.length());
    }

    private void uploadMetadata(InsecureRecording recording, String folder,
            String urlString, String sessionId) throws IOException {
        String url = METADATA_UPLOAD_URL;
        url = url.replace("<folder>", folder);
        url = url.replace("<id>", recording.getId());

        RecordingMetadata metadata = recording.getMetadata();
        String content = "";
        if (metadata != null) {
            content += "metadataPrimaryKey="
                + URLEncoder.encode(metadata.getPrimaryKey(), "UTF-8");
            for (String key : metadata.getKeys()) {
                String meta = "&" + URLEncoder.encode("metadata" + key,
                        "UTF-8");
                content += meta + "="
                    + URLEncoder.encode(metadata.getValue(key), "UTF-8");
                content += meta + "Visible=" + metadata.isVisible(key);
                content += meta + "Editable=" + metadata.isEditable(key);
                content += meta + "Multiline=" + metadata.isMultiline(key);
            }

            URL upload = new URL(urlString + url);
            HttpURLConnection connection =
                (HttpURLConnection) upload.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length",
                    String.valueOf(content.length()));
            connection.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);
            DataOutputStream output = new DataOutputStream(
                    connection.getOutputStream());
            output.writeBytes(content);
            output.close();

            if (connection.getResponseCode() != 200) {
                throw new IOException("Error setting recording metadata: "
                        + connection.getResponseCode() + ": "
                        + connection.getResponseMessage());
            }
        }
        progress.setProgress(AFTER_METADATA_PROGRESS);
    }

    private void uploadLayouts(InsecureRecording recording, String folder,
            String urlString, String sessionId) throws IOException {

        for (ReplayLayout layout : recording.getReplayLayouts()) {
            String url = LAYOUT_UPLOAD_URL;
            url = url.replace("<folder>", folder);
            url = url.replace("<id>", recording.getId());
            url = url.replace("<time>", String.valueOf(layout.getTime()));

            URL upload = new URL(urlString + url);
            HttpURLConnection connection =
                (HttpURLConnection) upload.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);
            if (connection.getResponseCode() != 200) {
                throw new IOException("Error setting layout: "
                        + connection.getResponseCode() + ": "
                        + connection.getResponseMessage());
            }
        }
        progress.setProgress(AFTER_LAYOUT_PROGRESS);
    }

    private void logout(String urlString, String sessionId) throws IOException {
        progress.setMessage("Logging out");
        String logoutUrl = LOGOUT_URL;
        URL logout = new URL(urlString + logoutUrl);
        HttpURLConnection authConnection =
            (HttpURLConnection) logout.openConnection();
        authConnection.setRequestProperty("Cookie",
                "JSESSIONID=" + sessionId);
        if (authConnection.getResponseCode() != 200) {
            throw new IOException("Could not logout");
        }
        progress.setProgress(AFTER_LOGOUT_PROGRESS);
    }

    private void uploadRecording(InsecureRecording recording, String urlString)
            throws IOException {
        String folder = recording.getFolder();
        String sessionId = login(urlString);
        String recordingId = uploadRecording(recording, folder, urlString,
                sessionId);
        recording.setId(recordingId);
        database.addRecording(recording, null);
        uploadMetadata(recording, folder, urlString, sessionId);
        uploadLayouts(recording, folder, urlString, sessionId);
        logout(urlString, sessionId);
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
                    configuration.addParameterValue(SERVER_CONFIG_PARAM, url);
                }
            } catch (MalformedURLException error) {
                currentUrlInvalid = true;
                JOptionPane.showMessageDialog(this, "The URL is not valid",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void updateProgress(long totalBytes, long currentBytes) {
        float fraction = (float) currentBytes / (float) totalBytes;
        int extra = (int) ((AFTER_RECORDING_PROGRESS - AFTER_LOGIN_PROGRESS)
            * fraction);
        progress.setProgress(AFTER_LOGIN_PROGRESS + extra);
    }
}
