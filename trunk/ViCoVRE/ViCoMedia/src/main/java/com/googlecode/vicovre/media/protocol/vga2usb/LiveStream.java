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

package com.googlecode.vicovre.media.protocol.vga2usb;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.control.FormatControl;
import javax.media.format.RGBFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;

import com.epiphan.vga2usb.Grabber;
import com.epiphan.vga2usb.PixelFormat;
import com.epiphan.vga2usb.RawFrame;
import com.epiphan.vga2usb.VideoMode;
import com.googlecode.vicovre.media.controls.BufferReadAheadControl;

public class LiveStream implements Runnable, PushBufferStream,
        FormatControl, BufferReadAheadControl {

    private static final int WAIT_GRABBER_TIME = 1000;

    private static final int BLANK_WIDTH = 1024;

    private static final int BLANK_HEIGHT = 768;

    private static final int NO_FRAME_MAX = 10;

    private static final PixelFormat DEFAULT_FORMAT = PixelFormat.RGB24;

    private static final HashMap<PixelFormat, Format> SUPPORTED_FORMATS =
        new HashMap<PixelFormat, Format>();
    static {
        SUPPORTED_FORMATS.put(PixelFormat.RGB24,
                translateFormat(-1, -1, PixelFormat.RGB24));
        SUPPORTED_FORMATS.put(PixelFormat.BGR24,
                translateFormat(-1, -1, PixelFormat.BGR24));
    }

    private Grabber grabber = null;

    private RawFrame lastFrame = null;

    private RawFrame frame = null;

    private int noFrameCount = 0;

    private BufferTransferHandler handler = null;

    private Integer handlerSync = new Integer(0);

    private PixelFormat selectedPixelFormat = DEFAULT_FORMAT;

    private int width = BLANK_WIDTH;

    private int height = BLANK_HEIGHT;

    private EmptyFrame emptyFrame = new EmptyFrame(width, height,
            selectedPixelFormat);

    private boolean stopped = true;

    private long sequence = 0;

    private int readAhead = 10;

    public static Format translateFormat(int width, int height,
            PixelFormat pixelFormat) {
        Dimension size = null;
        if ((width != -1) || (height != -1)) {
            size = new Dimension(width, height);
        }
        if (pixelFormat == PixelFormat.BGR24) {
            return new RGBFormat(size, width * height * 3, Format.byteArray, -1,
                    24, 3, 2, 1, 3, width * 3, Format.FALSE,
                    RGBFormat.LITTLE_ENDIAN);
        } else if (pixelFormat == PixelFormat.RGB24) {
            return new RGBFormat(size, width * height * 3, Format.byteArray, -1,
                    24, 1, 2, 3, 3, width * 3, Format.FALSE,
                    RGBFormat.LITTLE_ENDIAN);
        } else if (pixelFormat == PixelFormat.UYVY) {
            return new YUVFormat(size, width * height * 2, Format.byteArray, -1,
                    YUVFormat.YUV_YUYV, width, width / 2, 1, 0, 2);
        } else if (pixelFormat == PixelFormat.YUYV) {
            return new YUVFormat(size, width * height * 2, Format.byteArray, -1,
                    YUVFormat.YUV_YUYV, width, width / 2, 0, 1, 3);
        }
        return null;
    }

    public Format getFormat() {
        return translateFormat(width, height, selectedPixelFormat);
    }

    public void start() {
        if (stopped) {
            Thread t = new Thread(this);
            t.start();
        }
    }

    public void run() {
        if (stopped) {
            stopped = false;
            while (!stopped) {
                synchronized (handlerSync) {
                    while ((handler == null) && !stopped) {
                        try {
                            handlerSync.wait();
                        } catch (InterruptedException e) {
                            // Do Nothing
                        }
                    }
                    if (stopped) {
                        break;
                    }
                    frame = null;
                    if (grabber == null) {
                        try {
                            grabber = new Grabber();
                        } catch (IOException e) {
                            grabber = null;
                        }
                    }

                    if (grabber != null) {
                        try {
                            VideoMode videoMode = grabber.detectVideoMode();
                            if (videoMode != null) {
                                width = videoMode.getWidth();
                                height = videoMode.getHeight();
                                frame = grabber.grabRawFrame(
                                        selectedPixelFormat,
                                        false);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            grabber = null;
                        }
                    }

                    if (frame == null) {
                        width = BLANK_WIDTH;
                        height = BLANK_HEIGHT;
                    }
                    handler.transferData(this);
                    if (frame == null) {
                        if ((lastFrame == null)
                                || noFrameCount >= NO_FRAME_MAX) {
                            try {
                                handlerSync.wait(WAIT_GRABBER_TIME);
                            } catch (InterruptedException e) {
                                // Do Nothing
                            }
                        }
                    }
                }
            }
        }
    }

    public void read(Buffer buffer) throws IOException {
        synchronized (handlerSync) {
            byte[] data = null;
            if (frame == null) {
                if ((lastFrame != null) && (noFrameCount < NO_FRAME_MAX)) {
                    data = lastFrame.getPixelBuffer();
                    noFrameCount += 1;
                } else {
                    data = emptyFrame.getData();
                    lastFrame = null;
                }
            } else {
                data = frame.getPixelBuffer();
                lastFrame = frame;
                noFrameCount = 0;
            }
            buffer.setData(data);
            buffer.setOffset(0);
            buffer.setLength(data.length);
            buffer.setFormat(translateFormat(width, height,
                    selectedPixelFormat));
            buffer.setTimeStamp(System.currentTimeMillis() * 1000000);
            buffer.setSequenceNumber(sequence++);
            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            buffer.setDiscard(false);
        }
    }

    public void setTransferHandler(BufferTransferHandler handler) {
        synchronized (handlerSync) {
            this.handler = handler;
            handlerSync.notifyAll();
        }
    }

    public void close() {
        synchronized (handlerSync) {
            stopped = true;
            handlerSync.notifyAll();
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
        if (className.equals(FormatControl.class.getName())) {
            return this;
        } else if (className.equals(BufferReadAheadControl.class.getName())) {
            return this;
        }
        return null;
    }

    public Object[] getControls() {
        return new Object[]{this};
    }

    public Format[] getSupportedFormats() {
        return SUPPORTED_FORMATS.values().toArray(new Format[0]);
    }

    public boolean isEnabled() {
        return true;
    }

    public void setEnabled(boolean enabled) {
        // Does Nothing
    }

    public Format setFormat(Format format) {
        for (PixelFormat pixelFormat : SUPPORTED_FORMATS.keySet()) {
            Format testFormat = SUPPORTED_FORMATS.get(pixelFormat);
            if (testFormat.matches(format)) {
                selectedPixelFormat = pixelFormat;
                return testFormat;
            }
        }
        return null;
    }

    public Component getControlComponent() {
        return null;
    }

    public int getMaxBufferReadAhead() {
        return readAhead;
    }

    public void setMaxBufferReadAhead(int readAhead) {
        this.readAhead = readAhead;
    }

}
