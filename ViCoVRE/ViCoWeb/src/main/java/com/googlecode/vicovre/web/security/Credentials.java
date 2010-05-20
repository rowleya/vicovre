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

public interface Credentials {

    /**
     * Gets the id of the credentials to match in the security database
     * @return The id
     */
    String getId();

    /**
     * Gets a map of values for saving / updating the credentials
     * @return The values to save
     */
    HashMap<String, String> getValues();

    /**
     * Sets the values for loading / updating the credentials
     * @param values The values to load
     */
    void setValues(HashMap<String, String> values);

    /**
     * Determines if the given value is private
     * @param field The name of the value
     * @return True if the value is private, false otherwise
     */
    boolean isPrivate(String field);

    /**
     * Determines if the given value is fixed i.e. not editable
     * @param field The name of the value
     * @return True if the value is fixed, false otherwise
     */
    boolean isFixed(String field);

    /**
     * Reads credentials of this type from a request
     * @param request The request
     * @return True if the credentials are read, false if they are missing
     */
    boolean read(HttpServletRequest request);

    /**
     * Authenticates the credentials given against these credentials
     * @param credentials The credentials to check a match for
     * @param response The response to put any error message in
     * @return True if the credentials
     */
    boolean authenticate(Credentials credentials, HttpServletResponse response);
}
