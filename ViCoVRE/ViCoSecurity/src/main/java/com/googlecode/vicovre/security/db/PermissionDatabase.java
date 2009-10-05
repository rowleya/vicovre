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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

import org.xml.sax.SAXException;

import com.googlecode.vicovre.security.Group;
import com.googlecode.vicovre.security.OperationSet;
import com.googlecode.vicovre.security.Password;
import com.googlecode.vicovre.security.PermissionSet;
import com.googlecode.vicovre.security.Role;
import com.googlecode.vicovre.security.User;
import com.googlecode.vicovre.utils.ExtensionFilter;

public class PermissionDatabase {

    private static final String USER_EXT = ".user";

    private static final String GROUP_EXT = ".group";

    private static final String ACL_EXT = ".acl";

    private HashSet<String> operations = null;

    private HashMap<String, Role> roles = null;

    private HashMap<String, User> users = new HashMap<String, User>();

    private HashMap<String, Group> groups = new HashMap<String, Group>();

    private HashMap<String, HashMap<Integer, PermissionSet>> acls =
        new HashMap<String, HashMap<Integer, PermissionSet>>();

    private HashMap<User, File> userFiles =
        new HashMap<User, File>();

    private HashMap<Group, File> groupFiles =
        new HashMap<Group, File>();

    private HashMap<PermissionSet, File> aclFiles =
        new HashMap<PermissionSet, File>();

    private HashMap<String, String> defaultAclXml =
        new HashMap<String, String>();

    private PermissionSet groupsAcl = null;

    private PermissionSet usersAcl = null;

    private File dir = null;

    /**
     * Creates a new PermissionDatabase
     * @param directory The directory containing the database files
     * @param roleFile The name within the classpath of the roles xml file
     * @param operationFile The name within the classpath of the operations file
     * @param groupsAclFile The name within the classpath of the groups set acl
     * @param usersAclFile The name within the classpath of the users set acl
     * @throws IOException
     * @throws SAXException
     */
    public PermissionDatabase(String directory, String roleFile,
            String operationFile, String groupsAclFile, String usersAclFile)
            throws SAXException, IOException {
        this(directory, PermissionDatabase.class.getResourceAsStream(roleFile),
                PermissionDatabase.class.getResourceAsStream(operationFile),
                PermissionDatabase.class.getResourceAsStream(groupsAclFile),
                PermissionDatabase.class.getResourceAsStream(usersAclFile));
    }

    /**
     * Creates a new PermissionDatabase
     * @param directory The directory containing the database files
     * @param roleStream The stream from which to read the roles xml
     * @param operationStream The stream from which to read the operations xml
     * @throws IOException
     * @throws SAXException
     */
    public PermissionDatabase(String directory, InputStream roleStream,
            InputStream operationStream, InputStream groupsAclStream,
            InputStream usersAclStream) throws SAXException, IOException {
        operations = OperationReader.readOperations(operationStream);
        roles = RoleReader.readRoles(roleStream);
        dir = new File(directory);
        dir.mkdirs();

        File[] userFiles = dir.listFiles(new ExtensionFilter(USER_EXT));
        for (File file : userFiles) {
            FileInputStream input = new FileInputStream(file);
            User user = UserReader.readUser(input, roles);
            users.put(user.getUsername(), user);
            input.close();
            this.userFiles.put(user, file);
        }

        File[] groupFiles = dir.listFiles(new ExtensionFilter(GROUP_EXT));
        for (File file : groupFiles) {
            FileInputStream input = new FileInputStream(file);
            Group group = GroupReader.readGroup(input, users);
            groups.put(group.getName(), group);
            input.close();
            this.groupFiles.put(group, file);
        }

        File[] aclFiles = dir.listFiles(new ExtensionFilter(ACL_EXT));
        for (File file : aclFiles) {
            FileInputStream input = new FileInputStream(file);
            PermissionSet acl = AclReader.readAcl(input, operations, roles,
                    users, groups);
            this.aclFiles.put(acl, file);
            addAcl(acl);
        }

        groupsAcl = AclReader.readAcl(groupsAclStream, operations, roles,
                users, groups);
        usersAcl = AclReader.readAcl(usersAclStream, operations, roles,
                users, groups);
    }

    private String getId() {
        return "" + System.currentTimeMillis() + (int) Math.random() * 100000;
    }

    private void addAcl(PermissionSet acl) {
        HashMap<Integer, PermissionSet> classAcls =
            acls.get(acl.getClassName());
        if (classAcls == null) {
            classAcls = new HashMap<Integer, PermissionSet>();
        }
        classAcls.put(acl.getHashCode(), acl);
        acls.put(acl.getClassName(), classAcls);
    }

    public void setDefaultAcl(String className, String aclFile)
            throws IOException {
        setDefaultAcl(className, getClass().getResourceAsStream(aclFile));
    }

    public void setDefaultAcl(String className, InputStream aclStream)
            throws IOException {
        String file = new String();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(aclStream));
        String line = reader.readLine();
        while (line != null) {
            file += line + "\n";
            line = reader.readLine();
        }
        reader.close();
        defaultAclXml.put(className, file);
    }

    public PermissionSet createAcl(Object object,
            Variable... variables)
            throws SAXException, IOException {
        String defaultAcl = defaultAclXml.get(
                object.getClass().getCanonicalName());
        for (Variable var : variables) {
            defaultAcl = defaultAcl.replaceAll(var.getName(), var.getValue());
        }
        PermissionSet acl = AclReader.readAcl(
                new ByteArrayInputStream(defaultAcl.getBytes()),
                operations, roles, users, groups);
        acl.setProtectedObject(object);
        aclFiles.put(acl, new File(dir, getId() + ACL_EXT));
        setAcl(acl);
        return acl;
    }

    public PermissionSet getAcl(Object object) {
        HashMap<Integer, PermissionSet> classAcls =
            acls.get(object.getClass().getCanonicalName());
        if (classAcls != null) {
            return classAcls.get(object.hashCode());
        }
        return null;
    }

    public void setAcl(PermissionSet acl) throws IOException {
        File file = aclFiles.get(acl);
        synchronized (file) {
            addAcl(acl);
            FileOutputStream output = new FileOutputStream(file);
            AclReader.writeAcl(acl, output);
            output.close();
        }
    }

    public PermissionSet getGroupsAcl() {
        return groupsAcl;
    }

    public PermissionSet getUserAcl() {
        return usersAcl;
    }

    public String[] getOperations() {
        return operations.toArray(new String[0]);
    }

    public String[] getRoles() {
        return roles.keySet().toArray(new String[0]);
    }

    public User createUser(String username, String password,
            String... roles) throws SAXException, IOException {
        if (users.containsKey(username)) {
            return null;
        }
        User user = new User(username);
        Password pass = new Password(password);
        user.setPassword(pass);
        for (String role : roles) {
            user.addRole(this.roles.get(role));
        }
        userFiles.put(user, new File(dir, getId() + USER_EXT));
        setUser(user);
        createAcl(user, new Variable("?user", username));
        return user;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public void setUser(User user) throws IOException {
        File file = userFiles.get(user);
        synchronized (file) {
            users.put(user.getUsername(), user);
            FileOutputStream output = new FileOutputStream(file);
            UserReader.writeUser(user, output);
            output.close();
        }
    }

    public User[] getUsers() {
        return users.entrySet().toArray(new User[0]);
    }

    public Group addGroup(String name, String... users)
            throws IOException, SAXException {
        if (groups.containsKey(name)) {
            return null;
        }

        Group group = new Group(name);
        for (String user : users) {
            group.addUser(this.users.get(user));
        }
        groupFiles.put(group, new File(dir, getId() + GROUP_EXT));
        setGroup(group);
        createAcl(group, new Variable("?group", name));
        return group;
    }

    public Group getGroup(String name) {
        return groups.get(name);
    }

    public void setGroup(Group group) throws IOException {
        File file = groupFiles.get(group);
        synchronized (file) {
            groups.put(group.getName(), group);
            FileOutputStream output = new FileOutputStream(file);
            GroupReader.writeGroup(group, output);
            output.close();
        }
    }

    public Group[] getGroups() {
        return groups.entrySet().toArray(new Group[0]);
    }

    public boolean isUserAllowed(User user, Object object,
            OperationSet operation) {
        return isUserAllowed(user, getAcl(object), operation);
    }

    public boolean isUserAllowed(User user, PermissionSet acl,
            OperationSet operation) {
        return acl.userHasPermission(user, operation);
    }
}
