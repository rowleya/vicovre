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

package com.googlecode.vicovre.web.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.utils.XmlIo;

public class UserReader {

    public static User readUser(InputStream input)
            throws SAXException, IOException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        Node document = XmlIo.read(input);
        Node credentialNode = XmlIo.readNode(document, "credentials");
        String credentialsClassName = XmlIo.readAttr(credentialNode,
                "className", null);
        HashMap<String, String> credentialMap = new HashMap<String, String>();
        for (String field : credentialMap.keySet()) {
            String value = XmlIo.readValue(credentialNode, field);
            credentialMap.put(field, value);
        }
        Class<? extends Credentials> credentialsClass =
            (Class<? extends Credentials>) Class.forName(credentialsClassName);
        Credentials credentials = credentialsClass.newInstance();
        credentials.setValues(credentialMap);
        User user = new User();
        user.setName(XmlIo.readValue(document, "name"));
        user.setEmail(XmlIo.readValue(document, "email"));
        return user;
    }

    public static void writeUser(User user, OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<user>");
        HashMap<String, String> credentials = user.getCredentials().getValues();
        writer.println("<credentials className=\""
                + credentials.getClass().getCanonicalName() + "\">");
        for (String name : credentials.keySet()) {
            XmlIo.writeValue(name, credentials.get(name), writer);
        }
        writer.println("</credentials>");
        XmlIo.writeValue(user, "name", writer);
        XmlIo.writeValue(user, "email", writer);
        writer.println("</user>");
    }

}

