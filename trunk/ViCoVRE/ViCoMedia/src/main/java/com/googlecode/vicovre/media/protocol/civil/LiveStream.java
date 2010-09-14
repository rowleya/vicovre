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

package com.googlecode.vicovre.media.protocol.civil;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.control.FormatControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;

import com.googlecode.vicovre.media.controls.BufferReadAheadControl;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;

public class LiveStream implements PushBufferStream, CaptureObserver,
        FormatControl, BufferReadAheadControl {

    private CaptureSystem captureSystem = null;

    private CaptureStream captureStream = null;

    private String deviceId = null;

    private int input = 0;

    private int output = 0;

    private BufferTransferHandler handler = null;

    private Image image = null;

    private boolean transferring = false;

    private Integer transferSync = new Integer(0);

    private Integer bufferSync = new Integer(0);

    private Format format = null;

    private com.lti.civil.VideoFormat setFormat = null;

    private long sequence = 0;

    private int readAhead = 10;

    public LiveStream() throws CaptureException {
        captureSystem =
          DefaultCaptureSystemFactorySingleton.instance().createCaptureSystem();
    }

    public void setLocator(MediaLocator mediaLocator) throws CaptureException {
        String remainder = mediaLocator.getRemainder();

        int outputIndex = remainder.indexOf("output=");
        if (outputIndex != -1) {
            int start = outputIndex + "output=".length();
            int end = remainder.indexOf('&', outputIndex);
            if (end == -1) {
                end = remainder.length();
            }
            output = Integer.parseInt(remainder.substring(start, end));
        }

        int inputIndex = remainder.indexOf("input=");
        if (inputIndex != -1) {
            int start = inputIndex + "input=".length();
            int end = remainder.indexOf('&', inputIndex);
            if (end == -1) {
                end = remainder.length();
            }
            input = Integer.parseInt(remainder.substring(start, end));
        }

        if ((outputIndex >= 1) && (remainder.charAt(outputIndex - 1) == '?')) {
            remainder = remainder.substring(0, outputIndex - 1);
        } else if ((inputIndex >= 1)
                && (remainder.charAt(inputIndex - 1) == '?')) {
            remainder = remainder.substring(0, inputIndex - 1);
        }

        deviceId = remainder;

        captureStream = captureSystem.openCaptureDeviceStreamOutput(deviceId,
                output, input);
        captureStream.setObserver(this, readAhead);
        if (setFormat != null) {
            captureStream.setVideoFormat(setFormat);
        }
        format = convertCivilFormat(captureStream.getVideoFormat());
    }

    public void start() throws CaptureException {
        captureStream.start();
    }

    public void stop() throws CaptureException {
        captureStream.stop();
    }

    public void close() throws CaptureException {
        captureStream.dispose();
    }

    public Format getFormat() {
        return format;
    }

    public void read(Buffer buffer) throws IOException {
        if (image == null) {
            buffer.setDiscard(true);
            buffer.setLength(0);
            buffer.setOffset(0);
        } else {
            Object data = image.getObject();
            buffer.setData(data);
            buffer.setOffset(image.getOffset());
            buffer.setLength(Array.getLength(data));
            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            buffer.setFormat(format);
            buffer.setTimeStamp(System.currentTimeMillis() * 1000000);
            buffer.setSequenceNumber(sequence++);
            buffer.setDiscard(false);
        }
    }

    public void setTransferHandler(BufferTransferHandler handler) {
        synchronized (transferSync) {
            System.err.println("Transfer handler set to " + handler);
            this.handler = handler;
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

    public void onError(CaptureStream stream, CaptureException exception) {
        exception.printStackTrace();
    }

    public static VideoFormat convertCivilFormat(
            com.lti.civil.VideoFormat civilVideoFormat) {

        int width = civilVideoFormat.getWidth();
        int height = civilVideoFormat.getHeight();
        float fps = civilVideoFormat.getFPS();
        int type = civilVideoFormat.getFormatType();
        int dataType = civilVideoFormat.getDataType();
        Class<?> dataTypeClass = null;
        if (dataType == com.lti.civil.VideoFormat.DATA_TYPE_BYTE_ARRAY) {
            dataTypeClass = Format.byteArray;
        } else if (dataType ==
                com.lti.civil.VideoFormat.DATA_TYPE_SHORT_ARRAY) {
            dataTypeClass = Format.shortArray;
        } else if (dataType == com.lti.civil.VideoFormat.DATA_TYPE_INT_ARRAY) {
            dataTypeClass = Format.intArray;
        }
        Dimension size = new Dimension(width, height);

        if (type == com.lti.civil.VideoFormat.RGB24) {
            return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    24, 3, 2, 1);
        } else if (type == com.lti.civil.VideoFormat.RGB32) {
            if (dataTypeClass == Format.byteArray) {
                return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass,
                        fps, 32, 3, 2, 1);
            }
            return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    32, 0x00FF0000, 0x0000FF00, 0x000000FF);
        } else if (type == com.lti.civil.VideoFormat.RGB555) {
            return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    16, 0x7C00, 0x3E0, 0x1F);
        } else if (type == com.lti.civil.VideoFormat.RGB565) {
            return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    16, 0xF800, 0x7E0, 0x1F);
        } else if (type == com.lti.civil.VideoFormat.ARGB1555) {
            return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    16, 0x7C00, 0x3E0, 0x1F);
        } else if (type == com.lti.civil.VideoFormat.ARGB32) {
            return new RGBFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    32, 0x00FF0000, 0x0000FF00, 0x000000FF);
        } else if (type == com.lti.civil.VideoFormat.UYVY) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_YUYV, width * 2, width * 2, 1, 0, 2);
        } else if (type == com.lti.civil.VideoFormat.YUYV) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_YUYV, width * 2, width * 2, 0, 1, 3);
        } else if (type == com.lti.civil.VideoFormat.YVYU) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_YUYV, width * 2, width * 2, 0, 3, 1);
        } else if (type == com.lti.civil.VideoFormat.YUY2) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_YUYV, width * 2, width * 2, 0, 1, 3);
        } else if (type == com.lti.civil.VideoFormat.YV12) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_420, width, width / 2, 0,
                    (width * height) + (width/2 * height/2), width * height);
        } else if (type == com.lti.civil.VideoFormat.I420) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_420, width, width / 2, 0, width * height,
                    (width * height) + ((width/2) * (height/2)));
        } else if (type == com.lti.civil.VideoFormat.IYUV) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_420, width, width / 2, 0, width * height,
                    (width * height) + (width/2 * height/2));
        } else if (type == com.lti.civil.VideoFormat.YVU9) {
            return new YUVFormat(size, Format.NOT_SPECIFIED, dataTypeClass, fps,
                    YUVFormat.YUV_YVU9, -1, -1, -1, -1, -1);
        } else if (type == com.lti.civil.VideoFormat.MJPG) {
            return new VideoFormat(VideoFormat.MJPG, size, Format.NOT_SPECIFIED,
                    dataTypeClass, fps);
        }
        return null;
    }

    public void onNewImage(CaptureStream stream, Image image) {
        synchronized (transferSync) {
            if (transferring || (handler == null)) {
                return;
            }
            transferring = true;
        }

        synchronized (bufferSync) {
            this.image = image;
            handler.transferData(this);
        }

        synchronized (transferSync) {
            transferring = false;
        }
    }

    public Format[] getSupportedFormats() {
        try {
            List<com.lti.civil.VideoFormat> formatList =
                captureStream.enumVideoFormats();
            Vector<Format> formats = new Vector<Format>();

            for (int i = 0; i < formatList.size(); i++) {
                Format format = convertCivilFormat(formatList.get(i));
                if (format != null) {
                    formats.add(format);
                }
            }
            return formats.toArray(new Format[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return new Format[0];
        }
    }

    public boolean isEnabled() {
        return true;
    }

    public void setEnabled(boolean enabled) {
        // Do Nothing
    }

    public Format setFormat(Format format) {
        try {
            List<com.lti.civil.VideoFormat> formatList =
                captureStream.enumVideoFormats();

            for (int i = 0; i < formatList.size(); i++) {
                Format f = convertCivilFormat(formatList.get(i));
                if ((f != null) && f.matches(format)) {
                    setFormat = formatList.get(i);
                    captureStream.setVideoFormat(setFormat);
                    this.format = convertCivilFormat(
                            captureStream.getVideoFormat());
                    return this.format;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
