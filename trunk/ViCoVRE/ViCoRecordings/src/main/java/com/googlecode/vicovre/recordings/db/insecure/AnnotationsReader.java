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
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.utils.XmlIo;

public class AnnotationsReader {

    public static final List<Annotation> readAnnotations(InputStream input)
            throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        List<Annotation> annotations = new Vector<Annotation>();
        Node[] annotationNodes = XmlIo.readNodes(doc, "annotation");
        for (Node annotationNode : annotationNodes) {
            String id = XmlIo.readValue(annotationNode, "id");
            if (id == null) {
                throw new SAXException("Missing id for annotation");
            }
            String author = XmlIo.readValue(annotationNode, "author");
            if (author == null) {
                throw new SAXException("Missing author for annotation " + id);
            }
            String message = XmlIo.readValue(annotationNode, "message");
            if (message == null) {
                throw new SAXException("Missing message for annotation " + id);
            }
            String timestamp = XmlIo.readValue(annotationNode, "timestamp");
            if (timestamp == null) {
                throw new SAXException("Missing timestamp for annotation "
                        + id);
            }
            if (!timestamp.matches("\\d+")) {
                throw new SAXException("Timestamp is not a number for "
                        + "annotation " + id);
            }
            String[] tags = XmlIo.readValues(annotationNode, "tag");
            String[] people = XmlIo.readValues(annotationNode, "person");
            String responseTo = XmlIo.readValue(annotationNode, "responseTo");

            Annotation annotation = new Annotation(id,
                    Long.parseLong(timestamp), author,
                    StringEscapeUtils.unescapeXml(message), tags, people,
                    responseTo);
            annotations.add(annotation);
        }
        return annotations;
    }

    public static void writeAnnotations(OutputStream output,
            List<Annotation> annotations) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<annotations>");
        for (Annotation annotation : annotations) {
            writer.println("<annotation>");
            writer.println("<id>" + annotation.getId() + "</id>");
            writer.println("<author>" + annotation.getAuthor() + "</author>");
            writer.println("<message>"
                    + StringEscapeUtils.escapeXml(annotation.getMessage())
                    + "</message>");
            writer.println("<timestamp>" + annotation.getTimestamp()
                    + "</timestamp>");
            if (annotation.getTags() != null) {
                for (String tag : annotation.getTags()) {
                    writer.println("<tag>" + tag + "</tag>");
                }
            }
            if (annotation.getPeople() != null) {
                for (String person : annotation.getPeople()) {
                    writer.println("<person>" + person + "</person>");
                }
            }
            if (annotation.getResponseTo() != null) {
                writer.println("<responseTo>" + annotation.getResponseTo()
                        + "</responseTo>");
            }
            writer.println("</annotation>");
        }
        writer.println("</annotations>");
        writer.flush();
    }

}
