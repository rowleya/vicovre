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

package com.googlecode.vicovre.web.security;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.DecodingException;


public class UsernamePasswordCredentials implements Credentials {

    private String username = null;

    private Password password = null;

    public boolean read(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String pass = null;
        if (authorization == null) {
            username = request.getParameter("username");
            pass = request.getParameter("password");
        } else {
            if (authorization.startsWith("BASIC ")) {
                try {
                    String userPass = new String(Base64.decode(
                            authorization.substring(6)));
                    int index = userPass.indexOf(':');
                    username = userPass.substring(0, index);
                    pass = userPass.substring(index + 1);
                } catch (DecodingException e) {
                    e.printStackTrace();
                    return false;
                }

            }
        }
        if ((username == null) || (pass == null)) {
            return false;
        }

        password = new Password(pass);
        return true;
    }

    public String getId() {
        return username;
    }

    public HashMap<String, String> getValues() {
        HashMap<String, String> values = new HashMap<String, String>();
        values.put("username", username);
        values.put("password", password.getHashHexString());
        return values;
    }

    public void setValues(HashMap<String, String> values) {
        username = values.get("username");
        password.setHexStringHash(values.get("password"));
    }

    public boolean authenticate(Credentials credentials,
            HttpServletResponse response) {
        if (credentials instanceof UsernamePasswordCredentials) {
            UsernamePasswordCredentials c =
                (UsernamePasswordCredentials) credentials;
            if (c.username.equals(username) && c.password.equals(password)) {
                return true;
            }
        }
        response.setHeader("WWW-Authenticate", "BASIC realm=\"Secure\"");
        return false;
    }

    public boolean isPrivate(String field) {
        if (field.equals("password")) {
            return true;
        }
        return false;
    }

    public boolean isFixed(String field) {
        if (field.equals("username")) {
            return true;
        }
        return false;
    }
}
