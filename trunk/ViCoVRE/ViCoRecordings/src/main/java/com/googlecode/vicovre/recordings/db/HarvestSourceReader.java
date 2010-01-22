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

package com.googlecode.vicovre.recordings.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormat;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * Reads a harvest source from an index
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class HarvestSourceReader {

    /**
     * Reads a harvest source
     * @param input The stream from which to read the input
     * @return The harvest source read
     * @throws IOException
     * @throws SAXException
     */
    public static HarvestSource readHarvestSource(InputStream input,
            HarvestFormatRepository harvestFormatRepository,
            RtpTypeRepository typeRepository, Folder folder)
            throws SAXException, IOException {
        HarvestSource harvestSource = new HarvestSource(folder, typeRepository);
        Node doc = XmlIo.read(input);
        XmlIo.setString(doc, harvestSource, "name");
        XmlIo.setString(doc, harvestSource, "url");
        XmlIo.setString(doc, harvestSource, "updateFrequency");
        XmlIo.setString(doc, harvestSource, "subFolderMetadataItem");

        String fmt = XmlIo.readValue(doc, "format");
        HarvestFormat format = harvestFormatRepository.findFormat(fmt);
        if (format == null) {
            throw new SAXException("Harvest format " + fmt + " unknown");
        }
        harvestSource.setFormat(format);

        String ag3VenueServer = XmlIo.readValue(doc, "ag3VenueServer");
        if (ag3VenueServer != null) {
            harvestSource.setAg3VenueServer(ag3VenueServer);
            harvestSource.setAg3VenueUrl(XmlIo.readValue(doc, "ag3VenueUrl"));
        } else {
            Node[] addresses = XmlIo.readNodes(doc, "address");
            NetworkLocation[] locations = new NetworkLocation[addresses.length];
            for (int i = 0; i < locations.length; i++) {
                String ttl = XmlIo.readValue(addresses[i], "ttl");
                if (ttl != null) {
                    MulticastNetworkLocation location =
                        new MulticastNetworkLocation();
                    location.setTtl(ttl);
                    locations[i] = location;
                } else {
                    locations[i] = new UnicastNetworkLocation();
                }
                locations[i].setHost(XmlIo.readValue(addresses[i], "host"));
                locations[i].setPort(XmlIo.readValue(addresses[i], "port"));
            }
            harvestSource.setAddresses(locations);
        }

        String hour = XmlIo.readValue(doc, "hour");
        String minute = XmlIo.readValue(doc, "minute");
        if (hour != null) {
            harvestSource.setHour(Integer.parseInt(hour));
        }
        if (minute != null) {
            harvestSource.setMinute(Integer.parseInt(minute));
        }

        String updateFreq = harvestSource.getUpdateFrequency();
        if (updateFreq.equals(HarvestSource.UPDATE_ANUALLY)) {
            String month = XmlIo.readValue(doc, "month");
            String dayOfMonth = XmlIo.readValue(doc, "dayOfMonth");
            harvestSource.setMonth(Integer.parseInt(month));
            harvestSource.setDayOfMonth(Integer.parseInt(dayOfMonth));
        } else if (updateFreq.equals(HarvestSource.UPDATE_MONTHLY)) {
            String dayOfMonth = XmlIo.readValue(doc, "dayOfMonth");
            harvestSource.setDayOfMonth(Integer.parseInt(dayOfMonth));
        } else if (updateFreq.equals(HarvestSource.UPDATE_WEEKLY)) {
            String dayOfWeek = XmlIo.readValue(doc, "dayOfWeek");
            harvestSource.setDayOfWeek(Integer.parseInt(dayOfWeek));
        }
        return harvestSource;
    }

    public static void writeHarvestSource(HarvestSource harvestSource,
            OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<harvestSource>");
        XmlIo.writeValue(harvestSource, "name", writer);
        XmlIo.writeValue(harvestSource, "url", writer);
        XmlIo.writeValue(harvestSource, "updateFrequency", writer);
        XmlIo.writeValue(harvestSource, "subFolderMetadataItem", writer);
        XmlIo.writeValue("format", harvestSource.getFormat().getName(), writer);
        XmlIo.writeValue(harvestSource, "hour", writer);
        XmlIo.writeValue(harvestSource, "minute", writer);
        String updateFreq = harvestSource.getUpdateFrequency();
        if (updateFreq.equals(HarvestSource.UPDATE_ANUALLY)) {
            XmlIo.writeValue(harvestSource, "month", writer);
            XmlIo.writeValue(harvestSource, "dayOfMonth", writer);
        } else if (updateFreq.equals(HarvestSource.UPDATE_MONTHLY)) {
            XmlIo.writeValue(harvestSource, "dayOfMonth", writer);
        } else if (updateFreq.equals(HarvestSource.UPDATE_WEEKLY)) {
            XmlIo.writeValue(harvestSource, "dayOfWeek", writer);
        }
        if (harvestSource.getAg3VenueServer() != null) {
            XmlIo.writeValue(harvestSource, "ag3VenueServer", writer);
            XmlIo.writeValue(harvestSource, "ag3VenueUrl", writer);
        } else {
            for (NetworkLocation location : harvestSource.getAddresses()) {
                writer.println("<address>");
                if (location instanceof MulticastNetworkLocation) {
                    XmlIo.writeValue(location, "ttl", writer);
                }
                XmlIo.writeValue(location, "host", writer);
                XmlIo.writeValue(location, "port", writer);
                writer.println("</address>");
            }
        }
        writer.println("</harvestSource>");
        writer.flush();
    }

}
