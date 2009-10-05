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

package com.googlecode.vicovre.recordings.formats;

/**
 * Metadata for MAGIC lectures
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class MAGICExtraEventMetadata extends MAGICMetadata {

    private String organiserName = null;

    private String speakerName = null;

    /**
     * Returns the organiserName
     * @return the organiserName
     */
    public String getOrganiserName() {
        return organiserName;
    }

    /**
     * Sets the organiserName
     * @param organiserName the organiserName to set
     */
    public void setOrganiserName(String organiserName) {
        this.organiserName = organiserName;
    }

    /**
     * Returns the speakerName
     * @return the speakerName
     */
    public String getSpeakerName() {
        return speakerName;
    }

    /**
     * Sets the speakerName
     * @param speakerName the speakerName to set
     */
    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    /**
     *
     * @see com.googlecode.vicovre.recordings.RecordingMetadata#getDescription()
     */
    public String getDescription() {
        String description = "";
        description += "<b><a target='_blank' href='" + getUrl()
            + "'>Extra Event: " + speakerName + "</a></b>" + "\n";
        description += getType().substring(0, 1).toUpperCase()
            + getType().substring(1)
            + " organised by " + organiserName + " at " + getLocation();
        return description;
    }
}
