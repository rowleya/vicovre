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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.CacheControl;

import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.rest.responses.UserResponse;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.inject.Inject;

@Path("auth")
public class Auth {

    private static final String BASIC = "Basic ";

    @Inject("realm")
    private String realm = null;

    @Inject("securityDatabase")
    private SecurityDatabase securityDatabase = null;

    private CacheControl getNoCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return cacheControl;
    }

    @Path("form")
    @POST
    @Produces("text/plain")
    public Response formLogin(@QueryParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("onSuccess") String successUrl,
            @QueryParam("onFail") String failUrl,
            @Context HttpServletRequest request) throws URISyntaxException {
        String role = securityDatabase.login(username, password, request);
        if (role != null) {
            if (successUrl == null) {
                return Response.ok(role).build();
            }
            ResponseBuilder response = Response.status(302);
            return response.location(new URI(successUrl)).build();
        }
        if (failUrl == null) {
            return Response.status(Status.FORBIDDEN).build();
        }
        return Response.status(302).location(new URI(failUrl)).build();
    }

    @Path("basic")
    @GET
    @Produces("text/plain")
    public Response basicLogin(@QueryParam("onSuccess") String successUrl,
            @Context HttpServletRequest request) throws URISyntaxException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String role = null;
        if ((authHeader != null) && authHeader.startsWith(BASIC)) {
            String key = authHeader.substring(BASIC.length());
            key = Base64.base64Decode(key);
            String[] parts = key.split(":", 2);
            String username = parts[0];
            String password = parts[1];
            role = securityDatabase.login(username, password, request);
        }
        if (role == null) {
            return Response.status(Status.UNAUTHORIZED).header(
                HttpHeaders.WWW_AUTHENTICATE, "Basic realm=" + realm).cacheControl(getNoCache()).build();
        }

        if (successUrl == null) {
            return Response.ok(role).cacheControl(getNoCache()).build();
        }

        ResponseBuilder response = Response.status(302);
        return response.location(new URI(successUrl)).cacheControl(getNoCache()).build();
    }

    @Path("cert")
    @GET
    @Produces("text/plain")
    public Response certLogin(@QueryParam("onSuccess") String successUrl,
            @QueryParam("onFail") String failUrl,
            @Context HttpServletRequest request)
            throws CertificateEncodingException, URISyntaxException {
         X509Certificate[] certs = (X509Certificate[])
             request.getAttribute("javax.servlet.request.X509Certificate");
         String role = null;
         if (certs != null) {
             for (X509Certificate cert : certs) {
                 String username = cert.getSubjectX500Principal().getName();
                 String password = new String(Base64.encode(cert.getEncoded()));

                 role = securityDatabase.login(username, password, request);
                 if (role != null) {
                     break;
                 }
             }
         }

         if (role != null) {
             if (successUrl == null) {
                 return Response.ok(role).cacheControl(getNoCache()).build();
             }

             ResponseBuilder response = Response.status(302);
             return response.location(new URI(successUrl)).cacheControl(getNoCache()).build();
         }
         if (failUrl == null) {
             return Response.status(Status.FORBIDDEN).cacheControl(getNoCache()).build();
         }
         return Response.status(302).location(new URI(failUrl)).cacheControl(getNoCache()).build();
    }

    @Path("user")
    @GET
    @Produces({"application/json", "text/xml"})
    public Response getUser() {
        return Response.ok(new UserResponse(securityDatabase.getUsername(),
                securityDatabase.getRole())).cacheControl(getNoCache()).build();
    }

    @Path("logout")
    @GET
    public Response logout(@Context HttpServletRequest request) {
        securityDatabase.logout(request);
        return Response.ok().cacheControl(getNoCache()).build();
    }
}
