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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;

@XmlRootElement(name="event")
@XmlAccessorType(XmlAccessType.NONE)
public class CritterEvent {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "(\\d+)-(\\d+)-(.*)-(.*)");

    private String id = null;

    private long startTime = 0;

    private long endTime = 0;

    private String name = null;

    private String creator = null;

    private String tag = null;

    public CritterEvent() {
        // Does Nothing
    }

    public CritterEvent(String id, String tag) {
        this.id = id;
        this.tag = tag;
        Matcher matcher = ID_PATTERN.matcher(id);
        startTime = Long.parseLong(matcher.group(1)) * 1000;
        endTime = Long.parseLong(matcher.group(2)) * 1000;
        creator = matcher.group(3);
        name = matcher.group(4);
    }

    @XmlElement
    public String getId() {
        return id;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @XmlElement(name="startTime")
    public String getStartTimeString() {
        return RecordingConstants.DATE_FORMAT.format(new Date(startTime));
    }

    @XmlElement(name="endTime")
    public String getEndTimeString() {
        return RecordingConstants.DATE_FORMAT.format(new Date(endTime));
    }

    @XmlElement
    public String getCreator() {
        return creator;
    }

    @XmlElement
    public String getTag() {
        return tag;
    }
}
