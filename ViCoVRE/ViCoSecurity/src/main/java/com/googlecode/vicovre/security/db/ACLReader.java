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

public class ACLReader {

    public static ACL readACL(InputStream input, HashMap<String, User> users,
            HashMap<String, Group> groups, HashMap<String, Role> roles)
            throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        String id = XmlIo.readValue(doc, "id");
        String ownerName = XmlIo.readValue(doc, "owner");
        User owner = users.get(ownerName);
        if (owner == null) {
            throw new SAXException("Unknown owner " + owner);
        }
        String allowString = XmlIo.readValue(doc, "allow");
        boolean allow = Boolean.parseBoolean(allowString);

        ACL acl = new ACL(id, owner, allow);

        Node[] exceptions = XmlIo.readNodes(doc, "exception");
        for (Node exception : exceptions) {
            String type = XmlIo.readAttr(exception, "type", null);
            String name = XmlIo.readAttr(exception, "name", null);
            if (type == null) {
                throw new SAXException("Missing type on exception");
            } else if (name == null) {
                throw new SAXException("Missing name on exception");
            } else if (type.equals("user")) {
                User user = users.get(name);
                if (user == null) {
                    throw new SAXException("User " + name + " unknown");
                }
                acl.addException(user);
            } else if (type.equals("group")) {
                Group group = groups.get(name);
                if (group == null) {
                    throw new SAXException("Group " + name + " unknown");
                }
                acl.addException(group);
            } else if (type.equals("role")) {
                Role role = roles.get(name);
                if (role == null) {
                    throw new SAXException("Role " + name + " unknown");
                }
                acl.addException(role);
            }
        }

        return acl;
    }

    public static void writeACL(OutputStream output, ACL acl) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<acl>");
        writer.println("<id>" + acl.getId() + "</id>");
        writer.println("<owner>" + acl.getOwner().getUsername() + "</owner>");
        writer.println("<allow>" + acl.isAllow() + "</owner>");
        for (Entity entity : acl.getExceptions()) {
            String type = null;
            String name = null;
            if (entity instanceof User) {
                type = "user";
                name = ((User) entity).getUsername();
            } else if (entity instanceof Group) {
                type = "group";
                name = ((Group) entity).getName();
            } else if (entity instanceof Role) {
                type = "role";
                name = ((Role) entity).getName();
            }
            writer.println("<exception type=\"" + type + "\" name=\""
                    + name + "\"/>");
        }
        writer.println("</acl>");
        writer.flush();
    }

}
