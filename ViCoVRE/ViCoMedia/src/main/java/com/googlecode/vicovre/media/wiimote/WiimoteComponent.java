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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Timer;

public class WiimoteComponent extends JComponent implements PointsListener,
        ActionListener {

    private Image image = null;

    private List<Point> points = null;

    private Integer pointSync = new Integer(0);

    private Point currentPoint = null;

    private Dimension resolution = null;

    private Timer timer = new Timer(50, this);

    public WiimoteComponent() {
        timer.start();
    }

    public void setResolution(int width, int height) {
        setResolution(new Dimension(width, height));
    }

    public void setResolution(Dimension resolution) {
        this.resolution = resolution;
    }

    public void paint(Graphics graphics) {
        if (isVisible()) {
            int width = getWidth();
            int height = getHeight();
            if (resolution != null) {
                width = resolution.width;
                height = resolution.height;
            }
            if ((image == null) || (image.getWidth(this) != width)
                    || (image.getHeight(this) != height)) {
                image = createVolatileImage(width, height);
            }
            Graphics g = image.getGraphics();
            if (g != null) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.RED);
                synchronized (pointSync) {
                    if (points != null && !points.isEmpty()) {
                        Point last = points.get(0);
                        for (Point p : points) {
                            g.drawLine(last.x, last.y, p.x, p.y);
                            last = p;
                        }
                    }
                }
                if (currentPoint != null) {
                    g.setColor(Color.BLUE);
                    g.fillOval(currentPoint.x, currentPoint.y, 5, 5);
                }
            }
            graphics.drawImage(image, 0, 0, getWidth(), getHeight(), 0, 0,
                    image.getWidth(this), image.getHeight(this), this);
        }
    }

    public void updatePoints(List<Point> points, Point currentPoint) {
        synchronized (pointSync) {
            this.points = points;
            this.currentPoint = currentPoint;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (isVisible()) {
            repaint();
        }
    }
}
