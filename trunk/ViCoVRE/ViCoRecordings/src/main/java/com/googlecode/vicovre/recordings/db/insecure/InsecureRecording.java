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

package com.googlecode.vicovre.recordings.db.insecure;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.media.screencapture.ScreenChangeDetector;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;


/**
 * Represents a recording
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
@XmlRootElement(name="recording")
@XmlAccessorType(XmlAccessType.NONE)
public class InsecureRecording extends Recording {

    // The recording metadata
    private Metadata metadata = null;

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

    private HashMap<Long, ScreenChangeDetector> screenChangeDetectors =
        new HashMap<Long, ScreenChangeDetector>();

    private Vector<Long> pauseTimes = new Vector<Long>();

    // The directory holding the streams
    private File directory = null;

    // The folder holding the recording
    private String folder = null;

    // The lifetime of the recording
    private long lifetime = 0;

    private String emailAddress = null;

    private LayoutRepository layoutRepository = null;

    private RtpTypeRepository typeRepository = null;

    private HashMap<String, Annotation> annotations =
        new HashMap<String, Annotation>();

    public InsecureRecording() {
        // Does Nothing
    }

    public InsecureRecording(String folder, String id, File directory,
            LayoutRepository layoutRepostory,
            RtpTypeRepository typeRepository) {
        this.folder = folder;
        this.id = id;
        this.directory = directory;
        this.layoutRepository = layoutRepostory;
        this.typeRepository = typeRepository;

        if (id == null) {
            throw new RuntimeException("Null id recording in folder " + folder);
        }
    }

    public String getFolder() {
        return folder;
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

    public String getStartTimeString() {
        if (startTime != null) {
            return RecordingConstants.DATE_FORMAT.format(startTime);
        }
        return null;
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
        return new Vector<ReplayLayout>(replayLayouts.values());
    }

    /**
     * Gets a replay layout
     * @param time The time at which the layout applies
     * @return The layout or null if doesn't exist
     */
    public ReplayLayout getLayout(Long time) {
        return replayLayouts.get(time);
    }

    public void removeLayout(Long time) {
        replayLayouts.remove(time);
    }

    /**
     * Gets the metadata
     * @return The metadata
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata
     * @param metadata The metadata to set
     */
    public void setMetadata(Metadata metadata) {
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

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public boolean equals(Recording recording) {
        return recording.getId().equals(id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public int compareTo(Recording r) {
        int value = startTime.compareTo(r.getStartTime());
        if (value == 0) {
            return metadata.compareTo(r.getMetadata());
        }
        return value;
    }

    public void setId(String id) {
        File oldDir = directory;
        directory = new File(directory.getParent(), id);
        oldDir.renameTo(directory);
        this.id = id;
    }

    public boolean isEditable() {
        return true;
    }

    public boolean isPlayable() {
        return true;
    }

    public void annotateChanges(long time) throws IOException {
        if (!screenChangeDetectors.containsKey(time)) {
            ReplayLayout replayLayout = replayLayouts.get(time);
            Layout layout = layoutRepository.findLayout(replayLayout.getName());
            for (LayoutPosition position : layout.getStreamPositions()) {
                if (position.hasChanges()) {
                    Stream stream = replayLayout.getStream(position.getName());
                    try {
                        ScreenChangeDetector detector =
                            new ScreenChangeDetector(directory,
                                    stream.getSsrc(), typeRepository);
                        screenChangeDetectors.put(time, detector);
                        detector.run();
                        screenChangeDetectors.remove(time);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
            }
        }
    }

    public double getAnnotationProgress(long time) {
        if (screenChangeDetectors.containsKey(time)) {
            return screenChangeDetectors.get(time).getProgress();
        }
        return 1.0;
    }

    public List<Annotation> getAnnotations() {
        return new Vector<Annotation>(annotations.values());
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations.clear();
        for (Annotation annotation : annotations) {
            this.annotations.put(annotation.getId(), annotation);
        }
    }

    public void addAnnotation(Annotation annotation) throws IOException {
        annotations.put(annotation.getId(), annotation);
    }

    public void deleteAnnotation(Annotation annotation) throws IOException {
        annotations.remove(annotation.getId());
    }

    public boolean isAnnotatable() {
        return true;
    }

    public void updateAnnotation(Annotation annotation) throws IOException {
        annotations.put(annotation.getId(), annotation);
    }
}
