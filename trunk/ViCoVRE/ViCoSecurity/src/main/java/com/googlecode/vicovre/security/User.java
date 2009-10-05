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

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * Represents a user
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class User {

    private String username = null;

    private Password password = new Password();

    private HashSet<Role> roles = new HashSet<Role>();

    /**
     * Creates a user
     * @param username The username of the user
     */
    public User(String username) {
        this.username = username;
    }

    /**
     * Gets the username of the user (for storage)
     * @return The username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Determines if the user has the given role
     * @param role The role to check
     * @return True if the user has the role
     */
    public boolean hasRole(Role role) {
        for (Role r : roles) {
            if (r.isRoleOrSubrole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a role to a user
     * @param role The role to add
     */
    public void addRole(Role role) {
        roles.add(role);
    }

    /**
     * Gets the roles of the user
     * @return The roles
     */
    public List<Role> getRoles() {
        return new Vector<Role>(roles);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }

    /**
     * Sets the password of the user
     * @param password The password to set
     */
    public void setPassword(Password password) {
        this.password = password;
    }

    /**
     * Gets the password of the user
     * @return The password
     */
    public Password getPassword() {
        return password;
    }

    /**
     * Determines if the user is the same as this user
     * @param user The user to test
     * @return True if the user is the same
     */
    public boolean equals(User user) {
        return user.username.equals(username);
    }

    /**
     * Determines if this username is the same as the given username
     * @param username The username to check
     * @return True if the usernames match
     */
    public boolean equals(String username) {
        return this.username.equals(username);
    }

    /**
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return username.hashCode();
    }
}
