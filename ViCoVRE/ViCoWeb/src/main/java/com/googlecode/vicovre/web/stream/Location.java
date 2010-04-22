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

package com.googlecode.vicovre.web.stream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Vector;

import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.NetworkLocation;

import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.media.rtp.RTCPPacketSink;
import com.googlecode.vicovre.media.rtp.RTPPacketSink;

public class Location implements RTPPacketSink, RTCPPacketSink {

    private HashMap<String, Participant> participants
        = new HashMap<String, Participant>();

    private HashMap<Long, Vector<Participant>> ssrcsBeingViewed
        = new HashMap<Long, Vector<Participant>>();

    private BridgedRTPConnector connector = null;

    public Location(NetworkLocation location, BridgeDescription bridge)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {
        connector = new BridgedRTPConnector(bridge,
                new NetworkLocation[]{location});
        connector.setRtcpSink(this);
        connector.setRtpSink(this);
    }

    public void handleRTPPacket(DatagramPacket packet) throws IOException {
        // TODO Auto-generated method stub

    }

    public void handleRTCPPacket(DatagramPacket packet) throws IOException {
        // TODO Auto-generated method stub

    }
}