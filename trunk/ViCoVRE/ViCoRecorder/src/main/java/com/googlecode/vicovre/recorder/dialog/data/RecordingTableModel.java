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

package com.googlecode.vicovre.recorder.dialog.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.RecordingMetadata;

/**
 * A model for displaying recordings in a table
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordingTableModel extends AbstractTableModel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final String UPLOADED_FILE = ".uploaded";

    /**
     * The name of the start column
     */
    public static final String START_COLUMN = "Start";

    /**
     * The name of the end column
     */
    public static final String DURATION_COLUMN = "Duration";

    /**
     * The name of the event column
     */
    public static final String NAME_COLUMN = "Name";

    /**
     * The name of the uploaded column
     */
    public static final String UPLOADED_COLUMN = "Uploaded";

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("dd MMM yyyy 'at' HH:mm:ss");

    private static final NumberFormat TIME_FORMAT =
        new DecimalFormat("00");

    private static final String[] COLUMNS = new String[]{
        START_COLUMN, DURATION_COLUMN, NAME_COLUMN, UPLOADED_COLUMN};

    private Vector<Recording> recordings = new Vector<Recording>();

    private Date recordingStart = null;

    private RecordingMetadata currentRecordingMetadata = null;

    /**
     *
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
        return COLUMNS.length;
    }

    /**
     *
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
        return recordings.size() + 1;
    }

    /**
     *
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    /**
     *
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    public Class< ? > getColumnClass(int columnIndex) {
        if (getColumnName(columnIndex).equals(UPLOADED_COLUMN)) {
            return Boolean.class;
        }
        return String.class;
    }

    /**
     *
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    private static String getTimeText(long duration) {
        long remainder = duration / 1000;
        long hours = remainder / 3600;
        remainder -= hours * 3600;
        long minutes = remainder / 60;
        remainder -= minutes * 60;
        long seconds = remainder;

        return TIME_FORMAT.format(hours) + ":"
                + TIME_FORMAT.format(minutes) + ":"
                + TIME_FORMAT.format(seconds);
    }

    /**
     *
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        String column = getColumnName(columnIndex);
        if (rowIndex >= recordings.size()) {
            if (column.equals(START_COLUMN)) {
                if (recordingStart != null) {
                    return DATE_FORMAT.format(recordingStart);
                }
                return "Next Recording";
            } else if (column.equals(DURATION_COLUMN)) {
                if (recordingStart != null) {
                    return "Recording...";
                }
                return "";
            } else if (column.equals(NAME_COLUMN)) {
                if (currentRecordingMetadata != null) {
                    return currentRecordingMetadata.getPrimaryValue();
                }
                return "Double click to enter metadata";
            } else if (column.equals(UPLOADED_COLUMN)) {
                return false;
            }
            return null;
        }

        Recording recording = recordings.get(rowIndex);
        if (column.equals(START_COLUMN)) {
            return DATE_FORMAT.format(recording.getStartTime());
        } else if (column.equals(DURATION_COLUMN)) {
            return getTimeText(recording.getDuration());
        } else if (column.equals(NAME_COLUMN)) {
            RecordingMetadata metadata = recording.getMetadata();
            if (metadata != null) {
                return metadata.getPrimaryValue();
            }
            return "Double click to enter metadata";
        } else if (column.equals(UPLOADED_COLUMN)) {
            File uploadedFile = new File(recording.getDirectory(),
                    UPLOADED_FILE);
            return uploadedFile.exists();
        }
        return null;
    }

    /**
     * Adds a recording to the table
     * @param recording The recording to add
     * @param event The event recorded, or null if unknown
     */
    public void addRecording(Recording recording) {
        int index = Collections.binarySearch(recordings, recording);
        if (index < 0) {
            index = -index - 1;
        }

        recordings.insertElementAt(recording, index);
        fireTableRowsInserted(index, index);
    }

    /**
     * Deletes one or more recordings including the files on disk
     * @param rec The recordings to delete
     */
    public void deleteRecordings(Recording... rec) {
        for (Recording recording : rec) {
            int index = recordings.indexOf(recording);
            if (index != -1) {
                recordings.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
    }

    /**
     * Sets the metadata of the current recording
     * @param metadata The metadata to set
     */
    public void setCurrentRecordingMetadata(RecordingMetadata metadata) {
        this.currentRecordingMetadata = metadata;
        fireTableRowsUpdated(recordings.size(), recordings.size());
    }

    public void indicateMetadataUpdate(Recording recording) {
        int index = recordings.indexOf(recording);
        if (index != -1) {
            fireTableRowsUpdated(index, index);
        }
    }

    /**
     * Sets a recording to being uploaded
     * @param recording The recording uploaded
     * @param url The URL uploaded to
     * @throws IOException
     */
    public void setRecordingUploaded(Recording recording, String url)
            throws IOException {
        int index = recordings.indexOf(recording);
        if (index != -1) {
            recording = recordings.get(index);
            File file = new File(recording.getDirectory(), UPLOADED_FILE);
            FileOutputStream output = new FileOutputStream(file, true);
            PrintWriter writer = new PrintWriter(output);
            writer.println(url);
            output.close();
        }
    }

    /**
     * Gets a recording
     * @param index The index of the recording
     * @return The recording
     */
    public Recording getRecording(int index) {
        if (index < recordings.size()) {
            return recordings.get(index);
        }
        return null;
    }

    /**
     * Gets a set of recordings
     * @param rows The rows to get the recordings of
     * @return The recordings
     */
    public Recording[] getRecordings(int[] rows) {
        Vector<Recording> recs = new Vector<Recording>();
        for (int row : rows) {
            if (row < recordings.size()) {
                recs.add(recordings.get(row));
            } else {
                recs.add(null);
            }
        }
        return recs.toArray(new Recording[0]);
    }

    /**
     * Indicates that a recording has started at a specific date
     * @param date The date of the start of the recording
     */
    public void startRecording(Date date) {
        recordingStart = date;
    }

    /**
     * Indicates that the current recording has stopped
     * @param recording The recording that has stopped and will be added
     */
    public void finishRecording(Recording recording) {
        recording.setMetadata(currentRecordingMetadata);
        addRecording(recording);
        recordingStart = null;
        currentRecordingMetadata = null;
    }

    /**
     * Gets the metadata for the current recording
     * @return The metadata
     */
    public RecordingMetadata getCurrentRecordingMetadata() {
        return currentRecordingMetadata;
    }
}
