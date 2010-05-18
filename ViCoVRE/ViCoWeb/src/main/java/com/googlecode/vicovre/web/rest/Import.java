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
import java.io.InputStream;

import javax.media.format.UnsupportedFormatException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import com.googlecode.vicovre.web.convert.ConvertSession;
import com.googlecode.vicovre.web.convert.ConvertSessionManager;
import com.sun.jersey.spi.inject.Inject;

@Path("/import")
public class Import {

    @Inject
    private ConvertSessionManager convertSessionManager = null;

    private CacheControl getNoCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return cacheControl;
    }

    @GET
    @Path("/create")
    @Produces("text/plain")
    public Response createSession(
            @DefaultValue("false") @QueryParam("live") boolean live,
            @DefaultValue("") @QueryParam("name") String name,
            @Context UriInfo uriInfo) {
        String id = convertSessionManager.createSession(live, name);
        String url = uriInfo.getBaseUri() + "import/" + id;
        return Response.ok(url).cacheControl(getNoCache()).build();
    }

    @GET
    @Path("/{id}")
    public Response receiveStreams(@PathParam("id") String id,
            @DefaultValue("") @QueryParam("venue") String venue,
            @DefaultValue("") @QueryParam("address") String address,
            @DefaultValue("0") @QueryParam("port") int port) throws Exception {
        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        if (!venue.equals("")) {
            session.receiveStreams(venue);
        } else if (!address.equals("") && (port >= 2) && (port <= 65534)
                && ((port % 2) == 0)) {
            session.receiveStreams(address, port);
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/{id}")
    @Consumes("multipart/form-data")
    public Response uploadData(@PathParam("id") String id,
            @Context HttpServletRequest request)
            throws FileUploadException, IOException {

        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        String streamId = "0";
        long timestamp = System.currentTimeMillis();
        long timeclock = 1000;
        long frame = -1;
        boolean inter = false;
        InputStream input = null;
        String contentType = null;
        long contentLength = -1;

        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iter = upload.getItemIterator(request);
        while ((input == null) && iter.hasNext()) {
            FileItemStream item = iter.next();
            String name = item.getFieldName();
            if (name.equals("streamid")) {
                streamId = Streams.asString(item.openStream());
            } else if (name.equals("timestamp")) {
                timestamp = Long.parseLong(Streams.asString(item.openStream()));
            } else if (name.equals("timeclock")) {
                timeclock = Long.parseLong(Streams.asString(item.openStream()));
            } else if (name.equals("frame")) {
                frame = Long.parseLong(Streams.asString(item.openStream()));
            } else if (name.equals("inter")) {
                inter = Boolean.parseBoolean(Streams.asString(
                        item.openStream()));
            } else if (name.equals("item")) {
                contentType = item.getContentType();
                if (item.getHeaders() != null) {
                    String clHeader =
                        item.getHeaders().getHeader("Content-Length");
                    if (clHeader != null) {
                        contentLength = Long.parseLong(clHeader);
                    }
                }
                System.err.println("Opening input stream for item");
                input = item.openStream();
            }
        }

        if (input == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        try {
            session.addStream(input, contentType, streamId, timestamp,
                    timeclock, frame, inter, contentLength);
        } catch (UnsupportedFormatException e) {
            return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
        }
        input.close();

        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSession(@PathParam("id") String id) {
        ConvertSession session = convertSessionManager.getSession(id);
        if (session == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        session.close();
        convertSessionManager.deleteSession(id);
        return Response.ok().build();
    }
}
