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

import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.web.rest.response.FoldersResponse;
import com.sun.jersey.spi.inject.Inject;

@Path("/folders")
public class FolderHandler extends AbstractHandler {

    public FolderHandler(@Inject("database") RecordingDatabase database) {
        super(database);
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
        for (String subFolder : folderList) {
            getFolders(folder + "/" + subFolder, folders);
        }
    }

    @Path("/{folder: .*}")
    @PUT
    public Response createFolder(@PathParam("folder") String folder) {
        File file = getDatabase().getFile(folder);
        if (file.mkdirs()) {
            return Response.ok().build();
        }
        return Response.notModified().build();
    }
}
