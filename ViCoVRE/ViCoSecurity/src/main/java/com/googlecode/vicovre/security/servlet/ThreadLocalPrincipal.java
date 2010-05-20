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

package com.googlecode.vicovre.security.servlet;

import java.io.IOException;
import java.security.Principal;

import com.googlecode.vicovre.security.Role;
import com.googlecode.vicovre.security.User;
import com.googlecode.vicovre.security.db.PermissionDatabase;

/**
 * Stores the current Principal for use in authentication.
 * @author Andrew G D Rowley
 * @version 1.0
 */
public final class ThreadLocalPrincipal {

    private static ThreadLocal<Principal> principal =
        new ThreadLocal<Principal>();

    private static PermissionDatabase database = null;

    private static String guestRole = "GuestUser";

    private ThreadLocalPrincipal() {
        // Does Nothing
    }

    /**
     * Sets the permission database to use
     * @param database The database to use
     */
    public static final void setDatabase(PermissionDatabase database) {
        if (ThreadLocalPrincipal.database == null) {
            ThreadLocalPrincipal.database = database;
        } else {
            throw new RuntimeException("Cannot change database once set");
        }
    }

    /**
     * Sets the role of the guest user
     * @param guestRole The role
     */
    public static final void setGuestRole(String guestRole) {
        if (ThreadLocalPrincipal.guestRole == null) {
            ThreadLocalPrincipal.guestRole = guestRole;
        } else {
            throw new RuntimeException("Cannot change guest role once set");
        }
    }

    /**
     * Sets the current principal
     * @param principal The principal to set
     */
    public static final void setPrincipal(Principal principal) {
        ThreadLocalPrincipal.principal.set(principal);
    }

    /**
     * Gets the stored principal.
     * @return The stored principal
     */
    public static final Principal getPrincipal() {
        return principal.get();
    }

    /**
     * Gets the stored principal as a user.
     * @return The user
     * @throws IOException
     */
    public static final User getUser() {
        if (principal.get() != null) {
            String username = principal.get().getName();
            User user = database.getUser(username);
            return user;
        }
        User user = new User("");
        user.addRole(new Role(guestRole));
        return user;
    }

}
