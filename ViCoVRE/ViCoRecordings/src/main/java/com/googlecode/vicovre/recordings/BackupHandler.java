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

package com.googlecode.vicovre.recordings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.LayoutReader;
import com.googlecode.vicovre.recordings.db.insecure.RecordingListener;
import com.googlecode.vicovre.recordings.db.insecure.MetadataReader;
import com.googlecode.vicovre.utils.ExtensionFilter;

public class BackupHandler extends Thread implements RecordingListener {

    private static final long RETRY_INTERVAL = 5 * 60 * 1000;

    private static final int ADD = 0;

    private static final int DELETE = 1;

    private static final int UPDATE_LAYOUT = 2;

    private static final int UPDATE_LIFETIME = 3;

    private static final int UPDATE_METADATA = 4;

    private static final int MOVE = 5;

    private File backupDirectory = null;

    private LinkedList<Recording> recordingsToBeBackedUp =
        new LinkedList<Recording>();

    private HashSet<Recording> performingOperation = new HashSet<Recording>();

    private boolean done = false;

    private byte[] buffer = new byte[8096];

    private class BackupRetryTask extends TimerTask {

        private Recording recording = null;

        private Recording secondRecording = null;

        private int operation = 0;

        private BackupRetryTask(Recording recording, int operation) {
            this.recording = recording;
            this.operation = operation;
        }

        private BackupRetryTask(Recording recording, Recording secondRecording,
                int operation) {
            this.recording = recording;
            this.secondRecording = secondRecording;
            this.operation = operation;
        }

        public void run() {
            switch (operation) {
            case ADD:
                recordingAdded(recording);
                break;
            case DELETE:
                recordingDeleted(recording);
                break;
            case UPDATE_METADATA:
                recordingMetadataUpdated(recording);
                break;
            case UPDATE_LIFETIME:
                recordingLifetimeUpdated(recording);
                break;
            case UPDATE_LAYOUT:
                recordingLayoutsUpdated(recording);
                break;
            case MOVE:
                recordingMoved(recording, secondRecording);
                break;
            }
        }
    }

    public BackupHandler(String backupDir, RecordingDatabase database,
            boolean enabled) {
        if (enabled) {
            backupDirectory = new File(backupDir);
            backupDirectory.mkdirs();
            start();
            database.addRecordingListener(this);
        }
    }

    public void addRecording(Recording recording) {
        synchronized (recordingsToBeBackedUp) {
            recordingsToBeBackedUp.addLast(recording);
            recordingsToBeBackedUp.notifyAll();
        }
    }

    private File getRecordingBackupDirectory(Recording recording) {
        File backupDirectory =
            new File(this.backupDirectory, recording.getFolder());
        return new File(backupDirectory, recording.getId());
    }

    private void startOperation(Recording recording) {
        synchronized (performingOperation) {
            while (performingOperation.contains(recording)) {
                try {
                    performingOperation.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            performingOperation.add(recording);
        }
    }

    private void finishOperation(Recording recording) {
        synchronized (performingOperation) {
            performingOperation.remove(recording);
            performingOperation.notifyAll();
        }
    }

    private Recording getNextRecording() {
        synchronized (recordingsToBeBackedUp) {
            while (recordingsToBeBackedUp.isEmpty() && !done) {
                try {
                    recordingsToBeBackedUp.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            if (!recordingsToBeBackedUp.isEmpty()) {
                return recordingsToBeBackedUp.removeFirst();
            }
        }
        return null;
    }

    private void copyDirectory(File source, File destination)
            throws IOException {
        destination.mkdirs();
        File[] sourceFiles = source.listFiles();
        for (File sourceFile : sourceFiles) {
            File destFile = new File(destination, sourceFile.getName());
            if (!sourceFile.isDirectory()) {
                FileInputStream input = new FileInputStream(sourceFile);
                FileOutputStream output = new FileOutputStream(destFile);
                int bytesRead = input.read(buffer);
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead);
                    bytesRead = input.read(buffer);
                }
                input.close();
                output.close();
            } else {
                copyDirectory(sourceFile, destFile);
            }
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                file.delete();
            } else {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    public void run() {
        while (!done) {
            Recording currentRecording = getNextRecording();
            if (currentRecording != null) {
                startOperation(currentRecording);
                try {
                    File recordingDirectory = currentRecording.getDirectory();
                    File backupDirectory = getRecordingBackupDirectory(
                            currentRecording);
                    if (!backupDirectory.exists()) {
                        System.err.println("Backing up recording "
                            + recordingDirectory + " to " + backupDirectory);
                        backupDirectory.mkdirs();
                        File touchFile = new File(backupDirectory,
                                RecordingConstants.RECORDING_INPROGRESS);
                        touchFile.createNewFile();
                        copyDirectory(recordingDirectory, backupDirectory);
                        touchFile.delete();
                    }
                } catch (IOException e) {
                    System.err.println("Warning - error backing up recording "
                            + currentRecording.getDirectory()
                            + ": " + e.getMessage());
                    Timer retryTimer = new Timer();
                    retryTimer.schedule(new BackupRetryTask(currentRecording,
                            null, ADD), RETRY_INTERVAL);
                }

                finishOperation(currentRecording);
            }
        }
    }

    public void shutdown() {
        synchronized (recordingsToBeBackedUp) {
            done = true;
            recordingsToBeBackedUp.notifyAll();
        }

        synchronized (performingOperation) {
            while (!performingOperation.isEmpty()) {
                try {
                    performingOperation.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }
    }

    public void recordingAdded(Recording recording) {
        addRecording(recording);
    }

    public void recordingDeleted(Recording recording) {
        startOperation(recording);
        boolean deleteBackup = false;
        synchronized (recordingsToBeBackedUp) {
            deleteBackup = !recordingsToBeBackedUp.remove(recording);
        }
        if (deleteBackup) {
            File backupDirectory = getRecordingBackupDirectory(recording);
            deleteDirectory(backupDirectory);
        }
        finishOperation(recording);
    }

    public void recordingLayoutsUpdated(Recording recording) {
        synchronized (recordingsToBeBackedUp) {
            if (recordingsToBeBackedUp.contains(recording)) {
                return;
            }
        }

        startOperation(recording);
        File backupDirectory = getRecordingBackupDirectory(recording);
        File[] currentLayouts = backupDirectory.listFiles(
                new ExtensionFilter(RecordingConstants.LAYOUT));
        if (currentLayouts != null) {
            for (File layout : currentLayouts) {
               layout.delete();
            }
        }
        for (ReplayLayout layout : recording.getReplayLayouts()) {
            try {
                File layoutOutput = new File(backupDirectory,
                        layout.getName() + RecordingConstants.LAYOUT);
                FileOutputStream outputLayout = new FileOutputStream(
                        layoutOutput);
                LayoutReader.writeLayout(layout, outputLayout);
                outputLayout.close();
            } catch (IOException e) {
                System.err.println(
                    "Warning - error updating backup layouts for recording "
                        + backupDirectory + ": " + e.getMessage());
                Timer retryTimer = new Timer();
                retryTimer.schedule(new BackupRetryTask(recording,
                        null, UPDATE_LAYOUT), RETRY_INTERVAL);
            }
        }
        finishOperation(recording);
    }

    public void recordingLifetimeUpdated(Recording recording) {
        // Does Nothing
    }

    public void recordingMetadataUpdated(Recording recording) {
        synchronized (recordingsToBeBackedUp) {
            if (recordingsToBeBackedUp.contains(recording)) {
                return;
            }
        }
        startOperation(recording);
        File backupDirectory = getRecordingBackupDirectory(recording);
        try {
            File metadataFile = new File(backupDirectory,
                    RecordingConstants.METADATA);
            FileOutputStream outputStream = new FileOutputStream(metadataFile);
            MetadataReader.writeMetadata(recording.getMetadata(),
                    outputStream);
            outputStream.close();
        } catch (IOException e) {
            System.err.println(
                "Warning - error updating backup metadata for recording"
                    + backupDirectory + ": " + e.getMessage());
            Timer retryTimer = new Timer();
            retryTimer.schedule(new BackupRetryTask(recording,
                    null, UPDATE_METADATA), RETRY_INTERVAL);
        }
        finishOperation(recording);
    }

    public void recordingMoved(Recording oldRecording, Recording newRecording) {
        startOperation(oldRecording);
        startOperation(newRecording);
        if (!oldRecording.getDirectory().renameTo(
                newRecording.getDirectory())) {
            System.err.println(
                    "Warning - unknown error moving backup recording");
            Timer retryTimer = new Timer();
            retryTimer.schedule(new BackupRetryTask(oldRecording,
                    newRecording, MOVE), RETRY_INTERVAL);
        }
        finishOperation(oldRecording);
        finishOperation(newRecording);
    }

}
