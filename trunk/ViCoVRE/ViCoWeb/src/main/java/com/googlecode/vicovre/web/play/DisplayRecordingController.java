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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;

public class DisplayRecordingController implements Controller {

    private RecordingDatabase database = null;

    private LayoutRepository layoutRepository = null;

    public DisplayRecordingController(RecordingDatabase database,
            LayoutRepository layoutRepository) {
        this.database = database;
        this.layoutRepository = layoutRepository;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        File path = new File(database.getTopLevelFolder().getFile(),
                request.getRequestURI());
        path = path.getParentFile();

        Folder folder = database.getFolder(path.getParentFile());
        Recording recording = null;
        int width = 0;
        int height = 0;
        if (folder != null) {
            recording = folder.getRecording(path.getName());
            List<ReplayLayout> replayLayouts = recording.getReplayLayouts();
            if ((replayLayouts != null) && (replayLayouts.size() > 0)) {
                int minX = Integer.MAX_VALUE;
                int maxX = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = 0;
                for (ReplayLayout replayLayout : replayLayouts) {
                    Layout layout = layoutRepository.findLayout(
                            replayLayout.getName());
                    for (LayoutPosition position :
                            layout.getStreamPositions()) {
                        if ((position.getX() + position.getWidth()) > maxX) {
                            maxX = position.getX() + position.getWidth();
                        }
                        if ((position.getY() + position.getHeight()) > maxY) {
                            maxY = position.getY() + position.getHeight();
                        }
                        if (position.getX() < minX) {
                            minX = position.getX();
                        }
                        if (position.getY() < minY) {
                            minY = position.getY();
                        }
                    }
                }
                width = maxX;
                height = maxY;
            }
        }

        String folderPath = folder.getFile().getAbsolutePath().substring(
            database.getTopLevelFolder().getFile().getAbsolutePath().length()).
                replace(File.separator, "/");

        ModelAndView modelAndView = new ModelAndView("displayRecording");
        modelAndView.addObject("recording", recording);
        modelAndView.addObject("width", width);
        modelAndView.addObject("height", height);
        modelAndView.addObject("folder", folderPath);
        return modelAndView;
    }

}
