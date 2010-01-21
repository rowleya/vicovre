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
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ag3.interfaces.Venue;
import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.ClientProfile;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.StreamDescription;

import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

/**
 * Represents a recording to be made
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class UnfinishedRecording implements Comparable<UnfinishedRecording> {

    private static final String STOPPED = "Stopped";

    private static final String RECORDING = "Recording";

    private static final String PAUSED = "Paused";

    private static final String COMPLETED = "Completed";

    private static final String ERROR = "Error: ";

    private static final BridgeDescription CONNECTION = new BridgeDescription();

    static {
        CONNECTION.setServerType("Multicast");
    }

    private static int lastId = 0;

    private final int id;

    private RtpTypeRepository typeRepository = null;

    private Folder folder = null;

    private File file = null;

    private RecordArchiveManager manager = null;

    private BridgedRTPConnector connector = null;

    private RecordingMetadata metadata = null;

    private String ag3VenueServer = null;

    private String ag3VenueUrl = null;

    private NetworkLocation[] addresses = null;

    private Date startDate = null;

    private Date stopDate = null;

    private Timer startTimer = null;

    private Timer stopTimer = null;

    private String status = STOPPED;

    private Venue venue = null;

    private String connectionId = null;

    private Integer venueSync = new Integer(0);

    private ClientProfile clientProfile = new ClientProfile();

    private boolean recordingStarted = false;

    private boolean recordingFinished = false;

    private RecordingDatabase database = null;

    private class VenueUpdater extends Thread {
        public void run() {
            synchronized (venueSync) {
                while (connectionId != null) {
                    float lifetime = 10.0f;
                    try {
                        lifetime = venue.updateLifetime(connectionId,
                                lifetime);
                    } catch (Exception e) {
                        // Do Nothing
                    }
                    try {
                        venueSync.wait((long) (lifetime * 1000));
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
            }
        }
    }

    private class StartRecording extends TimerTask {

        public void run() {
            startRecording();
        }
    }

    private class StopRecording extends TimerTask {
        public void run() {
            stopRecording();
        }
    }

    /**
     * Creates a new TimerRecording
     * @param manager The manager used to start and stop the recording
     */
    public UnfinishedRecording(RtpTypeRepository typeRepository,
            Folder folder, RecordingDatabase database) {
        this.database = database;
        this.typeRepository = typeRepository;
        this.folder = folder;
        id = lastId++;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Gets the unique volatile id (changes on each run of service)
     * @return The id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the metadata
     * @return the metadata
     */
    public RecordingMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     * @param metadata the metadata to set
     */
    public void setMetadata(RecordingMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the folder
     * @return The folder
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * Returns the ag3VenueServer
     * @return the ag3VenueServer
     */
    public String getAg3VenueServer() {
        return ag3VenueServer;
    }

    /**
     * Sets the ag3VenueServer
     * @param ag3VenueServer the ag3VenueServer to set
     */
    public void setAg3VenueServer(String ag3VenueServer) {
        this.ag3VenueServer = ag3VenueServer;
    }

    /**
     * Returns the ag3VenueUrl
     * @return the ag3VenueUrl
     */
    public String getAg3VenueUrl() {
        return ag3VenueUrl;
    }

    /**
     * Sets the ag3VenueUrl
     * @param ag3VenueUrl the ag3VenueUrl to set
     */
    public void setAg3VenueUrl(String ag3VenueUrl) {
        this.ag3VenueUrl = ag3VenueUrl;
    }

    /**
     * Returns the addresses
     * @return the addresses
     */
    public NetworkLocation[] getAddresses() {
        return addresses;
    }

    /**
     * Sets the addresses
     * @param addresses the addresses to set
     */
    public void setAddresses(NetworkLocation[] addresses) {
        this.addresses = addresses;
    }

    /**
     * Returns the startDate
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the startDate
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the stopDate
     * @return the stopDate
     */
    public Date getStopDate() {
        return stopDate;
    }

    /**
     * Sets the stopDate
     * @param stopDate the stopDate to set
     */
    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    /**
     * Gets the current status of the recording
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Starts and/or stops the timers
     */
    public synchronized void updateTimers() {
        stopTimers();
        Date now = new Date();
        if ((startDate != null) && !recordingStarted &&
                ((stopDate == null) || stopDate.after(now))) {
            startTimer = new Timer();
            startTimer.schedule(new StartRecording(), startDate);
            System.err.println("Recording of " + getMetadata().getName() + " scheduled to start at " + startDate);
        } else if (startDate != null) {
            System.err.println("Not scheduling start of recording of " + getMetadata().getName() + " as already recording or finished");
        }
        if ((stopDate != null) && !recordingFinished && stopDate.after(now)) {
            stopTimer = new Timer();
            stopTimer.schedule(new StopRecording(), stopDate);
            System.err.println("Recording of " + getMetadata().getName() + " scheduled to stop at " + stopDate);
        } else if (stopDate != null) {
            System.err.println("Not scheduling stop of recording of " + getMetadata().getName() + " as already finished");
        }
    }

    public synchronized void stopTimers() {
        if (startTimer != null) {
            startTimer.cancel();
            startTimer = null;
        }
        if (stopTimer != null) {
            stopTimer.cancel();
            stopTimer = null;
        }
    }

    /**
     * Starts the recording
     */
    public synchronized void startRecording() {
        if (recordingStarted) {
            return;
        }
        manager = new RecordArchiveManager(typeRepository, folder);
        try {
            NetworkLocation[] addrs = addresses;
            if (ag3VenueUrl != null) {
                venue = new Venue(ag3VenueUrl);
                connectionId = venue.enter(clientProfile);
                VenueUpdater updater = new VenueUpdater();
                updater.start();
                StreamDescription[] streams = venue.getStreams();
                addrs = new NetworkLocation[streams.length];
                for (int i = 0; i < streams.length; i++) {
                    addrs[i] = streams[i].getLocation();
                }
            }
            connector = new BridgedRTPConnector(CONNECTION, addrs);
            connector.setRtcpSink(manager);
            connector.setRtpSink(manager);
            manager.enableRecording();
            status = RECORDING;
            recordingStarted = true;
        } catch (Exception e) {
            e.printStackTrace();
            status = ERROR + "Could not start: " + e.getMessage();
        }
    }

    /**
     * Stops the recording
     */
    public synchronized void stopRecording() {
        if (recordingFinished || !recordingStarted) {
            return;
        }
        manager.disableRecording();
        connector.close();
        try {
            manager.terminate();
        } catch (IOException e) {
            e.printStackTrace();
            status = ERROR + "Could not stop: " + e.getMessage();
        }
        if (venue != null) {
            synchronized (venueSync) {
                try {
                    venue.exit(connectionId);
                } catch (Exception e) {
                    // Do Nothing
                }
                connectionId = null;
                venueSync.notifyAll();
            }
        }

        Recording finishedRecording = getFinishedRecording();
        finishedRecording.setMetadata(getMetadata());
        try {
            database.addRecording(finishedRecording);
            recordingFinished = true;
            database.deleteUnfinishedRecording(this);
            status = COMPLETED;
        } catch (IOException e) {
            e.printStackTrace();
            status = "Error: " + e.getMessage();
        }
    }

    /**
     * Pauses the recording
     */
    public void pauseRecording() {
        if (!recordingStarted || recordingFinished) {
            return;
        }
        manager.disableRecording();
        status = PAUSED;
    }

    /**
     * Resumes the recording
     */
    public void resumeRecording() {
        if (!recordingStarted || recordingFinished) {
            return;
        }
        manager.enableRecording();
        status = RECORDING;
    }

    /**
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(UnfinishedRecording r) {
        return id - r.id;
    }

    public Recording getFinishedRecording() {
        if (manager != null) {
            return manager.getRecording();
        }
        return null;
    }

}