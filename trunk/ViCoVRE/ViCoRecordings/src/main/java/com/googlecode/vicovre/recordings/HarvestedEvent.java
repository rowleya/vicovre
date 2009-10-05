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

import java.util.Date;

import com.googlecode.vicovre.repositories.harvestFormat.HarvestedItem;

/**
 * Represents an event that has been harvested
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class HarvestedEvent extends HarvestedItem {

    private RecordingMetadata metadata = null;

    private Date startDate = null;

    private Date endDate = null;

    /**
     * Gets the start date of the event
     * @return The start date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Gets the end date of the event
     * @return The end date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Gets the metadata of the event
     * @return The metadata
     */
    public RecordingMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the start date of the event
     * @param date The start date
     */
    public void setStartDate(Date date) {
        this.startDate = date;
    }

    /**
     * Sets the end date of the event
     * @param date The end date
     */
    public void setEndDate(Date date) {
        this.endDate = date;
    }

    /**
     * Sets the metadata of the event
     * @param metadata The metadata
     */
    public void setMetadata(RecordingMetadata metadata) {
        this.metadata = metadata;
    }
}
