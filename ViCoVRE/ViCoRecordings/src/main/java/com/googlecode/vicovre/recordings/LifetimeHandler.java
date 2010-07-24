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
import java.util.Timer;
import java.util.TimerTask;

import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.utils.Emailer;

public class LifetimeHandler {

    private static final long DAY = 24 * 60 * 1000;

    private static final long[] REMINDER_BEFORE_TIMEOUT =
        new long[]{DAY, 3 * DAY, 7 * DAY};

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "EEE, MMM d, yyyy 'at' HH:mm");

    private Recording recording;

    private RecordingDatabase database = null;

    private Timer timer = null;

    private String emailAddress = null;

    private Emailer emailer = null;

    private class DeleteTask extends TimerTask {

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

        private SendReminderTask(long timeRemaining, long timeout) {
            this.timeRemaining = timeRemaining;
            this.timeout = timeout;
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
                        database.getTopLevelFolder().getFile());
                if (message != null) {
                    long daysRemaining = timeRemaining / DAY;
                    String day = "days";
                    if (daysRemaining == 1) {
                        day = "day";
                    }
                    message.replaceAll("${timeRemaining}",
                            daysRemaining + " " + day);
                    message.replaceAll("${deleteDate}",
                            DATE_FORMAT.format(new Date(timeout)));
                    emailer.send(emailAddress, subject, message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public LifetimeHandler(Recording recording, RecordingDatabase database,
            Emailer emailer) {
        this.recording = recording;
        this.database = database;
        this.emailer = emailer;
        updateLifetime();
    }

    public void updateLifetime() {
        if (timer != null) {
            timer.cancel();
        }
        if (recording.getLifetime() > 0) {
            timer = new Timer();
            long timeout = recording.getStartTime().getTime()
                + recording.getDuration()
                + recording.getLifetime();
            timer.schedule(new DeleteTask(), new Date(timeout));
            for (long time : REMINDER_BEFORE_TIMEOUT) {
                if ((timeout - time) > 0) {
                    timer.schedule(new SendReminderTask(time, timeout),
                        new Date(timeout - time));
                }
            }
        }
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
