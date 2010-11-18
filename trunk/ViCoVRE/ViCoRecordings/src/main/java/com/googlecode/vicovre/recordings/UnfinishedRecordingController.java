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
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.mail.EmailException;

import ag3.interfaces.Venue;
import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.StreamDescription;

import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.UnfinishedRecordingListener;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.Emailer;

public class UnfinishedRecordingController
        implements UnfinishedRecordingListener {

    private static final BridgeDescription CONNECTION = new BridgeDescription();
    static {
        CONNECTION.setServerType("Multicast");
    }

    private RecordingDatabase database = null;

    private RtpTypeRepository typeRepository = null;

    private LayoutRepository layoutRepository = null;

    private Emailer emailer = null;

    private HashMap<NetworkLocation, BridgedRTPConnector> connectors =
        new HashMap<NetworkLocation, BridgedRTPConnector>();

    private HashMap<NetworkLocation, Integer> locationCount =
        new HashMap<NetworkLocation, Integer>();

    private HashMap<UnfinishedRecording, NetworkLocation[]> addresses =
        new HashMap<UnfinishedRecording, NetworkLocation[]>();

    private HashMap<UnfinishedRecording, RecordArchiveManager> managers =
        new HashMap<UnfinishedRecording, RecordArchiveManager>();

    private HashMap<UnfinishedRecording, Timer> startTimers =
        new HashMap<UnfinishedRecording, Timer>();

    private HashMap<UnfinishedRecording, Timer> stopTimers =
        new HashMap<UnfinishedRecording, Timer>();

    private class StartRecording extends TimerTask {

        private UnfinishedRecording recording = null;

        private StartRecording(UnfinishedRecording recording) {
            this.recording = recording;
        }

        public void run() {
            startRecording(recording);
        }
    }

    private class StopRecording extends TimerTask {

        private UnfinishedRecording recording = null;

        private StopRecording(UnfinishedRecording recording) {
            this.recording = recording;
        }

        public void run() {
            stopRecording(recording);
        }
    }

    public UnfinishedRecordingController(RecordingDatabase database,
            RtpTypeRepository typeRepository,
            LayoutRepository layoutRepository, Emailer emailer) {
        this.database = database;
        this.typeRepository = typeRepository;
        this.layoutRepository = layoutRepository;
        this.emailer = emailer;
        database.addUnfinishedRecordingListener(this);
    }

    public void startRecording(UnfinishedRecording recording) {
        Timer startTimer = startTimers.remove(recording);
        if (startTimer != null) {
            startTimer.cancel();
        }
        if (recording.isStarted()) {
            return;
        }
        File directory = new File(database.getFile(recording.getFolder()),
                recording.getFinishedRecordingId());
        RecordArchiveManager manager = new RecordArchiveManager(
                layoutRepository, typeRepository,
                recording.getFolder(), recording.getFinishedRecordingId(),
                directory);
        managers.put(recording, manager);
        try {
            NetworkLocation[] addrs = recording.getAddresses();
            String ag3VenueUrl = recording.getAg3VenueUrl();
            if (ag3VenueUrl != null) {
                Venue venue = new Venue(ag3VenueUrl);
                StreamDescription[] streams = venue.getStreams();
                addrs = new NetworkLocation[streams.length];
                for (int i = 0; i < streams.length; i++) {
                    addrs[i] = streams[i].getLocation();
                }
            }
            for (int i = 0; i < addrs.length; i++) {
                synchronized (connectors) {
                    BridgedRTPConnector connector = connectors.get(addrs[i]);
                    NetworkLocation location = addrs[i];
                    if (connector == null) {
                        connector = new BridgedRTPConnector(CONNECTION,
                                new NetworkLocation[]{location});
                        connectors.put(location, connector);
                        locationCount.put(location, 1);
                    } else {
                        locationCount.put(location,
                                locationCount.get(location) + 1);
                    }
                    connector.addRtcpSink(manager);
                    connector.addRtpSink(manager);
                }
            }
            addresses.put(recording, addrs);
            manager.enableRecording();
            recording.setStatus(UnfinishedRecording.RECORDING);
            recording.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
            recording.setStatus(UnfinishedRecording.ERROR
                    + "Could not start: " + e.getMessage());
        }
    }

    public Recording stopRecording(UnfinishedRecording recording) {
        System.err.println("Stopping recording " + recording.getMetadata().getPrimaryValue() + ": " + recording.getId());
        Timer stopTimer = stopTimers.remove(recording);
        if (stopTimer != null) {
            stopTimer.cancel();
        }
        if (recording.isFinished() || !recording.isStarted()) {
            return null;
        }

        System.err.println("    Removing connectors");
        RecordArchiveManager manager = managers.remove(recording);
        NetworkLocation[] addrs = addresses.remove(recording);
        manager.disableRecording(false);
        synchronized (connectors) {
            for (NetworkLocation addr : addrs) {
                int count = locationCount.get(addr);
                if (count <= 1) {
                    BridgedRTPConnector connector = connectors.remove(addr);
                    connector.close();
                    locationCount.remove(addr);
                } else {
                    BridgedRTPConnector connector = connectors.get(addr);
                    connector.removeRtcpSink(manager);
                    connector.removeRtcpSink(manager);
                    locationCount.put(addr, count - 1);
                }
            }
        }
        System.err.println("    Terminating Manager");
        try {
            manager.terminate();
        } catch (IOException e) {
            e.printStackTrace();
            recording.setStatus(UnfinishedRecording.ERROR
                    + "Could not stop: " + e.getMessage());
        }

        Recording finishedRecording = manager.getRecording();
        finishedRecording.setMetadata(recording.getMetadata());
        if (!finishedRecording.getStreams().isEmpty()) {
            try {
                String emailAddress = recording.getEmailAddress();
                if (emailAddress != null) {
                    finishedRecording.setEmailAddress(emailAddress);
                }

                System.err.println("    Adding to database");
                recording.stopRecording();
                database.addRecording(finishedRecording, recording);
                database.deleteUnfinishedRecording(recording);
                recording.setStatus(UnfinishedRecording.COMPLETED);

                System.err.println("    Sending email");
                if ((emailAddress != null) && (emailer != null)) {
                    String message = MessageReader.readMessage(
                            "recordingComplete.txt",
                            finishedRecording.getDirectory().getParentFile(),
                            database.getFile(""));
                    if (message != null) {
                        message.replaceAll("${recording}",
                                 finishedRecording.getFolder() + "/"
                                 + finishedRecording.getId());
                        emailer.send(emailAddress, "Recording completed",
                            message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                recording.setStatus(UnfinishedRecording.ERROR + e.getMessage());
            } catch (EmailException e) {
                e.printStackTrace();
            }
            return finishedRecording;
        }
        recording.resetRecording();
        recording.setStatus(UnfinishedRecording.STOPPED
                + ": no streams recorded");
        return null;
    }

    public void schedule(UnfinishedRecording recording) {
        stopTimer(recording);
        Date now = new Date();
        Date startDate = recording.getStartDate();
        Date stopDate = recording.getStopDate();
        if ((startDate != null) && !recording.isStarted() &&
                ((stopDate == null) || stopDate.after(now))) {
            Timer startTimer = new Timer();
            startTimer.schedule(new StartRecording(recording), startDate);
            System.err.println("Recording of "
                    + recording.getMetadata().getPrimaryValue()
                    + " scheduled to start at " + startDate);
            startTimers.put(recording, startTimer);
        }
        if ((stopDate != null) && !recording.isFinished()
                && stopDate.after(now)) {
            Timer stopTimer = new Timer();
            stopTimer.schedule(new StopRecording(recording), stopDate);
            System.err.println("Recording of "
                    + recording.getMetadata().getPrimaryValue()
                    + " scheduled to stop at " + stopDate);
            stopTimers.put(recording, stopTimer);
        }
    }

    public void stopTimer(UnfinishedRecording recording) {
        Timer startTimer = startTimers.remove(recording);
        if (startTimer != null) {
            startTimer.cancel();
        }
        Timer stopTimer = stopTimers.remove(recording);
        if (stopTimer != null) {
            stopTimer.cancel();
        }
    }

    public void pauseRecording(UnfinishedRecording recording) {
        if (recording.isStarted() && !recording.isFinished()) {
            RecordArchiveManager manager = managers.get(recording);
            if (manager != null) {
                manager.disableRecording(true);
                recording.setStatus(UnfinishedRecording.PAUSED);
            }
        }
    }

    public void resumeRecording(UnfinishedRecording recording) {
        if (recording.isStarted() && !recording.isFinished()) {
            RecordArchiveManager manager = managers.get(recording);
            if (manager != null) {
                manager.enableRecording();
                recording.setStatus(UnfinishedRecording.RECORDING);
            }
        }
    }

    public void recordingAdded(UnfinishedRecording recording) {
        schedule(recording);
    }

    public void recordingDeleted(UnfinishedRecording recording) {
        stopRecording(recording);
        stopTimer(recording);
    }

    public void recordingUpdated(UnfinishedRecording recording) {
        schedule(recording);
    }

    public void shutdown() {
        Vector<UnfinishedRecording> recordings =
            new Vector<UnfinishedRecording>(managers.keySet());
        for (UnfinishedRecording recording : recordings) {
            stopRecording(recording);
        }

        recordings = new Vector<UnfinishedRecording>(startTimers.keySet());
        for (UnfinishedRecording recording : recordings) {
            stopTimer(recording);
        }

        recordings = new Vector<UnfinishedRecording>(stopTimers.keySet());
        for (UnfinishedRecording recording : recordings) {
            stopTimer(recording);
        }
    }
}
