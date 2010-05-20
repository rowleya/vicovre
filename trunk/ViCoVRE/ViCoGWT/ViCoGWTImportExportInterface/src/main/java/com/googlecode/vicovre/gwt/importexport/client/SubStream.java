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

package com.googlecode.vicovre.gwt.importexport.client;

import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public abstract class SubStream {

    private String id = null;

    private Stream stream = null;

    private String transmitId = null;

    public SubStream(JSONValue idValue, Stream stream,
            JSONValue transmitIdValue) {
        this.stream = stream;
        if (idValue != null) {
            JSONString string = idValue.isString();
            if (string != null) {
                id = string.stringValue();
            }
        }
        if (transmitIdValue != null) {
            JSONString string = transmitIdValue.isString();
            if (string != null) {
                transmitId = string.stringValue();
            }
        }
    }

    public String getIdString() {
        if (id == null) {
            return "Unknown ID!";
        }
        return id;
    }

    public String getIdValue() {
        return id;
    }

    public abstract String getType();

    public Stream getStream() {
        return stream;
    }

    public String getTransmitId() {
        return transmitId;
    }

    public void setTransmitId(String transmitId) {
        this.transmitId = transmitId;
    }

}
