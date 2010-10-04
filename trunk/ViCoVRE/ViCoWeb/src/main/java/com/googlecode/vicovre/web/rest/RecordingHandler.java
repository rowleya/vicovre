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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import org.apache.commons.mail.EmailException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.InsecureRecording;
import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;
import com.googlecode.vicovre.utils.Emailer;
import com.googlecode.vicovre.web.rest.response.RecordingsResponse;
import com.googlecode.vicovre.web.rest.response.ReplayLayoutsResponse;
import com.googlecode.vicovre.web.rest.response.StreamsResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("recording")
public class RecordingHandler extends AbstractHandler {

    private LayoutRepository layoutRepository = null;

    private RtpTypeRepository typeRepository = null;

    private String adminEmail = null;

    private Emailer emailer = null;

    public RecordingHandler(@Inject("database") RecordingDatabase database,
            @Inject("layoutRepository") LayoutRepository layoutRepository,
            @Inject RtpTypeRepository typeRepository,
            @Inject Emailer emailer) {
        super(database);
        this.layoutRepository = layoutRepository;
        this.typeRepository = typeRepository;
        this.adminEmail = emailer.getAdminEmailAddress();
        this.emailer = emailer;
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
        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Recording recording = getDatabase().getRecording(folder, id);
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
        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        return Response.ok(new ReplayLayoutsResponse(
                recording.getReplayLayouts())).build();
    }

    @Path("{folder:.*}/layout/{time}")
    @DELETE
    public Response deleteLayout(@Context UriInfo uriInfo,
            @PathParam("time") long time) throws IOException {
        String folder = getFolderPath(uriInfo, 1, 3);
        String id = getId(uriInfo, 2);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        recording.removeLayout(time);
        getDatabase().updateRecordingLayouts(recording);
        return Response.ok().build();
    }

    @Path("{folder:.*}")
    @POST
    @Consumes("application/zip")
    @Produces("text/plain")
    public Response addRecording(@Context UriInfo uriInfo,
            @PathParam("folder") String folder,
            @Context HttpServletRequest request,
            @QueryParam("startDate") String startDate)
            throws IOException, ParseException {

        Date start = RecordingConstants.DATE_FORMAT.parse(startDate);
        String recordingId = UnfinishedRecording.ID_DATE_FORMAT.format(start)
            + UUID.randomUUID().toString();
        File directory = new File(getDatabase().getFile(folder), recordingId);

        InsecureRecording recording =
            new InsecureRecording(folder, recordingId, directory,
                    layoutRepository, typeRepository);
        getDatabase().addRecording(recording, null);

        ZipInputStream input = new ZipInputStream(request.getInputStream());
        ZipEntry entry = input.getNextEntry();
        while (entry != null) {
            File streamFile = new File(recording.getDirectory(),
                    entry.getName());
            FileOutputStream output = new FileOutputStream(streamFile);
            byte[] buffer = new byte[8096];
            int bytesRead = input.read(buffer);
            while (bytesRead != -1) {
                output.write(buffer, 0, bytesRead);
                bytesRead = input.read(buffer);
            }
            output.close();
            entry = input.getNextEntry();
        }

        return Response.ok(
                uriInfo.getAbsolutePathBuilder().path(
                        recordingId).build().toString()).build();
    }

    @POST
    @Consumes("application/zip")
    @Produces("text/plain")
    public Response addRecording(@Context UriInfo uriInfo,
            @Context HttpServletRequest request,
            @QueryParam("startDate") String startDate)
            throws IOException, ParseException {
        return addRecording(uriInfo, "", request, startDate);
    }

    @Path("{folder:.*}/layout/{time}")
    @PUT
    public Response setLayout(@Context UriInfo uriInfo,
            @PathParam("time") long time,
            @QueryParam("name") String name,
            @QueryParam("audioStream") List<String> audioStreams,
            @QueryParam("endTime") @DefaultValue("0") long endTime)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 3);
        String id = getId(uriInfo, 2);

        Recording recording = getDatabase().getRecording(folder, id);
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
        recording.setReplayLayout(layout);
        getDatabase().updateRecordingLayouts(recording);

        return Response.ok().build();
    }

    @Path("{folder:.*}/annotateChanges/{time}")
    @PUT
    public Response annotateChanges(@Context UriInfo uriInfo,
            @PathParam("time") long time) throws IOException {
        String folder = getFolderPath(uriInfo, 1, 3);
        String id = getId(uriInfo, 2);
        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        recording.annotateChanges(time);
        return Response.ok().build();
    }

    @Path("{folder:.*}/annotateChanges/{time}")
    @GET
    @Produces("text/plain")
    public Response getChangesProgress(@Context UriInfo uriInfo,
            @PathParam("time") long time) throws IOException {
        String folder = getFolderPath(uriInfo, 1, 3);
        String id = getId(uriInfo, 2);
        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        return Response.ok(
                String.valueOf(recording.getAnnotationProgress(time))).build();
    }

    @Path("{folder: .*}")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getRecordings(@PathParam("folder") String folder) {
        return Response.ok(new RecordingsResponse(
                getDatabase().getRecordings(folder))).build();
    }

    @GET
    @Produces({"text/xml", "application/json"})
    public Response getRecordings() {
        return getRecordings("");
    }

    @Path("{folder: .*}")
    @PUT
    @Consumes("application/x-www-form-urlencoded")
    public Response updateMetadata(@Context UriInfo uriInfo,
            MultivaluedMap<String, String> formParams)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        recording.setMetadata(getMetadata(formParams));
        getDatabase().updateRecordingMetadata(recording);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @PUT
    public Response updateMetadata(@Context UriInfo uriInfo)
            throws IOException {
        return updateMetadata(uriInfo, uriInfo.getQueryParameters());
    }

    @Path("{folder: .*}")
    @DELETE
    public Response deleteRecording(@Context UriInfo uriInfo)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        getDatabase().deleteRecording(recording);
        return Response.ok().build();
    }

    @Path("{folder:.*}/acl/{type}")
    @PUT
    public Response setAcl(@Context UriInfo uriInfo,
            @QueryParam("public") boolean isPublic,
            @QueryParam("exceptionType") List<String> exceptionTypes,
            @QueryParam("exceptionName") List<String> exceptionNames)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 3);
        String id = getId(uriInfo, 2);
        String acltype = getId(uriInfo, 0);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            WriteOnlyEntity[] exceptions = new WriteOnlyEntity[0];
            if ((exceptionTypes != null) && (exceptionNames != null)) {
                if (exceptionTypes.size() != exceptionNames.size()) {
                    return Response.status(Status.BAD_REQUEST).entity(
                        "The number of exceptionType parameters must match"
                        + " the number of exceptionName parameters").build();
                }
                exceptions = new WriteOnlyEntity[exceptionTypes.size()];
                for (int i = 0; i < exceptionTypes.size(); i++) {
                    String type = exceptionTypes.get(i);
                    String name = exceptionNames.get(i);
                    exceptions[i] = new WriteOnlyEntity(name, type);
                }
            }
            if (acltype.equals("play")) {
                secureDb.setRecordingPlayAcl(recording, isPublic, exceptions);
            } else if (acltype.equals("read")) {
                secureDb.setRecordingReadAcl(recording, isPublic, exceptions);
            }
        }
        return Response.ok().build();
    }

    @Path("{folder:.*}/acl/{type}")
    @GET
    @Produces({"application/json", "text/xml"})
    public Response getAcl(@Context UriInfo uriInfo) throws IOException {
        String folder = getFolderPath(uriInfo, 1, 3);
        String id = getId(uriInfo, 2);
        String acltype = getId(uriInfo, 1);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }

        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            if (acltype.equals("play")) {
                return Response.ok(
                        secureDb.getRecordingPlayAcl(recording)).build();
            } else if (acltype.equals("read")) {
                return Response.ok(
                        secureDb.getRecordingReadAcl(recording)).build();
            }
        }
        return Response.ok().build();
    }

    @Path("{folder:.*}/requestAccess")
    @GET
    public Response requestAccess(@Context UriInfo uriInfo,
            @QueryParam("emailAddress") String emailAddress)
            throws IOException, EmailException {
        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Recording " + id + " not found");
        }
        String email = recording.getEmailAddress();
        if (email == null) {
            email = adminEmail;
        }

        String subject = "Access Request for " + folder + "/" + id
            + " (" + recording.getMetadata().getPrimaryValue() + ")";
        String message = "A user with e-mail address " + emailAddress
            + " is requesting access to one of your recordings:\n";
        message += "    " + recording.getMetadata().getPrimaryValue() + "\n\n";
        message += "If you would like to grant access to this recording,"
            + " please go to:\n";
        message += uriInfo.getBaseUriBuilder().path("..").path(
                recording.getFolder()).path(recording.getId()).path(
                "displayRecording.do").build().normalize().toString() + "\n";
        message += "log in, and then click on the \"Set Permissions\" button."
            + " You can then add the user from the list on the right and then"
            + " click on the OK button to set the changed permissions.";
        message += "If you do not want to grant access,"
            + " please ignore this message.\n";

        emailer.send(email, subject, message);
        return Response.ok().build();
    }

}
