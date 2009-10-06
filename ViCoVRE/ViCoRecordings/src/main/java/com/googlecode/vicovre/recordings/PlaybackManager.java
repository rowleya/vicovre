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

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.rtcp.SourceDescription;


import ag3.interfaces.Venue;
import ag3.interfaces.types.BridgeDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.StreamDescription;

import com.googlecode.vicovre.media.protocol.memetic.DataSource;
import com.googlecode.vicovre.media.protocol.memetic.StreamStartingListener;
import com.googlecode.vicovre.media.protocol.memetic.StreamStoppingListener;
import com.googlecode.vicovre.media.rtp.BridgedRTPConnector;
import com.googlecode.vicovre.repositories.rtptype.RTPType;

public class PlaybackManager implements StreamStoppingListener,
        StreamStartingListener {

    private static final BridgeDescription CONNECTION = new BridgeDescription();

    private static final HashMap<Integer, PlaybackManager> MANAGERS =
        new HashMap<Integer, PlaybackManager>();

    static {
        CONNECTION.setServerType("Multicast");
    }

    private static int lastId = 0;

    private Recording recording = null;

    private Venue venue = null;

    private ClientProfile clientProfile = new ClientProfile();

    private Capability[] capabilities;

    private RTPType[] types;

    private RTPManager[] managers = null;

    private BridgedRTPConnector connector;

    private SendStream[] sendStreams = null;

    private DataSource datasource = null;

    private HashMap<Integer, SourceDescription>[] sourceDescriptions = null;

    private final int id;

    public static int play(Recording recording,
            String ag3VenueUrl) {
        PlaybackManager manager =
            new PlaybackManager(recording);
        MANAGERS.put(manager.getId(), manager);
        manager.play(ag3VenueUrl);
        return manager.getId();
    }

    public static void stop(int id) {
        PlaybackManager manager = MANAGERS.remove(id);
        if (manager != null) {
            manager.stop();
        }
    }

    public static void pause(int id) {
        PlaybackManager manager = MANAGERS.get(id);
        if (manager != null) {
            manager.pause();
        }
    }

    public static void resume(int id) {
        PlaybackManager manager = MANAGERS.get(id);
        if (manager != null) {
            manager.resume();
        }
    }

    public static void seek(int id, long seek) {
        PlaybackManager manager = MANAGERS.get(id);
        if (manager != null) {
            manager.seek(seek);
        }
    }

    public static int getTime(int id) {
        PlaybackManager manager = MANAGERS.get(id);
        if (manager != null) {
            return manager.getTime();
        }
        return -1;
    }

    private PlaybackManager(Recording recording) {
        id = lastId++;
        this.recording = recording;
        HashSet<Capability> caps = new HashSet<Capability>();
        HashSet<RTPType> rtpTypes = new HashSet<RTPType>();
        List<Stream> streams = recording.getStreams();
        sourceDescriptions = new HashMap[streams.size()];
        for (int i = 0; i < streams.size(); i++) {
            Stream stream = streams.get(i);
            RTPType type = stream.getRtpType();
            rtpTypes.add(type);
            caps.add(getCapability(type));
            HashMap<Integer, SourceDescription> description =
                new HashMap<Integer, SourceDescription>();
            if (stream.getCname() != null) {
                description.put(SourceDescription.SOURCE_DESC_CNAME,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_CNAME,
                        stream.getCname(), 1, false));
            }
            if (stream.getName() != null) {
                description.put(SourceDescription.SOURCE_DESC_NAME,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_NAME,
                        "*R* " + stream.getName(), 3, false));
            }
            if (stream.getEmail() != null) {
                description.put(SourceDescription.SOURCE_DESC_EMAIL,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_EMAIL,
                        stream.getEmail(), 3, false));
            }
            if (stream.getPhone() != null) {
                description.put(SourceDescription.SOURCE_DESC_PHONE,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_PHONE,
                        stream.getPhone(), 3, false));
            }
            if (stream.getLocation() != null) {
                description.put(SourceDescription.SOURCE_DESC_LOC,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_LOC,
                        stream.getLocation(), 3, false));
            }
            if (stream.getTool() != null) {
                description.put(SourceDescription.SOURCE_DESC_TOOL,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_TOOL,
                        stream.getTool(), 3, false));
            }
            if (stream.getNote() != null) {
                description.put(SourceDescription.SOURCE_DESC_NOTE,
                        new SourceDescription(
                        SourceDescription.SOURCE_DESC_NOTE,
                        stream.getNote(), 3, false));
            }
            sourceDescriptions[i] = description;
        }
        capabilities = caps.toArray(new Capability[0]);
        types = rtpTypes.toArray(new RTPType[0]);
    }

    public int getId() {
        return id;
    }

    public int getTime() {
        return (int) datasource.getCurrentTime();
    }

    private Capability getCapability(RTPType type) {
        Capability cap = new Capability();
        cap.setType(type.getMediaType());
        Format format = type.getFormat();
        String codec = format.getEncoding();
        cap.setRole(Capability.PRODUCER);
        if (codec.equals("h264/rtp/iocom")) {
            codec = "H261";
        } else if (codec.equalsIgnoreCase("ULAW/rtp")) {
            codec = "PCMU";
        } else if (codec.endsWith("/rtp")) {
            codec = codec.substring(0, codec.indexOf("/rtp"));
        }
        cap.setCodec(codec.toUpperCase());
        if (format instanceof VideoFormat) {
            cap.setRate(90000);
            cap.setChannels(1);
        } else if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            if (af.getChannels() != -1) {
                cap.setChannels(af.getChannels());
            }
            if (af.getSampleRate() != -1) {
                cap.setRate((int) af.getSampleRate());
            }
        }
        return cap;
    }

    private NetworkLocation findLocationForType(StreamDescription[] streams,
            RTPType type) {
        Capability typeCap = getCapability(type);
        for (StreamDescription stream : streams) {
            for (Capability cap : stream.getCapability()) {
                if (cap.matches(typeCap)) {
                    return stream.getLocation();
                }
            }
        }
        return null;
    }

    private static String getTimeText(long duration) {
        NumberFormat timeFormat = NumberFormat.getIntegerInstance();
        timeFormat.setMinimumIntegerDigits(2);
        long remainder = duration / 1000;
        long hours = remainder / 3600;
        remainder -= hours * 3600;
        long minutes = remainder / 60;
        remainder -= minutes * 60;
        long seconds = remainder;

        return timeFormat.format(hours) + ":"
                + timeFormat.format(minutes) + ":"
                + timeFormat.format(seconds);
    }

    public void play(String ag3VenueUrl) {
        try {
            // Negotiate the streams
            venue = new Venue(ag3VenueUrl);
            String connectionId = venue.enter(clientProfile);
            StreamDescription[] streams =
                venue.negotiateCapabilities(connectionId, capabilities);
            NetworkLocation[] addrs = new NetworkLocation[streams.length];
            for (int i = 0; i < streams.length; i++) {
                addrs[i] = streams[i].getLocation();
            }

            // Create an RTPManager for sending the streams
            connector = new BridgedRTPConnector(CONNECTION, addrs);

            // Create a Datasource for the streams
            List<Stream> strms = recording.getStreams();
            File[] files = new File[strms.size()];
            for (int i = 0; i < strms.size(); i++) {
                files[i] = new File(recording.getDirectory(),
                        strms.get(i).getSsrc());
            }
            datasource = new DataSource(files);
            for (int i = 0; i < strms.size(); i++) {
                datasource.setFormat(i, strms.get(i).getRtpType().getFormat());
            }
            datasource.addStartingListener(this);
            datasource.addStoppingListener(this);

            // Create sendstreams and register them in the connector
            managers = new RTPManager[strms.size()];
            sendStreams = new SendStream[strms.size()];
            for (int i = 0; i < strms.size(); i++) {
                Stream stream = strms.get(i);
                managers[i] = RTPManager.newInstance();
                managers[i].initialize(connector);
                for (RTPType type : types) {
                    managers[i].addFormat(type.getFormat(), type.getId());
                }
                sendStreams[i] = managers[i].createSendStream(datasource, i);
                NetworkLocation location = findLocationForType(streams,
                        stream.getRtpType());
                connector.addStream(sendStreams[i].getSSRC(), location);
                SourceDescription note = sourceDescriptions[i].get(
                        SourceDescription.SOURCE_DESC_NOTE);
                String extra = "(Starts at "
                    + getTimeText(datasource.getStartOffset(i)) + ")";
                if (note == null) {
                    note = new SourceDescription(
                        SourceDescription.SOURCE_DESC_NOTE, extra, 3, false);
                } else {
                    note.setDescription(note.getDescription() + " " + extra);
                }
                sourceDescriptions[i].put(SourceDescription.SOURCE_DESC_NOTE,
                        note);
                sendStreams[i].setSourceDescription(
                        sourceDescriptions[i].values().toArray(
                                new SourceDescription[0]));
                sendStreams[i].start();
            }

            datasource.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        datasource.stop();
    }

    public void resume() {
        datasource.start();
    }

    public void stop() {
        datasource.stop();
        for (SendStream sendStream : sendStreams) {
            try {
                sendStream.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (RTPManager manager : managers) {
            manager.removeTargets("Leaving");
        }
        connector.close();
    }

    public void seek(long seek) {
        datasource.seek(seek, 1.0);
    }

    public void streamStopping(javax.media.protocol.DataSource datasource,
            int stream) {
        try {
            sendStreams[stream].stop();
            sendStreams[stream].close();
            managers[stream].removeTargets("Stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void streamStarting(javax.media.protocol.DataSource datasource,
            int stream) {
        SourceDescription note = sourceDescriptions[stream].get(
                SourceDescription.SOURCE_DESC_NOTE);
        Stream s = recording.getStreams().get(stream);
        if (s.getNote() == null) {
            sourceDescriptions[stream].remove(
                    SourceDescription.SOURCE_DESC_NOTE);
        } else {
            note.setDescription(s.getNote());
        }
        sendStreams[stream].setSourceDescription(
                sourceDescriptions[stream].values().toArray(
                        new SourceDescription[0]));
    }
}
