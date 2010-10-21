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

package com.googlecode.vicovre.streamer.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.googlecode.vicovre.media.screencapture.CaptureChangeListener;
import com.googlecode.vicovre.media.screencapture.ChangeDetection;

public class StreamUpdateListener implements CaptureChangeListener {

    private static final int WAIT_TIME = 10000;

    private HashMap<InetAddress, Boolean> imageUpdated =
        new HashMap<InetAddress, Boolean>();

    private byte[] imageData = new byte[0];

    private byte[] offlineData = null;

    private String name = null;

    private String id = null;

    private ChangeDetection changeDetection = new ChangeDetection();

    public StreamUpdateListener(String id, byte[] offlineData) {
        this.id = id;
        this.offlineData = offlineData;
        changeDetection.addScreenListener(this);
        changeDetection.setFirstSceneChangeThreshold(0.0);
        changeDetection.setImmediatlyNotifyChange(true);
    }

    private void indicateChange() {
        synchronized (imageUpdated) {
            for (InetAddress user : imageUpdated.keySet()) {
                imageUpdated.put(user, new Boolean(true));
            }
            imageUpdated.notifyAll();
        }
    }

    /**
     * Gets the current image to serve
     * @return The image to serve
     */
    protected byte[] getImageData() {
        if (imageData.length == 0) {
            return offlineData;
        }
        return imageData;
    }

    /**
     * Waits for the image to update
     * @return True if the image has updated, false otherwise
     */
    protected boolean waitForImageUpdate(InetAddress client) {
        boolean updated = false;
        synchronized (imageUpdated) {
            if (!imageUpdated.get(client)) {
                try {
                    imageUpdated.wait(WAIT_TIME);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            updated = imageUpdated.get(client);
            if (updated) {
                imageUpdated.put(client, new Boolean(false));
            }
        }
        return updated;
    }

    public void streamStopped() {
        synchronized (imageData) {
            imageData = new byte[0];
            indicateChange();
        }
    }

    public void addClient(InetAddress client) {
        if (!imageUpdated.containsKey(client)) {
            imageUpdated.put(client, new Boolean(true));
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void captureDone(long sequence) {
        BufferedImage image = changeDetection.getImage();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write the image data to the array
        synchronized (this) {
            imageData = output.toByteArray();
        }
        indicateChange();
    }

    public ChangeDetection getChangeDetection() {
        return changeDetection;
    }
}
