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

package com.googlecode.vicovre.security.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.utils.XmlIo;

public class GroupReader {

    public static Group readGroup(InputStream input,
            HashMap<String, User> users) throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        String name = XmlIo.readContent(doc, "name");
        String ownerName = XmlIo.readContent(doc, "owner");
        User owner = users.get(ownerName);
        if (owner == null) {
            throw new SAXException("Unknown owner " + ownerName + " of group "
                    + name);
        }
        Group group = new Group(name, owner);
        String[] userNames = XmlIo.readContents(doc, "user");
        for (String username : userNames) {
            User user = users.get(username);
            if (user == null) {
                throw new SAXException("Unknown user " + user + " in group "
                        + name);
            }
            group.addUser(user);
        }
        return group;
    }

    public static void writeGroup(OutputStream output, Group group) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<group>");
        writer.println("<name>" + group.getName() + "</name>");
        writer.println("<owner>" + group.getOwner().getUsername() + "</owner>");
        for (User user : group.getUsers()) {
            writer.println("<user>" + user.getUsername() + "</user>");
        }
        writer.println("</group>");
        writer.flush();
    }

}
