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
import java.awt.Graphics2D;
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

    public void drawThickLine(Graphics g, int x1, int y1, int x2, int y2,
            int thickness) {

        // The thick line is in fact a filled polygon
        int dX = x2 - x1;
        int dY = y2 - y1;
        // line length
        double lineLength = Math.sqrt(dX * dX + dY * dY);

        double scale = thickness / (2 * lineLength);

        // The x,y increments from an endpoint needed to create a rectangle...
        double ddx = -scale * dY;
        double ddy = scale * dX;
        ddx += (ddx > 0) ? 0.5 : -0.5;
        ddy += (ddy > 0) ? 0.5 : -0.5;
        int dx = (int)ddx;
        int dy = (int)ddy;

        // Now we can compute the corner points...
        int xPoints[] = new int[4];
        int yPoints[] = new int[4];

        xPoints[0] = x1 + dx; yPoints[0] = y1 + dy;
        xPoints[1] = x1 - dx; yPoints[1] = y1 - dy;
        xPoints[2] = x2 - dx; yPoints[2] = y2 - dy;
        xPoints[3] = x2 + dx; yPoints[3] = y2 + dy;

        g.fillPolygon(xPoints, yPoints, 4);
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
                            drawThickLine(g, last.x, last.y, p.x, p.y, 5);
                            last = p;
                        }
                    }
                }
                if (currentPoint != null) {
                    g.setColor(Color.BLUE);
                    g.fillOval(currentPoint.x, currentPoint.y, 10, 10);
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
