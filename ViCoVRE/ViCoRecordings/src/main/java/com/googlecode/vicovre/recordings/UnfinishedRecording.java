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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ag3.interfaces.types.NetworkLocation;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;

/**
 * Represents a recording to be made
 * @author Andrew G D Rowley
 * @version 1.0
 */
@XmlRootElement(name="unfinishedrecording")
@XmlAccessorType(XmlAccessType.NONE)
public class UnfinishedRecording implements Comparable<UnfinishedRecording> {

    public static final SimpleDateFormat ID_DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd_HHmmss-SSSS");

    public static final String STOPPED = "Stopped";

    public static final String RECORDING = "Recording";

    public static final String PAUSED = "Paused";

    public static final String COMPLETED = "Completed";

    public static final String ERROR = "Error: ";

    private String folder = null;

    private String id = null;

    private RecordingMetadata metadata = null;

    private String ag3VenueServer = null;

    private String ag3VenueUrl = null;

    private NetworkLocation[] addresses = null;

    private Date startDate = null;

    private Date stopDate = null;

    private String status = STOPPED;

    private boolean recordingStarted = false;

    private boolean recordingFinished = false;

    private String finishedRecordingId = null;

    private String oldFinishedRecordingId = null;

    private String emailAddress = null;

    public UnfinishedRecording() {
        // Does Nothing
    }

    /**
     * Creates a new TimerRecording
     * @param manager The manager used to start and stop the recording
     */
    public UnfinishedRecording(String folder, String id) {
        this.folder = folder;
        this.id = id;
        this.finishedRecordingId = getId();
    }

    /**
     * Gets the unique id
     * @return The id
     */
    @XmlElement
    public String getId() {
        return id;
    }

    /**
     * Returns the metadata
     * @return the metadata
     */
    @XmlElement
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
    @XmlElement
    public String getFolder() {
        return folder;
    }

    public boolean isStarted() {
        return recordingStarted;
    }

    public boolean isFinished() {
        return recordingFinished;
    }

    /**
     * Returns the ag3VenueServer
     * @return the ag3VenueServer
     */
    @XmlElement
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
    @XmlElement
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
    @XmlElement(name="address")
    @XmlJavaTypeAdapter(NetworkLocationAdapter.class)
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
    @XmlElement(name="startDate")
    public String getStartDateString() {
        if (startDate != null) {
            return RecordingConstants.DATE_FORMAT.format(startDate);
        }
        return null;
    }

    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the startDate
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        if (!recordingStarted) {
            oldFinishedRecordingId = finishedRecordingId;
            if (startDate == null) {
                finishedRecordingId = getId();
            } else {
                finishedRecordingId =
                    ID_DATE_FORMAT.format(startDate) + getId();
            }
        }
    }

    public void setStartDateString(String startDate) throws ParseException {
        setStartDate(RecordingConstants.DATE_FORMAT.parse(startDate));
    }

    public boolean hasStarted() {
        return recordingStarted;
    }

    /**
     * Returns the stopDate
     * @return the stopDate
     */
    @XmlElement(name="stopDate")
    public String getStopDateString() {
        if (stopDate != null) {
            return RecordingConstants.DATE_FORMAT.format(stopDate);
        }
        return null;
    }

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

    public void setStopDateString(String stopDate) throws ParseException {
        this.stopDate = RecordingConstants.DATE_FORMAT.parse(stopDate);
    }

    /**
     * Gets the current status of the recording
     * @return The status
     */
    @XmlElement
    public String getStatus() {
        return status;
    }

    /**
     * Starts the recording
     */
    public synchronized void startRecording() {
        recordingStarted = true;
    }

    /**
     * Stops the recording
     */
    public synchronized void stopRecording() {
        recordingFinished = true;
    }

    public void resetRecording() {
        recordingStarted = false;
        recordingFinished = false;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(UnfinishedRecording r) {
        if (startDate != null && r.startDate != null) {
            return startDate.compareTo(r.startDate);
        } else if (startDate != null) {
            return 1;
        } else {
            return -1;
        }
    }

    public String getFinishedRecordingId() {
        return finishedRecordingId;
    }

    public String getOldFinishedRecordingId() {
        return oldFinishedRecordingId;
    }

    @XmlElement
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

}
