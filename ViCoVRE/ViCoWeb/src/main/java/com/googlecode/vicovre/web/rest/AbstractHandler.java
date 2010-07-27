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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;

public abstract class AbstractHandler {

    private RecordingDatabase database = null;

    protected AbstractHandler(RecordingDatabase database) {
        this.database = database;
    }

    protected RecordingDatabase getDatabase() {
        return database;
    }

    protected String getFolderPath(UriInfo uriInfo, int removeStart,
            int removeEnd) {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        String folderPath = "";
        for (int i = removeStart;
                i < pathSegments.size() - removeEnd; i++) {
            folderPath += pathSegments.get(i).getPath();
        }
        return folderPath;
    }

    protected String getId(UriInfo uriInfo, int removeEnd) {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        String id = pathSegments.get(pathSegments.size() - 1
                - removeEnd).getPath();
        return id;
    }

    protected Folder getFolder(String folderPath) throws IOException {
        Folder folder = database.getTopLevelFolder();
        if ((folderPath != null) && !folderPath.equals("")) {
            folder = database.getFolder(
                new File(database.getTopLevelFolder().getFile(), folderPath));
            if (folder == null) {
                throw new IOException("Unknown folder " + folderPath);
            }
        }
        return folder;
    }

    protected void fillIn(RecordingMetadata metadata,
            MultivaluedMap<String, String> details) throws IOException {
        Class<?> cls = metadata.getClass();
        Method[] methods = cls.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get")
                    && method.getParameterTypes().length == 0) {
                String field = method.getName().substring("get".length());
                try {
                    Method setMethod = cls.getMethod("set" + field,
                            String.class);
                    if (setMethod != null) {
                        field = field.substring(0, 1).toLowerCase()
                            + field.substring(1);
                        String value = details.getFirst("metadata_" + field);
                        if (value != null) {
                            try {
                                setMethod.invoke(metadata, value);
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new IOException(
                                    "Error setting metadata value " + field);
                            }
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // Do Nothing
                }
            }
        }
    }
}
