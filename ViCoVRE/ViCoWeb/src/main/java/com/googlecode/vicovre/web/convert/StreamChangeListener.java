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

package com.googlecode.vicovre.web.convert;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.googlecode.vicovre.media.screencapture.CaptureChangeListener;

public class StreamChangeListener implements CaptureChangeListener {

    private HashMap<String, byte[]> encodedImage =
        new HashMap<String, byte[]>();

    private int streamid = -1;

    private ImportStream importStream = null;

    private LiveDataStream dataStream = null;

    public StreamChangeListener(int streamid, ImportStream importStream,
            LiveDataStream dataStream) {
        this.streamid = streamid;
        this.importStream = importStream;
        this.dataStream = dataStream;
    }

    public void captureDone(long sequence) {
        synchronized (encodedImage) {
            for (String contentType : encodedImage.keySet()) {
                encodedImage.remove(contentType);
            }
        }
        importStream.captureDone(streamid);
    }

    public byte[] getImage(String contentType) throws IOException {
        if (!contentType.startsWith("image/")) {
            return null;
        }
        String formatName = contentType.substring(6);

        synchronized (encodedImage) {
            byte[] imageData = encodedImage.get(contentType);
            if (imageData == null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                BufferedImage image = dataStream.getImage();
                ImageIO.write(image, formatName, output);
                imageData = output.toByteArray();
                encodedImage.put(contentType, imageData);
            }
            return imageData;
        }
    }

}
