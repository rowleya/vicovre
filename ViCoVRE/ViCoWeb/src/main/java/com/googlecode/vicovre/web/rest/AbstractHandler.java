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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;

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
            folderPath += pathSegments.get(i).getPath() + "/";
        }
        return folderPath;
    }

    protected String getId(UriInfo uriInfo, int removeEnd) {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        String id = pathSegments.get(pathSegments.size() - 1
                - removeEnd).getPath();
        return id;
    }

    protected Metadata getMetadata(
            MultivaluedMap<String, String> details) throws IOException {
        String primaryKey = details.getFirst("metadataPrimaryKey");
        if (primaryKey == null) {
            throw new IOException("Missing metadata primary key");
        }
        String primaryValue = details.getFirst("metadata" + primaryKey);
        if (primaryValue == null) {
            throw new IOException("Missing metadata primary value");
        }

        Metadata metadata = new Metadata(primaryKey,
                primaryValue);
        for (String key : details.keySet()) {
            if (key.startsWith("metadata") && !key.equals("metadataPrimaryKey")
                    && !key.equals("metadata" + primaryKey)
                    && !key.endsWith("Editable") && !key.endsWith("Visible")
                    && !key.endsWith("Multiline")) {
                String actualKey = key.substring("metadata".length());
                String value = details.getFirst(key);
                boolean visible = true;
                String visibleString = details.getFirst(key + "Visible");
                if (visibleString != null) {
                    visible = visibleString.equals("true");
                }
                boolean editable = true;
                String editableString = details.getFirst(key + "Editable");
                if (editableString != null) {
                    editable = editableString.equals("true");
                }
                boolean multiline = false;
                String multilineString = details.getFirst(key + "Multiline");
                if (multilineString != null) {
                    multiline = multilineString.equals("true");
                }
                metadata.setValue(actualKey, value, visible, editable,
                        multiline);
            }
        }
        return metadata;
    }

    protected WriteOnlyEntity[] getExceptions(List<String> exceptionNames,
            List<String> exceptionTypes) {
        WriteOnlyEntity[] exceptions = new WriteOnlyEntity[0];
        if ((exceptionTypes != null) && (exceptionNames != null)) {
            if (exceptionTypes.size() != exceptionNames.size()) {
                return null;
            }
            exceptions = new WriteOnlyEntity[exceptionTypes.size()];
            for (int i = 0; i < exceptionTypes.size(); i++) {
                String type = exceptionTypes.get(i);
                String name = exceptionNames.get(i);
                exceptions[i] = new WriteOnlyEntity(name, type);
            }
        }
        return exceptions;
    }
}
