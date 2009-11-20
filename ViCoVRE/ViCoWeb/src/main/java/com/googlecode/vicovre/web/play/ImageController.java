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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;

/**
 * A controller for getting images
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ImageController implements Controller {

    private RecordingDatabase database = null;

    public ImageController(RecordingDatabase database) {
        this.database = database;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String sessionId = request.getParameter("id");
        String streamId = request.getParameter("ssrc");
        String folderPath = request.getParameter("folder");
        Folder folder = database.getTopLevelFolder();
        if (folderPath != null && !folderPath.equals("")) {
            folder = database.getFolder(new File(folderPath));
        }
        Recording recording = folder.getRecording(sessionId);

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
        String off = request.getParameter("offset");
        if (off == null) {
            off = "0";
        }
        long offset = Long.parseLong(off);

        File file = new File(recording.getDirectory(),
                streamId + "_" + offset + ".jpg");
        BufferedImage image = ImageIO.read(file);
        OutputStream output = response.getOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(output);
        ImageWriter writer = null;
        JPEGImageWriteParam param = new JPEGImageWriteParam(Locale
                .getDefault());
        Iterator<ImageWriter> iter = null;
        param.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(compression);
        response.setHeader("Cache-Control", "max-age=86400");
        response.setContentType("image/jpeg");
        response.setHeader("Content-Disposition", "inline; filename="
                + streamId + "_" + offset + ".jpg" + ";");
        response.flushBuffer();
        if ((height != -1) || (width != -1)) {
            double scaleX = 0;
            double scaleY = 0;
            AffineTransform xform = null;
            BufferedImage oldImage = null;
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
            xform = AffineTransform.getScaleInstance(scaleX, scaleY);
            oldImage = image;
            image = new BufferedImage(width, height, image.getType());
            g = image.createGraphics();
            g.drawRenderedImage(oldImage, xform);
            g.dispose();
        }
        iter = ImageIO.getImageWritersByFormatName("jpg");
        if (iter.hasNext()) {
            writer = iter.next();
        }
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        ios.flush();
        writer.dispose();
        ios.close();

        return null;
    }
}
