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
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.googlecode.vicovre.recordings.db.RecordingDatabase;

public class LifetimeHandler {

    private Recording recording;

    private RecordingDatabase database = null;

    private Timer timer = null;

    private class DeleteTask extends TimerTask {

        public void run() {
            try {
                database.deleteRecording(recording);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public LifetimeHandler(Recording recording, RecordingDatabase database) {
        this.recording = recording;
        this.database = database;
        updateLifetime();
    }

    public void updateLifetime() {
        if (timer != null) {
            timer.cancel();
        }
        if (recording.getLifetime() > 0) {
            timer = new Timer();
            timer.schedule(new DeleteTask(), new Date(
                    recording.getStartTime().getTime() + recording.getDuration()
                        + recording.getLifetime()));
        }
    }
}
