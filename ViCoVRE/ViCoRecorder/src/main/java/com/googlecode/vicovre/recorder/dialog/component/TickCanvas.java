/**
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

package com.googlecode.vicovre.recorder.dialog.component;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A canvas for drawing ticks and crosses
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class TickCanvas extends Canvas {

    private static final long serialVersionUID = 1L;

    private boolean isTick = false;

    /**
     * Sets if the canvas should draw a tick or a cross
     * @param isTick True if it is a tick, false if it is a cross
     */
    public void setTick(boolean isTick) {
        this.isTick = isTick;
        repaint();
    }

    /**
     *
     * @see java.awt.Canvas#paint(java.awt.Graphics)
     */
    public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        int w = getWidth();
        int h = getHeight();
        g.clearRect(0, 0, w, h);
        g.setStroke(new BasicStroke(3.0f));
        if (isTick) {
            g.setColor(Color.GREEN);
            g.drawLine(0, 2 * h / 3, w / 3, h);
            g.drawLine(w / 3, h, w, 0);
        }
    }

    /**
     * Return if the canvas is ticked
     * @return True if ticked, false if cross
     */
    public boolean isTicked() {
        return isTick;
    }
}