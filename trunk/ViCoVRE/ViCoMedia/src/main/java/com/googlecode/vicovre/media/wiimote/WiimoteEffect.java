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

package com.googlecode.vicovre.media.wiimote;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Effect;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.media.controls.WiimotePointerControl;

public class WiimoteEffect implements Effect, PointsListener,
        WiimotePointerControl {

    private static final int RGB_32_COLOUR = 0xFF0000;

    private static final byte RGB_24_RED = (byte) (255 & 0xFF);

    private static final byte RGB_24_GREEN = 0;

    private static final byte RGB_24_BLUE = 0;

    private static final byte Y = 82;

    private static final byte U = 90;

    private static final byte V = (byte) (240 & 0xFF);

    private static final Format[] INPUT_FORMATS = new Format[]{
        new YUVFormat(YUVFormat.YUV_420),
        new RGBFormat(null, -1, Format.intArray, -1, 32, 0xFF0000, 0xFF00, 0xFF,
                1, -1, -1, -1),
        new RGBFormat(null, -1, Format.byteArray, -1, 24, -1, -1, -1,
                -1, -1, -1, -1)
    };

    private Dimension size = null;

    private boolean enabled = false;

    private Integer pointSync = new Integer(0);

    private Vector<Point> points = new Vector<Point>();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Format[] getSupportedInputFormats() {
        return INPUT_FORMATS;
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return INPUT_FORMATS;
        }
        for (int i = 0; i < INPUT_FORMATS.length; i++) {
            if (INPUT_FORMATS[i].matches(input)) {
                return new Format[]{input};
            }
        }
        return new Format[0];
    }

    public Dimension getSize() {
        return size;
    }

    private void drawRGBPoints(RGBFormat rgb, int offset, Object data,
            Vector<Point> points) {
        for (Point p : points) {
            int pos = (p.y * rgb.getLineStride())
                + (p.x * rgb.getPixelStride()) + offset;
            if (rgb.getBitsPerPixel() == 32) {
                int[] intData = (int[]) data;
                if (pos < intData.length) {
                    intData[pos] = RGB_32_COLOUR;
                }
            } else if (rgb.getBitsPerPixel() == 24) {
                byte[] byteData = (byte[]) data;
                if (pos < byteData.length) {
                    byteData[pos + (rgb.getRedMask() - 1)] = RGB_24_RED;
                    byteData[pos + (rgb.getGreenMask() - 1)] = RGB_24_GREEN;
                    byteData[pos + (rgb.getBlueMask() - 1)] = RGB_24_BLUE;
                }
            }
        }
    }

    private void drawYUVPoints(YUVFormat yuv, int offset, Object data,
            Vector<Point> points) {
        byte[] byteData = (byte[]) data;
        for (Point p : points) {
            if (yuv.getYuvType() == YUVFormat.YUV_420) {
                int cDivider = yuv.getStrideY() / yuv.getStrideUV();
                int cY = p.y / cDivider;
                int cX = p.x / cDivider;

                int yPos = offset + yuv.getOffsetY() + (p.y * yuv.getStrideY())
                    + p.x;
                int uPos = offset + yuv.getOffsetU() + (cY * yuv.getStrideUV())
                    + cX;
                int vPos = offset + yuv.getOffsetV() + (cY * yuv.getStrideUV())
                    + cX;

                if ((yPos < byteData.length) && (uPos < byteData.length)
                        && (vPos < byteData.length)) {
                    byteData[yPos] = Y;
                    byteData[uPos] = U;
                    byteData[vPos] = V;
                }
            }
        }
    }

    public int process(Buffer input, Buffer output) {
        output.copy(input);
        VideoFormat vf = (VideoFormat) input.getFormat();
        size = vf.getSize();
        if (enabled) {
            synchronized (pointSync) {
                if (vf instanceof RGBFormat) {
                    drawRGBPoints((RGBFormat) vf, output.getOffset(),
                            output.getData(), points);
                } else if (vf instanceof YUVFormat) {
                    drawYUVPoints((YUVFormat) vf, output.getOffset(),
                            output.getData(), points);
                }
            }
        }
        return BUFFER_PROCESSED_OK;
    }

    public Format setInputFormat(Format input) {
        for (int i = 0; i < INPUT_FORMATS.length; i++) {
            if (INPUT_FORMATS[i].matches(input)) {
                return input;
            }
        }
        return null;
    }

    public Format setOutputFormat(Format output) {
        for (int i = 0; i < INPUT_FORMATS.length; i++) {
            if (INPUT_FORMATS[i].matches(output)) {
                return output;
            }
        }
        return null;
    }

    public void close() {
        // Does Nothing
    }

    public String getName() {
        return "WiimoteEffect";
    }

    public void open() throws ResourceUnavailableException {
        // Does Nothing
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    private void addLine(Point p1, Point p2) {
        if (p1.equals(p2)) {
            points.add(p1);
            return;
        }

        double deltaX = Math.abs(p1.x - p2.x);
        double deltaY = Math.abs(p1.y - p2.y);

        int start = p1.x;
        int end = p2.x;
        double deltaErr = 0;
        int other = 0;
        int otherEnd = 0;
        if (deltaX > deltaY) {
            if (p1.x > p2.x) {
                start = p2.x;
                end = p1.x;
                other = p2.y;
                otherEnd = p1.y;
            } else {
                start = p1.x;
                end = p2.x;
                other = p1.y;
                otherEnd = p2.y;
            }
            deltaErr = deltaY / deltaX;
        } else {
            if (p1.y > p2.y) {
                start = p2.y;
                end = p1.y;
                other = p2.x;
                otherEnd = p1.x;
            } else {
                start = p1.y;
                end = p2.y;
                other = p1.x;
                otherEnd = p2.x;
            }
            deltaErr = deltaX / deltaY;
        }

        double error = 0;
        for (int i = start; i <= end; i++) {
            if (deltaX > deltaY) {
                points.add(new Point(i, other));
            } else {
                points.add(new Point(other, i));
            }
            error += deltaErr;
            if (error >= 0.5) {
                if (other > otherEnd) {
                    other -= 1;
                } else {
                    other += 1;
                }
                error -= 1.0;
            }
        }
    }

    public void updatePoints(List<Point> points, Point currentPoint) {
        synchronized (pointSync) {
            this.points.clear();
            if (points.size() > 0) {
                Point last = points.get(0);
                for (Point point : points) {
                    addLine(last, point);
                    last = point;
                }
            }
        }
    }

    public void disableWiimote(WiimoteControl wiimoteControl) {
        setEnabled(false);
        wiimoteControl.removePointsListener(this);
    }

    public void enableWiimote(WiimoteControl wiimoteControl) {
        wiimoteControl.setSize(size.width, size.height);
        wiimoteControl.addPointsListener(this);
        setEnabled(true);
    }

    public Component getControlComponent() {
        return null;
    }

}
