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

import org.apache.commons.lang.StringEscapeUtils;
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
        Node primaryKeyNode = XmlIo.readNode(doc, "primaryKey");
        String primaryKey = XmlIo.readAttr(primaryKeyNode, "name", null);
        String primaryValue = XmlIo.readAttr(primaryKeyNode, "value", null);
        if (primaryKey == null) {
            throw new SAXException("Missing primaryKey");
        }
        if (primaryValue == null) {
            throw new SAXException("Missing primaryValue");
        }
        RecordingMetadata metadata = new RecordingMetadata(primaryKey,
                primaryValue);
        Node[] keys = XmlIo.readNodes(doc, "key");
        for (Node keyNode : keys) {
            String key = XmlIo.readAttr(keyNode, "name", null);
            String value = XmlIo.readAttr(keyNode, "value", null);
            String visible = XmlIo.readAttr(keyNode, "visible", "true");
            String editable = XmlIo.readAttr(keyNode, "editable", "true");
            String multiline = XmlIo.readAttr(keyNode, "multiline", "false");
            metadata.setValue(key, value, visible.equals("true"),
                    editable.equals("true"), multiline.equals("true"));
        }
        return metadata;
    }

    public static RecordingMetadata readOldMetadata(InputStream input)
            throws IOException, SAXException{
        Node doc = XmlIo.read(input);
        RecordingMetadata metadata = new RecordingMetadata("name", "");
        String className = XmlIo.readValue(doc, "class");
        String[] fields = XmlIo.readFields(doc);
        for (String field : fields) {
            if (!field.equals("class")) {
                Node node = XmlIo.readNode(doc, field);
                String value = XmlIo.readAttr(node, "value", null);
                String key = RecordingMetadata.getKey(field);
                boolean editable = true;
                boolean visible = true;
                boolean multiline = false;
                if (className.startsWith(
                        "com.googlecode.vicovre.recordings.formats.MAGIC")) {
                    if (field.equals("Description")) {
                        editable = false;
                    } else {
                        visible = false;
                    }
                }
                if (field.equals("Description")) {
                    multiline = true;
                }
                metadata.setValue(key, value, visible, editable, multiline);
            }
        }
        return metadata;
    }

    public static void writeMetadata(RecordingMetadata metadata,
            OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<metadata>");
        String primaryKey = metadata.getPrimaryKey();
        writer.println("<primaryKey name=\"" + primaryKey
                + "\" value=\"" + metadata.getPrimaryValue() + "\"/>");
        for (String key : metadata.getKeys()) {
            if (!key.equals(primaryKey)) {
                writer.println("<key name=\"" + StringEscapeUtils.escapeXml(key)
                    + "\" value=\"" + StringEscapeUtils.escapeXml(
                            metadata.getValue(key))
                    + "\" visible=\"" + metadata.isVisible(key)
                    + "\" editable=\"" + metadata.isEditable(key)
                    + "\" mutliline=\"" + metadata.isMultiline(key)
                    + "\"/>");
            }
        }
        writer.println("</metadata>");
        writer.flush();
    }
}
