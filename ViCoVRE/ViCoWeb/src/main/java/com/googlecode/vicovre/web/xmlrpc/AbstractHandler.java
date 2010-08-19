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

package com.googlecode.vicovre.web.xmlrpc;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

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

    protected Folder getFolder(String folderPath) throws XmlRpcException {
        Folder folder = database.getTopLevelFolder();
        if ((folderPath != null) && !folderPath.equals("")) {
            folder = database.getFolder(
                new File(database.getTopLevelFolder().getFile(), folderPath));
            if (folder == null) {
                throw new XmlRpcException("Unknown folder " + folderPath);
            }
        }
        return folder;
    }

    public static RecordingMetadata getMetadata(
            Map<String, Object> details) throws XmlRpcException {
        String primaryKey = (String) details.get("primaryKey");
        if (primaryKey == null) {
            throw new XmlRpcException("Missing metadata primary key");
        }
        String primaryValue = (String) details.get(primaryKey);
        if (primaryValue == null) {
            throw new XmlRpcException("Missing metadata primary value");
        }

        RecordingMetadata metadata = new RecordingMetadata(primaryKey,
                primaryValue);
        for (String key : details.keySet()) {
            if (!key.equals("primaryKey") && !key.equals(primaryKey)
                    && !key.endsWith("Editable") && !key.endsWith("Visible")
                    && !key.endsWith("Multiline")) {
                String value = (String) details.get(key);
                boolean visible = true;
                String visibleString = (String) details.get(key + "Visible");
                if (visibleString != null) {
                    visible = visibleString.equals("true");
                }
                boolean editable = true;
                String editableString = (String) details.get(key + "Editable");
                if (editableString != null) {
                    editable = editableString.equals("true");
                }
                boolean multiline = false;
                String multilineString = (String) details.get(
                        key + "Multiline");
                if (multilineString != null) {
                    multiline = multilineString.equals("true");
                }
                metadata.setValue(key, value, visible, editable,
                        multiline);
            }
        }
        return metadata;
    }

    public static Map<String, Object> getDetails(RecordingMetadata metadata) {
        Map<String, Object> details = new HashMap<String, Object>();
        details.put("primaryKey", metadata.getPrimaryKey());
        for (String key : metadata.getKeys()) {
            details.put(key, metadata.getValue(key));
            if (!key.equals(metadata.getPrimaryKey())) {
                details.put(key + "Visible", metadata.isVisible(key));
                details.put(key + "Editable", metadata.isEditable(key));
            }
        }
        return details;
    }

    public static Map<String, Object> getDetails(Object object,
            String... exclude) throws XmlRpcException {
        Map<String, Object> details = new HashMap<String, Object>();

        Class<?> cls = object.getClass();
        Method[] methods = cls.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("get")
                    && method.getParameterTypes().length == 0) {
                String field = method.getName().substring("get".length());

                try {
                    Method setMethod = cls.getMethod("set" + field,
                            method.getReturnType());
                    if (setMethod != null) {
                        try {
                            field = field.substring(0, 1).toLowerCase()
                                + field.substring(1);
                             boolean excluded = false;
                             for (String excludeItem : exclude) {
                                 if (excludeItem.equals(field)) {
                                     excluded = true;
                                     break;
                                 }
                             }
                             if (!excluded) {
                                Object value = method.invoke(object);
                                if (value != null) {
                                    if (value.getClass().equals(Long.class)
                                            || value.getClass().equals(
                                                    Long.TYPE)) {
                                        details.put(field,
                                                ((Long) value).intValue());
                                    } else {
                                        details.put(field, value);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new XmlRpcException(
                                "Error getting metadata value " + field);
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // Do Nothing
                }
            }
        }
        return details;
        }
}
