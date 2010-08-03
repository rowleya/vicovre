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
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.layout.EditableLayoutRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.web.rest.response.LayoutsResponse;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

public class DownloadRecordingController implements Controller {

    private RecordingDatabase database = null;

    private LayoutRepository layoutRepository = null;

    private EditableLayoutRepository editableLayoutRepository = null;

    public DownloadRecordingController(RecordingDatabase database,
            LayoutRepository layoutRepository,
            EditableLayoutRepository editableLayoutRepository) {
        this.database = database;
        this.layoutRepository = layoutRepository;
        this.editableLayoutRepository = editableLayoutRepository;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        File path = new File(database.getTopLevelFolder().getFile(),
                request.getRequestURI().substring(
                        request.getContextPath().length()));
        path = path.getParentFile();

        Folder folder = database.getFolder(path.getParentFile());
        Recording recording = null;
        if (folder != null) {
            recording = folder.getRecording(path.getName());
        }

        String folderPath = folder.getFile().getAbsolutePath().substring(
            database.getTopLevelFolder().getFile().getAbsolutePath().length()).
                replace(File.separator, "/");

        JSONJAXBContext context = new JSONJAXBContext(
                JSONConfiguration.natural().build(), LayoutsResponse.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();
        StringWriter layoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(layoutRepository.findLayouts()),
                layoutWriter);
        StringWriter customLayoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(editableLayoutRepository.findLayouts()),
                customLayoutWriter);

        ModelAndView modelAndView = new ModelAndView("downloadRecording");
        modelAndView.addObject("recording", recording);
        modelAndView.addObject("folder", folderPath);
        modelAndView.addObject("layoutsJSON", layoutWriter.toString());
        modelAndView.addObject("customLayoutsJSON",
                customLayoutWriter.toString());
        return modelAndView;
    }

}
