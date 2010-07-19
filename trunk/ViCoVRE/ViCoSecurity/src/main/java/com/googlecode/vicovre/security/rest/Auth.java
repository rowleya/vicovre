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

package com.googlecode.vicovre.security.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.inject.Inject;

@Path("auth")
public class Auth {

    private static final String BASIC = "Basic ";

    @Inject
    private String realm = null;

    @Inject
    private SecurityDatabase securityDatabase = null;

    @Path("form")
    @POST
    public Response formLogin(@QueryParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("onSuccess") String successUrl,
            @QueryParam("onFail") String failUrl,
            @Context HttpServletRequest request) throws URISyntaxException {
        if (securityDatabase.login(username, password, request)) {
            if (successUrl == null) {
                return Response.ok().build();
            }
            return Response.ok().location(new URI(successUrl)).build();
        }
        if (failUrl == null) {
            return Response.status(Status.FORBIDDEN).build();
        }
        return Response.ok().location(new URI(failUrl)).build();
    }

    @Path("basic")
    @GET
    public Response basicLogin(@QueryParam("onSuccess") String successUrl,
            @Context HttpServletRequest request) throws URISyntaxException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean failed = false;
        if ((authHeader == null) || !authHeader.startsWith(BASIC)) {
            failed = true;
        } else {
            String key = authHeader.substring(BASIC.length());
            key = Base64.base64Decode(key);
            String[] parts = key.split(":", 2);
            String username = parts[0];
            String password = parts[1];
            if (!securityDatabase.login(username, password, request)) {
                failed = true;
            }
        }
        if (failed) {
            return Response.status(Status.UNAUTHORIZED).header(
                HttpHeaders.WWW_AUTHENTICATE, "Basic realm=" + realm).build();
        }
        if (successUrl == null) {
            return Response.ok().build();
        }
        return Response.ok().location(new URI(successUrl)).build();
    }

    @Path("cert")
    @GET
    public Response certLogin(@QueryParam("onSuccess") String successUrl,
            @QueryParam("onFail") String failUrl,
            @Context HttpServletRequest request)
            throws CertificateEncodingException, URISyntaxException {
         X509Certificate[] certs = (X509Certificate[])
             request.getAttribute("javax.servlet.request.X509Certificate");
         boolean failed = false;
         if (certs == null) {
             failed = true;
         } else {
             failed = true;
             for (X509Certificate cert : certs) {
                 String username = cert.getSubjectX500Principal().getName();
                 String password = new String(Base64.encode(cert.getEncoded()));
                 if (securityDatabase.login(username, password, request)) {
                     failed = false;
                     break;
                 }
             }
         }

         if (!failed) {
             if (successUrl == null) {
                 return Response.ok().build();
             }
             return Response.ok().location(new URI(successUrl)).build();
         }
         if (failUrl == null) {
             return Response.status(Status.FORBIDDEN).build();
         }
         return Response.ok().location(new URI(failUrl)).build();
    }

    @Path("role")
    @GET
    @Produces("text/plain")
    public Response getRole() {
        return Response.ok(securityDatabase.getRole()).build();
    }

    @Path("logout")
    @GET
    public Response logout(@Context HttpServletRequest request) {
        securityDatabase.logout(request);
        return Response.ok().build();
    }
}
