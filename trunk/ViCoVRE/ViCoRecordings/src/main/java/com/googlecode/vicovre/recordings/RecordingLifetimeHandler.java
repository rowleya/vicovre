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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.RecordingListener;
import com.googlecode.vicovre.utils.Emailer;

public class RecordingLifetimeHandler implements RecordingListener {

    private static final long DAY = 24 * 60 * 1000;

    private static final long[] REMINDER_BEFORE_TIMEOUT =
        new long[]{DAY, 3 * DAY, 7 * DAY};

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "EEE, MMM d, yyyy 'at' HH:mm");

    private RecordingDatabase database = null;

    private HashMap<Recording, Timer> timers = new HashMap<Recording, Timer>();

    private Emailer emailer = null;

    private class DeleteTask extends TimerTask {

        private Recording recording = null;

        private DeleteTask(Recording recording) {
            this.recording = recording;
        }

        public void run() {
            try {
                database.deleteRecording(recording);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReminderTask extends TimerTask {

        private long timeRemaining = 0;

        private long timeout = 0;

        private Recording recording;

        private SendReminderTask(long timeRemaining, long timeout,
                Recording recording) {
            this.timeRemaining = timeRemaining;
            this.timeout = timeout;
            this.recording = recording;
        }

        public void run() {
            String subject = "Recording Reminder";
            if (timeRemaining == REMINDER_BEFORE_TIMEOUT[0]) {
                subject = "Final " + subject;
            }
            try {
                String message = MessageReader.readMessage(
                        "recordingLifetimeReminder.txt",
                        recording.getDirectory(),
                        database.getFile(""));
                if (message != null) {
                    long daysRemaining = timeRemaining / DAY;
                    String day = "days";
                    if (daysRemaining == 1) {
                        day = "day";
                    }
                    message.replaceAll("${recording}",
                            recording.getFolder() + "/"
                            + recording.getId());
                    message.replaceAll("${timeRemaining}",
                            daysRemaining + " " + day);
                    message.replaceAll("${deleteDate}",
                            DATE_FORMAT.format(new Date(timeout)));
                    emailer.send(recording.getEmailAddress(), subject, message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public RecordingLifetimeHandler(RecordingDatabase database,
            Emailer emailer) {
        this.database = database;
        this.emailer = emailer;
        database.addRecordingListener(this);
    }

    public void schedule(Recording recording) {
        stopTimer(recording);
        if (recording.getLifetime() > 0) {
            Timer timer = new Timer();
            long timeout = recording.getStartTime().getTime()
                + recording.getDuration()
                + recording.getLifetime();
            timer.schedule(new DeleteTask(recording), new Date(timeout));
            for (long time : REMINDER_BEFORE_TIMEOUT) {
                if ((timeout - time) > 0) {
                    timer.schedule(
                        new SendReminderTask(time, timeout, recording),
                        new Date(timeout - time));
                }
            }
            timers.put(recording, timer);
        }
    }

    public void stopTimer(Recording recording) {
        Timer timer = timers.remove(recording);
        if (timer != null) {
            timer.cancel();
        }
    }

    public void recordingAdded(Recording recording) {
        schedule(recording);
    }

    public void recordingDeleted(Recording recording) {
        stopTimer(recording);
    }

    public void recordingLayoutsUpdated(Recording recording) {
        // Do Nothing
    }

    public void recordingLifetimeUpdated(Recording recording) {
        schedule(recording);
    }

    public void recordingMetadataUpdated(Recording recording) {
        // Do Nothing
    }

    public void shutdown() {
        Vector<Recording> recordings = new Vector<Recording>(timers.keySet());
        for (Recording recording : recordings) {
            stopTimer(recording);
        }
    }

    public void recordingMoved(Recording oldRecording, Recording newRecording) {
        stopTimer(oldRecording);
        schedule(newRecording);
    }
}
