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

package com.googlecode.vicovre.recordings.db.insecure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * A reader of recording Metadata
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordingMetadataReader {

    public static RecordingMetadata readMetadata(InputStream input)
            throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        String className = XmlIo.readValue(doc, "class");
        try {
            Class<?> cls = Class.forName(className);
            Object object = cls.newInstance();
            if (!(object instanceof RecordingMetadata)) {
                throw new RuntimeException("Type " + className
                        + " not RecordingMetadata");
            }
            RecordingMetadata metadata = (RecordingMetadata) object;
            Method[] methods = cls.getMethods();
            for (Method method : methods) {
                if (method.getName().startsWith("get")
                        && method.getParameterTypes().length == 0) {
                    String field = method.getName().substring("get".length());
                    try {
                        Method setMethod = cls.getMethod("set" + field,
                                method.getReturnType());
                        if (setMethod != null) {
                            String value = XmlIo.readValue(doc, field);
                            setMethod.invoke(object, value);
                        }
                    } catch (NoSuchMethodException e) {
                        // Do Nothing
                    }
                }
            }
            return metadata;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeMetadata(RecordingMetadata metadata,
            OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<metadata>");
        Class<?> cls = metadata.getClass();
        XmlIo.writeValue("class", cls.getCanonicalName(), writer);
        Method[] methods = cls.getMethods();
        try {
            for (Method method : methods) {
                if (method.getName().startsWith("get")
                        && method.getParameterTypes().length == 0) {
                    String field = method.getName().substring("get".length());
                    try {
                        Method setMethod = cls.getMethod("set" + field,
                                method.getReturnType());
                        if (setMethod != null) {
                            String value = (String) method.invoke(metadata);
                            XmlIo.writeValue(field, value, writer);
                        }
                    } catch (NoSuchMethodException e) {
                        // Do Nothing
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        writer.println("</metadata>");
        writer.flush();
    }
}
