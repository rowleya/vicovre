/*
 * @(#)RTCPReport.java
 * Created: 02-Dec-2005
 * Version: 1-1-alpha3
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
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
 */

package com.googlecode.vicovre.media.rtp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.media.rtp.rtcp.SourceDescription;

/**
 * Represents an RTCP Report
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public abstract class RTCPReport {

    /**
     * The number of bytes in a word
     */
    public static final int WORD_TO_BYTE_MULTIPLIER = 4;

    // The mask to convert a byte to an int
    private static final int BYTE_TO_INT_MASK = 0xFF;

    // The header of the report
    private RTCPHeader header;

    // The vector of feedback reports in this report
    private Vector<RTCPFeedback> feedbackReports = new Vector<RTCPFeedback>();

    // The source descriptions in the report
    private Vector<SourceDescription> sourceDescriptions =
        new Vector<SourceDescription>();

    // The number of bytes of SDES packets read
    private int sdesBytes = 0;

    // The CNAME of the source
    private String cName = null;

    // True if this is a bye event
    private boolean isBye = false;

    // The reason for leaving if this is a bye packet
    private String byeReason = "";

    // The ssrc of the report
    private long ssrc = 0;

    /**
     * Creates a new RTCPReport
     * @param data The data of the report
     * @param offset The offset in the data where the report starts
     * @param length The length of the report in the data
     * @throws IOException
     */
    public RTCPReport(byte data[], int offset, int length) throws IOException {
        header = new RTCPHeader(data, offset, length);
        this.ssrc = header.getSsrc();
        if (header.isPadding()) {
            throw new IOException("First packet has padding");
        } else if (((header.getLength() + 1)
                * WORD_TO_BYTE_MULTIPLIER) >= length) {
            throw new IOException("Invalid Length");
        }
    }

    /**
     * Reads feedback reports from the data
     * @param data The data to read the feedback reports from
     * @param offset The offset into the data where the reports start
     * @param length The length of the data
     * @throws IOException
     */
    protected void readFeedbackReports(byte data[], int offset, int length)
            throws IOException {
        for (int i = 0; i < header.getReceptionCount(); i++) {
            RTCPFeedback feedback = new RTCPFeedback(data, offset, length);
            feedbackReports.add(feedback);
            offset += RTCPFeedback.SIZE;
        }
    }

    /**
     * Reads the source description from the data
     * @param data The data to read the source description from
     * @param offset The offset into the data where the SDES packet starts
     * @param length The length of the data
     * @throws IOException
     */
    protected void readSourceDescription(byte data[], int offset, int length)
            throws IOException {
        if (length > 0) {
            // Only do this if there is an SDES header
            RTCPHeader sdesHeader = new RTCPHeader(data, offset, length);

            if (sdesHeader.getPacketType() == RTCPHeader.PT_SDES) {
                ssrc = sdesHeader.getSsrc();
                sdesBytes = (sdesHeader.getLength() + 1)
                    * WORD_TO_BYTE_MULTIPLIER;
                DataInputStream stream =
                    new DataInputStream(new ByteArrayInputStream(
                        data, offset + RTCPHeader.SIZE, length));
                int type = SourceDescription.SOURCE_DESC_CNAME;
                while (type != 0) {
                    type = stream.readUnsignedByte();
                    if (type != 0) {
                        int len = stream.readUnsignedByte();
                        byte[] desc = new byte[len];
                        stream.readFully(desc);
                        String descStr = new String(desc, "UTF-8");
                        SourceDescription description =
                            new SourceDescription(type, descStr, 0, false);
                        sourceDescriptions.add(description);
                        if (type == SourceDescription.SOURCE_DESC_CNAME) {
                            cName = descStr;
                        }
                    }
                }
            }
        }
    }

    protected void readBye(byte data[], int offset, int length)
            throws IOException {
        if (length > 0) {
            RTCPHeader sdesHeader = new RTCPHeader(data, offset, length);
            if (sdesHeader.getPacketType() == RTCPHeader.PT_BYE) {
                isBye = true;
                if (((sdesHeader.getLength() + 1) * WORD_TO_BYTE_MULTIPLIER)
                        > RTCPHeader.SIZE) {
                    int len = data[offset + RTCPHeader.SIZE] & BYTE_TO_INT_MASK;
                    if ((len < (length - RTCPHeader.SIZE)) && (len > 0)) {
                        byeReason = new String(data, offset + RTCPHeader.SIZE
                                + 1, len);
                    }
                }
            }
        }
    }

    /**
     *
     * @see javax.media.rtp.rtcp.Report#getSSRC()
     */
    public long getSSRC() {
        return ssrc;
    }

    /**
     *
     * @see javax.media.rtp.rtcp.Report#getFeedbackReports()
     */
    public Vector<RTCPFeedback> getFeedbackReports() {
        return feedbackReports;
    }

    /**
     *
     * @see javax.media.rtp.rtcp.Report#getSourceDescription()
     */
    public Vector<SourceDescription> getSourceDescription() {
        return sourceDescriptions;
    }

    /**
     * Gets the cName of the source of the report
     * @return the cName, or null if none sent
     */
    public String getCName() {
        return cName;
    }

    /**
     * Returns true if a bye packet was added to the report
     * @return true if there is a bye packet
     */
    public boolean isByePacket() {
        return isBye;
    }

    /**
     * Returns the reason for the bye
     * @return The bye reason
     */
    public String getByeReason() {
        return byeReason;
    }

    protected RTCPHeader getHeader() {
        return header;
    }

    protected int getSdesBytes() {
        return sdesBytes;
    }
}
