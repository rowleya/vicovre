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

package com.googlecode.vicovre.web.convert;

import java.io.IOException;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;

import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;
import ag3.interfaces.types.ConnectionDescription;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.media.rtp.AGController;
import com.googlecode.vicovre.media.rtp.StreamListener;

public class StreamReceiver implements StreamListener {

    private static final Capability VIDEO_CAPABILITY =
        new Capability(Capability.PRODUCER, Capability.VIDEO, "H261",
            Capability.VIDEO_RATE, 1);

    private static final Capability AUDIO_CAPABILITY =
        new Capability(Capability.PRODUCER, Capability.AUDIO, "L16",
            Capability.AUDIO_16KHZ, 1);

    private ClientProfile clientProfile = new ClientProfile();

    private BridgeDescription bridge = new BridgeDescription();

    private AGController agController = null;

    private ConvertSession session = null;

    public StreamReceiver(ConvertSession session, String venue)
            throws Exception {
        init(session);
        ConnectionDescription connection = new ConnectionDescription();
        connection.setUri(venue);
        agController.joinVenue(connection);
    }

    public StreamReceiver(ConvertSession session, String address, int port)
            throws Exception {
        init(session);
        UnicastNetworkLocation location = new UnicastNetworkLocation();
        agController.connectToLocation(location);
    }

    private void init(ConvertSession session) {
        this.session = session;
        bridge.setName("Multicast");
        bridge.setServerType("multicast");
        agController = new AGController(bridge,
                new Capability[]{AUDIO_CAPABILITY, VIDEO_CAPABILITY}, null,
                clientProfile, "ViCoVRE");
        agController.setListener(this);
    }

    public void close() {
        agController.leaveCurrentVenue();
    }

    public void addAudioStream(long ssrc, DataSource dataSource,
            AudioFormat format) {
        try {
            session.addStream(dataSource, String.valueOf(ssrc));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addVideoStream(long ssrc, DataSource dataSource,
            VideoFormat format) {
        try {
            session.addStream(dataSource, String.valueOf(ssrc));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeAudioStream(long ssrc) {
        // Does Nothing
    }

    public void removeVideoStream(long ssrc) {
        // Does Nothing
    }

    public void setAudioStreamSDES(long ssrc, int item, String value) {
        // Does Nothing
    }

    public void setVideoStreamSDES(long ssrc, int item, String value) {
        // Does Nothing
    }



}
