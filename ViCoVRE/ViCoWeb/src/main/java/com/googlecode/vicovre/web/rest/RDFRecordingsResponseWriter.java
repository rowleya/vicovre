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

package com.googlecode.vicovre.web.rest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;

@Provider
@Produces("application/rdf+xml")
public class RDFRecordingsResponseWriter
        implements MessageBodyWriter<RecordingDatabase> {

    private static final DateTimeFormatter DATETIME_FORMAT =
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormat.forPattern("yyyy-MM-dd");

    private Map<String, String> namespaces = new HashMap<String, String>();

    private Map<String, String> metadataMap = new HashMap<String, String>();

    private Map<String, String> splitMetadataMap =
        new HashMap<String, String>();

    private Map<String, String> uriMetadataMap = new HashMap<String, String>();

    private Map<String, String> splitUriMetadataMap =
        new HashMap<String, String>();

    private @Context UriInfo uriInfo = null;

    public RDFRecordingsResponseWriter() {
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        namespaces.put("dc", "http://purl.org/dc/elements/1.1/");
        namespaces.put("eswc",
                "http://www.eswc2006.org/technologies/ontology#");
        namespaces.put("crew", "http://www.crew-vre.net/ontology#");
        namespaces.put("iugo", "http://www.ilrt.bristol.ac.uk/iugo#");
        namespaces.put("pos", "http://www.w3.org/2003/01/geo/wgs84_pos#");

        metadataMap.put("description", "dc:description");
        metadataMap.put("tag", "iugo:hasTag");
        uriMetadataMap.put("skosLocation", "iugo:hasLocation");
        uriMetadataMap.put("programme", "eswc:hasProgramme");
        uriMetadataMap.put("proceedings", "eswc:hasProceedings");
        uriMetadataMap.put("subject", "iugo:hasSubject");
        splitMetadataMap.put("tags", "iugo:hasTag");
        splitUriMetadataMap.put("subjects", "iugo:hasSubject");
    }

    public long getSize(RecordingDatabase database, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType) {
        return RecordingDatabase.class.isAssignableFrom(aClass);
    }

    private void startEvent(PrintWriter writer, String folder, String id) {
        UriBuilder builder = uriInfo.getBaseUriBuilder().path("..");
        builder = builder.path(folder).path(id);
        writer.println("    "
                + "<rdf:Description rdf:about=\""
                + builder.build().normalize().toString() + "\">");
    }

    private void endEvent(PrintWriter writer) {
        writer.println("    "
                + "</rdf:Description>");
    }

    private void writeTitle(PrintWriter writer, String title) {
        writer.println("        "
                + "<dc:title>" + title + "</dc:title>");
    }

    private void writeTimes(PrintWriter writer, long startTime, long endTime) {
        writer.println("        "
                + "<eswc:hasStartDateTime rdf:datatype=\""
                + "http://www.w3.org/2001/XMLSchema#dateTime\">"
                + DATETIME_FORMAT.print(startTime)
                + "</eswc:hasStartDateTime>");
        writer.println("        "
                + "<eswc:hasEndDateTime rdf:datatype=\""
                + "http://www.w3.org/2001/XMLSchema#dateTime\">"
                + DATETIME_FORMAT.print(endTime)
                + "</eswc:hasEndDateTime>");
    }

    private void writeEvent(PrintWriter writer, long startTime, long endTime,
            String title, String partOf) {
        writer.println("        "
                + "<rdf:type rdf:resource=\""
                + "http://www.eswc2006.org/technologies/ontology#Event\"/>");
        writeTitle(writer, title);
        writeTimes(writer, startTime, endTime);
        writePartOf(writer, partOf);
    }

    private void writeMainEvent(PrintWriter writer, long startTime,
            long endTime, String title) {
        writer.println("        "
                + "<rdf:type rdf:resource=\""
                + "http://www.ilrt.bristol.ac.uk/iugo#MainEvent\"/>");
        writeTitle(writer, title);
        writeTimes(writer, startTime, endTime);
        writer.println("        "
                + "<crew:hasStartDate rdf:datatype=\""
                + "http://www.w3.org/2001/XMLSchema#date\">"
                + DATE_FORMAT.print(startTime)
                + "</crew:hasStartDate>");
        writer.println("        "
                + "<crew:hasEndDate rdf:datatype=\""
                + "http://www.w3.org/2001/XMLSchema#date\">"
                + DATE_FORMAT.print(endTime)
                + "</crew:hasEndDate>");
    }

    private void writePartOf(PrintWriter writer, String partOf) {
        writer.println("        "
                + "<eswc:isPartOf rdf:resource=\""
                + partOf
                + "\"/>");
    }

    private void writePart(PrintWriter writer, String part) {
        writer.println("        "
                + "<eswc:hasPart rdf:resource=\""
                + part
                + "\"/>");
    }

    private String writeMetadata(PrintWriter writer,
            Metadata metadata) {
        for (String key : metadata.getKeys()) {
            if (metadata.isVisible(key)) {
                String value = metadata.getValue(key);

                String property = metadataMap.get(key);
                if (property != null) {
                    writer.println("        "
                            + "<" + property + ">"
                            + value
                            + "</" + property + ">");
                }

                String splitProperty = splitMetadataMap.get(key);
                if (splitProperty != null) {
                    String[] splitItems = value.split("[ \\t;,:]*");
                    for (String item : splitItems) {
                        writer.println("        "
                                + "<" + property + ">"
                                + item
                                + "</" + property + ">");
                    }
                }

                String uriProperty = uriMetadataMap.get(key);
                if (uriProperty != null) {
                    writer.println("        "
                            + "<" + property + "rdf:resource=\""
                            + value + "\"/>");
                }

                String splitUriProperty = splitUriMetadataMap.get(key);
                if (splitUriProperty != null) {
                    String[] splitItems = value.split("[ \\t;,]*");
                    for (String item : splitItems) {
                        writer.println("        "
                                + "<" + property + "rdf:resource=\""
                                + item + "\"/>");
                    }
                }
            }
        }

        String locationData = "";
        String location = metadata.getValue("location");
        if (location != null) {
            UriBuilder builder = uriInfo.getBaseUriBuilder().path("..");
            builder = builder.path("location");
            builder = builder.path(location.replace(' ', '_'));
            String uri = builder.build().normalize().toString();

            locationData = "    "
                + "<rdf:Description about=\"" + uri + ">\n";
            locationData = "        "
                + "<dc:title>" + location + "</dc:title>\n";
            String latitude = metadata.getValue("latitude");
            String longitude = metadata.getValue("longitude");
            if ((latitude != null) && (longitude != null)) {
                locationData += "        "
                    + "<pos:lat>" + latitude + "</pos:lat>\n";
                locationData += "        "
                    + "<pos:long>" + longitude + "</pos:long>\n";
            }
            locationData = "    "
                + "</rdf:Description>\n";

            writer.println("        "
                    + "<iugo:hasLocation rdf:resource=\""
                    + uri + "\"/>");
        }
        return locationData;
    }

    private void writeRecording(PrintWriter writer, Recording recording,
            String partOf) {
        long startTime = recording.getStartTime().getTime();
        long endTime = startTime + recording.getDuration();
        Metadata metadata = recording.getMetadata();
        String title = metadata.getPrimaryValue();
        startEvent(writer, recording.getFolder(), recording.getId());
        if (partOf == null) {
            writeMainEvent(writer, startTime, endTime, title);
        } else {
            writeEvent(writer, startTime, endTime, title, partOf);
        }

        String locationData = writeMetadata(writer, metadata);
        writer.print(locationData);

        UriBuilder builder = uriInfo.getBaseUriBuilder().path("..");
        builder = builder.path(recording.getFolder());
        builder = builder.path(recording.getId());
        builder = builder.path("displayRecording.do");
        writer.println("        "
                + "<crew:hasRecording>"
                + builder.build().normalize().toString()
                + "</crew:hasRecording>");
        endEvent(writer);
    }

    private String getId(String folder) {
        UriBuilder builder = uriInfo.getBaseUriBuilder().path("..");
        builder = builder.path(folder);
        return builder.build().normalize().toString();
    }

    private String getTitle(String folder) {
        return new File(folder).getName();
    }

    private class Times {
        private long startTime = 0;
        private long endTime = 0;

        private Times(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private Times writeFolder(RecordingDatabase database, PrintWriter writer,
            String folder, String partOf) {
        String id = getId(folder);
        String title = getTitle(folder);
        List<Recording> recordings = database.getRecordings(folder);
        List<String> subfolders = database.getSubFolders(folder);

        long minStartTime = Long.MAX_VALUE;
        long maxEndTime = Long.MIN_VALUE;
        String parent = id;
        if (folder.equals("")) {
            parent = null;
        }
        for (Recording recording : recordings) {
            if (recording.isPlayable()) {
                long startTime = recording.getStartTime().getTime();
                long endTime = startTime + recording.getDuration();
                minStartTime = Math.min(startTime, minStartTime);
                maxEndTime = Math.max(endTime, maxEndTime);
                writeRecording(writer, recording, parent);
            }
        }

        for (String subfolder : subfolders) {
            Times times = writeFolder(database, writer,
                    folder + "/" + subfolder, parent);
            minStartTime = Math.min(times.startTime, minStartTime);
            maxEndTime = Math.max(times.endTime, maxEndTime);
        }

        if (!folder.equals("") && (minStartTime < Long.MAX_VALUE)
                && (maxEndTime > Long.MIN_VALUE)) {
            startEvent(writer, folder, "");
            if (partOf == null) {
                writeMainEvent(writer, minStartTime, maxEndTime, title);
            } else {
                writeEvent(writer, minStartTime, maxEndTime, title, partOf);
            }
            for (String subfolder : subfolders) {
                String subfolderPath = folder + "/" + subfolder;
                writePart(writer, getId(subfolderPath));
            }
            String locationData = writeMetadata(writer,
                    database.getFolderMetadata(folder));
            writer.print(locationData);
            endEvent(writer);
        }

        return new Times(minStartTime, maxEndTime);
    }

    public void writeTo(RecordingDatabase database, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> map, OutputStream output)
            throws IOException, WebApplicationException {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<rdf:RDF");
        for (String namespace : namespaces.keySet()) {
            writer.println("        xmlns:" + namespace + "=\""
                    + namespaces.get(namespace) + "\"");
        }
        writer.println("        >");
        writeFolder(database, writer, "", null);
        writer.println("</rdf:RDF>");
        writer.flush();
    }

}
