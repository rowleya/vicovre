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

package com.googlecode.vicovre.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * Represents a group of users
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Group {

    private String name = null;

    private HashMap<Role, HashSet<User>> users =
        new HashMap<Role, HashSet<User>>();

    /**
     * Creates a new group
     * @param name The name of the group
     */
    public Group(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the group
     * @return The name of the group
     */
    public String getName() {
        return name;
    }

    public List<Role> getRoles() {
        return new Vector<Role>(users.keySet());
    }

    /**
     * Gets the users in the group
     * @role The role of the users to get
     * @return The users in the group with the specified role
     */
    public List<User> getUsers(Role role) {
        return new Vector<User>(users.get(role));
    }

    /**
     * Determines if the given user is in the group
     * @param user The user to check
     * @return True if the user is in the group
     */
    public boolean containsUser(User user) {
        for (Role role : users.keySet()) {
            if (users.get(role).contains(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean userHasRole(User user, Role role) {
        HashSet<User> usersInRole = users.get(role);
        if ((usersInRole != null) && usersInRole.contains(user)) {
            return true;
        }
        return false;
    }

    /**
     * Adds a user to the group
     * @param user The user to add
     */
    public void addUser(User user, Role role) {
        HashSet<User> usersWithRole = users.get(role);
        if (usersWithRole == null) {
            usersWithRole = new HashSet<User>();
        }
        usersWithRole.add(user);
        users.put(role, usersWithRole);
    }

    /**
     * Determines if the groups are the same
     * @param group The group to check
     * @return True if the names of the group are the same
     */
    public boolean equals(Group group) {
        return group.name.equals(name);
    }

    /**
     * Determines if the group names are the same
     * @param name The name to check
     * @return True if the name is the same as this group's name
     */
    public boolean equals(String name) {
        return this.name.equals(name);
    }

    /**
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return name.hashCode();
    }

}
