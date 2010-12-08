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
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.DELETE;
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

import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.UnicastNetworkLocation;

import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.UnfinishedRecordingController;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;
import com.googlecode.vicovre.web.rest.response.UnfinishedRecordingsResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("/record")
public class UnfinishedRecordingHandler extends AbstractHandler {

    private UnfinishedRecordingController recordingController = null;

    public UnfinishedRecordingHandler(
            @Inject("database") RecordingDatabase database,
            @Inject UnfinishedRecordingController recordingController) {
        super(database);
        this.recordingController = recordingController;
    }

    private void fillIn(UnfinishedRecording recording,
            MultivaluedMap<String, String> details) throws IOException {

        String startDateString = details.getFirst("startDate");
        System.err.println("Start date = " + startDateString);
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

        String frequency = details.getFirst("repeatFrequency");
        if ((frequency != null)
                && !frequency.equals(UnfinishedRecording.NO_REPEAT)) {
            recording.setRepeatFrequency(frequency);

            String startHour = details.getFirst("repeatStartHour");
            String startMinute = details.getFirst("repeatStartMinute");
            String duration = details.getFirst("repeatDurationMinutes");
            String repeatItemFrequency = details.getFirst(
                    "repeatItemFrequency");

            recording.setRepeatStartHour(Integer.parseInt(startHour));
            recording.setRepeatStartMinute(Integer.parseInt(startMinute));
            recording.setRepeatDurationMinutes(Integer.parseInt(duration));
            recording.setRepeatItemFrequency(Integer.parseInt(
                    repeatItemFrequency));

            if (frequency.equals(UnfinishedRecording.REPEAT_DAILY)) {
                String ignoreWeekends = details.getFirst("ignoreWeekends");
                if (ignoreWeekends != null) {
                    recording.setIgnoreWeekends(ignoreWeekends.equals("true"));
                }
            } else if (frequency.equals(UnfinishedRecording.REPEAT_WEEKLY)) {
                String dayOfWeek = details.getFirst("repeatDayOfWeek");
                recording.setRepeatDayOfWeek(Integer.parseInt(dayOfWeek));
            } else if (frequency.equals(UnfinishedRecording.REPEAT_MONTHLY)) {
                String dayOfMonth = details.getFirst("repeatDayOfMonth");
                if ((dayOfMonth != null) && !dayOfMonth.equals("0")) {
                    recording.setRepeatDayOfMonth(Integer.parseInt(dayOfMonth));
                } else {
                    String dayOfWeek = details.getFirst("repeatDayOfWeek");
                    String weekOfMonth = details.getFirst("repeatWeekNumber");
                    recording.setRepeatDayOfWeek(Integer.parseInt(dayOfWeek));
                    recording.setRepeatWeekNumber(Integer.parseInt(
                            weekOfMonth));
                    recording.setRepeatDayOfMonth(0);
                }
            } else if (frequency.equals(UnfinishedRecording.REPEAT_ANNUALLY)) {
                String month = details.getFirst("repeatMonth");
                recording.setRepeatMonth(Integer.parseInt(month));
                String dayOfMonth = details.getFirst("repeatDayOfMonth");
                if ((dayOfMonth != null) && !dayOfMonth.equals("0")) {
                    recording.setRepeatDayOfMonth(Integer.parseInt(dayOfMonth));
                } else {
                    String dayOfWeek = details.getFirst("repeatDayOfWeek");
                    String weekOfMonth = details.getFirst("repeatWeekNumber");
                    recording.setRepeatDayOfWeek(Integer.parseInt(dayOfWeek));
                    recording.setRepeatWeekNumber(Integer.parseInt(
                            weekOfMonth));
                    recording.setRepeatDayOfMonth(0);
                }
            }
        }

        recording.setEmailAddress(details.getFirst("emailAddress"));
    }

    @Path("{folder: .*}")
    @POST
    @Produces("text/plain")
    public Response addUnfinishedRecording(
            @PathParam("folder") String folder,
            @Context UriInfo uriInfo)
            throws IOException {

        String id = UUID.randomUUID().toString();
        UnfinishedRecording recording = new UnfinishedRecording(folder, id);
        fillIn(recording, uriInfo.getQueryParameters());
        recording.setMetadata(getMetadata(uriInfo.getQueryParameters()));
        getDatabase().addUnfinishedRecording(recording, null);

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
        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown id " + id);
        }
        fillIn(recording, uriInfo.getQueryParameters());
        recording.setMetadata(getMetadata(uriInfo.getQueryParameters()));
        getDatabase().updateUnfinishedRecording(recording);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @DELETE
    public Response deleteUnfinishedRecording(
            @Context UriInfo uriInfo)
            throws IOException {

        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
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
            @PathParam("folder") String folder) {
        List<UnfinishedRecording> recordings =
            getDatabase().getUnfinishedRecordings(folder);
        return Response.ok(new UnfinishedRecordingsResponse(
                recordings)).build();
    }

    @GET
    @Produces({"text/xml", "application/json"})
    public Response getUnfinishedRecordings() {
        return getUnfinishedRecordings("");
    }

    @Path("{folder: .*}/start")
    @PUT
    @Produces("text/plain")
    public Response startRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown recording id " + id);
        }
        recordingController.startRecording(recording);
        return Response.ok(recording.getStatus()).build();
    }

    @Path("{folder: .*}/stop")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response stopRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown recording id " + id);
        }
        Recording finishedRecording =
            recordingController.stopRecording(recording);
        if (finishedRecording != null) {
            return Response.ok(finishedRecording).build();
        }
        return Response.serverError().entity(recording.getStatus()).build();
    }

    @Path("{folder: .*}/pause")
    @PUT
    @Produces("text/plain")
    public Response pauseRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
        if (recording == null) {
            throw new IOException("Unknown recording id " + id);
        }
        recordingController.pauseRecording(recording);
        return Response.ok(recording.getStatus()).build();
    }

    @Path("{folder: .*}/resume")
    @PUT
    @Produces("text/plain")
    public Response resumeRecording(@Context UriInfo uriInfo)
            throws IOException {

        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
        if (recording == null) {
            throw new FileNotFoundException("Unknown recording id " + id);
        }
        recordingController.resumeRecording(recording);
        return Response.ok(recording.getStatus()).build();
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
        String acltype = getId(uriInfo, 1);

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
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

        UnfinishedRecording recording = getDatabase().getUnfinishedRecording(
                folder, id);
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
}
