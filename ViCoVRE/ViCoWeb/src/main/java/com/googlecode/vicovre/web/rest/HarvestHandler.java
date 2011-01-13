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

import com.googlecode.onevre.ag.types.network.MulticastNetworkLocation;
import com.googlecode.onevre.ag.types.network.NetworkLocation;
import com.googlecode.onevre.ag.types.network.UnicastNetworkLocation;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Harvester;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormat;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;
import com.googlecode.vicovre.web.rest.response.HarvestSourcesResponse;
import com.googlecode.vicovre.web.rest.response.UnfinishedRecordingsResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("harvest")
public class HarvestHandler extends AbstractHandler {

    private Harvester harvester = null;

    private HarvestFormatRepository harvestFormatRepository = null;

    public HarvestHandler(@Inject("database") RecordingDatabase database,
            @Inject Harvester harvester,
            @Inject HarvestFormatRepository harvestFormatRepository) {
        super(database);
        this.harvester = harvester;
        this.harvestFormatRepository = harvestFormatRepository;
    }

    private void fillIn(HarvestSource harvestSource,
            MultivaluedMap<String, String> details) throws IOException {
        String name = details.getFirst("name");
        if (name == null) {
            throw new IOException("Missing name");
        }
        harvestSource.setName(name);

        String url = details.getFirst("url");
        if (url == null) {
            throw new IOException("Missing url");
        }
        harvestSource.setUrl(url);

        String formatName = details.getFirst("format");
        HarvestFormat format = harvestFormatRepository.findFormat(formatName);
        if (format == null) {
            throw new IOException("Unknown format " + format);
        }
        harvestSource.setFormat(format);

        String updateFrequency = details.getFirst("updateFrequency");
        if (updateFrequency == null) {
            throw new IOException("Missing updateFrequency");
        }
        harvestSource.setUpdateFrequency(updateFrequency);

        String hour = details.getFirst("hour");
        String minute = details.getFirst("minute");
        if (hour != null) {
            harvestSource.setHour(Integer.parseInt(hour));
        }
        if (minute != null) {
            harvestSource.setMinute(Integer.parseInt(minute));
        }

        String month = details.getFirst("month");
        String dayOfMonth = details.getFirst("dayOfMonth");
        String dayOfWeek = details.getFirst("dayOfWeek");
        if (updateFrequency.equals(HarvestSource.UPDATE_ANUALLY)) {
            if (month == null) {
                throw new IOException("Missing month for annual update");
            }
            if (dayOfMonth == null) {
                throw new IOException(
                        "Missing dayOfMonth for annual update");
            }
            harvestSource.setMonth(Integer.parseInt(month));
            harvestSource.setDayOfMonth(Integer.parseInt(dayOfMonth));
        } else if (updateFrequency.equals(HarvestSource.UPDATE_MONTHLY)) {
            if (dayOfMonth == null) {
                throw new IOException(
                        "Missing dayOfMonth for monthly update");
            }
            harvestSource.setDayOfMonth(Integer.parseInt(dayOfMonth));
        } else if (updateFrequency.equals(HarvestSource.UPDATE_WEEKLY)) {
            if (dayOfWeek == null) {
                throw new IOException(
                        "Missing dayOfWeek for weekly update");
            }
            harvestSource.setDayOfWeek(Integer.parseInt(dayOfWeek));
        } else if (!updateFrequency.equals(HarvestSource.UPDATE_MANUALLY)){
            throw new IOException("Unknown update frequency "
                    + updateFrequency);
        }

        String ag3VenueServer = details.getFirst("ag3VenueServer");
        List<String> addresses = details.get("host");
        if (ag3VenueServer != null) {
            String ag3VenueUrl = details.getFirst("ag3VenueUrl");
            if (ag3VenueUrl == null) {
                throw new IOException("Missing ag3VenueUrl");
            }
            harvestSource.setAg3VenueServer(ag3VenueServer);
            harvestSource.setAg3VenueUrl(ag3VenueUrl);
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
            harvestSource.setAddresses(locations);
        } else {
            throw new IOException("Missing ag3VenueServer or addresses");
        }
    }

    @Path("{folder: .*}")
    @POST
    @Produces("text/plain")
    public Response addHarvestSource(
            @PathParam("folder") String folder,
            @Context UriInfo uriInfo) throws IOException {
        String id = UUID.randomUUID().toString();
        HarvestSource harvestSource = new HarvestSource(folder, id);
        fillIn(harvestSource, uriInfo.getQueryParameters());
        getDatabase().addHarvestSource(harvestSource);
        return Response.ok(
                uriInfo.getAbsolutePathBuilder().path(
                        harvestSource.getId()).build().toString()).build();
    }

    @POST
    @Produces("text/plain")
    public Response addHarvestSource(@Context UriInfo uriInfo)
            throws IOException {
        return addHarvestSource("", uriInfo);
    }

    @Path("{folder: .*}")
    @PUT
    public Response updateHarvestSource(@Context UriInfo uriInfo)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        HarvestSource harvestSource = getDatabase().getHarvestSource(
                folder, id);
        if (harvestSource == null) {
            throw new IOException("Unknown id " + id);
        }
        fillIn(harvestSource, uriInfo.getQueryParameters());
        getDatabase().updateHarvestSource(harvestSource);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @DELETE
    public Response deleteHarvestSource(@Context UriInfo uriInfo)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        HarvestSource harvestSource = getDatabase().getHarvestSource(
                folder, id);
        if (harvestSource == null) {
            throw new IOException("Unknown id " + id);
        }
        getDatabase().deleteHarvestSource(harvestSource);
        return Response.ok().build();
    }

    @Path("{folder: .*}")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getHarvestSources(
            @PathParam("folder") String folder) {
        List<HarvestSource> harvestSources =
            getDatabase().getHarvestSources(folder);
        return Response.ok(new HarvestSourcesResponse(harvestSources)).build();
    }

    @GET
    @Produces({"text/xml", "application/json"})
    public Response getHarvestSources() {
        return getHarvestSources("");
    }

    @Path("{folder: .*}/harvest")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response harvest(@Context UriInfo uriInfo) throws IOException {
        String folder = getFolderPath(uriInfo, 1, 2);
        String id = getId(uriInfo, 1);

        HarvestSource harvestSource = getDatabase().getHarvestSource(
                folder, id);
        if (harvestSource == null) {
            throw new IOException("Unknown id " + id + " in folder " + folder);
        }

        List<UnfinishedRecording> recordings = harvester.harvest(harvestSource);
        if (recordings != null) {
            return Response.ok(new UnfinishedRecordingsResponse(
                    recordings)).build();
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                harvestSource.getStatus()).build();
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

        HarvestSource source = getDatabase().getHarvestSource(folder, id);
        if (source == null) {
            throw new FileNotFoundException("Harvest Source " + id
                    + " not found");
        }

        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            WriteOnlyEntity[] exceptions = getExceptions(exceptionNames,
                    exceptionTypes);
            if (acltype.equals("play")) {
                secureDb.setRecordingPlayAcl(source, isPublic, exceptions);
            } else if (acltype.equals("read")) {
                if (exceptions.length > 0) {
                    return Response.status(Status.BAD_REQUEST).entity(
                            "There can be no exceptions to the read ACL"
                            ).build();
                }
                secureDb.setRecordingReadAcl(source, isPublic);
            } else if (acltype.equals("annotate")) {
                secureDb.setRecordingAnnotateAcl(source, isPublic,
                        exceptions);
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
        String acltype = getId(uriInfo, 0);

        HarvestSource source = getDatabase().getHarvestSource(folder, id);
        if (source == null) {
            throw new FileNotFoundException("Harvest Source " + id
                    + " not found");
        }

        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            if (acltype.equals("play")) {
                return Response.ok(
                        secureDb.getRecordingPlayAcl(source)).build();
            } else if (acltype.equals("read")) {
                return Response.ok(
                        secureDb.getRecordingReadAcl(source)).build();
            } else if (acltype.equals("annotate")) {
                return Response.ok(
                        secureDb.getRecordingAnnotateAcl(source)).build();
            }
        }
        return Response.ok().build();
    }
}
