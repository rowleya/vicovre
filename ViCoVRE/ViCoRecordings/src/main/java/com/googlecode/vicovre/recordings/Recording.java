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
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.googlecode.vicovre.annotations.Annotation;


/**
 * Represents a recording
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */

@XmlRootElement(name="recording")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Recording implements Comparable<Recording> {

    @XmlElement
    public abstract String getFolder();

    /**
     * Returns the directory
     * @return the directory
     */
    public abstract File getDirectory();

    /**
     * Returns the id
     * @return the id
     */
    @XmlElement
    public abstract String getId();

    /**
     * Returns the startTime
     * @return the startTime
     */
    public abstract Date getStartTime();

    @XmlElement(name="startTime")
    public abstract String getStartTimeString();

    /**
     * Returns the duration in ms
     * @return the duration
     */
    @XmlElement
    public abstract long getDuration();

    /**
     * Returns the streams
     * @return the streams
     */
    public abstract List<Stream> getStreams();

    /**
     * Gets a stream
     * @param ssrc The ssrc of the stream to get
     * @return The stream, or null if doesn't exist
     */
    public abstract Stream getStream(String ssrc);

    /**
     * Sets the replay layout
     * @param replayLayout The layout to set
     */
    public abstract void setReplayLayout(ReplayLayout replayLayout);

    /**
     * Gets the replay layouts
     * @return The replay layouts
     */
    public abstract List<ReplayLayout> getReplayLayouts();

    /**
     * Gets a replay layout
     * @param time The time at which the layout applies
     * @return The layout or null if doesn't exist
     */
    public abstract ReplayLayout getLayout(Long time);

    public abstract void removeLayout(Long time);

    /**
     * Gets the metadata
     * @return The metadata
     */
    @XmlElement
    public abstract Metadata getMetadata();

    /**
     * Sets the metadata
     * @param metadata The metadata to set
     */
    public abstract void setMetadata(Metadata metadata);

    public abstract List<Long> getPauseTimes();

    public abstract void setLifetime(long lifetime);

    @XmlElement
    public abstract long getLifetime();

    public abstract void setEmailAddress(String emailAddress);

    @XmlElement
    public abstract String getEmailAddress();

    @XmlElement
    public abstract boolean isPlayable();

    @XmlElement
    public abstract boolean isEditable();

    @XmlElement
    public abstract boolean isAnnotatable();

    public abstract void annotateChanges(long time) throws IOException;

    public abstract double getAnnotationProgress(long time);

    public abstract List<Annotation> getAnnotations();

    public abstract void setAnnotations(List<Annotation> annotations);

    public abstract void addAnnotation(Annotation annotation)
        throws IOException;

    public abstract void deleteAnnotation(Annotation annotation)
        throws IOException;

    public abstract void updateAnnotation(Annotation annotation)
        throws IOException;
}
