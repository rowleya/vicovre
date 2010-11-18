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

package com.googlecode.vicovre.media.rtp;

public class RTCPSDES {

    private static final int SDES_CNAME = 1;

    private static final int SDES_NAME = 2;

    private static final int SDES_EMAIL = 3;

    private static final int SDES_PHONE = 4;

    private static final int SDES_LOC = 5;

    private static final int SDES_TOOL = 6;

    private static final int SDES_NOTE = 7;

    private String cname = null;

    private String name = null;

    private String email = null;

    private String phone = null;

    private String location = null;

    private String tool = null;

    private String note = null;

    public RTCPSDES(byte[] data, int offset, int len) {

        // The SDES items have the following format:
        // 1st 8 bits: SDES item id
        // 2nd 8 bits: item length in bytes
        // following bits: n bytes of data
        short length = 0;
        int curptr = offset;
        byte[] pSDES = data;

        while (pSDES[curptr] != 0) {
            int id = pSDES[curptr];
            int itemStart = curptr + RTCPHeader.SDES_LENGTH_LENGTH
                + RTCPHeader.SDES_TYPE_LENGTH;
            length = (short) (pSDES[curptr
                + RTCPHeader.SDES_TYPE_LENGTH] & 0xFF);
            String value = new String(pSDES, itemStart, length);
            switch (id) {
            case SDES_CNAME:
                cname = value;
                break;

            case SDES_NAME:
                name = value;
                break;

            case SDES_EMAIL:
                email = value;
                break;

            case SDES_PHONE:
                phone = value;
                break;

            case SDES_LOC:
                location = value;
                break;

            case SDES_TOOL:
                tool = value;
                break;

            case SDES_NOTE:
                note = value;
                break;

            default:
                break;
            }
            curptr += length + RTCPHeader.SDES_LENGTH_LENGTH
                + RTCPHeader.SDES_TYPE_LENGTH;
        }

    }

    /**
     * Returns the cname
     * @return the cname
     */
    public String getCname() {
        return cname;
    }

    /**
     * Returns the name
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the email
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the phone
     * @return the phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Returns the location
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the tool
     * @return the tool
     */
    public String getTool() {
        return tool;
    }

    /**
     * Returns the note
     * @return the note
     */
    public String getNote() {
        return note;
    }
}
