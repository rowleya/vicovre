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

package com.googlecode.vicovre.security.rest;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.rest.responses.UsersResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("user")
public class User {

    @Inject
    private SecurityDatabase database = null;

    @GET
    @Produces({"application/json", "text/xml"})
    public Response getUsers() {
        List<String> users = database.getUsers();
        return Response.ok(new UsersResponse(users)).build();
    }

    @Path("{username}/role")
    @GET
    @Produces("text/plain")
    public Response getUserRole(@PathParam("username") String username) {
        return Response.ok(database.getRole(username)).build();
    }

    @Path("{username}")
    @PUT
    public Response addUser(@PathParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("role") String role) throws IOException {
        if (role == null) {
            database.addUser(username, password);
        } else {
            database.addUser(username, password, role);
        }
        return Response.ok().build();
    }

    @Path("{username}/password")
    @PUT
    public Response setUserPassword(@PathParam("username") String username,
            @QueryParam("oldPassword") String oldPassword,
            @QueryParam("password") String password) throws IOException {
        if (oldPassword == null) {
            database.setUserPassword(username, password);
        } else {
            database.setPassword(oldPassword, password);
        }
        return Response.ok().build();
    }

    @Path("{username}/role")
    @PUT
    public Response setUserRole(@PathParam("username") String username,
            @QueryParam("role") String role) throws IOException {
        database.setUserRole(username, role);
        return Response.ok().build();
    }

    @Path("{username}")
    @DELETE
    public Response deleteUser(@PathParam("username") String username) {
        database.deleteUser(username);
        return Response.ok().build();
    }
}
