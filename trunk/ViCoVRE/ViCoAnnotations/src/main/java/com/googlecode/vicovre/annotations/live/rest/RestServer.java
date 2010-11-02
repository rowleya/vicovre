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

package com.googlecode.vicovre.annotations.live.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.annotations.live.Client;
import com.googlecode.vicovre.annotations.live.DoneMessage;
import com.googlecode.vicovre.annotations.live.Message;
import com.googlecode.vicovre.annotations.live.NameInUseException;
import com.googlecode.vicovre.annotations.live.Server;
import com.sun.jersey.spi.inject.Inject;

@Path("/annotations")
public class RestServer {

    private static final String CLIENT = "client";

    private Server server = null;

    public RestServer(@Inject Server server) {
        this.server = server;
    }

    private CacheControl getNoCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return cacheControl;
    }

    @Path("/login")
    @POST
    public Response login(@QueryParam("name") String name,
                        @QueryParam("email") String email,
                        @Context HttpServletRequest request) {
        if ((name != null) && (email != null)
                && !name.equals("") && !email.equals("")) {
            try {
                Client client = server.createClient(name, email);
                HttpSession session = request.getSession();
                session.setAttribute(CLIENT, client);
                session.setMaxInactiveInterval(365 * 24 * 60 * 60);
                return Response.ok().build();
            } catch (NameInUseException e) {
                return Response.status(Status.CONFLICT).build();
            }
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @Path("/send")
    @POST
    public Response sendMessage(@Context HttpServletRequest request,
            @QueryParam("timestamp") long timestamp,
            @QueryParam("author") String author,
            @QueryParam("message") String message,
            @QueryParam("tag") List<String> tags,
            @QueryParam("person") List<String> people,
            @QueryParam("responseTo") String responseTo) {
        HttpSession session = request.getSession();
        Client client = (Client) session.getAttribute(CLIENT);
        if (client != null) {
            client.setMessage(new Annotation(timestamp, author, message,
                    tags.toArray(new String[0]), people.toArray(new String[0]),
                    responseTo));
            return Response.status(Status.OK).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @Path("/edit/{id}")
    @PUT
    public Response editMessage(@Context HttpServletRequest request,
            @PathParam("id") String id,
            @QueryParam("timestamp") long timestamp,
            @QueryParam("author") String author,
            @QueryParam("message") String message,
            @QueryParam("tag") List<String> tags,
            @QueryParam("person") List<String> people,
            @QueryParam("responseTo") String responseTo) {
        HttpSession session = request.getSession();
        Client client = (Client) session.getAttribute(CLIENT);
        if (client != null) {

            client.setMessage(new Annotation(id, timestamp, author, message,
                    tags.toArray(new String[0]), people.toArray(new String[0]),
                    responseTo));
            return Response.status(Status.OK).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @Path("/date")
    @GET
    @Produces("text/plain")
    public Response getDate() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        return Response.ok(timestamp).cacheControl(getNoCache()).build();
    }

    @Produces({"text/xml", "application/json"})
    @GET
    public Response getMessage(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        Client client = (Client) session.getAttribute(CLIENT);
        if (client != null) {
            Message message = client.getNextMessage();
            return Response.ok(message).cacheControl(getNoCache()).build();
        }
        return Response.ok(new DoneMessage()).cacheControl(
                getNoCache()).build();
    }

    @Path("/close")
    @DELETE
    public Response close(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        Client client = (Client) session.getAttribute(CLIENT);
        if (client != null) {
            client.close();
            session.removeAttribute(CLIENT);
        }
        return Response.ok().build();
    }
}
