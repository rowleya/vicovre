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
 * Represents a role in the recording system
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Role {

    private String name = null;

    private HashSet<Role> subRoles = new HashSet<Role>();

    /**
     * Creates a new Role
     * @param name The name of the role
     */
    public Role(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the role
     * @return The name of the role
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the roles that a subroles of this one
     * @return The subroles
     */
    public List<Role> getSubRoles() {
        return new Vector<Role>(subRoles);
    }

    /**
     * Adds a sub-role to this role
     * @param role The role to add
     */
    public void addSubRole(Role role) {
        if (role.subRoles.contains(this)) {
            throw new RuntimeException("Adding role " + role.name
                    + " as subrole of " + name + " would create a loop!");
        }
        if (!subRoles.contains(role)) {
            subRoles.add(role);
        }
    }

    /**
     * Determines if the given role is a role or subrole of this one
     * @param role The role to check
     * @return True if the role is the same or is a subrole of this one
     */
    public boolean isRoleOrSubrole(Role role) {
        if (role.name.equals(name)) {
            return true;
        }

        for (Role sub : subRoles) {
            if (sub.isRoleOrSubrole(role)) {
                return true;
            }
        }
        return false;
    }
}
