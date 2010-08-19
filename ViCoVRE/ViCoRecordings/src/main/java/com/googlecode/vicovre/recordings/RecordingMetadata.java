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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Metadata for recordings
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
@XmlRootElement(name="metadata")
@XmlAccessorType(XmlAccessType.NONE)
public class RecordingMetadata implements Comparable<RecordingMetadata> {

    private String primaryKey = null;

    private Vector<String> keys = new Vector<String>();

    private HashMap<String, RecordingMetadataElement> data =
        new HashMap<String, RecordingMetadataElement>();

    public RecordingMetadata(String primaryKey, String primaryValue) {
        this.primaryKey = primaryKey;
        data.put(primaryKey, new RecordingMetadataElement(primaryKey,
                primaryValue, true, true, false));
        keys.add(primaryKey);
    }

    @XmlElement
    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getPrimaryValue() {
        return getValue(primaryKey);
    }

    public List<String> getKeys() {
        return new Vector<String>(keys);
    }

    @XmlElement(name="key")
    public List<RecordingMetadataElement> getElements() {
        Vector<RecordingMetadataElement> elements =
            new Vector<RecordingMetadataElement>();
        for (String key : keys) {
            elements.add(data.get(key));
        }
        return elements;
    }

    private String getActualValue(String key) {
        RecordingMetadataElement element = data.get(key);
        if (element != null) {
            return element.getValue();
        }
        return null;
    }

    public static String getDisplayName(String key) {
        String displayName = "";
        displayName += Character.toUpperCase(key.charAt(0));
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c)) {
                displayName += " ";
                displayName += c;
            } else {
                displayName += c;
            }
        }
        return displayName;
    }

    public static String getKey(String displayName) {
        String key = "";
        key += Character.toLowerCase(displayName.charAt(0));
        for (int i = 1; i < displayName.length(); i++) {
            char c = displayName.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                key += Character.toUpperCase(displayName.charAt(i));
            } else {
                key += c;
            }
        }
        return key;
    }

    public String getValue(String key) {
        String value = getActualValue(key);
        if (value != null) {
            for (String otherKey : data.keySet()) {
                if (!key.equals(otherKey)) {
                    String otherValue = getActualValue(key);
                    if (otherValue != null) {
                        value.replace("${" + otherKey + "}", otherValue);
                    }
                }
            }
        }
        return value;
    }

    public boolean isVisible(String key) {
        return data.get(key).isVisible();
    }

    public boolean isEditable(String key) {
        return data.get(key).isEditable();
    }

    public boolean isMultiline(String key) {
        return data.get(key).isMultiline();
    }

    public void setValue(String key, String value) {
        RecordingMetadataElement element = data.get(key);
        if (element == null) {
            element = new RecordingMetadataElement(key, value);
            data.put(key, element);
            keys.add(key);
        } else if (element.isEditable()) {
            element.setValue(value);
        }
    }

    public void setValueVisible(String key, boolean visible) {
        RecordingMetadataElement element = data.get(key);
        if (element == null) {
            element = new RecordingMetadataElement(key, "");
            data.put(key, element);
            keys.add(key);
        }
        element.setVisible(visible);
    }

    public void setValueEditable(String key, boolean editable) {
        RecordingMetadataElement element = data.get(key);
        if (element == null) {
            element = new RecordingMetadataElement(key, "");
            data.put(key, element);
            keys.add(key);
        }
        element.setEditable(editable);
    }

    public void setValueMultiline(String key, boolean multiline) {
        RecordingMetadataElement element = data.get(key);
        if (element == null) {
            element = new RecordingMetadataElement(key, "");
            data.put(key, element);
            keys.add(key);
        }
        element.setMultiline(multiline);
    }

    public void setValue(String key, String value, boolean visible,
            boolean editable, boolean multiline) {
        data.put(key, new RecordingMetadataElement(key, value, visible,
                editable, multiline));
        if (!keys.contains(key)) {
            keys.add(key);
        }
    }

    public int compareTo(RecordingMetadata m) {
        return getValue(primaryKey).compareTo(m.getValue(m.primaryKey));
    }

}
