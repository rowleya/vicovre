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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;
import com.googlecode.vicovre.web.rest.response.RecordingsResponse;
import com.googlecode.vicovre.web.rest.response.StreamsResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("recording")
public class RecordingHandler extends AbstractHandler {

    private LayoutRepository layoutRepository = null;

    public RecordingHandler(@Inject("database") RecordingDatabase database,
            @Inject("layoutRepository") LayoutRepository layoutRepository) {
        super(database);
        this.layoutRepository = layoutRepository;
        if (!Misc.isCodecsConfigured()) {
            try {
                Misc.configureCodecs("/knownCodecs.xml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Path("{folder: .*}/streams")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getStreams(@Context UriInfo uriInfo) throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        return Response.ok(new StreamsResponse(recording.getStreams())).build();
    }

    @Path("{folder: .*}/layouts")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getLayouts(@Context UriInfo uriInfo)
            throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        return Response.ok(recording.getReplayLayouts()).build();
    }

    @Path("{folder:.*}/layout/{time}")
    @DELETE
    public Response deleteLayout(@Context UriInfo uriInfo,
            @PathParam("time") long time) throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        recording.removeLayout(time);
        return Response.ok().build();
    }

    @Path("{folder:.*}/layout/{time}")
    @PUT
    public Response setLayout(@Context UriInfo uriInfo,
            @PathParam("time") long time,
            @QueryParam("name") String name,
            @QueryParam("audioStream") List<String> audioStreams,
            @QueryParam("endTime") @DefaultValue("0") long endTime)
            throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        ReplayLayout layout = new ReplayLayout(layoutRepository);
        layout.setName(name);
        List<ReplayLayoutPosition> positions = layout.getLayoutPositions();
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        for (ReplayLayoutPosition position : positions) {
            String posName = position.getName();
            String streamId = params.getFirst(posName);
            Stream stream = recording.getStream(streamId);
            if (stream == null) {
                throw new FileNotFoundException("Stream " + streamId
                        + " not found");
            }
            layout.setStream(posName, stream);
        }

        if (audioStreams != null) {
            for (String streamId : audioStreams) {
                Stream stream = recording.getStream(streamId);
                if (stream == null) {
                    throw new FileNotFoundException("Stream " + streamId
                            + " not found");
                }
                layout.addAudioStream(stream);
            }
        }

        layout.setRecording(recording);
        layout.setEndTime(endTime);

        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getRecordings(@PathParam("folder") String folderPath)
            throws IOException {
        Folder folder = getFolder(folderPath);
        return Response.ok(new RecordingsResponse(
                folder.getRecordings())).build();
    }

    @GET
    @Produces({"text/xml", "application/json"})
    public Response getRecordings() throws IOException {
        return getRecordings("");
    }

    @Path("{folder: .*}")
    @PUT
    public Response updateMetadata(@Context UriInfo uriInfo)
            throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        fillIn(recording.getMetadata(), uriInfo.getQueryParameters());
        getDatabase().updateRecordingMetadata(recording);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @DELETE
    public Response deleteRecording(@Context UriInfo uriInfo)
            throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        getDatabase().deleteRecording(recording);
        return Response.ok().build();
    }

    @Path("{folder:.*}/acl")
    @PUT
    public Response setAcl(@Context UriInfo uriInfo,
            @QueryParam("public") boolean isPublic,
            @QueryParam("exceptionType") List<String> exceptionTypes,
            @QueryParam("exceptionName") List<String> exceptionNames)
            throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            if (exceptionTypes.size() != exceptionNames.size()) {
                return Response.status(Status.BAD_REQUEST).entity(
                        "The number of exceptionType parameters must match"
                        + " the number of exceptionName parameters").build();
            }
            WriteOnlyEntity[] exceptions =
                new WriteOnlyEntity[exceptionTypes.size()];
            for (int i = 0; i < exceptionTypes.size(); i++) {
                String type = exceptionTypes.get(i);
                String name = exceptionNames.get(i);
                exceptions[i] = new WriteOnlyEntity(name, type);
            }
            secureDb.setRecordingAcl(recording, isPublic, exceptions);
        }
        return Response.ok().build();
    }

    @Path("{folder:.*}/acl")
    @GET
    @Produces({"application/json", "text/xml"})
    public Response getAcl(@Context UriInfo uriInfo) throws IOException {
        String folderPath = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            return Response.ok(secureDb.getRecordingAcl(recording)).build();
        }
        return Response.ok().build();
    }

}
