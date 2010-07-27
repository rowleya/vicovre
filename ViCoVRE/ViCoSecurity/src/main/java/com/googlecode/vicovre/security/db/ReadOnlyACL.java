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

import java.util.List;
import java.util.Vector;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="acl")
public class ReadOnlyACL {

    private boolean allow = false;

    private List<ReadOnlyEntity> exceptions = new Vector<ReadOnlyEntity>();

    protected ReadOnlyACL(ACL acl) {
        allow = acl.isAllow();
        for (Entity entity : acl.getExceptions()) {
            String name = null;
            String type = null;
            if (entity instanceof User) {
                name = ((User) entity).getUsername();
                type = SecurityDatabase.TYPE_USER;
            } else if (entity instanceof Group) {
                name = ((Group) entity).getName();
                type = SecurityDatabase.TYPE_GROUP;
            } else if (entity instanceof Role) {
                name = ((Role) entity).getName();
                type = SecurityDatabase.TYPE_ROLE;
            }
            ReadOnlyEntity roEntity = new ReadOnlyEntity(name, type);
            exceptions.add(roEntity);
        }
    }

    @XmlElement(name="allow")
    public boolean isAllow() {
        return allow;
    }

    @XmlElement(name="exceptions")
    public List<ReadOnlyEntity> getExceptions() {
        return exceptions;
    }

}
