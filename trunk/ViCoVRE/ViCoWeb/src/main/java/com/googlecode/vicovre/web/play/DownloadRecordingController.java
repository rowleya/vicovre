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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.layout.EditableLayoutRepository;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.SecurityDatabase;
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

    private SecurityDatabase securityDatabase = null;

    private EditableLayoutRepository layoutRepository = null;

    private RtpTypeRepository typeRepository = null;

    public DownloadRecordingController(RecordingDatabase database,
            SecurityDatabase securityDatabase,
            EditableLayoutRepository layoutRepository,
            RtpTypeRepository typeRepository) throws IOException, SAXException {
        if (!Misc.isCodecsConfigured()) {
            Misc.configureCodecs("/knownCodecs.xml");
        }
        this.database = database;
        this.securityDatabase = securityDatabase;
        this.layoutRepository = layoutRepository;
        this.typeRepository = typeRepository;
    }

    private long getMaxDuration(Recording recording, String[] audioStreams,
            String[] videoStreams, String[] syncStreams) {
        long minStart = Long.MAX_VALUE;
        long maxEnd = 0;
        for (int i = 0; i < audioStreams.length; i++) {
            Stream stream = recording.getStream(audioStreams[i]);
            minStart = Math.min(minStart, stream.getStartTime().getTime());
            maxEnd = Math.max(maxEnd, stream.getEndTime().getTime());
        }
        for (int i = 0; i < videoStreams.length; i++) {
            Stream stream = recording.getStream(videoStreams[i]);
            minStart = Math.min(minStart, stream.getStartTime().getTime());
            maxEnd = Math.max(maxEnd, stream.getEndTime().getTime());
        }
        for (int i = 0; i < syncStreams.length; i++) {
            Stream stream = recording.getStream(syncStreams[i]);
            minStart = Math.min(minStart, stream.getStartTime().getTime());
        }
        return maxEnd - minStart;
    }

    public void doDownload(String format, Recording recording,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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
        } else {

            String[] videoStreams = request.getParameterValues("video");
            String[] audioStreams = request.getParameterValues("audio");
            String[] syncStreams = request.getParameterValues("sync");

            String[] widths = request.getParameterValues("width");
            String[] heights = request.getParameterValues("height");
            String[] xs = request.getParameterValues("x");
            String[] ys = request.getParameterValues("y");

            String off = request.getParameter("offset");
            String strt = request.getParameter("start");
            String dur = request.getParameter("duration");

            if ((videoStreams == null) && (audioStreams == null)
                    && (syncStreams == null)) {
                ReplayLayout replayLayout = recording.getReplayLayouts().get(0);
                if (replayLayout != null) {
                    audioStreams = replayLayout.getAudioStreamIds().toArray(
                            new String[0]);
                    syncStreams = new String[0];
                    List<ReplayLayoutPosition> positions =
                        replayLayout.getLayoutPositions();
                    videoStreams = new String[positions.size()];
                    widths = new String[positions.size()];
                    heights = new String[positions.size()];
                    xs = new String[positions.size()];
                    ys = new String[positions.size()];
                    Layout layout = layoutRepository.findLayout(
                            replayLayout.getName());
                    for (int i = 0; i < positions.size(); i++) {
                        ReplayLayoutPosition pos = positions.get(i);
                        LayoutPosition layoutPos = layout.findStreamPosition(
                                pos.getName());
                        videoStreams[i] = pos.getStreamId();
                        widths[i] = String.valueOf(layoutPos.getWidth());
                        heights[i] = String.valueOf(layoutPos.getHeight());
                        xs[i] = String.valueOf(layoutPos.getX());
                        ys[i] = String.valueOf(layoutPos.getY());
                    }
                    strt = String.valueOf(replayLayout.getTime());
                    dur = String.valueOf(replayLayout.getEndTime()
                            - replayLayout.getTime());
                }
            }
            if (videoStreams == null) {
                videoStreams = new String[0];
            }
            if (audioStreams == null) {
                audioStreams = new String[0];
            }
            if (syncStreams == null) {
                syncStreams = new String[0];
            }

            if (off == null) {
                off = "0.0";
            }
            long offset = (long) (Double.parseDouble(off) * 1000);
            if (strt == null) {
                strt = "0";
            }
            long start = Long.parseLong(strt);

            long maxDuration = getMaxDuration(recording, audioStreams,
                    videoStreams, syncStreams);
            if (offset > maxDuration) {
                offset = maxDuration;
            }
            maxDuration -= start;

            if (dur == null) {
                dur = String.valueOf(maxDuration);
            }
            long duration = Long.parseLong(dur);
            if (duration > maxDuration) {
                duration = maxDuration;
            }

            System.err.println("Downloading, duration = " + duration + " start = " + start + " offset = " + offset);

            Rectangle[] rects = new Rectangle[videoStreams.length];
            for (int i = 0; i < videoStreams.length; i++) {
                videoStreams[i] = new File(recording.getDirectory(),
                        videoStreams[i]).getAbsolutePath();
                rects[i] = new Rectangle(
                        Integer.parseInt(xs[i]), Integer.parseInt(ys[i]),
                        Integer.parseInt(widths[i]),
                        Integer.parseInt(heights[i]));
            }
            for (int i = 0; i < audioStreams.length; i++) {
                audioStreams[i] = new File(recording.getDirectory(),
                        audioStreams[i]).getAbsolutePath();
            }
            for (int i = 0; i < syncStreams.length; i++) {
                syncStreams[i] = new File(recording.getDirectory(),
                        syncStreams[i]).getAbsolutePath();
            }

            Dimension outSize = null;
            String width = request.getParameter("outwidth");
            String height = request.getParameter("outheight");
            if ((width != null) && (height != null)) {
                outSize = new Dimension(Integer.parseInt(width),
                        Integer.parseInt(height));
                if ((outSize.width % 16) != 0) {
                    outSize.width = outSize.width
                        + (16 - (outSize.width % 16));
                }
                if ((outSize.height % 16) != 0) {
                    outSize.height = outSize.height
                        + (16 - (outSize.height % 16));
                }
            }

            double generationSpeed = 1.5;
            String genSpeed = request.getParameter("genspeed");
            if (genSpeed != null) {
                generationSpeed = Double.parseDouble(genSpeed);
            }

            String bgColour = request.getParameter("bgColour");
            int backgroundColour = 0x000000;
            if (bgColour != null) {
                backgroundColour = Integer.parseInt(bgColour);
            }

            String autoGain = request.getParameter("agc");
            boolean agc = false;
            if (autoGain != null) {
                agc = autoGain.equals("true");
            }

            try {
                VideoExtractor extractor = new VideoExtractor(format,
                        videoStreams, rects, audioStreams, syncStreams,
                        backgroundColour, typeRepository, outSize);
                extractor.setAutoGain(agc);
                extractor.setGenerationSpeed(generationSpeed);
                response.setContentType(format);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Content-Disposition",
                        "attachment; filename=\""
                        + recording.getMetadata().getPrimaryValue()
                        + "." + FORMAT_EXT_MAP.get(format) + "\";");

                // Generate the stream
                response.flushBuffer();
                extractor.transferToStream(response.getOutputStream(),
                        start, offset, duration - offset);
            } catch (EOFException e) {
                System.err.println("User disconnected");
            } catch (SocketException e) {
                System.err.println("User disconnected");
            } catch (Exception e) {

                // Handle Apache exception
                if (e.getClass().getSimpleName().equals("ClientAbortException")) {
                    System.err.println("User disconnected");
                } else {
                    e.printStackTrace();
                    throw new IOException(e.getMessage());
                }
            }
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
            System.err.println("Downloading, format = " + format);
            doDownload(format, recording, request, response);
            return null;
        }

        JSONJAXBContext context = new JSONJAXBContext(
                JSONConfiguration.natural().build(), LayoutsResponse.class,
                Recording.class, StreamsResponse.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter layoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(layoutRepository.findFixedLayouts()),
                layoutWriter);

        StringWriter customLayoutWriter = new StringWriter();
        marshaller.marshallToJSON(
                new LayoutsResponse(layoutRepository.findEditableLayouts()),
                customLayoutWriter);

        StringWriter recordingWriter = new StringWriter();
        marshaller.marshallToJSON(recording, recordingWriter);

        StringWriter streamsWriter = new StringWriter();
        try {
            marshaller.marshallToJSON(new StreamsResponse(recording.getStreams()),
                    streamsWriter);
        } catch (UnauthorizedException e) {
            // Do Nothing
        }

        ModelAndView modelAndView = new ModelAndView("downloadRecording");
        modelAndView.addObject("recording", recording);
        modelAndView.addObject("streamsJSON", streamsWriter.toString());
        modelAndView.addObject("folder", recording.getFolder());
        modelAndView.addObject("layoutsJSON", layoutWriter.toString());
        modelAndView.addObject("customLayoutsJSON",
                customLayoutWriter.toString());
        modelAndView.addObject("canPlay", recording.isPlayable());
        modelAndView.addObject("role", securityDatabase.getRole());
        return modelAndView;
    }

}
