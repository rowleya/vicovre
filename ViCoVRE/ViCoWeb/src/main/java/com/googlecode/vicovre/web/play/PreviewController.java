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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.googlecode.vicovre.media.preview.PreviewGenerator;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

public class PreviewController implements Controller {

    private RecordingDatabase database = null;

    private RtpTypeRepository typeRepository = null;

    public PreviewController(RecordingDatabase database,
            RtpTypeRepository typeRepository) {
        this.database = database;
        this.typeRepository = typeRepository;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String folder = request.getRequestURI().substring(
                request.getContextPath().length());
        File path = new File(folder);
        String id = path.getParentFile().getName();
        folder = path.getParentFile().getParent();

        Recording recording = database.getRecording(folder, id);
        String streamId = request.getParameter("ssrc");
        Stream stream = recording.getStream(streamId);
        long duration = (stream.getEndTime().getTime()
            - stream.getStartTime().getTime()) * 1000000;

        String comp = request.getParameter("compression");
        if (comp == null) {
            comp = "0.5";
        }
        float compression = Float.valueOf(comp);
        String h = request.getParameter("height");
        String w = request.getParameter("width");
        if (h == null) {
            h = "-1";
        }
        if (w == null) {
            w = "-1";
        }
        int width = Integer.parseInt(w);
        int height = Integer.parseInt(h);

        File[] files = recording.getDirectory().listFiles(
                new PreviewFilter(streamId));

        if (files.length == 0) {
            String streamFile = new File(recording.getDirectory(),
                    streamId).getAbsolutePath();
            PreviewGenerator.generate(duration, streamFile, typeRepository,
                    recording.getDirectory(), streamId + PreviewFilter.PREFIX,
                    20);
            files = recording.getDirectory().listFiles(
                    new PreviewFilter(streamId));
        }

        Arrays.sort(files);

        BufferedImage finalImage = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            BufferedImage image = ImageIO.read(file);

            double scaleX = 0;
            double scaleY = 0;
            Graphics2D g = null;
            scaleX = width / (double) image.getWidth();
            scaleY = height / (double) image.getHeight();
            if (height == -1) {
                scaleY = scaleX;
                height = (int) (image.getHeight() * scaleY);
            } else if (width == -1) {
                scaleX = scaleY;
                width = (int) (image.getWidth() * scaleX);
            }

            if (finalImage == null) {
                finalImage = new BufferedImage(width, height * files.length,
                        BufferedImage.TYPE_INT_RGB);
            }

            g = finalImage.createGraphics();
            g.drawImage(image, 0, height * i, width, height, null);
            g.dispose();
        }

        OutputStream output = response.getOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(output);
        ImageWriter writer = null;
        ImageWriteParam param = new ImageWriteParam(Locale.getDefault());
        Iterator<ImageWriter> iter = null;
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(compression);
        response.setHeader("Cache-Control", "max-age=86400");
        response.setContentType("image/png");
        response.setHeader("Content-Disposition", "inline; filename="
                + streamId + "_preview.png" + ";");
        response.flushBuffer();
        iter = ImageIO.getImageWritersByFormatName("png");
        if (iter.hasNext()) {
            writer = iter.next();
        }
        writer.setOutput(ios);
        writer.write(null, new IIOImage(finalImage, null, null), param);
        ios.flush();
        writer.dispose();
        ios.close();

        return null;
    }

}
