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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.googlecode.vicovre.security.db.PermissionDatabase;
import com.googlecode.vicovre.security.servlet.ThreadLocalPrincipal;

/**
 * A filter that allows users to login
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class LoginFilter implements Filter {

    private Class<? extends Credentials> credentialsClass = null;

    private UserDatabase database = null;

    public void destroy() {
        // Does Nothing
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        boolean failed = false;
        if ((request instanceof HttpServletRequest)
                && (response instanceof HttpServletResponse)) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpSession session = httpRequest.getSession();
            Credentials authenticatedCredentials = (Credentials)
                session.getAttribute(LoginFilter.class.getName()
                        + ".credentials");
            if (authenticatedCredentials == null) {
                try {
                    Credentials credentials = credentialsClass.newInstance();
                    credentials.read(httpRequest);
                    User user = database.getUser(credentials.getId());
                    if (user.getCredentials().authenticate(credentials,
                            httpResponse)) {
                        session.setAttribute(LoginFilter.class.getName()
                            + ".credentials", credentials);
                        ThreadLocalPrincipal.setPrincipal(new SimplePrincipal(
                                credentials.getId()));
                    } else {
                        failed = true;
                    }
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            } else {
                ThreadLocalPrincipal.setPrincipal(new SimplePrincipal(
                        authenticatedCredentials.getId()));
            }
        }
        if (!failed) {
            chain.doFilter(request, response);
        }
        ThreadLocalPrincipal.setPrincipal(null);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        String credentialsClass = filterConfig.getInitParameter(
                "credentialsClass");
        if (credentialsClass == null) {
            throw new ServletException("credentialsClass must be specified");
        }
        try {
            Class<?> clazz = Class.forName(credentialsClass);
            if (clazz.isInstance(Credentials.class)) {
                this.credentialsClass = (Class<? extends Credentials>)
                    clazz;
            } else {
                throw new ServletException("Class " + clazz
                        + " does not implement Credentials");
            }
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        }

        String databasePath = filterConfig.getInitParameter("databasePath");
        if (databasePath == null) {
            throw new ServletException("databasePath must be specified");
        }
        database = new UserDatabase(databasePath);

        String guestRole = filterConfig.getInitParameter("guestRole");
        if (guestRole != null) {
            ThreadLocalPrincipal.setGuestRole(guestRole);
        }

        String permissionDatabaseLocation =
            filterConfig.getInitParameter("permissionDatabase");
        if (permissionDatabaseLocation == null) {
            throw new ServletException(
                    "The permissionDatabase location must be specified");
        }
        String roleFile =
            filterConfig.getInitParameter("roles");
        if (roleFile == null) {
            throw new ServletException("The roles location must be specified");
        }
        String operationsFile =
            filterConfig.getInitParameter("operations");
        if (operationsFile == null) {
            throw new ServletException(
                    "The operations location must be specified");
        }

        try {
            PermissionDatabase database = new PermissionDatabase(
                    permissionDatabaseLocation, roleFile, operationsFile);
            ThreadLocalPrincipal.setDatabase(database);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

}
