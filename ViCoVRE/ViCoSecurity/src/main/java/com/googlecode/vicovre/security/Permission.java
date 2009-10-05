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
 * Represents a permission
 * @author Andrew G D Rowley
 * @version 1.0
 */
public abstract class Permission {

    private OperationSet operation;

    private boolean allow = false;

    /**
     * Creates a new permission
     * @param operation The operation(s) of the permission
     * @param allow True to allow the permission, false to deny it
     */
    public Permission(OperationSet operation, boolean allow) {
        this.operation = operation;
        this.allow = allow;
    }

    /**
     * Gets the operation(s) of this permission
     * @return The operation(s)
     */
    public OperationSet getOperation() {
        return operation;
    }

    /**
     * Determines if this permission contains the given operations
     * @param operations The operations to check
     * @return True if this permission contains the operations
     */
    public boolean contains(OperationSet operations) {
        return this.operation.contains(operations);
    }

    /**
     * Determines if this permission allows or denies the given operation
     * @return True if allow, false if deny
     */
    public boolean allow() {
        return allow;
    }

    /**
     * Determines if the user has the given permission
     * @param user The user to check
     * @return True if the user has the permission, false otherwise
     */
    public abstract boolean userHasPermission(User user);
}
