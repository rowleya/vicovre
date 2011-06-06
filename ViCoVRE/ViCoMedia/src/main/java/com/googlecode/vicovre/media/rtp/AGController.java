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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.LocalParticipant;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewParticipantEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.ReceiverReportEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SenderReportEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.rtcp.Report;
import javax.media.rtp.rtcp.SourceDescription;

import org.xml.sax.SAXException;

import com.googlecode.onevre.ag.agclient.ClientUpdateThread;
import com.googlecode.onevre.ag.types.BridgeDescription;
import com.googlecode.onevre.ag.types.Capability;
import com.googlecode.onevre.ag.types.ClientProfile;
import com.googlecode.onevre.ag.types.ConnectionDescription;
import com.googlecode.onevre.ag.types.StreamDescription;
import com.googlecode.onevre.ag.types.network.NetworkLocation;
import com.googlecode.onevre.ag.types.server.Venue;
import com.googlecode.onevre.types.soap.exceptions.SoapException;
import com.googlecode.vicovre.media.Misc;

/**
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class AGController {

    private ClientProfile profile = null;

    private Capability[] capabilities = null;

    private BridgeDescription bridge = null;

    private String encryptionKey = null;

    private Venue currentVenue = null;

    private ClientUpdateThread currentVenueUpdater = null;

    private String currentConnectionId = null;

    private Vector<RTPManager> receiveManagers = new Vector<RTPManager>();

    private HashMap<NetworkLocation, BridgedRTPConnector> agConnectors =
        new HashMap<NetworkLocation, BridgedRTPConnector>();

    private HashMap<Capability, NetworkLocation> locations =
        new HashMap<Capability, NetworkLocation>();

    private Vector<UpdateHandler> updateHandlers = new Vector<UpdateHandler>();

    private HashSet<Long> existingSsrcs = new HashSet<Long>();

    private HashMap<Integer, Format> mappedFormats =
        new HashMap<Integer, Format>();

    private StreamListener listener = null;

    private JoinListener joinListener = null;

    private String tool = null;

    /**
     * @param bridge The bridge to connect to
     * @param capabilities The capabilities to connect with
     * @param encryptionKey The encryption key to use
     * @param profile The client profile
     */
    public AGController(BridgeDescription bridge, Capability[] capabilities,
            String encryptionKey, ClientProfile profile, String tool) {
        this.bridge = bridge;
        this.capabilities = capabilities;
        this.encryptionKey = encryptionKey;
        this.profile = profile;
        this.tool = tool;
    }

    /**
     * Maps a format to an RTP Type identifier
     * @param rtpType The RTP Type
     * @param format The format to map to
     */
    public void mapFormat(int rtpType, Format format) {
        mappedFormats.put(rtpType, format);
        if (receiveManagers != null) {
            for (int i = 0; i < receiveManagers.size(); i++) {
                receiveManagers.get(i).addFormat(format, rtpType);
            }
        }
    }

    /**
     * Sets the stream listener
     * @param listener The listener to set
     */
    public void setListener(StreamListener listener) {
        this.listener = listener;
        for (int i = 0; i < updateHandlers.size(); i++) {
            updateHandlers.get(i).setStreamListener(listener);
        }
    }

    public void setJoinListener(JoinListener listener) {
        this.joinListener = listener;
    }

    private void addAllFormats(RTPManager manager) {
        for (int i : mappedFormats.keySet()) {
            manager.addFormat(mappedFormats.get(i), i);
        }
    }

    private long getNewSsrc(long ssrc) {
        while (existingSsrcs.contains(ssrc)) {
            ssrc = (long) (Math.random() * Integer.MAX_VALUE);
        }
        existingSsrcs.add(ssrc);
        return ssrc;
    }

    /**
     * Joins a venue
     * @param venueDescription The venue description
     * @throws SAXException
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws UnsupportedEncryptionException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws SoapException
     */
    public void joinVenue(final ConnectionDescription venueDescription)
            throws IllegalAccessException, IOException,
            ClassNotFoundException, InstantiationException,
            UnsupportedEncryptionException, SoapException {
        leaveCurrentVenue();
        currentVenue = new Venue(
                venueDescription.getUri(), false);
        currentConnectionId = currentVenue.enter(profile);
        currentVenueUpdater = new ClientUpdateThread(
                currentVenue, currentConnectionId);
        StreamDescription[] streams =
            currentVenue.negotiateCapabilities(
                currentConnectionId, capabilities);

        for (int i = 0; i < streams.length; i++) {
            Vector<Capability> streamCaps = streams[i].getCapability();
            boolean matches = false;
            for (int j = 0; (j < streamCaps.size()) && !matches; j++) {
                Capability cap = streamCaps.get(j);
                locations.put(cap, streams[i].getLocation());
                for (int k = 0; (k < capabilities.length) && !matches; k++) {
                    if (cap.matches(capabilities[k])) {
                        matches = true;
                    }
                }
            }
            if (matches) {
                NetworkLocation location = streams[i].getLocation();
                connectToLocation(location);
            }
        }
    }

    public void setLocationCapabilities(NetworkLocation location,
            Capability[] capabilites) {
        for (Capability capability : capabilites) {
            locations.put(capability, location);
        }
    }

    public void connectToLocation(NetworkLocation location)
        throws ClassNotFoundException, InstantiationException,
        IllegalAccessException, IOException, UnsupportedEncryptionException {
        BridgedRTPConnector connector = new BridgedRTPConnector(bridge,
                new NetworkLocation[]{location});
        connector.setEncryption(encryptionKey);
        UpdateHandler handler = new UpdateHandler();
        RTPManager manager = RTPManager.newInstance();
        addAllFormats(manager);
        manager.addReceiveStreamListener(handler);
        manager.addRemoteListener(handler);
        manager.addSessionListener(handler);
        manager.initialize(connector);
        LocalParticipant localParticipant =
            manager.getLocalParticipant();

        localParticipant.setSourceDescription(Misc.createSourceDescription(
                profile, null, tool));

        Report report = (Report) localParticipant.getReports().get(0);
        long localssrc = report.getSSRC();
        if (localssrc < 0) {
            localssrc += Math.pow(2, 32);
        }
        connector.addStream(localssrc, location);

        agConnectors.put(location, connector);
        updateHandlers.add(handler);
        receiveManagers.add(manager);
    }

    public void updateProfile(ClientProfile profile) {
        this.profile = profile;
        for (RTPManager manager : receiveManagers) {
            manager.getLocalParticipant().setSourceDescription(
                    Misc.createSourceDescription(profile, null, tool));
        }
    }

    public void setEncryptionKey(String key)
            throws UnsupportedEncryptionException {
        this.encryptionKey = key;
        for (BridgedRTPConnector connector : agConnectors.values()) {
            connector.setEncryption(key);
        }
    }

    public void setBridge(BridgeDescription bridge)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {
        this.bridge = bridge;
        for (BridgedRTPConnector connector : agConnectors.values()) {
            connector.setBridge(bridge);
        }
    }

    public BridgedRTPConnector getConnectorForCapability(
            Capability capability) {
        NetworkLocation location = locations.get(capability);
        return agConnectors.get(location);
    }

    /**
     * Leaves the current venue
     */
    public void leaveCurrentVenue() {
        if (currentVenueUpdater != null) {
            currentVenueUpdater.close();
            currentVenueUpdater = null;
        }
        if (currentConnectionId != null) {
            try {
                currentVenue.exit(currentConnectionId);
            } catch (Exception e) {
                // Do Nothing
            }
            currentConnectionId = null;
            currentVenue = null;
        }

        if (receiveManagers != null) {
            for (int i = 0; i < receiveManagers.size(); i++) {
                RTPManager manager = receiveManagers.get(i);
                if (manager != null) {
                    manager.removeTargets("Leaving");
                    manager.dispose();
                }
            }
            receiveManagers.clear();
        }

        for (BridgedRTPConnector connector : agConnectors.values()) {
            connector.close();
        }
        agConnectors.clear();
        updateHandlers.clear();
    }

    public void addRTPSink(RTPPacketSink rtpSink) {
        for (BridgedRTPConnector connector : agConnectors.values()) {
            connector.addRtpSink(rtpSink);
        }
    }

    public void addRTCPSink(RTCPPacketSink rtcpSink) {
        for (BridgedRTPConnector connector : agConnectors.values()) {
            connector.addRtcpSink(rtcpSink);
        }
    }

    private class UpdateHandler implements ReceiveStreamListener,
            RemoteListener, SessionListener {

        private HashMap<Long, Stream> streams = new HashMap<Long, Stream>();

        private HashMap<Long, Long> ssrcMap = new HashMap<Long, Long>();

        /**
         * @see javax.media.rtp.ReceiveStreamListener#update(
         *     javax.media.rtp.event.ReceiveStreamEvent)
         */
        public void update(ReceiveStreamEvent event) {
            ReceiveStream stream = event.getReceiveStream();
            if (event instanceof NewReceiveStreamEvent) {
                DataSource ds = stream.getDataSource();
                RTPControl ctl = (RTPControl) ds.getControl(
                        "javax.media.rtp.RTPControl");
                if (ctl != null) {
                    Format format = ctl.getFormat();
                    long realSsrc = event.getReceiveStream().getSSRC();
                    if (realSsrc < 0) {
                        realSsrc = realSsrc
                            + (((long) Integer.MAX_VALUE + 1) * 2);
                    }
                    if (!ssrcMap.containsKey(realSsrc)) {
                        long ssrc = getNewSsrc(realSsrc);
                        ssrcMap.put(realSsrc, ssrc);
                        String name = "Waiting for name (" + ssrc + ")";
                        Stream dataStream = new Stream(ssrc, ds, format);
                        dataStream.setSdes(SourceDescription.SOURCE_DESC_NAME,
                                name);
                        streams.put(realSsrc, dataStream);
                        if (listener != null) {
                            if (format instanceof VideoFormat) {
                                listener.addVideoStream(ssrc, ds,
                                        (VideoFormat) format);
                                listener.setVideoStreamSDES(ssrc,
                                    SourceDescription.SOURCE_DESC_NAME, name);
                            } else if (format instanceof AudioFormat) {
                                listener.addAudioStream(ssrc, ds,
                                        (AudioFormat) format);
                                listener.setAudioStreamSDES(ssrc,
                                    SourceDescription.SOURCE_DESC_NAME, name);
                            }
                        }
                    }
                }
            } else if (event instanceof ByeEvent) {
                if (stream != null) {
                    DataSource ds = stream.getDataSource();
                    RTPControl ctl = (RTPControl) ds.getControl(
                            "javax.media.rtp.RTPControl");
                    if (ctl != null) {
                        Format format = ctl.getFormat();
                        long realSsrc = event.getReceiveStream().getSSRC();
                        if (realSsrc < 0) {
                            realSsrc = realSsrc
                                + (((long) Integer.MAX_VALUE + 1) * 2);
                        }
                        long ssrc = ssrcMap.get(realSsrc);
                        streams.remove(realSsrc);
                        if (format instanceof VideoFormat) {
                            if (listener != null) {
                                listener.removeVideoStream(ssrc);
                            }
                        } else if (format instanceof AudioFormat) {
                            if (listener != null) {
                                listener.removeAudioStream(ssrc);
                            }
                        }
                    }
                }
            }
        }

        /**
         * @see javax.media.rtp.RemoteListener#update(
         *     javax.media.rtp.event.RemoteEvent)
         */
        public void update(RemoteEvent event) {
            if ((event instanceof SenderReportEvent)
                    || (event instanceof ReceiverReportEvent)) {

                // Get the report
                Report report = null;
                if (event instanceof SenderReportEvent) {
                    report = ((SenderReportEvent) event).getReport();
                } else {
                    report = ((ReceiverReportEvent) event).getReport();
                }
                if ((report != null) && (report.getParticipant() != null)) {
                    long realSsrc = report.getSSRC();
                    if (realSsrc < 0) {
                        realSsrc = realSsrc
                            + (((long) Integer.MAX_VALUE + 1) * 2);
                    }
                    if (ssrcMap.containsKey(realSsrc)) {
                        long ssrc = ssrcMap.get(realSsrc);
                        Vector< ? > sdes =
                            report.getSourceDescription();
                        Stream stream = streams.get(realSsrc);
                        if ((sdes != null) && (stream != null)) {
                            for (int i = 0; i < sdes.size(); i++) {
                                SourceDescription d =
                                    (SourceDescription) sdes.get(i);
                                if (listener != null) {
                                    if (stream.format instanceof VideoFormat) {
                                        listener.setVideoStreamSDES(ssrc,
                                             d.getType(), d.getDescription());
                                    } else if (stream.format
                                            instanceof AudioFormat) {
                                        listener.setAudioStreamSDES(ssrc,
                                             d.getType(), d.getDescription());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Sets the stream listener
         * @param listener The listener
         */
        public void setStreamListener(StreamListener listener) {
            if (listener != null) {
                for (Stream stream : streams.values()) {
                    if (stream.format instanceof VideoFormat) {
                        listener.addVideoStream(stream.ssrc, stream.dataSource,
                                (VideoFormat) stream.format);
                        stream.setVideoStreamSdes(listener);
                    } else if (stream.format instanceof AudioFormat) {
                        listener.addAudioStream(stream.ssrc, stream.dataSource,
                                (AudioFormat) stream.format);
                        stream.setAudioStreamSdes(listener);
                    }
                }
            }
        }

        /**
         *
         * @see javax.media.rtp.SessionListener#update(
         *     javax.media.rtp.event.SessionEvent)
         */
        public void update(SessionEvent event) {
            if (event instanceof NewParticipantEvent) {
                if (joinListener != null) {
                    Participant participant =
                        ((NewParticipantEvent) event).getParticipant();
                    joinListener.participantJoined((SourceDescription[])
                            participant.getSourceDescription().toArray(
                                    new SourceDescription[0]));
                }
            }
        }
    }

    private class Stream {

        private long ssrc = 0;

        private DataSource dataSource = null;

        private Format format = null;

        private HashMap<Integer, String> sdes = new HashMap<Integer, String>();

        private Stream(long ssrc, DataSource dataSource, Format format) {
            this.ssrc = ssrc;
            this.dataSource = dataSource;
            this.format = format;
        }

        private void setSdes(int item, String value) {
            sdes.put(item, value);
        }

        private void setVideoStreamSdes(StreamListener listener) {
            for (int item : sdes.keySet()) {
                listener.setVideoStreamSDES(ssrc, item, sdes.get(item));
            }
        }

        private void setAudioStreamSdes(StreamListener listener) {
            for (int item : sdes.keySet()) {
                listener.setAudioStreamSDES(ssrc, item, sdes.get(item));
            }
        }
    }
}
