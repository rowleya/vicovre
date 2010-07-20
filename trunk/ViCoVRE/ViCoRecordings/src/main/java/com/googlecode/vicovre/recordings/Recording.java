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

package com.googlecode.vicovre.recordings;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.googlecode.vicovre.recordings.db.Folder;


/**
 * Represents a recording
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Recording implements Comparable<Recording> {

    // The recording metadata
    private RecordingMetadata metadata = null;

    // The id of the recording
    private String id = null;

    // The start time of the recording
    private Date startTime = new Date(0);

    // The end time of the recording
    private long duration = 0;

    // The streams in the recording
    private HashMap<String, Stream> streams = new HashMap<String, Stream>();

    private HashMap<Long, ReplayLayout> replayLayouts =
        new HashMap<Long, ReplayLayout>();

    private Vector<Long> pauseTimes = new Vector<Long>();

    // The directory holding the streams
    private File directory = null;

    // The folder holding the recording
    private Folder folder = null;

    public Recording(Folder folder, String id) {
        this.folder = folder;
        this.id = id;
        this.directory = new File(folder.getFile(), id);
        if (id == null) {
            throw new RuntimeException("Null id recording in folder " + folder);
        }
    }

    /**
     * Returns the directory
     * @return the directory
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Returns the id
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the startTime
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Returns the duration in ms
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Returns the streams
     * @return the streams
     */
    public List<Stream> getStreams() {
        return new Vector<Stream>(streams.values());
    }

    /**
     * Gets a stream
     * @param ssrc The ssrc of the stream to get
     * @return The stream, or null if doesn't exist
     */
    public Stream getStream(String ssrc) {
        return streams.get(ssrc);
    }

    /**
     * Sets the streams
     * @param streams the streams to set
     */
    public void setStreams(List<Stream> streams) {
        if (streams != null) {
            for (Stream stream : streams) {
                stream.setRecording(this);
                this.streams.put(stream.getSsrc(), stream);
            }
        } else {
            this.streams.clear();
        }
    }

    public void updateTimes() {
        startTime = null;
        Date endTime = null;
        for (Stream stream : streams.values()) {
            if ((startTime == null)
                    || ((stream.getStartTime() != null)
                            && stream.getStartTime().before(startTime))) {
                startTime = stream.getStartTime();
            }
            if ((endTime == null) || ((stream.getEndTime() != null)
                    && stream.getEndTime().after(endTime))) {
                endTime = stream.getEndTime();
            }
        }
        if (endTime != null && startTime != null) {
            duration = endTime.getTime() - startTime.getTime();
        } else {
            duration = 0;
        }
    }

    /**
     * Sets the replay layouts
     * @param replayLayouts The replay layouts
     */
    public void setReplayLayouts(List<ReplayLayout> replayLayouts) {
        if (replayLayouts != null) {
            for (ReplayLayout layout : replayLayouts) {
                layout.setRecording(this);
                this.replayLayouts.put(layout.getTime(), layout);
            }
        } else {
            this.replayLayouts.clear();
        }
    }

    /**
     * Sets the replay layout
     * @param replayLayout The layout to set
     */
    public void setReplayLayout(ReplayLayout replayLayout) {
        replayLayouts.put(replayLayout.getTime(), replayLayout);
    }

    /**
     * Gets the replay layouts
     * @return The replay layouts
     */
    public List<ReplayLayout> getReplayLayouts() {
        if (!replayLayouts.isEmpty()) {
            return new Vector<ReplayLayout>(replayLayouts.values());
        }
        Vector<ReplayLayout> replayLayouts = new Vector<ReplayLayout>();
        try {
            for (DefaultLayout layout : folder.getDefaultLayouts()) {

                // Try to match the positions to the streams
                ReplayLayout replayLayout = layout.matchLayout(this);
                if (replayLayout != null) {
                    replayLayouts.add(replayLayout);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: error reading layouts");
            e.printStackTrace();
        }
        return replayLayouts;
    }

    /**
     * Gets a replay layout
     * @param time The time at which the layout applies
     * @return The layout or null if doesn't exist
     */
    public ReplayLayout getLayout(Long time) {
        return replayLayouts.get(time);
    }

    /**
     * Gets the metadata
     * @return The metadata
     */
    public RecordingMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     * @param metadata The metadata to set
     */
    public void setMetadata(RecordingMetadata metadata) {
        this.metadata = metadata;
    }

    public List<Long> getPauseTimes() {
        return pauseTimes;
    }

    public void setPauseTimes(List<Long> pauseTimes) {
        this.pauseTimes.clear();
        this.pauseTimes.addAll(pauseTimes);
    }

    public void addPauseTime(Long time) {
        pauseTimes.add(time);
    }

    public boolean equals(Recording recording) {
        return recording.id.equals(id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public int compareTo(Recording r) {
        int value = startTime.compareTo(r.startTime);
        if (value == 0) {
            return metadata.compareTo(r.metadata);
        }
        return value;
    }
}
