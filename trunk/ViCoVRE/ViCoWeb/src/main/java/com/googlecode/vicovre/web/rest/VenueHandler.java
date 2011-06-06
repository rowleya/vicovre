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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.googlecode.onevre.ag.types.ConnectionDescription;
import com.googlecode.onevre.ag.types.server.VenueServer;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.web.rest.response.VenueServersResponse;
import com.googlecode.vicovre.web.rest.response.VenuesResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("/venue")
public class VenueHandler {

    private static final long VENUE_REFRESH_TIMEOUT = 3600000;

    private static final HashMap<String, ConnectionDescription[]> KNOWN_VENUES =
        new HashMap<String, ConnectionDescription[]>();

    private static final HashMap<String, Long> KNOWN_VENUES_CACHE_TIME =
        new HashMap<String, Long>();

    @Inject("database")
    private RecordingDatabase database = null;

    private String fillInVenueServer(String server) throws MalformedURLException {
        URL url = null;
        try {
            url = new URL(server);
        } catch (MalformedURLException e) {
            url = new URL("https://" + server);
        }
        int port = url.getPort();
        if (port == -1) {
            port = 8000;
        }
        String protocol = url.getProtocol();
        if (protocol == null || protocol.equals("")) {
            protocol = "https";
        }
        final String venueServerUrl = protocol + "://"
            + url.getHost() + ":" + port + "/VenueServer";
        return venueServerUrl;
    }

    @Path("/servers")
    @GET
    @Produces("application/json")
    public Response getVenueServers() {
        return Response.ok(new VenueServersResponse(Arrays.asList(
                database.getKnownVenueServers()))).build();
    }

    @Path("/server")
    @GET
    @Produces("text/plain")
    public Response getVenueServer(@QueryParam("url") String server) {
        try {
            String venueServerUrl = fillInVenueServer(server);
            return Response.ok(venueServerUrl).build();
        } catch (MalformedURLException e1) {
            return Response.status(
                    Status.INTERNAL_SERVER_ERROR).entity(
                            "Invalid URL").build();
        }
    }

    @Path("/venues")
    @GET
    @Produces("application/json")
    public Response getVenues(@QueryParam("url") String server) {
        try {
            System.err.println("Server = " + server);
            String venueServerUrl = fillInVenueServer(server);
            ConnectionDescription[] venues = KNOWN_VENUES.get(venueServerUrl);
            long lastCacheTime = 0;
            if (venues != null) {
                lastCacheTime = KNOWN_VENUES_CACHE_TIME.get(venueServerUrl);
            }
            if ((venues == null) ||
                    ((System.currentTimeMillis() - lastCacheTime)
                            > VENUE_REFRESH_TIMEOUT)) {
                VenueServer venueServer = new VenueServer(venueServerUrl);
                venues = venueServer.getVenues(null);
                Arrays.sort(venues);
                KNOWN_VENUES.put(venueServerUrl, venues);
                KNOWN_VENUES_CACHE_TIME.put(venueServerUrl,
                        System.currentTimeMillis());
                database.addVenueServer(venueServerUrl);
            }
            return Response.ok(new VenuesResponse(venueServerUrl,
                    venues)).build();
        } catch (Throwable e) {
            e.printStackTrace();
            return Response.serverError().entity(
                    "Error: " + e.getMessage()).build();
        }
    }
}
