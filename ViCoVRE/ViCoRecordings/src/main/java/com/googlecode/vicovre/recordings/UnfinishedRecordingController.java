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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.mail.EmailException;

import com.googlecode.onevre.ag.types.BridgeDescription;
import com.googlecode.onevre.ag.types.StreamDescription;
import com.googlecode.onevre.ag.types.network.NetworkLocation;
import com.googlecode.onevre.ag.types.server.Venue;
import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.UnfinishedRecordingListener;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.Emailer;

public class UnfinishedRecordingController
        implements UnfinishedRecordingListener {

    public static final SimpleDateFormat ID_DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd_HHmmss-SSSS");

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
        String id = ID_DATE_FORMAT.format(new Date()) + recording.getId();
        File directory = new File(database.getFile(recording.getFolder()), id);
        RecordArchiveManager manager = new RecordArchiveManager(
                layoutRepository, typeRepository,
                recording.getFolder(), id, directory);
        managers.put(recording, manager);
        try {
            NetworkLocation[] addrs = recording.getAddresses();
            String ag3VenueUrl = recording.getAg3VenueUrl();
            if (ag3VenueUrl != null) {
                Venue venue = new Venue(ag3VenueUrl, false);
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
        System.err.println("Stopping recording "
                + recording.getMetadata().getPrimaryValue() + ": "
                + recording.getId());
        Timer stopTimer = stopTimers.remove(recording);
        if (stopTimer != null) {
            stopTimer.cancel();
        }
        if (recording.isFinished() || !recording.isStarted()) {
            return null;
        }

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

                recording.stopRecording();
                database.addRecording(finishedRecording, recording);
                if (recording.getRepeatFrequency().equals(
                        UnfinishedRecording.NO_REPEAT)) {
                    database.finishUnfinishedRecording(recording);
                    recording.setStatus(UnfinishedRecording.COMPLETED);
                } else {
                    recording.resetRecording();
                    recording.setStatus(UnfinishedRecording.STOPPED);
                    schedule(recording);
                }

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
            System.err.println("Stopped recording "
                    + recording.getMetadata().getPrimaryValue() + ": "
                    + recording.getId());
            return finishedRecording;
        }
        System.err.println("Stopped empty recording "
                + recording.getMetadata().getPrimaryValue()
                + ": " + recording.getId());
        if (recording.getRepeatFrequency().equals(
                UnfinishedRecording.NO_REPEAT)) {
            recording.resetRecording();
            recording.setStatus(UnfinishedRecording.STOPPED
                    + ": no streams recorded");
        } else {
            recording.resetRecording();
            recording.setStatus(UnfinishedRecording.STOPPED);
            schedule(recording);
        }

        return null;
    }

    public void schedule(UnfinishedRecording recording) {
        stopTimer(recording);
        Date now = new Date();
        Date startDate = recording.getStartDate();
        Date stopDate = recording.getStopDate();
        String frequency = recording.getRepeatFrequency();

        if (!frequency.equals(UnfinishedRecording.NO_REPEAT)) {
            int startHour = recording.getRepeatStartHour();
            int startMinute = recording.getRepeatStartMinute();
            int durationMinutes = recording.getRepeatDurationMinutes();
            Calendar nowCal = Calendar.getInstance();
            Calendar nextStart = Calendar.getInstance();
            if (startDate != null) {
                nextStart.setTime(startDate);
            }
            nextStart.set(Calendar.HOUR_OF_DAY, startHour);
            nextStart.set(Calendar.MINUTE, startMinute);
            nextStart.set(Calendar.SECOND, 0);
            nextStart.set(Calendar.MILLISECOND, 0);
            Calendar nextEnd = Calendar.getInstance();

            if (frequency.equals(UnfinishedRecording.REPEAT_DAILY)) {
                int dayFrequency = recording.getRepeatItemFrequency();
                nextEnd.setTime(nextStart.getTime());
                nextEnd.add(Calendar.MINUTE, durationMinutes);
                while (nextEnd.before(nowCal)) {
                    nextStart.add(Calendar.DAY_OF_MONTH, dayFrequency);
                    nextEnd.add(Calendar.DAY_OF_MONTH, dayFrequency);

                    while (recording.getIgnoreWeekends()
                            && ((nextStart.get(Calendar.DAY_OF_WEEK)
                                    == Calendar.SATURDAY)
                                        || nextStart.get(Calendar.DAY_OF_WEEK)
                                            == Calendar.SUNDAY)) {
                        nextStart.add(Calendar.DAY_OF_MONTH, 1);
                        nextEnd.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            } else if (frequency.equals(UnfinishedRecording.REPEAT_WEEKLY)) {
                int weekFrequency = recording.getRepeatItemFrequency();
                int dayOfWeek = recording.getRepeatDayOfWeek();
                nextStart.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                nextEnd.setTime(nextStart.getTime());
                nextEnd.add(Calendar.MINUTE, durationMinutes);

                while (nextEnd.before(nowCal)) {
                    nextStart.add(Calendar.DAY_OF_MONTH, weekFrequency * 7);
                    nextEnd.add(Calendar.DAY_OF_MONTH, weekFrequency * 7);
                }
            } else if (frequency.equals(UnfinishedRecording.REPEAT_MONTHLY)) {
                int monthFrequency = recording.getRepeatItemFrequency();
                int dayOfMonth = recording.getRepeatDayOfMonth();
                if (dayOfMonth > 0) {
                    nextStart.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    nextEnd.setTime(nextStart.getTime());
                    nextEnd.add(Calendar.MINUTE, durationMinutes);
                    while (nextEnd.before(nowCal)) {
                        nextStart.add(Calendar.MONTH, monthFrequency);
                        nextEnd.add(Calendar.MONTH, monthFrequency);
                    }
                } else {
                    int dayOfWeek = recording.getRepeatDayOfWeek();
                    int weekOfMonth = recording.getRepeatWeekNumber();
                    nextStart.setFirstDayOfWeek(dayOfWeek);
                    nextStart.setMinimalDaysInFirstWeek(7);
                    nextStart.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                    nextStart.set(Calendar.WEEK_OF_MONTH, weekOfMonth);
                    nextEnd.setTime(nextStart.getTime());
                    nextEnd.add(Calendar.MINUTE, durationMinutes);
                    while (nextEnd.before(nowCal)) {
                        if (weekOfMonth > 0) {
                            nextStart.add(Calendar.MONTH, monthFrequency);
                        } else {
                            nextStart.add(Calendar.MONTH, monthFrequency + 1);
                        }
                        nextStart.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                        nextStart.set(Calendar.WEEK_OF_MONTH, weekOfMonth);

                        nextEnd.setTime(nextStart.getTime());
                        nextEnd.add(Calendar.MINUTE, durationMinutes);
                    }
                }
            } else if (frequency.equals(UnfinishedRecording.REPEAT_ANNUALLY)) {
                int yearFrequency = recording.getRepeatItemFrequency();
                int month = recording.getRepeatMonth();
                int dayOfMonth = recording.getRepeatDayOfMonth();
                if (dayOfMonth > 0) {
                    nextStart.set(Calendar.MONTH, month);
                    nextStart.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    nextEnd.setTime(nextStart.getTime());
                    nextEnd.add(Calendar.MINUTE, durationMinutes);
                    while (nextEnd.before(nowCal)) {
                        nextStart.add(Calendar.YEAR, yearFrequency);
                        nextEnd.add(Calendar.YEAR, yearFrequency);
                    }
                } else {
                    int dayOfWeek = recording.getRepeatDayOfWeek();
                    int weekOfMonth = recording.getRepeatWeekNumber();
                    nextStart.setFirstDayOfWeek(dayOfWeek);
                    nextStart.setMinimalDaysInFirstWeek(7);
                    nextStart.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                    nextStart.set(Calendar.WEEK_OF_MONTH, weekOfMonth);
                    if (weekOfMonth > 0) {
                        nextStart.set(Calendar.MONTH, month);
                    } else {
                        nextStart.set(Calendar.MONTH, month + 1);
                    }
                    nextEnd.setTime(nextStart.getTime());
                    nextEnd.add(Calendar.MINUTE, durationMinutes);
                    while (nextEnd.before(nowCal)) {
                        nextStart.add(Calendar.YEAR, yearFrequency);
                        if (weekOfMonth > 0) {
                            nextStart.set(Calendar.MONTH, month);
                        } else {
                            nextStart.set(Calendar.MONTH, month + 1);
                        }
                        nextStart.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                        nextStart.set(Calendar.WEEK_OF_MONTH, weekOfMonth);
                        nextEnd.setTime(nextStart.getTime());
                        nextEnd.add(Calendar.MINUTE, durationMinutes);
                    }
                }
            }

            if ((stopDate != null) && nextStart.getTime().after(stopDate)) {
                return;
            }

            startDate = nextStart.getTime();
            stopDate = nextEnd.getTime();
            recording.setStartDate(startDate);
        }

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
