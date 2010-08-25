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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;

import wiiusej.values.IRSource;

public class WiimoteCalibrationComponent extends JComponent
        implements ActionListener {

    private static final double ASPECT_RATIO = 4.0 / 3.0;

    private Integer pointSync = new Integer(0);

    private IRSource[] points = null;

    private Image image = null;

    private Timer timer = new Timer(50, this);

    public WiimoteCalibrationComponent() {
        timer.start();
    }

    public void paint(Graphics graphics) {
        if (image == null) {
            image = createVolatileImage(1024, 768);
        }

        Graphics g = image.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.YELLOW);

        synchronized (pointSync) {
            if (points != null) {
                for (IRSource point : points) {
                    int size = point.getSize() * 20;
                    g.fillOval(point.getX(), point.getY(), size,
                            size);
                }
            }
        }

        graphics.drawImage(image, 0, 0, getWidth(), getHeight(),
            0, 0, image.getWidth(this), image.getHeight(this), this);
    }

    public void setPoints(IRSource[] points) {
        synchronized (pointSync) {
            this.points = points;
        }
    }

    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        int newWidth = (int) (height * ASPECT_RATIO);
        int newX = x + ((width - newWidth) / 2);
        super.setBounds(newX, y, newWidth, height);
    }

    public void actionPerformed(ActionEvent e) {
        if (isVisible()) {
            repaint();
        }
    }

}
