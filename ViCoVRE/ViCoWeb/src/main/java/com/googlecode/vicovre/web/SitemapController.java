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

package com.googlecode.vicovre.web;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.recordings.CritterAnnotationHarvester;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;

public class SitemapController implements Controller {

    private RecordingDatabase database = null;

    private CritterAnnotationHarvester critterHarvester = null;

    public SitemapController(RecordingDatabase database,
            CritterAnnotationHarvester critterHarvester) {
        this.database = database;
        this.critterHarvester = critterHarvester;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String folder = request.getRequestURI().substring(
                request.getContextPath().length());
        File path = new File(folder);
        folder = path.getParent();

        String baseUrl = request.getScheme() + "://" + request.getServerName()
            + ":" + request.getServerPort() + request.getContextPath();

        System.err.println("Folder = " + folder);
        List<Recording> recordings = database.getRecordings(folder);
        HashMap<String, List<Annotation>> annotationMap =
                new HashMap<String, List<Annotation>>();
        for (Recording recording : recordings) {
            List<Annotation> annotations =
                new Vector<Annotation>(recording.getAnnotations());
            String critterEvent = recording.getMetadata().getValue(
                "critterEvent");
            if ((critterEvent != null) && (critterHarvester != null)) {
                List<Annotation> critterAnnotations =
                    critterHarvester.harvestAnnotations(critterEvent);
                annotations.addAll(critterAnnotations);
            }
            annotationMap.put(recording.getId(), annotations);
        }

        ModelAndView model = new ModelAndView("sitemap");
        model.addObject("recordings", recordings);
        model.addObject("baseUrl", baseUrl);
        model.addObject("annotations", annotationMap);
        return model;
    }

}
