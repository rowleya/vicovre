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
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.repositories.layout.EditableLayoutRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.rest.responses.GroupsResponse;
import com.googlecode.vicovre.security.rest.responses.UsersResponse;
import com.googlecode.vicovre.web.rest.response.LayoutsResponse;
import com.googlecode.vicovre.web.rest.response.StreamsResponse;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

public class GWTController implements Controller {

    private SecurityDatabase securityDatabase = null;

    private LayoutRepository layoutRepository = null;

    private EditableLayoutRepository editableLayoutRepository = null;

    public GWTController(SecurityDatabase securityDatabase,
            LayoutRepository layoutRepository,
            EditableLayoutRepository editableLayoutRepository)
            throws IOException, SAXException {
        if (!Misc.isCodecsConfigured()) {
            Misc.configureCodecs("/knownCodecs.xml");
        }
        this.securityDatabase = securityDatabase;
        this.layoutRepository = layoutRepository;
        this.editableLayoutRepository = editableLayoutRepository;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        JSONJAXBContext context = new JSONJAXBContext(
                JSONConfiguration.natural().build(), LayoutsResponse.class,
                Recording.class, StreamsResponse.class,
                UsersResponse.class, GroupsResponse.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter layoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(layoutRepository.findLayouts()),
                layoutWriter);

        StringWriter customLayoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(editableLayoutRepository.findLayouts()),
                customLayoutWriter);

        StringWriter usersWriter = new StringWriter();
        marshaller.marshallToJSON(
                new UsersResponse(securityDatabase.getUsers()), usersWriter);

        StringWriter groupsWriter = new StringWriter();
        marshaller.marshallToJSON(
                new GroupsResponse(securityDatabase.getGroups()), groupsWriter);

        ModelAndView modelAndView = new ModelAndView("gwt");
        modelAndView.addObject("layoutsJSON", layoutWriter.toString());
        modelAndView.addObject("customLayoutsJSON",
                customLayoutWriter.toString());
        modelAndView.addObject("usersJSON", usersWriter.toString());
        modelAndView.addObject("groupsJSON", groupsWriter.toString());
        return modelAndView;
    }

}