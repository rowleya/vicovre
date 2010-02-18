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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringEscapeUtils;

import com.googlecode.vicovre.repositories.liveAnnotation.LiveAnnotationType;
import com.googlecode.vicovre.repositories.liveAnnotation.LiveAnnotationTypeRepository;

public class LiveAnnotation {

    public static final String PUBLIC = "public";

    public static final String PRIVATE = "private";

    private Map<String, String> body = new HashMap<String, String>();

    private long timestamp = 0;

    private String type = null;

    private String author = null;

    private String privacy = PUBLIC;

    private String id = null;

    private LiveAnnotationType liveAnnotationType = null;

    public LiveAnnotation(
            LiveAnnotationTypeRepository liveAnnotationTypeRepository,
            Map<String, List<String>> parameters) {
        type = parameters.get("type").get(0);
        author = parameters.get("author").get(0);
        if (parameters.get("privacy") != null) {
            privacy = parameters.get("privacy").get(0);
        }

        if (parameters.get("timestamp") != null) {
            String timestampString = parameters.get("timestamp").get(0);
            timestamp = Long.parseLong(timestampString);
        } else {
            timestamp = System.currentTimeMillis();
        }

        if (parameters.get("id") != null) {
            id = parameters.get("id").get(0);
        } else {
            id = timestamp + "_" + UUID.randomUUID().toString();
        }

        liveAnnotationType =
            liveAnnotationTypeRepository.findLiveAnnotationType(type);

        List<String> bodyItems = liveAnnotationType.getFields();
        int noMissing = 0;
        for (String item : bodyItems) {
            String value = parameters.get(item).get(0);
            if (value != null) {
                body.put(item, value);
            } else {
                noMissing += 1;
            }
        }

        if (noMissing > 0) {
            int minMissing = noMissing;
            List<String> convertableTypes = liveAnnotationType.getConversions();
            for (int i = 0; (i < convertableTypes.size())
                    && (noMissing > 0); i++) {
                String convertableType = convertableTypes.get(i);
                LiveAnnotationType conversion =
                    liveAnnotationTypeRepository.findLiveAnnotationType(
                            convertableType);
                noMissing = 0;
                for (String item : conversion.getFields()) {
                    if (!body.containsKey(item)) {
                        noMissing += 1;
                    }
                }
                if (noMissing < minMissing) {
                    minMissing = noMissing;
                    type = convertableType;
                    liveAnnotationType = conversion;
                }
            }
        }
    }

    public LiveAnnotation(LiveAnnotationType liveAnnotationType) {
        this.liveAnnotationType = liveAnnotationType;
        this.type = liveAnnotationType.getName();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAuthor(String source) {
        this.author = source;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = Long.parseLong(timestamp);
    }

    public void setValueOf(String name, String value) {
        String methodName = "set" + name.substring(0, 1).toUpperCase()
                + name.substring(1);
        try {
            Method method = getClass().getMethod(methodName, String.class);
            method.invoke(this, new Object[] {value});
        } catch (NoSuchMethodException e) {
            body.put(name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPrivacy() {
        return privacy;
    }

    public String getThumbnail() {
        return liveAnnotationType.getThumbnail();
    }

    public String toXml() {
        String out = "<annotation>";
        out += "<type>" + type + "</type>";
        out += "<id>" + StringEscapeUtils.escapeXml(id) + "</id>";
        out += "<author>" + StringEscapeUtils.escapeXml(author) + "</author>";
        out += "<privacy>" + privacy + "</privacy>";
        out += "<timestamp>" + timestamp + "</timestamp>";
        Iterator<String> bodyKeys = body.keySet().iterator();
        while (bodyKeys.hasNext()) {
            String key = bodyKeys.next();
            out += "<" + key + ">"
                    + StringEscapeUtils.escapeXml(
                            body.get(key).replaceAll("\n", " "))
                    + "</" + key + ">";
        }
        out += "</annotation>";
        return out;
    }

    public String getToolText() {
        return liveAnnotationType.formatAnnotation("input", body);
    }

    public boolean equals(LiveAnnotation annotation) {
        return id.equals(annotation.id);
    }

    public int hashCode() {
        return id.hashCode();
    }

}
