package com.googlecode.vicovre.recordings.db.secure;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.security.UnauthorizedException;

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

@XmlRootElement(name="recording")
@XmlAccessorType(XmlAccessType.NONE)
public class SecureRecording extends Recording {

    private Recording recording = null;

    private String folder = null;

    private String id = null;

    private SecureRecordingDatabase database = null;

    public SecureRecording(SecureRecordingDatabase database,
            Recording recording) {
        this.database = database;
        this.recording = recording;
        folder = recording.getFolder();
        id = recording.getId();
    }

    private void checkEdit() {
        if (!database.canEditRecording(folder, id)) {
            throw new UnauthorizedException(
                "Only someone who can edit the recording can do this");
        }
    }

    private void checkPlayOrEdit() {
         if (!database.canPlayRecording(folder, id)
                 && !database.canEditRecording(folder, id)) {
             throw new UnauthorizedException(
                 "Only someone who can play or edit the recording can do this");
         }
    }

    private void checkRead() {
        if (!database.canReadRecording(folder, id)) {
            throw new UnauthorizedException(
                "Only someone who can read the recording can do this");
        }
    }

    public File getDirectory() {
        checkPlayOrEdit();
        return recording.getDirectory();
    }

    public long getDuration() {
        checkRead();
        return recording.getDuration();
    }

    public String getEmailAddress() {
        checkRead();
        return recording.getEmailAddress();
    }

    public String getFolder() {
        checkRead();
        return recording.getFolder();
    }

    public String getId() {
        checkRead();
        return recording.getId();
    }

    public ReplayLayout getLayout(Long time) {
        checkPlayOrEdit();
        return recording.getLayout(time);
    }

    public long getLifetime() {
        checkRead();
        return recording.getLifetime();
    }

    public RecordingMetadata getMetadata() {
        checkRead();
        return recording.getMetadata();
    }

    public List<Long> getPauseTimes() {
        checkPlayOrEdit();
        return recording.getPauseTimes();
    }

    public List<ReplayLayout> getReplayLayouts() {
        checkPlayOrEdit();
        return recording.getReplayLayouts();
    }

    public Date getStartTime() {
        checkRead();
        return recording.getStartTime();
    }

    public String getStartTimeString() {
        checkRead();
        return recording.getStartTimeString();
    }

    public Stream getStream(String ssrc) {
        checkPlayOrEdit();
        return recording.getStream(ssrc);
    }

    public List<Stream> getStreams() {
        checkPlayOrEdit();
        return recording.getStreams();
    }

    public void removeLayout(Long time) {
        checkEdit();
        recording.removeLayout(time);
    }

    public void setEmailAddress(String emailAddress) {
        checkEdit();
        recording.setEmailAddress(emailAddress);
    }

    public void setLifetime(long lifetime) {
        checkEdit();
        recording.setLifetime(lifetime);
    }

    public void setMetadata(RecordingMetadata metadata) {
        checkEdit();
        recording.setMetadata(metadata);
    }

    public void setReplayLayout(ReplayLayout replayLayout) {
        checkEdit();
        recording.setReplayLayout(replayLayout);
    }

    public int compareTo(Recording o) {
        return recording.compareTo(o);
    }

    public boolean isEditable() {
        checkRead();
        return database.canEditRecording(folder, id);
    }

    public boolean isPlayable() {
        checkRead();
        return database.canPlayRecording(folder, id);
    }

    public void annotateChanges(long time) throws IOException {
        checkEdit();
        recording.annotateChanges(time);
    }

    public double getAnnotationProgress(long time) {
        checkEdit();
        return recording.getAnnotationProgress(time);
    }

}
