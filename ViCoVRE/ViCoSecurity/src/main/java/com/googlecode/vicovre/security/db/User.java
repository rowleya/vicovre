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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class User extends Entity {

    public static final User GUEST = new User("Guest", Role.USER);

    private String username = null;

    private String passwordHash = null;

    private Set<Group> groups = new HashSet<Group>();

    private Set<ACL> owned = new HashSet<ACL>();

    private Role role = null;

    protected User(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    protected void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    protected void setRole(Role role) {
        this.role = role;
    }

    protected void addGroup(Group group) {
        if (this.groups.add(group)) {
            group.addUser(this);
        }
    }

    protected void deleteGroup(Group group) {
        if (this.groups.remove(group)) {
            group.deleteUser(this);
        }
    }

    protected String getUsername() {
        return username;
    }

    protected String getPasswordHash() {
        return passwordHash;
    }

    protected List<Group> getGroups() {
        return new Vector<Group>(groups);
    }

    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return ((User) obj).username.equals(username);
        }
        return false;
    }

    public int hashCode() {
        return username.hashCode();
    }

    protected Role getRole() {
        return role;
    }

    protected void addOwned(ACL owned) {
        this.owned.add(owned);
    }

    protected void removeOwned(ACL owned) {
        this.owned.remove(owned);
    }

    protected boolean isOwner() {
        return !owned.isEmpty();
    }
}
