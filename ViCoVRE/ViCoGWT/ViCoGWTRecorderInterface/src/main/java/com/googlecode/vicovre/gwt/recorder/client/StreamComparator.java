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

package com.googlecode.vicovre.gwt.recorder.client;

import java.util.Comparator;
import java.util.Date;

import com.googlecode.vicovre.gwt.client.json.JSONRecording;
import com.googlecode.vicovre.gwt.client.json.JSONStream;

public class StreamComparator implements Comparator<JSONStream> {

    private static String getStreamName(JSONStream stream) {
        String text = "";
        if (stream.getName() != null) {
            text = stream.getName();
            if (stream.getNote() != null) {
                text += " - " + stream.getNote();
            }
        } else if (stream.getCname() != null) {
            text = stream.getCname();
        } else {
            text = stream.getSsrc();
        }
        return text;
    }

    public int compare(JSONStream s1, JSONStream s2) {
        if ((s1.getCname() == null) && (s2.getCname() == null)) {
            return s1.getSsrc().compareTo(s2.getSsrc());
        } else if (s1.getCname() == null) {
            return 1;
        } else if (s2.getCname() == null) {
            return -1;
        }
        if (!s1.getCname().equals(s2.getCname())) {
            return s1.getCname().compareTo(s2.getCname());
        }
        String s1Name = getStreamName(s1);
        String s2Name = getStreamName(s2);
        if (!s1Name.equals(s2Name)) {
            return s1Name.compareTo(s2Name);
        }
        Date s1Start = JSONRecording.DATE_FORMAT.parse(s1.getStartTime());
        Date s2Start = JSONRecording.DATE_FORMAT.parse(s2.getStartTime());
        return s1Start.compareTo(s2Start);
    }


}
