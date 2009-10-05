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

import com.googlecode.vicovre.security.Password;
import com.googlecode.vicovre.security.Role;
import com.googlecode.vicovre.security.User;
import com.googlecode.vicovre.utils.XmlIo;

public class UserReader {

    public static User readUser(InputStream input,
            HashMap<String, Role> knownRoles) throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        String username = XmlIo.readValue(doc, "username");
        String password = XmlIo.readValue(doc, "password");
        String[] roles = XmlIo.readValues(doc, "role");

        User user = new User(username);
        Password pass = new Password();
        pass.setHexStringHash(password);
        user.setPassword(pass);
        for (int i = 0; i < roles.length; i++) {
            Role role = knownRoles.get(roles[i]);
            if (role == null) {
                throw new SAXException("Role " + roles[i] + " not known");
            }
            user.addRole(role);
        }
        return user;
    }

    public static void writeUser(User user, OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<user>");
        XmlIo.writeValue(user, "username", writer);
        XmlIo.writeValue("password", user.getPassword().getHashHexString(),
                writer);
        for (Role role : user.getRoles()) {
            XmlIo.writeValue("role", role.getName(), writer);
        }
        writer.println("</user>");
        writer.flush();
    }
}
