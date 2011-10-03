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

package com.googlecode.vicovre.web.rest;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import com.googlecode.vicovre.repositories.layout.EditableLayoutRepository;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutExistsException;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.web.rest.response.LayoutsResponse;
import com.sun.jersey.spi.inject.Inject;

/**
 * Handler of layouts.
 * @author Andrew G D Rowley
 * @version 1.0
 */
@Path("layout")
public class LayoutHandler {

    private EditableLayoutRepository layoutRepository = null;

    private CacheControl getNoCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return cacheControl;
    }

    /**
     * Creates a LayoutHandler.
     * @param database The database
     * @param layoutRepository The layout repository
     */
    public LayoutHandler(
            @Inject final EditableLayoutRepository layoutRepository) {
        this.layoutRepository = layoutRepository;
    }

    /**
     * Gets the layouts.
     * @return The layouts
     */
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getLayouts() {
        List<Layout> layouts = layoutRepository.findLayouts();
        return Response.ok(new LayoutsResponse(layouts)).cacheControl(getNoCache()).build();
    }

    @Path("fixed")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getFixedLayouts() {
        List<Layout> layouts = layoutRepository.findFixedLayouts();
        return Response.ok(new LayoutsResponse(layouts)).cacheControl(getNoCache()).build();
    }

    @Path("custom")
    @GET
    @Produces({"text/xml", "application/json"})
    public Response getCustomLayouts() {
        List<Layout> layouts = layoutRepository.findEditableLayouts();
        return Response.ok(new LayoutsResponse(layouts)).cacheControl(getNoCache()).build();
    }

    @Path("custom/{layoutName}")
    @PUT
    public Response addLayout(@Context UriInfo uriInfo,
            @PathParam("layoutName") String layoutName,
            @QueryParam("position") List<String> positionName) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        Layout layout = new Layout();
        layout.setName(layoutName);
        List<LayoutPosition> positions = new Vector<LayoutPosition>();
        for (int i = 0; i < positionName.size(); i++) {
            LayoutPosition position = new LayoutPosition();
            String name = positionName.get(i);
            position.setName(name);
            position.setX(Integer.parseInt(params.getFirst(name + "X")));
            position.setY(Integer.parseInt(params.getFirst(name + "Y")));
            position.setWidth(Integer.parseInt(
                    params.getFirst(name + "Width")));
            position.setHeight(Integer.parseInt(
                    params.getFirst(name + "Height")));
            String opacity = params.getFirst(name + "Opacity");
            if (opacity != null) {
                position.setOpacity(Double.parseDouble(opacity));
            }
            String assignable = params.getFirst(name + "IsAssignable");
            if (assignable != null) {
                position.setAssignable(Boolean.parseBoolean(assignable));
            }
            String hasChanges = params.getFirst(name + "HasChanges");
            if (hasChanges != null) {
                position.setChanges(Boolean.parseBoolean(hasChanges));
            }
            String hasAudio = params.getFirst(name + "HasAudio");
            if (hasAudio != null) {
                position.setAudio(Boolean.parseBoolean(hasAudio));
            }
            positions.add(position);
        }
        layout.setStreamPostions(positions);
        try {
            layoutRepository.addLayout(layout);
        } catch (LayoutExistsException e) {
            return Response.status(Status.CONFLICT).entity(
                    e.getMessage()).build();
        } catch (IOException e) {
            return Response.serverError().build();
        }

        return Response.ok().build();
    }

}
