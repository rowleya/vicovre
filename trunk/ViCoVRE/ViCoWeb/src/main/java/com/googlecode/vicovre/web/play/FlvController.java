/*
 * @(#)ImageExtractorServlet.java
 * Created: 14 Nov 2007
 * Version: 1.0
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
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
 */

package com.googlecode.vicovre.web.play;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

/**
 * A servlet for streaming out in FLV format
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class FlvController implements Controller {

    private static final double DEFAULT_GENERATION_SPEED = 1.5;

    private RtpTypeRepository rtpTypeRepository = null;

    private RecordingDatabase database = null;

    public FlvController(RecordingDatabase database,
            RtpTypeRepository rtpTypeRepository)
            throws IOException, SAXException {
        if (!Misc.isCodecsConfigured()) {
            Misc.configureCodecs("/knownCodecs.xml");
        }
        this.rtpTypeRepository = rtpTypeRepository;
        this.database = database;

    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String sessionId = request.getParameter("id");
        String folder = request.getParameter("folder");

        Recording recording = database.getRecording(folder, sessionId);
        File path = recording.getDirectory();

        String strt = request.getParameter("start");
        if (strt == null) {
            strt = "0.0";
        }
        long start = (long) (Double.parseDouble(strt) * 1000);
        String dur = request.getParameter("duration");
        long maxDuration = recording.getDuration() - start;
        long duration = 0;
        if (dur != null) {
            duration = (long) (Double.parseDouble(dur) * 1000);
        } else {
            duration = maxDuration;
        }

        String[] videoStreams = request.getParameterValues("video");
        if (videoStreams == null) {
            videoStreams = new String[0];
        }
        String[] audioStreams = request.getParameterValues("audio");
        if (audioStreams == null) {
            audioStreams = new String[0];
        }
        String[] syncStreams = request.getParameterValues("sync");
        if (syncStreams == null) {
            syncStreams = new String[0];
        }

        String offShift = request.getParameter("offsetShift");
        if (offShift == null) {
            offShift = "0";
        }
        long offsetShift = Long.parseLong(offShift);

        Rectangle[] rects = new Rectangle[videoStreams.length];
        String[] widths = request.getParameterValues("width");
        String[] heights = request.getParameterValues("height");
        String[] xs = request.getParameterValues("x");
        String[] ys = request.getParameterValues("y");
        String[] opacity = request.getParameterValues("opacity");
        double[] opacities = new double[videoStreams.length];
        for (int i = 0; i < videoStreams.length; i++) {
            videoStreams[i] = new File(path, videoStreams[i]).getAbsolutePath();
            rects[i] = new Rectangle(
                    Integer.parseInt(xs[i]), Integer.parseInt(ys[i]),
                    Integer.parseInt(widths[i]), Integer.parseInt(heights[i]));
            opacities[i] = Double.parseDouble(opacity[i]);
        }
        for (int i = 0; i < audioStreams.length; i++) {
            audioStreams[i] = new File(path, audioStreams[i]).getAbsolutePath();
        }
        for (int i = 0; i < syncStreams.length; i++) {
            syncStreams[i] = new File(path, syncStreams[i]).getAbsolutePath();
        }

        Dimension outSize = null;
        String width = request.getParameter("outwidth");
        String height = request.getParameter("outheight");
        if ((width != null) && (height != null)) {
            outSize = new Dimension(Integer.parseInt(width),
                    Integer.parseInt(height));
            if ((outSize.width % 16) != 0) {
                outSize.width = outSize.width + (16 - (outSize.width % 16));
            }
            if ((outSize.height % 16) != 0) {
                outSize.height = outSize.height + (16 - (outSize.height % 16));
            }
        }

        double generationSpeed = DEFAULT_GENERATION_SPEED;
        String genSpeed = request.getParameter("genspeed");
        if (genSpeed != null) {
            generationSpeed = Double.parseDouble(genSpeed);
        }

        String bgColour = request.getParameter("bgColour");
        int backgroundColour = 0x000000;
        if (bgColour != null) {
            backgroundColour = Integer.parseInt(bgColour);
        }

        String contentType = request.getHeader("Accept");
        if (contentType == null) {
            contentType = "video/x-flv";
        }

        try {
            VideoExtractor extractor = new VideoExtractor(contentType,
                    videoStreams, rects, opacities, audioStreams, syncStreams,
                    backgroundColour, rtpTypeRepository, outSize);
            extractor.setGenerationSpeed(generationSpeed);
            response.setContentType(contentType);
            response.setStatus(HttpServletResponse.SC_OK);

            // Generate the stream
            response.flushBuffer();
            extractor.transferToStream(response.getOutputStream(),
                    offsetShift, start, duration - start);
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
        return null;
    }
}
