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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormat;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

public class HarvestHandler extends AbstractHandler {

    private HarvestFormatRepository harvestFormatRepository = null;

    private RtpTypeRepository typeRepository = null;

    public HarvestHandler(RecordingDatabase recordingDatabase,
            HarvestFormatRepository harvestFormatRepository,
            RtpTypeRepository typeRepository) {
        super(recordingDatabase);
        this.harvestFormatRepository = harvestFormatRepository;
        this.typeRepository = typeRepository;
    }

    private void fillIn(HarvestSource harvestSource,
            Map<String, Object> details) throws XmlRpcException {
        String name = (String) details.get("name");
        if (name == null) {
            throw new XmlRpcException("Missing name");
        }
        harvestSource.setName(name);

        String url = (String) details.get("url");
        if (url == null) {
            throw new XmlRpcException("Missing url");
        }
        harvestSource.setUrl(url);

        String formatName = (String) details.get("format");
        HarvestFormat format = harvestFormatRepository.findFormat(formatName);
        if (format == null) {
            throw new XmlRpcException("Unknown format " + format);
        }
        harvestSource.setFormat(format);

        String updateFrequency = (String) details.get("updateFrequency");
        if (updateFrequency == null) {
            throw new XmlRpcException("Missing updateFrequency");
        }
        harvestSource.setUpdateFrequency(updateFrequency);

        Integer hour = (Integer) details.get("hour");
        Integer minute = (Integer) details.get("minute");
        if (hour != null) {
            harvestSource.setHour(hour);
        }
        if (minute != null) {
            harvestSource.setMinute(minute);
        }


        Integer month = (Integer) details.get("month");
        Integer dayOfMonth = (Integer) details.get("dayOfMonth");
        Integer dayOfWeek = (Integer) details.get("dayOfWeek");
        if (updateFrequency.equals(HarvestSource.UPDATE_ANUALLY)) {
            if (month == null) {
                throw new XmlRpcException("Missing month for annual update");
            }
            if (dayOfMonth == null) {
                throw new XmlRpcException(
                        "Missing dayOfMonth for annual update");
            }
            harvestSource.setMonth(month);
            harvestSource.setDayOfMonth(dayOfMonth);
        } else if (updateFrequency.equals(HarvestSource.UPDATE_MONTHLY)) {
            if (dayOfMonth == null) {
                throw new XmlRpcException(
                        "Missing dayOfMonth for monthly update");
            }
            harvestSource.setDayOfMonth(dayOfMonth);
        } else if (updateFrequency.equals(HarvestSource.UPDATE_WEEKLY)) {
            if (dayOfWeek == null) {
                throw new XmlRpcException(
                        "Missing dayOfWeek for weekly update");
            }
            harvestSource.setDayOfWeek(dayOfWeek);
        } else if (!updateFrequency.equals(HarvestSource.UPDATE_MANUALLY)){
            throw new XmlRpcException("Unknown update frequency "
                    + updateFrequency);
        }

        String ag3VenueServer = (String) details.get("ag3VenueServer");
        Map<String, Object>[] addresses =
            (Map<String, Object>[]) details.get("addresses");
        if (ag3VenueServer != null) {
            String ag3VenueUrl = (String) details.get("ag3VenueUrl");
            if (ag3VenueUrl == null) {
                throw new XmlRpcException("Missing ag3VenueUrl");
            }
            harvestSource.setAg3VenueServer(ag3VenueServer);
            harvestSource.setAg3VenueUrl(ag3VenueUrl);
        } else if (addresses != null) {
            NetworkLocation[] locations = new NetworkLocation[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                Integer ttl = (Integer) addresses[i].get("ttl");
                if (ttl != null) {
                    MulticastNetworkLocation location =
                        new MulticastNetworkLocation();
                    location.setTtl(ttl);
                    locations[i] = location;
                } else {
                    locations[i] = new UnicastNetworkLocation();
                }
                String host = (String) addresses[i].get("host");
                if (host == null) {
                    throw new XmlRpcException("Missing host of address " + i);
                }
                locations[i].setHost(host);
                Integer port = (Integer) addresses[i].get("port");
                if (port == null) {
                    throw new XmlRpcException("Missing port of address " + i);
                }
                locations[i].setPort(port);
            }
            harvestSource.setAddresses(locations);
        } else {
            throw new XmlRpcException("Missing ag3VenueServer or addresses");
        }
    }

    public Integer addHarvestSource(String folderPath,
            Map<String, Object> details) throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        HarvestSource harvestSource = new HarvestSource(folder, typeRepository);

        fillIn(harvestSource, details);

        try {
            getDatabase().addHarvestSource(harvestSource);
        } catch (IOException e) {
            e.printStackTrace();
            throw new XmlRpcException("Error adding harvest source: "
                    + e.getMessage());
        }
        return harvestSource.getId();
    }

    public Boolean updateHarvestSource(String folderPath, int id,
            Map<String, Object> details) throws XmlRpcException {
        System.err.println("Updating source " + id);
        Folder folder = getFolder(folderPath);
        HarvestSource harvestSource = folder.getHarvestSource(id);
        if (harvestSource == null) {
            throw new XmlRpcException("Unknown id " + id);
        }
        fillIn(harvestSource, details);
        try {
            getDatabase().updateHarvestSource(harvestSource);
        } catch (IOException e) {
            e.printStackTrace();
            throw new XmlRpcException("Error updating harvest source: "
                    + e.getMessage());
        }
        return true;
    }

    public Boolean deleteHarvestSource(String folderPath, int id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        HarvestSource harvestSource = folder.getHarvestSource(id);
        if (harvestSource == null) {
            throw new XmlRpcException("Unknown id " + id);
        }
        getDatabase().deleteHarvestSource(harvestSource);
        return true;
    }

    public String harvestNow(String folderPath, int id) throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        HarvestSource harvestSource = folder.getHarvestSource(id);
        if (harvestSource == null) {
            throw new XmlRpcException("Unknown id " + id);
        }
        harvestSource.run();
        return harvestSource.getStatus();
    }

    public Map<String, Object>[] getSources(String folderPath)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        List<HarvestSource> harvestSources = folder.getHarvestSources();
        Map<String, Object>[] sources = new Map[harvestSources.size()];
        for (int i = 0; i < harvestSources.size(); i++) {
            HarvestSource source = harvestSources.get(i);
            sources[i] = new HashMap<String, Object>();
            sources[i].put("id", source.getId());
            sources[i].put("name", source.getName());
            sources[i].put("url", source.getUrl());

            String updateFreq = source.getUpdateFrequency();
            sources[i].put("updateFrequency", updateFreq);
            sources[i].put("hour", source.getHour());
            sources[i].put("minute", source.getMinute());
            if (updateFreq.equals(HarvestSource.UPDATE_ANUALLY)) {
                sources[i].put("month", source.getMonth());
                sources[i].put("dayOfMonth", source.getDayOfMonth());
            } else if (updateFreq.equals(HarvestSource.UPDATE_MONTHLY)) {
                sources[i].put("dayOfMonth", source.getDayOfMonth());
            } else if (updateFreq.equals(HarvestSource.UPDATE_WEEKLY)) {
                sources[i].put("dayOfWeek", source.getDayOfWeek());
            }

            if (source.getAg3VenueServer() != null) {
                sources[i].put("ag3VenueServer", source.getAg3VenueServer());
                sources[i].put("ag3VenueUrl", source.getAg3VenueUrl());
            } else {
                NetworkLocation[] locations = source.getAddresses();
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
                sources[i].put("addresses", addresses);
            }
        }
        return sources;
    }

}
