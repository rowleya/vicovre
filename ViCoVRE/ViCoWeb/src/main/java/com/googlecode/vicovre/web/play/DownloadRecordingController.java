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
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;

import javax.media.ResourceUnavailableException;
import javax.media.format.UnsupportedFormatException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.layout.EditableLayoutRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.web.rest.response.LayoutsResponse;
import com.googlecode.vicovre.web.rest.response.StreamsResponse;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

public class DownloadRecordingController implements Controller {

    private static final HashMap<String, String> FORMAT_EXT_MAP =
        new HashMap<String, String>();
    static {
        FORMAT_EXT_MAP.put("audio/mpeg", "mp3");
        FORMAT_EXT_MAP.put("audio/x-ms-wma", "wma");
        FORMAT_EXT_MAP.put("video/x-flv", "flv");
        FORMAT_EXT_MAP.put("video/x-ms-wmv", "wmv");
        FORMAT_EXT_MAP.put("video/mp4", "mp4");
    }

    private RecordingDatabase database = null;

    private LayoutRepository layoutRepository = null;

    private EditableLayoutRepository editableLayoutRepository = null;

    private RtpTypeRepository typeRepository = null;

    public DownloadRecordingController(RecordingDatabase database,
            LayoutRepository layoutRepository,
            EditableLayoutRepository editableLayoutRepository,
            RtpTypeRepository typeRepository) throws IOException, SAXException {
        if (!Misc.isCodecsConfigured()) {
            Misc.configureCodecs("/knownCodecs.xml");
        }
        this.database = database;
        this.layoutRepository = layoutRepository;
        this.editableLayoutRepository = editableLayoutRepository;
        this.typeRepository = typeRepository;
    }

    public void doDownload(String format, Recording recording,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, UnsupportedFormatException,
            ResourceUnavailableException {
        if (format.equals("application/x-agvcr")) {
            OutputStream output = response.getOutputStream();
            String[] streams = request.getParameterValues("stream");
            response.setContentType("application/x-agvcr");
            String name = recording.getId();
            if ((recording.getMetadata() != null)
                    && (recording.getMetadata().getPrimaryValue() != null)) {
                name = recording.getMetadata().getPrimaryValue();
            }
            response.setHeader("Content-Disposition", "inline; filename="
                    + name + ".agvcr" + ";");
            response.flushBuffer();
            AGVCRWriter writer = new AGVCRWriter(typeRepository,
                    recording, streams, output);
            writer.write();
        } else if (format.startsWith("audio")) {
            String[] audioStreams = request.getParameterValues("audio");

            String offsetString = request.getParameter("offset");
            if (offsetString == null) {
                offsetString = "0";
            }
            long offset = Long.parseLong(offsetString);

            long minStart = Long.MAX_VALUE;
            long maxEnd = 0;
            for (int i = 0; i < audioStreams.length; i++) {
                Stream stream = recording.getStream(audioStreams[i]);
                minStart = Math.min(minStart, stream.getStartTime().getTime());
                maxEnd = Math.max(maxEnd, stream.getEndTime().getTime());
            }
            long maxDuration = maxEnd - minStart;
            if (offset > maxDuration) {
                offset = maxDuration;
            }
            maxDuration -= offset;

            String durationString = request.getParameter("duration");
            if (durationString == null) {
                durationString = String.valueOf(maxDuration);
            }
            long duration = Long.parseLong(durationString);
            if (duration > maxDuration) {
                duration = maxDuration;
            }

            String genSpeed = request.getParameter("genSpeed");
            if (genSpeed == null) {
                genSpeed = "0";
            }

            String[] audioFilenames = new String[audioStreams.length];
            for (int i = 0; i < audioStreams.length; i++) {
                File file = new File(recording.getDirectory(), audioStreams[i]);
                audioFilenames[i] = file.getAbsolutePath();
            }
            VideoExtractor extractor = new VideoExtractor(format, null, null,
                    audioFilenames, null, 0, typeRepository, null);
            extractor.setGenerationSpeed(Double.parseDouble(genSpeed));

            response.setContentType(format);
            String name = recording.getId();
            if ((recording.getMetadata() != null)
                    && (recording.getMetadata().getPrimaryValue() != null)) {
                name = recording.getMetadata().getPrimaryValue();
            }
            response.setHeader("Content-Disposition", "inline; filename="
                    + name + "." + FORMAT_EXT_MAP.get(format) + ";");
            response.flushBuffer();
            extractor.transferToStream(response.getOutputStream(), 0, offset,
                    duration, null);
        } else if (format.startsWith("video")) {

        }
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String folder = request.getRequestURI().substring(
                request.getContextPath().length());
        File path = new File(folder);
        String id = path.getParentFile().getName();
        folder = path.getParentFile().getParent();

        Recording recording = database.getRecording(folder, id);

        String format = request.getParameter("format");
        if (format != null) {
            doDownload(format, recording, request, response);
            return null;
        }

        JSONJAXBContext context = new JSONJAXBContext(
                JSONConfiguration.natural().build(), LayoutsResponse.class,
                Recording.class, StreamsResponse.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter layoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(layoutRepository.findLayouts()),
                layoutWriter);

        StringWriter customLayoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(editableLayoutRepository.findLayouts()),
                customLayoutWriter);

        StringWriter recordingWriter = new StringWriter();
        marshaller.marshallToJSON(recording, recordingWriter);

        StringWriter streamsWriter = new StringWriter();
        marshaller.marshallToJSON(new StreamsResponse(recording.getStreams()),
                streamsWriter);

        ModelAndView modelAndView = new ModelAndView("downloadRecording");
        modelAndView.addObject("recording", recording);
        modelAndView.addObject("streamsJSON", streamsWriter.toString());
        modelAndView.addObject("folder", folder);
        modelAndView.addObject("layoutsJSON", layoutWriter.toString());
        modelAndView.addObject("customLayoutsJSON",
                customLayoutWriter.toString());
        return modelAndView;
    }

}
