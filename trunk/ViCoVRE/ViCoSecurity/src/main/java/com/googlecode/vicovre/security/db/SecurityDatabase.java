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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.mail.EmailException;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.security.AlreadyExistsException;
import com.googlecode.vicovre.security.InvalidOperationException;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.UnknownException;
import com.googlecode.vicovre.security.servlet.CurrentUser;
import com.googlecode.vicovre.security.servlet.SecurityFilter;
import com.googlecode.vicovre.utils.Emailer;
import com.googlecode.vicovre.utils.ExtensionFilter;

public class SecurityDatabase {

    public static final String TYPE_USER = "user";

    public static final String TYPE_GROUP = "group";

    public static final String TYPE_ROLE = "role";

    private static final String VALID_USERNAME =
        "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";

    private static final long VERIFICATION_TIMEOUT = 24 * 60 * 60 * 1000;

    private HashMap<String, User> unverifiedUsers = new HashMap<String, User>();

    private HashSet<String> unverifiedUsernames = new HashSet<String>();

    private HashMap<String, User> users = new HashMap<String, User>();

    private HashMap<String, Group> groups = new HashMap<String, Group>();

    private HashMap<File, HashMap<String, ACL>> acls =
        new HashMap<File, HashMap<String,ACL>>();

    private Vector<ACLListener> aclListeners = new Vector<ACLListener>();

    private File topLevelFolder = null;

    private Emailer emailer = null;

    private User adminUser = null;

    private class VerificationTimeOut extends TimerTask {

        private String hash = null;

        private VerificationTimeOut(String hash) {
            this.hash = hash;
        }

        public void run() {
            User user = unverifiedUsers.remove(hash);
            if (user != null) {
                unverifiedUsernames.remove(user.getUsername());
                File unverifiedFile = new File(topLevelFolder,
                        hash + ".unverifiedUser");
                unverifiedFile.delete();
                try {
                    deleteUser(user);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public SecurityDatabase(String directory, Emailer emailer)
            throws SAXException, IOException {
        this(directory, emailer, new String[0]);
    }

    public SecurityDatabase(String directory, Emailer emailer,
            String... usersToCreate) throws SAXException, IOException {
        this.emailer = emailer;
        topLevelFolder = new File(directory);
        if (!topLevelFolder.exists()) {
            topLevelFolder.mkdirs();
        }

        File[] userFiles =
            topLevelFolder.listFiles(new ExtensionFilter(".user"));
        for (File userFile : userFiles) {
            FileInputStream input = new FileInputStream(userFile);
            User user = UserReader.readUser(input, Role.ROLES);
            if (user.getRole().equals(Role.ADMINISTRATOR)) {
                adminUser = user;
            }
            users.put(user.getUsername(), user);
            input.close();
        }
        if (adminUser == null) {
            adminUser = new User("admin", Role.ADMINISTRATOR);
            adminUser.setPasswordHash(
                    "7b902e6ff1db9f560443f2048974fd7d386975b0");
            users.put(adminUser.getUsername(), adminUser);
        }

        File[] unverifiedUserFiles =
            topLevelFolder.listFiles(new ExtensionFilter(".unverifiedUser"));
        for (File unverifiedUserFile : unverifiedUserFiles) {
            FileInputStream input = new FileInputStream(unverifiedUserFile);
            User user = UserReader.readUser(input, Role.ROLES);
            input.close();
            String hash = unverifiedUserFile.getName();
            hash = hash.substring(0, hash.length() - 15);
            unverifiedUsers.put(hash, user);
            unverifiedUsernames.add(user.getUsername());
        }

        File[] groupFiles =
            topLevelFolder.listFiles(new ExtensionFilter(".group"));
        for (File groupFile : groupFiles) {
            FileInputStream input = new FileInputStream(groupFile);
            Group group = GroupReader.readGroup(input, users, unverifiedUsers);
            groups.put(group.getName(), group);
        }

        for (String userToCreate : usersToCreate) {
            String[] parts = userToCreate.split("/");
            try {
                addUser(parts[0], parts[1], parts[2]);
            } catch (AlreadyExistsException e) {
                // Do Nothing
            } catch (Exception e) {
                System.err.println("Warning, error adding " + userToCreate
                        + ": " + e.getMessage());
            }
        }

        readACLs(topLevelFolder, "");

        for (String hash : unverifiedUsers.keySet()) {
            File unverifiedUserFile = new File(topLevelFolder,
                    hash + ".unverifiedUser");
            Timer timer = new Timer();
            long timeout = (unverifiedUserFile.lastModified()
                    + VERIFICATION_TIMEOUT) - System.currentTimeMillis();
            if (timeout > 0) {
                timer.schedule(new VerificationTimeOut(hash), timeout);
            } else {
                User user = unverifiedUsers.remove(hash);
                unverifiedUsernames.remove(user.getUsername());
                unverifiedUserFile.delete();
                deleteUser(user);
            }
        }
    }

    public void addACLListener(ACLListener listener) {
        aclListeners.add(listener);
        for (HashMap<String, ACL> folder : acls.values()) {
            for (ACL acl : folder.values()) {
                listener.ACLCreated(acl.getFolder(), acl.getId());
            }
        }
    }

    private File getFile(String folder) {
        if ((folder == null) || folder.equals("/") || folder.equals("")
                || folder.equals("\\")) {
            return topLevelFolder;
        }
        if (folder.endsWith("/")) {
            folder = folder.substring(0, folder.length() - 1);
        }
        return new File(topLevelFolder, folder);
    }

    private void readACLs(File folder, String folderName)
            throws SAXException, IOException {
        File[] aclFiles = folder.listFiles();
        HashMap<String, ACL> aclMap = new HashMap<String, ACL>();
        for (File aclFile : aclFiles) {
            if (aclFile.getName().endsWith(".acl")) {
                FileInputStream input = new FileInputStream(aclFile);
                ACL acl = ACLReader.readACL(folderName, input, users,
                        unverifiedUsers, groups, Role.ROLES);
                aclMap.put(acl.getId(), acl);
                for (ACLListener listener : aclListeners) {
                    listener.ACLCreated(acl.getFolder(), acl.getId());
                }
            } else if (aclFile.isDirectory()) {
                readACLs(aclFile, folderName + "/" + aclFile.getName());
            }
        }
        acls.put(folder, aclMap);
    }

    private void deleteEntity(Entity entity) throws IOException {
        List<ACL> acls = entity.getAcls();
        for (ACL acl : acls) {
            File folder = getFile(acl.getFolder());
            acl.removeException(entity);
            writeAcl(folder, acl);
        }
    }

    private void deleteUser(User user) throws IOException {
        if (user.isOwner()) {
            throw new InvalidOperationException(
                    "The user still owns some items");
        }
        deleteEntity(user);
        List<Group> groups = user.getGroups();
        for (Group group : groups) {
            group.deleteUser(user);
            writeGroup(group);
        }
    }

    private void deleteGroup(Group group) throws IOException {
        deleteEntity(group);
        List<User> users = group.getUsers();
        for (User user : users) {
            user.deleteGroup(group);
        }
    }

    private void deleteAcl(ACL acl) {
        List<Entity> exceptions = acl.getExceptions();
        for (Entity entity : exceptions) {
            entity.removeACL(acl);
        }
        acl.getOwner().removeOwned(acl);
    }

    private void writeUser(User user) throws IOException {
        FileOutputStream output = new FileOutputStream(
                new File(topLevelFolder, user.getUsername() + ".user"));
        UserReader.writeUser(output, user);
        output.close();
    }

    private void checkPassword(String password) {
        if (password.length() < 6) {
            throw new InvalidOperationException(
                    "Passwords must be at least 6 characheters long");
        }
    }

    private void checkUsername(String username) {
        if (!username.matches(VALID_USERNAME)) {
            throw new InvalidOperationException(
                    "A username must be a valid e-mail address");
        }
    }

    private void addUser(String username, String password, Role role)
            throws IOException {
        checkUsername(username);
        checkPassword(password);
        if (!users.containsKey(username)
                && !unverifiedUsernames.contains(username)) {
            User user = new User(username, role);
            synchronized (user) {
                Password.setPassword(password, user);
                users.put(username, user);
                writeUser(user);
            }
        } else {
            throw new AlreadyExistsException(
                    "User " + username + " already exists");
        }
    }

    public String addUnverifiedUser(String username, String password,
            String verifyUrl) throws IOException, EmailException {
        synchronized (users) {
            boolean newPassword = false;
            if (password == null) {
                newPassword = true;
                password = UUID.randomUUID().toString();
            }
            checkUsername(username);
            checkPassword(password);
            if (!users.containsKey(username)
                    && !unverifiedUsernames.contains(username)) {
                User user = new User(username, Role.AUTHUSER);
                Password.setPassword(password, user);
                String hash = UUID.randomUUID().toString();

                String message = "";
                if (newPassword) {
                    message += "Someone has granted you access to a recording,"
                        + " but you are not yet registered.\n";
                    message += "If you would like to access the recording, "
                        + " please to go the following address to complete "
                        + " your registration:";
                    message += "    " + verifyUrl.replace("$hash", hash)
                        + "\n\n";
                    message += "and then sign in using this e-mail address and "
                        + " the following password to access the recording:\n";
                    message += "    " + password + "\n\n";
                    message += "If you do not wish to register,"
                        + " please ignore this message.\n";
                } else {
                    message += "Someone has signed up with this email "
                        + " address.\n";
                    message += "If this was you, please go to the following"
                        + " address to complete your registration:\n";
                    message += "    " + verifyUrl.replace("$hash", hash)
                        + "\n\n";
                    message += "If this was not you, please ignore this "
                        + " message.\n";
                }
                emailer.send(username, "New User Verification", message);
                unverifiedUsers.put(hash, user);
                unverifiedUsernames.add(username);
                Timer timer = new Timer();
                timer.schedule(new VerificationTimeOut(hash),
                        VERIFICATION_TIMEOUT);
                FileOutputStream output = new FileOutputStream(
                        new File(topLevelFolder, hash + ".unverifiedUser"));
                UserReader.writeUser(output, user);
                output.close();
                return hash;
            }
            throw new AlreadyExistsException(
                "User " + username + " already exists");
        }
    }

    public void verifyUser(String hash) {
        synchronized (users) {
            User user = unverifiedUsers.remove(hash);
            if (user == null) {
                throw new UnknownException("The hash specified is invalid");
            }
            unverifiedUsernames.remove(user.getUsername());
            File unverifiedFile = new File(topLevelFolder,
                    hash + ".unverifiedUser");
            unverifiedFile.renameTo(new File(topLevelFolder,
                    user.getUsername() + ".user"));
            users.put(user.getUsername(), user);
        }
    }

    public void addUser(String username, String password, String roleName)
            throws IOException {
        synchronized (users) {
            if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR)) {
                throw new UnauthorizedException(
                   "Only an administrator can add a user with a specific role");
            }
            Role role = Role.ROLES.get(roleName);
            if (roleName == null) {
                throw new UnknownException("Unknown role " + roleName);
            }
            if (role.equals(Role.USER)) {
                throw new InvalidOperationException(
                        "Cannot create a guest user!");
            }

            addUser(username, password, role);
        }
    }

    public void setUserPassword(String username, String newPassword)
            throws IOException {
        synchronized (users) {
            User user = users.get(username);
            if (user == null) {
                throw new UnknownException("Unknown user " + username);
            }

            if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR)) {
                throw new UnauthorizedException(
                    "Only an administrator can change another user's password");
            }

            synchronized (user) {
                checkPassword(newPassword);
                Password.setPassword(newPassword, user);
                writeUser(user);
            }
        }
    }

    public void setPassword(String oldPassword,	String newPassword)
            throws IOException {
        User user = CurrentUser.get();
        synchronized (user) {
            if (user.getRole().equals(Role.USER)) {
                throw new UnauthorizedException(
                        "Only authorized users can change their password");
            }
            if (!Password.isPassword(oldPassword, user)) {
                throw new UnauthorizedException("Old password is incorrect");
            }
            checkPassword(newPassword);
            Password.setPassword(newPassword, user);
            writeUser(user);
        }
    }

    public void setUserRole(String username, String roleName)
            throws IOException {
        if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR)) {
            throw new UnauthorizedException(
                    "Only an administrator can change a user's role");
        }
        synchronized (users) {
            User user = users.get(username);
            if (user == null) {
                throw new UnknownException("Unknown user " + username);
            }
            Role role = Role.ROLES.get(roleName);
            if (role == null) {
                throw new UnknownException("Unknown role " + roleName);
            }
            if (role.equals(Role.USER)) {
                throw new InvalidOperationException(
                        "Cannot create a guest user!");
            }
            synchronized (user) {
                user.setRole(role);
                writeUser(user);
            }
        }
    }

    public void deleteUser(String username) throws IOException {
        synchronized (users) {
            User user = users.get(username);
            if (user == null) {
                throw new UnknownException("Unknown user " + username);
            }

            if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR)
                    && !CurrentUser.get().equals(user)) {
                throw new UnauthorizedException(
                    "Only an administrator or the user themself"
                    + " can delete a user");
            }

            synchronized (user) {
                deleteUser(user);
                users.remove(username);
                File userFile = new File(topLevelFolder,
                        user.getUsername() + ".user");
                userFile.delete();
            }
        }
    }

    public List<String> getUsers() {
        if (!CurrentUser.get().getRole().is(Role.WRITER)) {
            throw new UnauthorizedException(
                    "You must have write permission to see the user list");
        }
        return new Vector<String>(users.keySet());
    }

    private void writeGroup(Group group) throws IOException {
        FileOutputStream output = new FileOutputStream(group.getName()
                + ".group");
        GroupReader.writeGroup(output, group);
        output.close();
    }

    private void checkGroupName(String groupname) {
        if (!groupname.matches(VALID_USERNAME)) {
            throw new InvalidOperationException(
                    "A group name must be between 4 and 50 characters and"
                    + " only contain the characters"
                    + " a to z, A to Z, 0 to 9, _ or -");
        }
    }

    public void addGroup(String name) throws IOException {
        if (!CurrentUser.get().getRole().is(Role.WRITER)) {
            throw new UnauthorizedException(
                    "You have write permissions to be able to create a group");
        }
        synchronized (groups) {
            if (groups.containsKey(name)) {
                throw new AlreadyExistsException("Group already exists!");
            }
            checkGroupName(name);
            Group group = new Group(name, CurrentUser.get());
            synchronized (group) {
                groups.put(group.getName(), group);
                writeGroup(group);
            }
        }
    }

    public void setGroupOwner(String name, String owner) throws IOException {
        synchronized (groups) {
            Group group = groups.get(name);
            if (group == null) {
                throw new UnknownException("Unknown group " + group);
            }
            synchronized (users) {
                User user = users.get(name);
                if (user == null) {
                    throw new UnknownException("Unknown user " + owner);
                }
                synchronized (group) {
                    if (!CurrentUser.get().getRole().equals(Role.ADMINISTRATOR)
                            && !CurrentUser.get().equals(group.getOwner())) {
                        throw new UnauthorizedException(
                            "You must be an administrator or the current owner "
                            + "of the group to change the owner");
                    }
                    group.setOwner(user);
                    writeGroup(group);
                }
            }
        }
    }

    public void setGroupUsers(String name, List<String> usernames)
            throws IOException {
        synchronized (groups) {
            Group group = groups.get(name);
            if (group == null) {
                throw new UnknownException("Unknown group " + group);
            }
            synchronized (group) {
                if (!CurrentUser.get().getRole().equals(Role.ADMINISTRATOR)
                        && !CurrentUser.get().equals(group.getOwner())) {
                    throw new UnauthorizedException(
                        "You must be an administrator or the current owner "
                        + "of the group to change users in the group");
                }
                synchronized (users) {
                    List<User> usersToSet = new Vector<User>();
                    for (String username : usernames) {
                        User user = users.get(username);
                        if (user == null) {
                            throw new UnknownException(
                                    "Unknown user " + username);
                        }
                        usersToSet.add(user);
                    }
                    group.clearUsers();
                    for (User user : usersToSet) {
                        synchronized (user) {
                            group.addUser(user);
                        }
                    }
                    writeGroup(group);
                }
            }
        }
    }

    public void deleteGroup(String name) throws IOException {
        Group group = groups.get(name);
        if (group == null) {
            throw new UnknownException("Unknown group " + group);
        }
        synchronized (group) {
            if (!CurrentUser.get().getRole().equals(Role.ADMINISTRATOR)
                    && !CurrentUser.get().equals(group.getOwner())) {
                throw new UnauthorizedException(
                    "You must be an administrator or the current owner "
                    + "of the group to delete it");
            }
            deleteGroup(group);
            groups.remove(name);
            File groupFile = new File(topLevelFolder,
                    group.getName() + ".group");
            groupFile.delete();
        }
    }

    public List<String> getGroups() {
        if (!CurrentUser.get().getRole().is(Role.WRITER)) {
            throw new UnauthorizedException(
                    "You must have write permission to see the group list");
        }
        List<String> groupList = new Vector<String>();
        for (Group group : groups.values()) {
            if (CurrentUser.get().getRole().is(Role.ADMINISTRATOR) ||
                    CurrentUser.get().equals(group.getOwner())) {
                groupList.add(group.getName());
            }
        }
        return groupList;
    }

    public String getGroupOwner(String name) {
        Group group = groups.get(name);
        if (group == null) {
            throw new UnknownException("Unknown group " + name);
        }
        if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR) &&
                !CurrentUser.get().equals(group.getOwner())) {
            throw new UnauthorizedException(
                "Only an administrator or the group owner can see"
                + " the group users");
        }
        return group.getOwner().getUsername();
    }

    public List<String> getGroupUsers(String name) {
        Group group = groups.get(name);
        if (group == null) {
            throw new UnknownException("Unknown group " + name);
        }
        if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR) &&
                !CurrentUser.get().equals(group.getOwner())) {
            throw new UnauthorizedException(
                "Only an administrator or the group owner can see"
                + " the group users");
        }
        List<String> userList = new Vector<String>();
        for (User user : group.getUsers()) {
            userList.add(user.getUsername());
        }
        return userList;
    }

    public List<String> getRoles() {
        if (CurrentUser.get().getRole().equals(Role.ADMINISTRATOR)) {
            throw new UnauthorizedException(
                    "You must be an adminstrator to see the roles");
        }
        return new Vector<String>(Role.ROLES.keySet());
    }

    public String getRole(String username) {
        User user = users.get(username);
        if (user == null) {
            throw new UnknownException("Unknown user " + username);
        }
        if (CurrentUser.get().getRole().equals(Role.ADMINISTRATOR)) {
            throw new UnauthorizedException(
                    "You must be an adminstrator to get the role of a user");
        }
        return user.getRole().getName();
    }

    public String getRole() {
        User user = CurrentUser.get();
        return user.getRole().getName();
    }

    public String getUsername() {
        User user = CurrentUser.get();
        if (user.equals(User.GUEST)) {
            return null;
        }
        return user.getUsername();
    }

    public String login(String username, String password,
            HttpServletRequest request) {
        synchronized (users) {
            User user = users.get(username);
            if (user == null) {
                return null;
            }
            synchronized (user) {
                if (Password.isPassword(password, user)) {
                    HttpSession session = request.getSession();
                    session.setAttribute(SecurityFilter.SESSION_USER, user);
                    return user.getRole().getName();
                }
            }
            return null;
        }
    }

    public void logout(HttpServletRequest request) {
         HttpSession session = request.getSession();
         session.removeAttribute(SecurityFilter.SESSION_USER);
    }

    private void writeAcl(File folder, ACL acl) throws IOException {
        FileOutputStream output = new FileOutputStream(new File(folder,
                acl.getId() + ".acl"));
        ACLReader.writeACL(output, acl);
        output.close();
    }

    private Vector<Entity> getEntities(WriteOnlyEntity[] exceptions) {
        Vector<Entity> entities = new Vector<Entity>();
        for (WriteOnlyEntity entity : exceptions) {
            String name = entity.getName();
            String type = entity.getType();
            if (type.equals(TYPE_USER)) {
                User user = users.get(name);
                if (user == null) {
                    throw new UnknownException("Unknown user " + name);
                }
                entities.add(user);
            } else if (type.equals(TYPE_GROUP)) {
                Group group = groups.get(name);
                if (group == null) {
                    throw new UnknownException("Unknown group " + group);
                }
                entities.add(group);
            } else if (type.equals(TYPE_ROLE)) {
                Role role = Role.ROLES.get(name);
                if (role == null) {
                    throw new UnknownException("Unknown role " + role);
                }
                entities.add(role);
            }
        }
        return entities;
    }

    private User getCurrentUser(String requesterFolder, String requesterId) {
        User user = CurrentUser.get();
        if (user != null) {
            return user;
        }

        if (requesterId != null) {
            File folderFile = getFile(requesterFolder);
            HashMap<String, ACL> aclList = acls.get(folderFile);
            if (aclList != null) {
                ACL acl = aclList.get(requesterId);
                if (acl != null) {
                    if (acl.canProxy()) {
                        return acl.getOwner();
                    }
                }
            }
        }

        return User.GUEST;
    }

    public void createAcl(String creatorFolder, String creatorId,
            String folder, String id, boolean allow, boolean canProxy,
            Role requiredRole, WriteOnlyEntity... exceptions)
            throws IOException {
        User currentUser = getCurrentUser(creatorFolder, creatorId);
        if (!currentUser.getRole().is(requiredRole)) {
            throw new UnauthorizedException(
                    "You do not have permission to perform this operation");
        }
        synchronized (acls) {
            File folderFile = getFile(folder);
            HashMap<String, ACL> aclList = acls.get(folderFile);
            if (aclList == null) {
                aclList = new HashMap<String, ACL>();
            }
            if (aclList.containsKey(id)) {
                throw new AlreadyExistsException(
                        "ACL for " + id + " already exists");
            }

            Vector<Entity> entities = getEntities(exceptions);

            if (!folderFile.exists()) {
                folderFile.mkdirs();
            }
            ACL acl = new ACL(folder, id, currentUser, allow, canProxy);
            synchronized (acl) {
                for (Entity entity : entities) {
                    acl.addException(entity);
                }
                aclList.put(acl.getId(), acl);
                acls.put(folderFile, aclList);

                writeAcl(folderFile, acl);
            }
            for (ACLListener listener : aclListeners) {
                listener.ACLCreated(folder, id);
            }
        }
    }

    private ACL obtainAcl(File folderFile, String folder, String id,
            boolean allowByDefault, boolean check) {
        ACL acl = null;
        HashMap<String, ACL> aclList = acls.get(folderFile);
        try {
            if (aclList == null) {
                throw new UnknownException("Unknown ACL " + id);
            }
            acl = aclList.get(id);
            if (acl == null) {
                throw new UnknownException("Unknown ACL " + id);
            }
        } catch (UnknownException e) {

            if (aclList == null) {
                aclList = new HashMap<String, ACL>();
                acls.put(folderFile, aclList);
            }
            acl = new ACL(folder, id, adminUser, allowByDefault, false);
            aclList.put(id, acl);
        }

        if (check) {
            if (!CurrentUser.get().getRole().is(Role.ADMINISTRATOR)
                    && !CurrentUser.get().equals(acl.getOwner())) {
                throw new UnauthorizedException(
                    "You must be an administrator or"
                    + " the owner of this object to perform this operation");
            }
        }
        return acl;
    }

    public void setAcl(String folder, String id, boolean allow,
            WriteOnlyEntity... exceptions) throws IOException {

        File folderFile = getFile(folder);
        synchronized (acls) {
            ACL acl = obtainAcl(folderFile, folder, id, allow, true);

            Vector<Entity> entities = getEntities(exceptions);
            acl.setAllow(allow);
            acl.clearExceptions();
            synchronized (acl) {
                for (Entity entity : entities) {
                    acl.addException(entity);
                }
                folderFile.mkdirs();
                writeAcl(folderFile, acl);
            }
        }
        for (ACLListener listener : aclListeners) {
            listener.ACLUpdated(folder, id);
        }
    }

    public void setAclOwner(String folder, String id, String owner)
            throws IOException {
        File folderFile = getFile(folder);
        synchronized (acls) {
            ACL acl = obtainAcl(folderFile, folder, id, false, true);

            User user = users.get(owner);
            if (user == null) {
                throw new UnknownException("Unknown user " + owner);
            }

            synchronized (acl) {
                acl.setOwner(user);
                writeAcl(folderFile, acl);
            }
        }
        for (ACLListener listener : aclListeners) {
            listener.ACLUpdated(folder, id);
        }
    }

    public void deleteAcl(String folder, String id) {
        File folderFile = getFile(folder);
        synchronized (acls) {
            ACL acl = obtainAcl(folderFile, folder, id, false, false);

            User currentUser = getCurrentUser(folder, id);

            if (!currentUser.getRole().is(Role.ADMINISTRATOR)
                    && !currentUser.equals(acl.getOwner())) {
                throw new UnauthorizedException(
                    "You must be an administrator or"
                    + " the owner of this object to perform this operation");
            }

            synchronized (acl) {
                deleteAcl(acl);
                HashMap<String, ACL> aclList = acls.get(folderFile);
                aclList.remove(id);
                if (aclList.isEmpty()) {
                    acls.remove(folderFile);
                }
                File aclFile = new File(folderFile, acl.getId() + ".acl");
                aclFile.delete();
            }
            for (ACLListener listener : aclListeners) {
                listener.ACLDeleted(folder, id);
            }
        }
    }

    public ReadOnlyACL getAcl(String folder, String id,
            boolean allowByDefault) {
        File folderFile = getFile(folder);
        synchronized (acls) {
            ACL acl = obtainAcl(folderFile, folder, id, allowByDefault, true);
            synchronized (acl) {
                return new ReadOnlyACL(acl);
            }
        }
    }

    public boolean isAllowed(String folder, String id, boolean def) {
        return isAllowed(null, null, folder, id, def);
    }

    public boolean isAllowed(String creatorFolder, String creatorId,
            String folder, String id, boolean def) {
        File folderFile = getFile(folder);
        synchronized (acls) {
            try {
                ACL acl = obtainAcl(folderFile, folder, id, def, false);
                synchronized (acl) {
                    return acl.isAllowed(getCurrentUser(creatorFolder,
                            creatorId));
                }
            } catch (UnknownException e) {

                // If there is no ACL, return false
                return def;
            }
        }
    }

    public boolean hasRole(Role role) {
        return CurrentUser.get().getRole().is(role);
    }
}
