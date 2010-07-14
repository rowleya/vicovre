/*
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

package com.googlecode.vicovre.annotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LiveAnnotationParser extends DefaultHandler {

    // The XML parser

    private String currentElement = null;
    private String currentValue = null;

    private LiveAnnotation annotation = null;

    public void parse(String xml) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        String txt = xml;
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new ByteArrayInputStream(txt.getBytes()), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parse(InputStream inputStream) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void characters(char[] buf, int offset, int len) throws SAXException {
        String s = new String(buf, offset, len);
        if (!s.trim().equals("")) {
            currentValue += s;
        }
    }

    public void startElement(String namespace, String lname, String qname,
            Attributes attr) {
        Class< ? > cls = annotation.getClass();
        if (attr.getLength() != 0) {
            String name = "set" + qname.substring(0, 1).toUpperCase()
                + qname.substring(1);
            try {
                Method meth = cls.getMethod(name, Attributes.class);
                meth.invoke(annotation, new Object[] {attr});
            } catch (Exception e) {
                System.err.println("no method: " + name);
            }
            currentElement = null;
        } else {
            currentElement = qname;
            currentValue = "";
        }
    }

    public void endElement(String namespaceURI, String localName, String qname)
            throws SAXException {
        if (currentElement != null) {
            String name = qname.substring(0, 1).toUpperCase() + qname.substring(1);
            annotation.setValueOf(name, currentValue);
        }
    }

}