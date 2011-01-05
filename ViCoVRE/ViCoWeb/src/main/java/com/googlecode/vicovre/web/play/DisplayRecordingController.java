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

package com.googlecode.vicovre.web.play;

import java.io.File;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.ReadOnlyACL;
import com.googlecode.vicovre.security.db.ReadOnlyEntity;
import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.rest.responses.GroupsResponse;
import com.googlecode.vicovre.security.rest.responses.UsersResponse;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

public class DisplayRecordingController implements Controller {

    private RecordingDatabase database = null;

    private SecurityDatabase securityDatabase = null;

    public DisplayRecordingController(RecordingDatabase database,
            SecurityDatabase securityDatabase) {
        this.database = database;
        this.securityDatabase = securityDatabase;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String folder = request.getRequestURI().substring(
                request.getContextPath().length());
        File path = new File(folder);
        String id = path.getParentFile().getName();
        folder = path.getParentFile().getParent();

        Recording recording = database.getRecording(folder, id);

        JSONJAXBContext context = new JSONJAXBContext(
                JSONConfiguration.natural().build(),
                UsersResponse.class, GroupsResponse.class,
                ReadOnlyACL.class, ReadOnlyEntity.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter usersWriter = new StringWriter();
        try {
            marshaller.marshallToJSON(
                new UsersResponse(securityDatabase.getUsers()), usersWriter);
        } catch (UnauthorizedException e) {
            // Do Nothing
        }

        StringWriter groupsWriter = new StringWriter();
        try {
            marshaller.marshallToJSON(
                new GroupsResponse(securityDatabase.getGroups()), groupsWriter);
        } catch (UnauthorizedException e) {
            // Do Nothing
        }

        StringWriter aclWriter = new StringWriter();
        StringWriter readAclWriter = new StringWriter();
        if (recording != null) {
            if (database instanceof SecureRecordingDatabase) {
                SecureRecordingDatabase secureDatabase =
                    (SecureRecordingDatabase) database;
                try {
                    marshaller.marshallToJSON(
                            secureDatabase.getRecordingPlayAcl(recording),
                            aclWriter);
                    marshaller.marshallToJSON(
                            secureDatabase.getRecordingReadAcl(recording),
                            readAclWriter);
                } catch (UnauthorizedException e) {
                    // Do Nothing
                }
            }
        }

        String startTime = request.getParameter("startTime");
        if (startTime == null) {
            startTime = "0";
        }

        String autoGain = request.getParameter("agc");
        if (autoGain == null) {
            autoGain = "false";
        }

        ModelAndView modelAndView = new ModelAndView("displayRecording");
        modelAndView.addObject("recording", recording);
        modelAndView.addObject("usersJSON", usersWriter.toString());
        modelAndView.addObject("groupsJSON", groupsWriter.toString());
        modelAndView.addObject("role", securityDatabase.getRole());
        modelAndView.addObject("aclJSON", aclWriter.toString());
        modelAndView.addObject("readAclJSON", readAclWriter.toString());
        modelAndView.addObject("startTime", startTime);
        modelAndView.addObject("agc", autoGain);
        return modelAndView;
    }
}
