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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.web.rest.response.UnfinishedRecordingsResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("/record")
public class UnfinishedRecordingHandler extends AbstractHandler {

    private RtpTypeRepository typeRepository = null;

    public UnfinishedRecordingHandler(@Inject RecordingDatabase database,
            @Inject RtpTypeRepository typeRepository) {
        super(database);
        this.typeRepository = typeRepository;
    }

    private void fillIn(UnfinishedRecording recording,
            MultivaluedMap<String, String> details) throws IOException {

        String startDateString = details.getFirst("startDate");
        if (startDateString != null) {
            try {
                recording.setStartDateString(startDateString);
            } catch (ParseException e) {
                throw new IOException(e);
            }
        } else {
            recording.setStartDate(null);
        }
        String stopDateString = details.getFirst("stopDate");
        if (stopDateString != null) {
            try {
                recording.setStopDateString(stopDateString);
            } catch (ParseException e) {
                throw new IOException(e);
            }
        } else {
            recording.setStopDate(null);
        }

        String ag3VenueServer = details.getFirst("ag3VenueServer");
        List<String> addresses = details.get("host");
        if (ag3VenueServer != null) {
            String ag3VenueUrl = details.getFirst("ag3VenueUrl");
            if (ag3VenueUrl == null) {
                throw new IOException("Missing ag3VenueUrl");
            }
            recording.setAg3VenueServer(ag3VenueServer);
            recording.setAg3VenueUrl(ag3VenueUrl);
            recording.setAddresses(null);
        } else if (addresses != null) {
            List<String> ports = details.get("port");
            List<String> ttls = details.get("ttl");
            NetworkLocation[] locations = new NetworkLocation[addresses.size()];
            for (int i = 0; i < addresses.size(); i++) {
                String ttl = ttls.get(i);
                if (ttl != null) {
                    MulticastNetworkLocation location =
                        new MulticastNetworkLocation();
                    location.setTtl(ttl);
                    locations[i] = location;
                } else {
                    locations[i] = new UnicastNetworkLocation();
                }
                locations[i].setHost(addresses.get(i));
                String port = ports.get(i);
                if (port == null) {
                    throw new IOException("Missing port of address " + i);
                }
                locations[i].setPort(port);
            }
            recording.setAddresses(locations);
            recording.setAg3VenueServer(null);
            recording.setAg3VenueUrl(null);
        } else {
            throw new IOException("Missing ag3VenueServer or addresses");
        }
    }

    public void fillIn(RecordingMetadata metadata,
            MultivaluedMap<String, String> details) throws IOException {
        Class<?> cls = metadata.getClass();
        Method[] methods = cls.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get")
                    && method.getParameterTypes().length == 0) {
                String field = method.getName().substring("get".length());
                try {
                    Method setMethod = cls.getMethod("set" + field,
                            String.class);
                    if (setMethod != null) {
                        field = field.substring(0, 1).toLowerCase()
                            + field.substring(1);
                        String value = details.getFirst("metadata_" + field);
                        if (value != null) {
                            try {
                                setMethod.invoke(metadata, value);
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new IOException(
                                    "Error setting metadata value " + field);
                            }
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // Do Nothing
                }
            }
        }
    }

    @Path("{folder: .*}")
    @POST
    @Produces("text/plain")
    public Response addUnfinishedRecording(
            @PathParam("folder") String folderPath,
            @Context UriInfo uriInfo)
            throws IOException {
        Folder folder = getFolder(folderPath);

        File file = File.createTempFile("recording",
                RecordingConstants.UNFINISHED_RECORDING_INDEX,
                folder.getFile());
        UnfinishedRecording recording = new UnfinishedRecording(
                typeRepository, folder, file, getDatabase());
        fillIn(recording, uriInfo.getQueryParameters());
        RecordingMetadata metadata = new RecordingMetadata();
        fillIn(metadata, uriInfo.getQueryParameters());
        recording.setMetadata(metadata);
        getDatabase().addUnfinishedRecording(recording);

        return Response.ok(
                uriInfo.getAbsolutePathBuilder().path(
                        recording.getId()).build().toString()).build();
    }

    @POST
    @Produces("text/plain")
    public Response addUnifinishedRecording(@Context UriInfo uriInfo)
            throws IOException {
        return addUnfinishedRecording("", uriInfo);
    }

    @Path("{folder: .*}")
    @PUT
    public Response updateUnfinishedRecording(
            @Context UriInfo uriInfo) throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown id " + id);
        }
        fillIn(recording, uriInfo.getQueryParameters());
        fillIn(recording.getMetadata(), uriInfo.getQueryParameters());
        getDatabase().updateUnfinishedRecording(recording);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @DELETE
    public Response deleteUnfinishedRecording(
            @Context UriInfo uriInfo)
            throws IOException {

        String folderPath = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown id " + id);
        }
        getDatabase().deleteUnfinishedRecording(recording);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getUnfinishedRecordings(
            @PathParam("folder") String folderPath)
            throws IOException {
        Folder folder = getFolder(folderPath);
        List<UnfinishedRecording> recordings = folder.getUnfinishedRecordings();
        return Response.ok(new UnfinishedRecordingsResponse(
                recordings)).build();
    }

    @GET
    @Produces({"text/xml", "application/json"})
    public Response getUnfinishedRecordings() throws IOException {
        return getUnfinishedRecordings("");
    }

    @Path("{folder: .*}/start")
    @PUT
    @Produces("text/plain")
    public Response startRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown recording id " + id);
        }
        recording.startRecording();
        return Response.ok(recording.getStatus()).build();
    }

    @Path("{folder: .*}/stop")
    @PUT
    @Produces("text/plain")
    public Response stopRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown recording id " + id);
        }
        recording.stopRecording();
        return Response.ok(recording.getStatus()).build();
    }

    @Path("{folder: .*}/pause")
    @PUT
    @Produces("text/plain")
    public Response pauseRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new IOException("Unknown recording id " + id);
        }
        recording.pauseRecording();
        return Response.ok(recording.getStatus()).build();
    }

    @Path("{folder: .*}/resume")
    @PUT
    @Produces("text/plain")
    public Response resumeRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown recording id " + id);
        }
        recording.resumeRecording();
        return Response.ok(recording.getStatus()).build();
    }
}
