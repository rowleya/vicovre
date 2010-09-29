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
import com.googlecode.vicovre.security.rest.responses.GroupsResponse;
import com.googlecode.vicovre.security.rest.responses.UsersResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("group")
public class Group {

    @Inject
    private SecurityDatabase database = null;

    @GET
    @Produces({"application/json", "text/xml"})
    public Response getGroups() {
        List<String> groups = database.getGroups();
        return Response.ok(new GroupsResponse(groups)).build();
    }

    @Path("{group}/owner")
    @GET
    @Produces("text/plain")
    public Response getGroupOwner(@PathParam("group") String groupName) {
        return Response.ok(database.getGroupOwner(groupName)).build();
    }

    @Path("{group}/users")
    @GET
    @Produces({"application/json", "text/xml"})
    public Response getGroupUsers(@PathParam("group") String groupName) {
        List<String> users = database.getGroupUsers(groupName);
        return Response.ok(new UsersResponse(users)).build();
    }

    @Path("{group}")
    @PUT
    public Response addGroup(@PathParam("group") String groupName)
            throws IOException {
        database.addGroup(groupName);
        return Response.ok().build();
    }

    @Path("{group}/owner")
    @PUT
    public Response setGroupOwner(@PathParam("group") String groupName,
            @QueryParam("username") String username) throws IOException {
        database.setGroupOwner(groupName, username);
        return Response.ok().build();
    }

    @Path("{group}/users")
    @PUT
    public Response setGroupUsers(@PathParam("group") String groupName,
            @QueryParam("username") List<String> username) throws IOException {
        database.setGroupUsers(groupName, username);
        return Response.ok().build();
    }

    @Path("{group}")
    @DELETE
    public Response deleteGroup(@PathParam("group") String groupName)
            throws IOException {
        database.deleteGroup(groupName);
        return Response.ok().build();
    }


}
