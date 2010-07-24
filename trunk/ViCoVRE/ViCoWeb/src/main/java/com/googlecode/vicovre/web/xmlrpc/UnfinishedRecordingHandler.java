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

package com.googlecode.vicovre.web.xmlrpc;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.Emailer;

public class UnfinishedRecordingHandler extends AbstractHandler {

    private RtpTypeRepository typeRepository = null;

    private Emailer emailer = null;

    public UnfinishedRecordingHandler(RecordingDatabase database,
            RtpTypeRepository typeRepository, Emailer emailer) {
        super(database);
        this.typeRepository = typeRepository;
        this.emailer = emailer;
    }

    private void fillIn(UnfinishedRecording recording,
            Map<String, Object> details) throws XmlRpcException {

        recording.setStartDate((Date) details.get("startDate"));
        recording.setStopDate((Date) details.get("stopDate"));

        String ag3VenueServer = (String) details.get("ag3VenueServer");
        Object[] addresses = (Object[]) details.get("addresses");
        if (ag3VenueServer != null) {
            String ag3VenueUrl = (String) details.get("ag3VenueUrl");
            if (ag3VenueUrl == null) {
                throw new XmlRpcException("Missing ag3VenueUrl");
            }
            recording.setAg3VenueServer(ag3VenueServer);
            recording.setAg3VenueUrl(ag3VenueUrl);
        } else if (addresses != null) {
            NetworkLocation[] locations = new NetworkLocation[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                Map<String, Object> address =
                    (Map<String, Object>) addresses[i];
                Integer ttl = (Integer) address.get("ttl");
                if (ttl != null) {
                    MulticastNetworkLocation location =
                        new MulticastNetworkLocation();
                    location.setTtl(ttl);
                    locations[i] = location;
                } else {
                    locations[i] = new UnicastNetworkLocation();
                }
                String host = (String) address.get("host");
                if (host == null) {
                    throw new XmlRpcException("Missing host of address " + i);
                }
                locations[i].setHost(host);
                Integer port = (Integer) address.get("port");
                if (port == null) {
                    throw new XmlRpcException("Missing port of address " + i);
                }
                locations[i].setPort(port);
            }
            recording.setAddresses(locations);
        } else {
            throw new XmlRpcException("Missing ag3VenueServer or addresses");
        }
    }

    public String addUnfinishedRecording(String folderPath,
            Map<String, Object> details) throws XmlRpcException {
        Folder folder = getFolder(folderPath);

        try {
            File file = File.createTempFile("recording",
                    RecordingConstants.UNFINISHED_RECORDING_INDEX,
                    folder.getFile());
            UnfinishedRecording recording = new UnfinishedRecording(typeRepository,
                    folder, file, getDatabase(), emailer);
            fillIn(recording, details);
            RecordingMetadata metadata = new RecordingMetadata();
            fillIn(metadata, (Map<String, Object>)
                    details.get("metadata"));
            recording.setMetadata(metadata);
            getDatabase().addUnfinishedRecording(recording, null);

            return recording.getId();
        } catch (IOException e) {
            e.printStackTrace();
            throw new XmlRpcException("Error adding recording: "
                    + e.getMessage());
        }
    }

    public Boolean updateUnfinishedRecording(String folderPath, String id,
            Map<String, Object> details) throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Unknown id " + id);
        }
        fillIn(recording, details);
        fillIn(recording.getMetadata(),
                (Map<String, Object>) details.get("metadata"));
        try {
            getDatabase().updateUnfinishedRecording(recording);
        } catch (IOException e) {
            e.printStackTrace();
            throw new XmlRpcException("Error updating recording: "
                    + e.getMessage());
        }
        return true;
    }

    public Boolean deleteUnfinishedRecording(String folderPath, String id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Unknown id " + id);
        }
        try {
            getDatabase().deleteUnfinishedRecording(recording);
        } catch (IOException e) {
            throw new XmlRpcException("Could not delete recording: "
                    + e.getMessage());
        }
        return true;
    }

    public Map<String, Object>[] getUnfinishedRecordings(String folderPath)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        List<UnfinishedRecording> recordings = folder.getUnfinishedRecordings();
        Map<String, Object>[] recs = new Map[recordings.size()];
        for (int i = 0; i < recordings.size(); i++) {
            UnfinishedRecording rec = recordings.get(i);
            recs[i] = new HashMap<String, Object>();
            recs[i].put("id", rec.getId());
            recs[i].put("metadata", getDetails(rec.getMetadata()));
            if (rec.getStartDate() != null) {
                recs[i].put("startDate", rec.getStartDate());
            }
            if (rec.getStopDate() != null) {
                recs[i].put("stopDate", rec.getStopDate());
            }
            recs[i].put("status", rec.getStatus());
            if (rec.getAg3VenueServer() != null) {
                recs[i].put("ag3VenueServer", rec.getAg3VenueServer());
                recs[i].put("ag3VenueUrl", rec.getAg3VenueUrl());
            } else {
                NetworkLocation[] locations = rec.getAddresses();
                Map<String, Object>[] addresses = new Map[locations.length];
                for (int j = 0; j < addresses.length; j++) {
                    addresses[j] = new HashMap<String, Object>();
                    if (locations[j] instanceof MulticastNetworkLocation) {
                        addresses[j].put("ttl", ((MulticastNetworkLocation)
                                locations[j]).getTtl());
                    }
                    addresses[j].put("host", locations[j].getHost());
                    addresses[j].put("port", locations[j].getPort());
                }
                recs[i].put("addresses", addresses);
            }
        }
        return recs;
    }

    public String startRecording(String folderPath, String id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Unknown recording id " + id);
        }
        recording.startRecording();
        return recording.getStatus();
    }

    public String stopRecording(String folderPath, String id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Unknown recording id " + id);
        }
        recording.stopRecording();
        return recording.getStatus();
    }

    public String pauseRecording(String folderPath, String id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Unknown recording id " + id);
        }
        recording.pauseRecording();
        return recording.getStatus();
    }

    public String resumeRecording(String folderPath, String id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Unknown recording id " + id);
        }
        recording.resumeRecording();
        return recording.getStatus();
        }
}
