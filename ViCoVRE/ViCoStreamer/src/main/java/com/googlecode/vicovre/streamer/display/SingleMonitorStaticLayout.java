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

package com.googlecode.vicovre.streamer.display;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Arrays;
import java.util.Comparator;

/**
 * An Automatic Desktop Layout for Single Monitor systems
 *
 * @author Andrew G D Rowley
 */
public class SingleMonitorStaticLayout implements DesktopLayoutManager,
        Comparator<VideoWindow> {

    // The amount to divide the size by
    private int divisor = 1;

    /**
     * Creates a new MultiMonitorLayout
     *
     * @param divisor
     *            The amount to divide the size of each window by
     */
    public SingleMonitorStaticLayout(int divisor) {
        this.divisor = divisor;
    }

    /**
     * @see rtpReceiver.DesktopLayoutManager#addLayoutWindow(java.awt.Window)
     */
    public void addLayoutWindow(Window win) {
        // Does Nothing
    }

    /**
     * @see rtpReceiver.DesktopLayoutManager
     *      #layoutDesktop(rtpReceiver.DesktopContainer)
     */
    public void layoutDesktop(DesktopContainer desktop) {

        // Get the background window as the background
        Rectangle desktopBounds = desktop.getBounds();
        Window background = desktop.getBackgroundWindow();
        Rectangle backgroundBounds = new Rectangle(desktopBounds);

        // Layout the other windows
        VideoWindow[] windows = (VideoWindow[]) desktop.getWindows();
        Arrays.sort(windows, this);
        int x = 0;
        int y = 0;
        int lastXStart = 0;
        int lastYStart = 0;
        int maxRowHeight = 0;
        int maxColumnWidth = 0;
        boolean modifyX = true;
        for (int i = 0; i < windows.length; i++) {
            if (windows[i] != background) {
                windows[i].setVisible(true);
                Dimension size = windows[i].getPreferredSize();
                size.width /= divisor;
                size.height /= divisor;
                windows[i].setSize(size);
                Rectangle bounds = windows[i].getBounds();
                if (!modifyX && (bounds.width > maxColumnWidth)) {
                    maxColumnWidth = bounds.width;
                }
                if (modifyX && (bounds.height > maxRowHeight)) {
                    maxRowHeight = bounds.height;
                }
                if (modifyX && ((bounds.width + x) > desktopBounds.width)) {
                    modifyX = false;
                    x = lastXStart;
                    y = lastYStart + maxRowHeight;
                    lastYStart = y;
                } else if (!modifyX
                        && ((bounds.height + y) > desktopBounds.height)) {
                    modifyX = true;
                    y = lastYStart;
                    x = lastXStart + maxColumnWidth;
                    lastXStart = x;
                }
                bounds.x = x;
                bounds.y = y;
                windows[i].setBounds(bounds);
                if (modifyX) {
                    x += bounds.width;
                } else {
                    y += bounds.height;
                }
            }
        }

        // Display the background window in the lower right hand corner
        if (background != null) {
            if (modifyX) {
                x = lastXStart;
                y = lastYStart + maxRowHeight;
                lastYStart = y;
            } else {
                y = lastYStart;
                x = lastXStart + maxColumnWidth;
                lastXStart = x;
            }
            backgroundBounds.x = x;
            backgroundBounds.y = y;
            int remainingWidth = desktopBounds.width - x;
            int remainingHeight = desktopBounds.height - y;
            double widthRatio = (double) backgroundBounds.width
                    / (double) remainingWidth;
            double heightRatio = (double) backgroundBounds.height
                    / (double) remainingHeight;
            if (widthRatio > heightRatio) {
                backgroundBounds.width /= widthRatio;
                backgroundBounds.height /= widthRatio;
            } else {
                backgroundBounds.width /= heightRatio;
                backgroundBounds.height /= heightRatio;
            }
            background.setVisible(true);
            background.setBounds(backgroundBounds);
        }
    }

    /**
     * @see rtpReceiver.DesktopLayoutManager#removeLayoutWindow(java.awt.Window)
     */
    public void removeLayoutWindow(Window win) {
        // Does Nothing
    }

    /**
     * Sorts windows by CNAME if they have one
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(VideoWindow o1, VideoWindow o2) {
        return o1.getCNAME().compareTo(o2.getCNAME());
    }

}