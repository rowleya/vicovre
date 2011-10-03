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

import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.web.convert.ChangeListener;
import com.googlecode.vicovre.web.convert.ConvertSession;
import com.googlecode.vicovre.web.convert.ConvertSessionManager;
import com.googlecode.vicovre.web.rest.response.ChangeResponse;
import com.googlecode.vicovre.web.rest.response.SessionResponse;
import com.googlecode.vicovre.web.rest.response.SessionsResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("/export")
public class Export {

    @Inject
    private ConvertSessionManager convertSessionManager = null;

    @Inject
    private RtpTypeRepository rtpTypeRepository = null;

    @Inject("defaultAudioRtpType")
    private Integer defaultAudioRtpType = null;

    @Inject("defaultVideoRtpType")
    private Integer defaultVideoRtpType = null;

    @Inject("defaultAGAudioRtpType")
    private Integer defaultAGAudioRtpType = null;

    @Inject("defaultAGVideoRtpType")
    private Integer defaultAGVideoRtpType = null;

    private CacheControl getNoCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return cacheControl;
    }

    @GET
    @Path("/list")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response listSessions() {
        return Response.ok(new SessionsResponse(
                convertSessionManager)).cacheControl(getNoCache()).build();
    }

    @GET
    @Path("/{id}/list")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response listStreams(@PathParam("id") String id) {
        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(new SessionResponse(session)).cacheControl(
                getNoCache()).build();
    }

    @GET
    @Path("/{id}/{streamid}/changed")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response hasChanged(@PathParam("id") String id,
            @PathParam("streamid") String streamId,
            @DefaultValue("0") @QueryParam("substream") int substream,
            @DefaultValue("") @QueryParam("changeId") String changeId,
            @DefaultValue("10000") @QueryParam("waitTime") int waitTime)
            throws FileNotFoundException {
        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).cacheControl(getNoCache()).build();
        }
        ChangeListener listener =
            session.getChangeListener(streamId, substream, changeId);
        ChangeResponse response = new ChangeResponse(listener.getId(),
                listener.waitForChange(waitTime));
        return Response.ok(response).cacheControl(getNoCache()).build();
    }

    @POST
    @Path("/{id}/{streamid}")
    @Produces("text/plain")
    public Response sendStream(@PathParam("id") String id,
            @PathParam("streamid") String streamId,
            @DefaultValue("0") @QueryParam("substreamid") int substream,
            @DefaultValue("-1") @QueryParam("rtptype") int rtpTypeNo,
            @DefaultValue("false") @QueryParam("ag") boolean ag,
            @DefaultValue("") @QueryParam("name") String name,
            @DefaultValue("") @QueryParam("note") String note,
            @DefaultValue("-1") @QueryParam("width") int width,
            @DefaultValue("-1") @QueryParam("height") int height,
            @DefaultValue("") @QueryParam("venue") String venue,
            @DefaultValue("") @QueryParam("address") String address,
            @DefaultValue("-1") @QueryParam("port") int port,
            @DefaultValue("127") @QueryParam("ttl") int ttl)
            throws FileNotFoundException {

        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (name.equals("")) {
            name = session.getName();
        }
        if (note.equals("")) {
            note = streamId;
        }
        System.err.println("Name = " + name + " note = " + note);

        int rtpType = rtpTypeNo;
        if (rtpType == -1) {
            Format format = session.getFormat(streamId, substream);
            if (format instanceof VideoFormat) {
                if (ag) {
                    rtpType = defaultAGVideoRtpType;
                } else {
                    rtpType = defaultVideoRtpType;
                }
            } else {
                if (ag) {
                    rtpType = defaultAGAudioRtpType;
                } else {
                    rtpType = defaultAudioRtpType;
                }
            }
        }

        RTPType realRtpType = rtpTypeRepository.findRtpType(rtpType);

        String sendid = null;

        try {
            if (!venue.equals("")) {
                sendid = session.sendStream(streamId, substream, venue, name,
                        note, realRtpType, width, height);
            } else if (!address.equals("") && (port >= 2) && (port <= 65534)
                    && ((port % 2) == 0)) {
                sendid = session.sendStream(streamId, substream, address, port,
                        ttl, name, note, realRtpType, width, height);
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }

        return Response.ok(sendid).cacheControl(getNoCache()).build();
    }

    @DELETE
    @Path("/{id}/{streamid}/{sendid}")
    public Response endSendStream(@PathParam("id") String id,
            @PathParam("streamid") String streamId,
            @PathParam("sendid") String sendId) {
        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        session.endSendStream(sendId);
        return Response.ok().cacheControl(getNoCache()).build();
    }

    @GET
    @Path("/{id}")
    public Response getFile(@PathParam("id") String id,
            @Context HttpServletRequest request) {

        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).cacheControl(getNoCache()).build();
        }

        return Response.ok().cacheControl(getNoCache()).build();
    }

}
