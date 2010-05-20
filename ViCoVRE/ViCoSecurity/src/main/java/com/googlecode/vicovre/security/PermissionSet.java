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

import java.util.LinkedList;
import java.util.List;

/**
 * An ACL for a resource
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class PermissionSet {

    public static final boolean ALLOW = true;

    public static final boolean DENY = false;

    private String className = null;

    private String name = null;

    private int hashCode = 0;

    private boolean defaultPermission = DENY;

    private PermissionSet perms = null;

    private List<Permission> permissions = new LinkedList<Permission>();

    public void setClassName(String className) {
        this.className = className;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public String getClassName() {
        return className;
    }

    public int getHashCode() {
        return hashCode;
    }

    public String getName() {
        return name;
    }

    public void setProtectedObject(Object object) {
        className = object.getClass().getCanonicalName();
        hashCode = object.hashCode();
    }

    public void setSetPermissions(PermissionSet perms) {
        this.perms = perms;
    }

    public PermissionSet getSetPermissions() {
        return perms;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void addPermission(Permission permission, int position) {
        if (position > permissions.size()) {
            position = permissions.size();
        }
        permissions.add(position, permission);
    }

    public void setDefaultPermission(boolean allow) {
        this.defaultPermission = allow;
    }

    public boolean getDefaultPermission() {
        return defaultPermission;
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void deletePermission(Permission permission) {
        permissions.remove(permission);
    }

    public void deletePermission(int index) {
        permissions.remove(index);
    }

    public void setUserVariable(User user) {
        for (Permission permission : permissions) {
            if (permission instanceof UserPermission) {
                UserPermission userPerm = (UserPermission) permission;
                if (userPerm.isVariable()) {
                    userPerm.setUser(user);
                }
            }
        }
        if (perms != null) {
            perms.setUserVariable(user);
        }
    }

    public void setGroupVariable(Group group) {
        for (Permission permission : permissions) {
            if (permission instanceof GroupPermission) {
                GroupPermission groupPerm = (GroupPermission) permission;
                if (groupPerm.isVariable()) {
                    groupPerm.setGroup(group);
                }
            }
        }
        if (perms != null) {
            perms.setGroupVariable(group);
        }
    }

    public boolean userHasPermission(User user, OperationSet operations) {
        for (Permission permission : permissions) {
            if (permission.contains(operations)) {
                if (permission.userHasPermission(user)) {
                    return permission.allow();
                }
            }
        }
        return defaultPermission;
    }
}
