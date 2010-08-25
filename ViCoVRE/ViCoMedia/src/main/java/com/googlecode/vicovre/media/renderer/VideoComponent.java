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

package com.googlecode.vicovre.media.renderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.util.List;

import javax.swing.JComponent;

import com.googlecode.vicovre.media.wiimote.PointsListener;

public class VideoComponent extends JComponent implements PointsListener {

    private Image image = null;

    private Point currentPoint = null;

    public VideoComponent() {
        setOpaque(false);
        setDoubleBuffered(false);
    }

    protected void setImage(Image image) {
        this.image = image;
    }

    public void paint(Graphics g) {
        if (isVisible()) {
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), 0, 0,
                        image.getWidth(this), image.getHeight(this), this);
                if (currentPoint != null) {
                    g.setColor(Color.BLUE);
                    double scaleX = (double) getWidth()
                        / image.getWidth(this);
                    double scaleY = (double) getHeight()
                        / image.getHeight(this);
                    int x = (int) (currentPoint.x * scaleX);
                    int y = (int) (currentPoint.y * scaleY);
                    g.fillOval(x, y, 5, 5);
                }
            }
        }
    }

    public void updatePoints(List<Point> points, Point currentPoint) {
        this.currentPoint = currentPoint;
    }
}
