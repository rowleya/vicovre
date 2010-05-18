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
import java.net.InetAddress;

import javax.media.Format;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceStream;
import javax.media.rtp.RTPConnector;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;

import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;
import ag3.interfaces.types.ConnectionDescription;
import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.processor.SimpleProcessor;
import com.googlecode.vicovre.media.rtp.AGController;
import com.googlecode.vicovre.repositories.rtptype.RTPType;

public class TransmitStream {

    private static final Capability VIDEO_CAPABILITY =
        new Capability(Capability.PRODUCER, Capability.VIDEO, "H261",
            Capability.VIDEO_RATE, 1);

    private static final Capability AUDIO_CAPABILITY =
        new Capability(Capability.PRODUCER, Capability.AUDIO, "L16",
            Capability.AUDIO_16KHZ, 1);

    private DataSource dataSource = null;

    private int stream = 0;

    private ClientProfile clientProfile = new ClientProfile();

    private BridgeDescription bridge = new BridgeDescription();

    private AGController agController = null;

    private RTPManager rtpManager = null;

    private SimpleProcessor processor = null;

    private SendStream sendStream = null;

    private String note = null;

    public TransmitStream(DataSource dataSource, int stream) {
        this.dataSource = dataSource;
        this.stream = stream;
        bridge.setName("Multicast");
        bridge.setServerType("multicast");
        agController = new AGController(bridge,
                new Capability[]{AUDIO_CAPABILITY, VIDEO_CAPABILITY}, null,
                clientProfile, "ViCoVRE");
    }

    public void setName(String name) {
        clientProfile.setName(name);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void transmit(String address, int port, int ttl, RTPType rtpType,
            Format format)
            throws Exception {
        InetAddress addr = InetAddress.getByName(address);
        NetworkLocation location = null;
        if (addr.isMulticastAddress()) {
            MulticastNetworkLocation mcLocation =
                new MulticastNetworkLocation();
            mcLocation.setTtl(ttl);
            location = mcLocation;
        } else {
            location = new UnicastNetworkLocation();
        }

        location.setHost(address);
        location.setPort(port);

        agController.setLocationCapabilities(location,
                new Capability[]{VIDEO_CAPABILITY, AUDIO_CAPABILITY});
        agController.connectToLocation(location);
        transmit(rtpType, format);
    }

    public void transmit(String venue, RTPType rtpType, Format format)
            throws Exception {
        ConnectionDescription connection = new ConnectionDescription();
        connection.setUri(venue);
        agController.joinVenue(connection);
        transmit(rtpType, format);
    }

    private void transmit(RTPType rtpType, Format format)
            throws IOException, UnsupportedFormatException {
        RTPConnector rtpConnector = null;
        if (rtpType.getFormat() instanceof VideoFormat) {
            rtpConnector = agController.getConnectorForCapability(
                    VIDEO_CAPABILITY);
        } else {
            rtpConnector = agController.getConnectorForCapability(
                    AUDIO_CAPABILITY);
        }

        Format fmt = format;
        if (fmt == null) {
            fmt = rtpType.getFormat();
        }

        dataSource.start();
        if (dataSource instanceof PullBufferDataSource) {
            PullBufferStream sourceStream = ((PullBufferDataSource)
                    dataSource).getStreams()[stream];
            processor = new SimpleProcessor(sourceStream.getFormat(), fmt);
        } else if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream sourceStream = ((PushBufferDataSource)
                    dataSource).getStreams()[stream];
            processor = new SimpleProcessor(sourceStream.getFormat(), fmt);
        } else {
            throw new IOException("Can only read buffer DataSource");
        }

        rtpManager = RTPManager.newInstance();
        rtpManager.addFormat(fmt, rtpType.getId());
        rtpManager.initialize(rtpConnector);

        DataSource data = processor.getDataOutput(dataSource, stream);
        sendStream = rtpManager.createSendStream(data, 0);
        sendStream.setSourceDescription(Misc.createSourceDescription(
                clientProfile, note, "ViCoVRE"));

        sendStream.start();
        processor.start(dataSource, stream);
    }

    public void stop() {
        dataSource.disconnect();
        processor.stop();
        sendStream.close();
        rtpManager.removeTargets("Leaving");
        rtpManager.dispose();
    }

}
