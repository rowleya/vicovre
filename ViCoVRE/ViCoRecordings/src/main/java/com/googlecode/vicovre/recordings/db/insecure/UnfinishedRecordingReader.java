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

package com.googlecode.vicovre.recordings.db.insecure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.Emailer;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * Reads an unfinished recording from an index
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class UnfinishedRecordingReader {

    /**
     * Reads a recording
     * @param input The stream from which to read the input
     * @return The recording read
     * @throws IOException
     * @throws SAXException
     */
    public static UnfinishedRecording readRecording(InputStream input,
            File file, InsecureFolder folder,
            RtpTypeRepository typeRepository, RecordingDatabase database,
            Emailer emailer)
            throws SAXException, IOException {
        UnfinishedRecording recording = new UnfinishedRecording(typeRepository,
                folder, file, database, emailer);
        Node doc = XmlIo.read(input);
        XmlIo.setDate(doc, recording, "startDate");
        XmlIo.setDate(doc, recording, "stopDate");
        String ag3VenueServer = XmlIo.readValue(doc, "ag3VenueServer");
        if (ag3VenueServer != null) {
            recording.setAg3VenueServer(ag3VenueServer);
            recording.setAg3VenueUrl(XmlIo.readValue(doc, "ag3VenueUrl"));
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
            recording.setAddresses(locations);
        }

        String prefix = file.getName();
        prefix = prefix.substring(0, prefix.indexOf(
                        RecordingConstants.UNFINISHED_RECORDING_INDEX));
        File metadataFile = new File(folder.getFile(),
                prefix + RecordingConstants.METADATA);
        File oldMetadataFile = new File(folder.getFile(),
                prefix + RecordingConstants.OLD_METADATA);
        if (metadataFile.exists()) {
            FileInputStream inputStream = new FileInputStream(metadataFile);
            RecordingMetadata metadata = RecordingMetadataReader.readMetadata(
                    inputStream);
            recording.setMetadata(metadata);
            inputStream.close();
        } else if (oldMetadataFile.exists()) {
            FileInputStream inputStream = new FileInputStream(oldMetadataFile);
            RecordingMetadata metadata =
                RecordingMetadataReader.readOldMetadata(inputStream);
            recording.setMetadata(metadata);
            inputStream.close();
            FileOutputStream outputStream = new FileOutputStream(metadataFile);
            RecordingMetadataReader.writeMetadata(metadata, outputStream);
            outputStream.close();
            oldMetadataFile.delete();
        }
        return recording;
    }

    public static void writeRecording(UnfinishedRecording recording,
            OutputStream output) throws IOException {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<recording>");
        XmlIo.writeDate(recording, "startDate", writer);
        XmlIo.writeDate(recording, "stopDate", writer);

        if (recording.getAg3VenueServer() != null) {
            XmlIo.writeValue(recording, "ag3VenueServer", writer);
            XmlIo.writeValue(recording, "ag3VenueUrl", writer);
        } else {
            for (NetworkLocation location : recording.getAddresses()) {
                writer.println("<address>");
                if (location instanceof MulticastNetworkLocation) {
                    XmlIo.writeValue(location, "ttl", writer);
                }
                XmlIo.writeValue(location, "host", writer);
                XmlIo.writeValue(location, "port", writer);
                writer.println("</address>");
            }
        }

        RecordingMetadata metadata = recording.getMetadata();
        if (metadata != null) {
             String prefix = recording.getFile().getName();
             prefix = prefix.substring(0, prefix.indexOf(
                             RecordingConstants.UNFINISHED_RECORDING_INDEX));
            File metadataFile = new File(recording.getFolder().getFile(),
                    prefix + RecordingConstants.METADATA);
            FileOutputStream outputStream = new FileOutputStream(metadataFile);
            RecordingMetadataReader.writeMetadata(metadata, outputStream);
            outputStream.close();
        }

        writer.println("</recording>");
        writer.flush();
    }

}
