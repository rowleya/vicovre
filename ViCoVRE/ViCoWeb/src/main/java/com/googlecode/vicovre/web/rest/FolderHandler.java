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
import java.util.List;
import java.util.Vector;

import javax.ws.rs.GET;
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

import com.googlecode.vicovre.recordings.BooleanFieldSet;
import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;
import com.googlecode.vicovre.web.rest.response.FoldersResponse;
import com.googlecode.vicovre.web.rest.response.StreamsMetadataResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("/folders")
public class FolderHandler extends AbstractHandler {

    private LayoutRepository layoutRepository = null;

    public FolderHandler(@Inject("database") RecordingDatabase database,
            @Inject("layoutRepository") LayoutRepository layoutRepository) {
        super(database);
        this.layoutRepository = layoutRepository;
    }

    @Path("/list")
    @GET
    @Produces("application/json")
    public Response getFolders() {
        Vector<String> folders = new Vector<String>();
        getFolders("", folders);
        return Response.ok(new FoldersResponse(folders)).build();
    }

    private void getFolders(String folder, Vector<String> folders) {
        folders.add(folder);
        List<String> folderList = getDatabase().getSubFolders(folder);
        if (folderList != null) {
            for (String subFolder : folderList) {
                getFolders(folder + "/" + subFolder, folders);
            }
        }
    }

    @Path("/{parent: .*}/{folder}")
    @PUT
    public Response createFolder(@PathParam("parent") String parent,
            @PathParam("folder") String folder) throws IOException {
        if (getDatabase().addFolder(parent, folder, null)) {
            return Response.ok().build();
        }
        return Response.notModified().build();
    }

    @Path("/{folder}")
    @PUT
    public Response createFolder(@PathParam("folder") String folder)
            throws IOException {
        return createFolder("", folder);
    }

    @Path("/{folder: .*}/streams")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getAllStreamMetadata(@PathParam("folder") String folder) {
        StreamsMetadataResponse response = new StreamsMetadataResponse();
        List<Recording> recordings = getDatabase().getRecordings(folder);
        for (Recording recording : recordings) {
            List<Stream> streams = recording.getStreams();
            response.addStreams(streams);
        }
        return Response.ok(response).build();
    }

    @Path("/streams")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getAllStreamMetadata() {
        return getAllStreamMetadata("");
    }

    @Path("/{folder: .*}/layout")
    @PUT
    public Response setDefaultFolderLayout(@PathParam("folder") String folder,
            @QueryParam("name") String name,
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime,
            @Context UriInfo uriInfo) throws IOException {
        LayoutRepository repository = layoutRepository;
        Layout layout = layoutRepository.findLayout(name);
        if (layout == null) {
            return Response.status(Status.NOT_FOUND).entity(
                    "Layout " + name + " not found").build();
        }

        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        DefaultLayout defaultLayout = new DefaultLayout(repository);
        defaultLayout.setName(name);
        defaultLayout.setTime(startTime);
        defaultLayout.setEndTime(endTime);
        for (LayoutPosition position : layout.getStreamPositions()) {
            if (position.isAssignable()) {
                BooleanFieldSet set = new BooleanFieldSet(
                        BooleanFieldSet.AND_OPERATION);
                String positionName =
                    params.getFirst(position.getName() + "Name");
                String positionNote =
                    params.getFirst(position.getName() + "Note");
                if (positionName == null) {
                    System.err.println("Missing position " + position.getName());
                    return Response.status(Status.BAD_REQUEST).entity(
                        "No position " + position.getName()
                        + " in request").build();
                }
                set.addField("name", positionName);
                if (positionNote != null) {
                    set.addField("note", positionNote);
                }
                defaultLayout.setField(position.getName(), set);
            }
        }

        List<String> audioNames = params.get("audioName");
        for (String audioName : audioNames) {
            BooleanFieldSet set = new BooleanFieldSet(
                    BooleanFieldSet.AND_OPERATION);
            set.addField("name", audioName);
            defaultLayout.addAudioStream(set);
        }

        getDatabase().setDefaultLayout(folder, defaultLayout);

        return Response.ok().build();
    }

    @Path("/layout")
    @PUT
    public Response setDefaultFolderLayout(
            @QueryParam("name") String name,
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime,
            @Context UriInfo uriInfo) throws IOException {
        return setDefaultFolderLayout("", name, startTime, endTime, uriInfo);
    }

    @Path("/{folder: .*}/metadata")
    @PUT
    public Response setFolderMetadata(@Context UriInfo uriInfo)
            throws IOException {
        String folder = getFolderPath(uriInfo, 1, 1);
        getDatabase().setFolderMetadata(folder,
                getMetadata(uriInfo.getQueryParameters()));
        return Response.ok().build();
    }

    @Path("/metadata")
    @PUT
    public Response setBaseFolderMetadata(@Context UriInfo uriInfo)
            throws IOException {
        return setFolderMetadata(uriInfo);
    }
    @Path("/{folder: .*}/metadata")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getFolderMetadata(@PathParam("folder") String folder) {
        Metadata metadata = getDatabase().getFolderMetadata(folder);
        return Response.ok(metadata).build();
    }

    @Path("/metadata")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getFolderMetadata() {
        return getFolderMetadata("");
    }

    @Path("/{folder: .*}/acl")
    @PUT
    public Response setAcl(@PathParam("folder") String folder,
            @QueryParam("public") boolean isPublic,
            @QueryParam("exceptionType") List<String> exceptionTypes,
            @QueryParam("exceptionName") List<String> exceptionNames)
            throws IOException {
        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            WriteOnlyEntity[] exceptions = getExceptions(exceptionNames,
                    exceptionTypes);
            if (exceptions == null) {
                Response.status(Status.BAD_REQUEST).entity(
                        "The number of exceptionType parameters must match"
                        + " the number of exceptionName parameters").build();
            }
            secureDb.setFolderReadAcl(folder, isPublic, exceptions);
        }
        return Response.ok().build();
    }

    @Path("/{folder: .*}/acl")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getAcl(@PathParam("folder") String folder) {
        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            return Response.ok(secureDb.getFolderReadAcl(folder)).build();
        }
        return Response.ok().build();
    }

    @Path("/{folder: .*}/lifetime")
    @GET
    @Produces("text/plain")
    public Response getLifetime(@PathParam("folder") String folder) {
        RecordingDatabase database = getDatabase();
        long lifetime = database.getFolderLifetime(folder);
        return Response.ok(String.valueOf(lifetime)).build();
    }

    @Path("/{folder: .*}/lifetime")
    @PUT
    public Response setLifetime(@PathParam("folder") String folder,
            @QueryParam("lifetime") long lifetime) throws IOException {
        RecordingDatabase database = getDatabase();
        database.setFolderLifetime(folder, lifetime);
        return Response.ok().build();
    }

    @Path("/{folder: .*}/owner")
    @GET
    @Produces("text/plain")
    public Response getOwner(@PathParam("folder") String folder) {
        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            return Response.ok(secureDb.getFolderOwner(folder)).build();
        }
        return Response.ok().build();
    }

    @Path("/{folder: .*}/owner")
    @PUT
    public Response setOwner(@PathParam("folder") String folder,
            @QueryParam("owner") String owner) throws IOException {
        RecordingDatabase database = getDatabase();
        if (database instanceof SecureRecordingDatabase) {
            SecureRecordingDatabase secureDb =
                (SecureRecordingDatabase) database;
            secureDb.setFolderOwner(folder, owner);
        }
        return Response.ok().build();
    }
}
