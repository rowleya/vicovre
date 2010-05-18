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

package com.googlecode.vicovre.media.protocol.image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;

public class ImageStream implements PushBufferStream {

    private static final int BITS = 32;

    private static final int RED = 0xFF0000;

    private static final int GREEN = 0xFF00;

    private static final int BLUE = 0xFF;

    private Integer imageSync = new Integer(0);

    private BufferedImage image = null;

    private RGBFormat format = new RGBFormat(null, -1, Format.intArray, -1,
            BITS, RED, GREEN, BLUE, 1, -1, VideoFormat.FALSE,
            RGBFormat.LITTLE_ENDIAN);

    private int[] data = new int[0];

    private long sequence = 0;

    private long timestamp = 0;

    private BufferTransferHandler handler = null;

    private boolean live = false;

    public ImageStream(boolean live) {
        this.live = live;
    }

    public void readImage(InputStream input, long timestamp,
            long sequence) throws IOException, UnsupportedFormatException {
        synchronized (imageSync) {

            while (!live && (image != null) && (handler != null)) {
                try {
                    imageSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
            image = ImageIO.read(input);
            if (image == null) {
                throw new UnsupportedFormatException(null);
            }
            Dimension size = new Dimension(image.getWidth(), image.getHeight());
            if ((size.width % 16) != 0) {
                size.width += 16 - (size.width % 16);
            }
            if ((size.height % 16) != 0) {
                size.height += 16 - (size.height % 16);
            }
            format = new RGBFormat(
                    size, size.width * size.height,
                    Format.intArray, -1, BITS, RED, GREEN, BLUE, 1,
                    image.getWidth(), Format.FALSE, RGBFormat.LITTLE_ENDIAN);
            if (data.length < format.getMaxDataLength()) {
                data = new int[format.getMaxDataLength()];
            }
            this.timestamp = timestamp;
            this.sequence = sequence;
            if (handler != null) {
                handler.transferData(this);
            }
            imageSync.notifyAll();
        }
    }

    public Format getFormat() {
        return format;
    }

    public void read(Buffer buffer) throws IOException {
        synchronized (imageSync) {
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), data, 0,
                    image.getWidth());
            buffer.setData(data);
            buffer.setOffset(0);
            buffer.setLength(data.length);
            buffer.setFormat(format);
            buffer.setSequenceNumber(sequence);
            buffer.setTimeStamp(timestamp * 1000);
            buffer.setFlags(Buffer.FLAG_RELATIVE_TIME);
            image = null;
            imageSync.notifyAll();
        }
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        synchronized (imageSync) {
            this.handler = transferHandler;
            if (image != null) {
                handler.transferData(this);
            }
        }
    }

    public boolean endOfStream() {
        return false;
    }

    public ContentDescriptor getContentDescriptor() {
        return new ContentDescriptor(ContentDescriptor.RAW);
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

}
