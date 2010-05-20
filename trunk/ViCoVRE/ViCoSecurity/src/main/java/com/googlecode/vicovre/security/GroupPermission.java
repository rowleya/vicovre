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

/**
 * A Group Permission object
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class GroupPermission extends Permission {

    /**
     * The name of the variable that indicates a variable group
     */
    public static final String VARIABLE = "?group";

    private Group group = null;

    private Role role = null;

    /**
     * Creates a new GroupPermission
     * @param operations The operations of the permission
     * @param allow True to allow the permission
     * @param group The group to assign the permission to
     * @param role The role to assign the permission to
     */
    public GroupPermission(OperationSet operations, boolean allow, Group group,
            Role role) {
        super(operations, allow);
        this.group = group;
        this.role = role;
    }

    /**
     * Creates a new variable GroupPermission
     * @param operations The operations of the permission
     * @param allow True to allow the permission
     * @param role The role to assign the permission to
     */
    public GroupPermission(OperationSet operations, boolean allow, Role role) {
        super(operations, allow);
        this.role = role;
    }

    /**
     * Set the group of a variable permission - only works if not already set
     * @param group The group to set
     */
    public void setGroup(Group group) {
        if (this.group == null) {
            this.group = group;
        }
    }

    /**
     * Determines if the group is currently a variable
     * @return True if the group has not been set
     */
    public boolean isVariable() {
        return group == null;
    }

    /**
     * Gets the name of the group
     * @return The name of the group
     */
    public String getGroupName() {
        return group.getName();
    }

    /**
     * Gets the name of the role
     * @return The name of the role
     */
    public String getRoleName() {
        return role.getName();
    }

    /**
     *
     * @see com.googlecode.vicovre.security.Permission#
     *     userHasPermission(
     *     com.googlecode.vicovre.security.User)
     */
    public boolean userHasPermission(User user) {
        return group.userHasRole(user, role);
    }
}
