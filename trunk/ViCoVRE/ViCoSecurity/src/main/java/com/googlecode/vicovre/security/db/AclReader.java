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
import java.util.HashSet;
import java.util.Vector;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.security.Group;
import com.googlecode.vicovre.security.GroupPermission;
import com.googlecode.vicovre.security.OperationSet;
import com.googlecode.vicovre.security.Permission;
import com.googlecode.vicovre.security.PermissionSet;
import com.googlecode.vicovre.security.Role;
import com.googlecode.vicovre.security.RolePermission;
import com.googlecode.vicovre.security.User;
import com.googlecode.vicovre.security.UserPermission;
import com.googlecode.vicovre.utils.XmlIo;

public class AclReader {

    public static PermissionSet readAcl(InputStream input,
            HashSet<String> knownOperations,
            HashMap<String, Role> knownRoles,
            HashMap<String, User> knownUsers,
            HashMap<String, Group> knownGroups)
            throws SAXException, IOException {

        Node doc = XmlIo.read(input);
        PermissionSet lastPermissions = null;
        String className = XmlIo.readValue(doc, "className");
        String hashCode = XmlIo.readValue(doc, "hashCode");
        Node[] permissionSets = XmlIo.readNodes(doc, "permissionSet");
        for (int i = 0; i < permissionSets.length; i++) {
            PermissionSet permissions = new PermissionSet();
            NodeList permissionSet = permissionSets[i].getChildNodes();
            for (int j = 0; j < permissionSet.getLength(); j++) {
                Node perm = permissionSet.item(j);

                OperationSet operations = new OperationSet();
                NodeList ops = perm.getChildNodes();
                for (int k = 0; k < ops.getLength(); k++) {
                    Node op = ops.item(k);
                    if (op.getNodeName().equals("operation")) {
                        String operation = op.getTextContent();
                        if (!knownOperations.contains(operation)) {
                            throw new SAXException("Operation " + operation
                                    + " not known");
                        }
                        operations.addOperation(operation);
                    }
                }

                NamedNodeMap attrs = perm.getAttributes();
                String allowString =
                    attrs.getNamedItem("allow").getTextContent();
                if (!allowString.equals("true")
                        && !allowString.equals("false")) {
                    throw new SAXException("allow must be true or false");
                }
                boolean allow = Boolean.parseBoolean(allowString);

                if (perm.getNodeName().equals("user")) {
                    String username =
                        attrs.getNamedItem("username").getTextContent();
                    User user = knownUsers.get(username);
                    if (user == null) {
                        throw new SAXException("User " + username
                                + " not known");
                    }
                    UserPermission userPerm = new UserPermission(operations,
                            allow,  user);
                    permissions.addPermission(userPerm);
                } else if (perm.getNodeName().equals("role")) {
                    String roleName =
                        attrs.getNamedItem("name").getTextContent();
                    Role role = knownRoles.get(roleName);
                    if (role == null) {
                        throw new SAXException("Role " + roleName
                                + " not known");
                    }
                    RolePermission rolePerm = new RolePermission(operations,
                            allow, role);
                    permissions.addPermission(rolePerm);
                } else if (perm.getNodeName().equals("group")) {
                    String groupName =
                        attrs.getNamedItem("name").getTextContent();
                    Group group = knownGroups.get(groupName);
                    if (group == null) {
                        throw new SAXException("Group " + groupName
                                + " not known");
                    }
                    String roleName =
                        attrs.getNamedItem("role").getTextContent();
                    Role role = knownRoles.get(roleName);
                    if (role == null) {
                        throw new SAXException("Role " + roleName
                                + " not known");
                    }
                    GroupPermission groupPerm = new GroupPermission(operations,
                            allow, group, role);
                    permissions.addPermission(groupPerm);
                } else {
                    throw new SAXException("Permission " + perm.getNodeName()
                            + " not known");
                }
            }
            permissions.setSetPermissions(lastPermissions);
            lastPermissions = permissions;
        }
        lastPermissions.setClassName(className);
        lastPermissions.setHashCode(Integer.parseInt(hashCode));
        return lastPermissions;
    }

    public static void writeAcl(PermissionSet permissionSet,
            OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<permissions>");

        // Get the permission sets in order
        Vector<PermissionSet> sets = new Vector<PermissionSet>();
        sets.add(permissionSet);
        PermissionSet set = permissionSet;
        while (set != null) {
            set = set.getSetPermissions();
            if (set != null) {
                sets.add(set);
            }
        }

        XmlIo.writeValue(permissionSet, "className", writer);
        XmlIo.writeValue(permissionSet, "hashCode", writer);

        // Output the permission sets in reverse order
        for (int i = sets.size() - 1; i >= 0; i--) {
            set = sets.get(i);
            writer.println("<permissionSet>");
            for (Permission permission : set.getPermissions()) {
                if (permission instanceof UserPermission) {
                    UserPermission perm = (UserPermission) permission;
                    writer.println("<user allow=\"" + perm.allow()
                            + "\" username=\"" + perm.getUsername() + "\">");
                } else if (permission instanceof RolePermission) {
                    RolePermission perm = (RolePermission) permission;
                    writer.println("<role allow=\"" + perm.allow()
                            + "\" name=\"" + perm.getRoleName() + "\">");
                } else if (permission instanceof GroupPermission) {
                    GroupPermission perm = (GroupPermission) permission;
                    writer.println("<group allow=\"" + perm.allow()
                            + "\" name=\"" + perm.getGroupName()
                            + "\" role=\"" + perm.getRoleName() + "\">");
                }

                OperationSet operations = permission.getOperation();
                for (String operation : operations.getOperations()) {
                    writer.println("<operation>" + operation + "</operation>");
                }

                if (permission instanceof UserPermission) {
                    writer.println("</user>");
                } else if (permission instanceof RolePermission) {
                    writer.println("</role>");
                } else if (permission instanceof GroupPermission) {
                    writer.println("</group>");
                }
            }
            writer.println("</permissionSet>");
        }

        writer.println("<permissions>");
        writer.flush();
    }
}
