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
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.repositories.rtptype.impl.RtpTypeRepositoryXmlImpl;

/**
 * A servlet for streaming out in FLV format
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class FlvController implements Controller {

    private static final double DEFAULT_GENERATION_SPEED = 1.5;

    private static final String EXT = ".jpg";

    private static final int EXT_LEN = EXT.length();

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
        String folderPath = request.getParameter("folder");
        File videoFile = null;

        String off = request.getParameter("start");
        if (off == null) {
            off = "0.0";
        }
        long offset = (long) (Double.parseDouble(off) * 1000);
        String dur = request.getParameter("duration");
        long duration = (long) (Double.parseDouble(dur) * 1000);
        String videoStream = request.getParameter("video");
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
        Folder folder = database.getTopLevelFolder();
        if (folderPath != null && !folderPath.equals("")) {
            folder = database.getFolder(new File(
                    database.getTopLevelFolder().getFile(), folderPath));
        }
        Recording recording = folder.getRecording(sessionId);
        File path = recording.getDirectory();

        videoFile = new File(path, videoStream);
        for (int i = 0; i < audioStreams.length; i++) {
            audioStreams[i] = new File(path, audioStreams[i]).getAbsolutePath();
        }
        for (int i = 0; i < syncStreams.length; i++) {
            syncStreams[i] = new File(path, syncStreams[i]).getAbsolutePath();
        }

        Dimension size = null;
        String width = request.getParameter("width");
        String height = request.getParameter("height");
        if ((width != null) && (height != null)) {
            size = new Dimension(Integer.parseInt(width),
                    Integer.parseInt(height));
        }

        double generationSpeed = DEFAULT_GENERATION_SPEED;
        String genSpeed = request.getParameter("genspeed");
        if (genSpeed != null) {
            generationSpeed = Double.parseDouble(genSpeed);
        }

        try {
            VideoExtractor extractor = new VideoExtractor(
                    videoFile.getAbsolutePath(),
                    audioStreams, syncStreams, size, rtpTypeRepository);
            extractor.setGenerationSpeed(generationSpeed);
            response.setContentType("video/x-flv");
            response.setStatus(HttpServletResponse.SC_OK);

            // Search for a file with name <videoStream>_<time>.yuv.zip
            // where <time> is <= offset
            File frameFile = null;
            File[] files = path.listFiles(
                    new StreamFileFilter(videoStream));
            if (files==null) {
                throw new IOException("Recording does not exist in Directory: "+ path.getAbsolutePath());
            }
            if (files.length > 0) {
                long[] times = new long[files.length];
                for (int i = 0; i < times.length; i++) {
                    String fName = files[i].getName();
                    String fTime = fName.substring(videoStream.length() + 1,
                            fName.length() - EXT_LEN);
                    times[i] = Long.parseLong(fTime);
                }
                Arrays.sort(times);
                int pos = Arrays.binarySearch(times, offset);
                if (pos < 0) {
                    pos = -pos - 1;
                    if (pos >= times.length) {
                        pos = times.length - 1;
                    }
                }
                frameFile = new File(path, videoStream + "_"
                        + times[pos] + EXT);
            }
            // Generate the stream
            extractor.transferToStream(response.getOutputStream(),
                    offsetShift, offset,
                    duration, frameFile);
            response.flushBuffer();
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

    private class StreamFileFilter implements FileFilter {

        private String streamName = null;

        private StreamFileFilter(String streamName) {
            this.streamName = streamName + "_";
        }

        /**
         *
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(File pathname) {
            if (pathname.getName().startsWith(streamName)
                    && pathname.getName().endsWith(EXT)) {
                return true;
            }
            return false;
        }

    }
}