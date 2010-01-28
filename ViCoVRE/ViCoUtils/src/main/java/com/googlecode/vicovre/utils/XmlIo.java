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

package com.googlecode.vicovre.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class XmlIo {

    /**
     * The format of stored or transmitted dates
     */
    public static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSSS");

    public static Node read(InputStream input)
            throws SAXException, IOException {
        DOMParser parser = new DOMParser();
        InputSource source = new InputSource(input);
        parser.parse(source);
        return parser.getDocument().getDocumentElement();
    }

    private static Object getValue(Object object, String field) {
        String method = "get" + field.substring(0, 1).toUpperCase()
            + field.substring(1);
        try {
            Method getMethod = object.getClass().getMethod(method);
            Object value = getMethod.invoke(object);
            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeValue(Object object, String field,
            PrintWriter writer) {
        Object value = getValue(object, field);
        if (value != null) {
            writeValue(field, value.toString(), writer);
        }
    }

    public static void writeDate(Object object, String field,
            PrintWriter writer) {
        Object date = getValue(object, field);
        if (date != null) {
            writeValue(field, DATE_FORMAT.format(date), writer);
        }
    }

    public static void writeValue(String field, String value,
            PrintWriter writer) {
        writer.println("    <" + field + " value=\""
                + StringEscapeUtils.escapeXml(value) + "\"/>");
    }

    public static void setValue(Object object, String field, Object value,
            Class<?> type) {
        String method = "set" + field.substring(0, 1).toUpperCase()
            + field.substring(1);
        try {
            Method setMethod = object.getClass().getMethod(method, type);
            setMethod.invoke(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String readValue(Node node, String field) {
        String[] values = readValues(node, field);
        if (values.length > 0) {
            return values[0];
        }
        return null;
    }

    public static Node readNode(Node node, String field) {
        Node[] nodes = readNodes(node, field);
        if (nodes.length > 0) {
            return nodes[0];
        }
        return null;
    }

    public static String readContent(Node node, String field) {
        String[] contents = readContents(node, field);
        if (contents.length > 0) {
            return contents[0];
        }
        return null;
    }

    public static Node[] readNodes(Node node, String field) {
        NodeList list = node.getChildNodes();
        Vector<Node> values = new Vector<Node>();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeName().equals(field)) {
                values.add(n);
            }
        }
        return values.toArray(new Node[0]);
    }

    public static String[] readFields(Node node) {
        NodeList list = node.getChildNodes();
        Vector<String> values = new Vector<String>();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n instanceof Element) {
                values.add(n.getNodeName());
            }
        }
        return values.toArray(new String[0]);
    }

    public static String[] readValues(Node node, String field) {
        Node[] nodes = readNodes(node, field);
        String[] values = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            Node value = nodes[i].getAttributes().getNamedItem("value");
            values[i] = value.getTextContent();
        }
        return values;
    }

    public static String[] readContents(Node node, String field) {
        Node[] nodes = readNodes(node, field);
        String[] values = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            values[i] = nodes[i].getTextContent();
        }
        return values;
    }

    public static void setInt(Node doc, Object object, String field) {
        String value = readValue(doc, field);
        if (value != null) {
            setValue(object, field, Integer.valueOf(value), Integer.TYPE);
        }
    }

    public static void setString(Node doc, Object object, String field) {
        setValue(object, field, readValue(doc, field), String.class);
    }

    public static void setDate(Node doc, Object object, String field) {
        String date = null;
        try {
            date = readValue(doc, field);
            if (date != null) {
                Date dateValue = DATE_FORMAT.parse(date);
                setValue(object, field, dateValue, Date.class);
            }
        } catch (Exception e) {
            System.err.println("Warning: error parsing date " + date);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void setLong(Node doc, Object object, String field) {
        String value = readValue(doc, field);
        if (value != null) {
            setValue(object, field, Long.valueOf(value), Long.TYPE);
        }
    }

    public static String readAttr(Node doc, String field, String def) {
        Node attr = doc.getAttributes().getNamedItem(field);
        if (attr == null) {
            return def;
        }
        return attr.getTextContent();
    }

    public static String[] readAttrs(Node doc) {
        NamedNodeMap attr = doc.getAttributes();
        Vector<String> values = new Vector<String>();
        for (int i = 0; i < attr.getLength(); i++) {
            values.add(attr.item(i).getLocalName());
        }
        return values.toArray(new String[0]);
    }
}
