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

package com.googlecode.vicovre.annotations;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="annotation")
@XmlAccessorType(XmlAccessType.NONE)
public class Annotation {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss");

    private long timestamp = 0;

    private String author = null;

    private boolean isPublic = true;

    private String id = null;

    private String[] tags = null;

    private String[] people = null;

    private String message = null;

    private String responseTo = null;

    public Annotation() {
        // Does Nothing
    }

    public Annotation(long timestamp,
            String author, String message, String[] tags, String[] people,
            String responseTo) {
        this(UUID.randomUUID().toString(), timestamp, author, message, tags,
                people, responseTo);
    }

    public Annotation(String id, long timestamp,
            String author, String message, String[] tags, String[] people,
            String responseTo) {
        this.id = id;
        this.timestamp = timestamp;
        this.author = author;
        this.tags = tags;
        this.message = message;
        this.people = people;
        this.responseTo = responseTo;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    @XmlElement
    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @XmlElement(name="timestamp")
    public String getTimestampString() {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    @XmlElement
    public boolean isPublic() {
        return isPublic;
    }

    @XmlElement(name="tag")
    public String[] getTags() {
        return tags;
    }

    @XmlElement(name="person")
    public String[] getPeople() {
        return people;
    }

    @XmlElement
    public String getMessage() {
        return message;
    }

    @XmlElement
    public String getResponseTo() {
        return responseTo;
    }

    public boolean equals(Annotation annotation) {
        return id.equals(annotation.id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public boolean hasTag(String tag) {
        if (tags == null) {
            return false;
        }
        for (String testTag : tags) {
            if (testTag.equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }

}
