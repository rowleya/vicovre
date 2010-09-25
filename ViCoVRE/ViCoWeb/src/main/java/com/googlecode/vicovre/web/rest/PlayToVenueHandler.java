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

import java.io.IOException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.googlecode.vicovre.recordings.PlaybackManager;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.sun.jersey.spi.inject.Inject;

@Path("play")
public class PlayToVenueHandler extends AbstractHandler {

    public PlayToVenueHandler(
            @Inject("database") RecordingDatabase database) {
        super(database);
    }

    @Path("{folder: .*}")
    @POST
    @Produces("text/plain")
    public Response play(@Context UriInfo uriInfo,
            @QueryParam("ag3VenueUrl") String ag3VenueUrl,
            @QueryParam("seek") @DefaultValue("0") int seek)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 1);
        String id = getId(uriInfo, 0);

        Recording recording = getDatabase().getRecording(folder, id);
        if (recording == null) {
            throw new IOException("Unknown recording " + id);
        }
        int playId = PlaybackManager.play(recording, ag3VenueUrl, seek);
        return Response.ok(uriInfo.getBaseUriBuilder().path("play").path(
                String.valueOf(playId)).build().toString()).build();
    }

    @Path("{id}/stop")
    @GET
    public Response stop(@PathParam("id") int id) {
        PlaybackManager.stop(id);
        return Response.ok().build();
    }

    @Path("{id}/pause")
    @GET
    public Response pause(@PathParam("id") int id) {
        PlaybackManager.pause(id);
        return Response.ok().build();
    }

    @Path("{id}/resume")
    @GET
    public Response resume(@PathParam("id") int id) {
        PlaybackManager.resume(id);
        return Response.ok().build();
    }

    @Path("{id}/seek")
    @GET
    public Response seek(@PathParam("id") int id,
            @QueryParam("seek") int seek) {
        PlaybackManager.seek(id, seek);
        return Response.ok().build();
    }

    @Path("{id}/time")
    @GET
    @Produces("text/plain")
    public Response getTime(@PathParam("id") int id) {
        return Response.ok(String.valueOf(PlaybackManager.getTime(id))).build();
    }
}
