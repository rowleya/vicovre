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

import com.googlecode.vicovre.security.servlet.CurrentUser;

public class ACL {

    private String id = null;

    private User owner = null;

    private boolean canProxy = false;

    private boolean allow = false;

    private Set<Entity> exceptions = new HashSet<Entity>();

    protected ACL(String id, User owner, boolean allow, boolean canProxy) {
        this.owner = owner;
        this.allow = allow;
        this.canProxy = canProxy;
        owner.addOwned(this);
    }

    protected void setAllow(boolean allow) {
        this.allow = allow;
    }

    protected void setOwner(User owner) {
        this.owner = owner;
    }

    protected void addException(Entity exception) {
        if (exceptions.add(exception)) {
            exception.addACL(this);
        }
    }

    protected void removeException(Entity exception) {
        if (exceptions.remove(exception)) {
            exception.removeACL(this);
        }
    }

    protected void clearExceptions() {
        for (Entity entity : exceptions) {
            entity.removeACL(this);
        }
    }

    public boolean isAllowed(User user) {
        if (user.equals(owner)) {
            return true;
        }

        boolean isException = exceptions.contains(user);
        if (!isException) {
            for (Group group : user.getGroups()) {
                isException = exceptions.contains(group);
                if (isException) {
                    break;
                }
            }
        }
        if (!isException) {
            Role role = user.getRole();
            while (!isException && role != null) {
                isException = exceptions.contains(role);
                role = role.getSubRole();
            }
        }

        return allow != isException;
    }

    public boolean isAllowed() {
        return isAllowed(CurrentUser.get());
    }

    protected boolean canProxy() {
        return canProxy;
    }

    protected boolean isAllow() {
        return allow;
    }

    protected String getId() {
        return id;
    }

    protected User getOwner() {
        return owner;
    }

    protected List<Entity> getExceptions() {
        return new Vector<Entity>(exceptions);
    }

    protected void delete() {
        for (Entity entity : exceptions) {
            entity.removeACL(this);
        }
        owner.removeOwned(this);
    }
}
