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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="key")
public class MetadataElement {

    private String name = null;

    private String value = null;

    private boolean visible = true;

    private boolean editable = true;

    private boolean multiline = false;

    public MetadataElement() {
        // Does Nothing
    }

    public MetadataElement(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public MetadataElement(String name, String value, boolean visible,
            boolean editable, boolean multiline) {
        this(name, value);
        this.visible = visible;
        this.editable = editable;
        this.multiline = multiline;
    }


    public void setValue(String value) {
        this.value = value;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public String getValue() {
        return value;
    }

    @XmlElement
    public boolean isEditable() {
        return editable;
    }

    @XmlElement
    public boolean isVisible() {
        return visible;
    }

    @XmlElement
    public boolean isMultiline() {
        return multiline;
    }
}
