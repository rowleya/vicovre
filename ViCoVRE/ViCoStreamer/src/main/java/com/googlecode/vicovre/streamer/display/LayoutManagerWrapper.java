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

import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Window;

/**
 * Wraps Java Layout Managers so they can be used as desktop layout managers
 *
 * @author Andrew G D Rowley
 */
public class LayoutManagerWrapper implements DesktopLayoutManager {

    // The layout manager
    private LayoutManager layout = null;

    /**
     * Creates a new LayoutManagerWrapper
     *
     * @param layout
     *            The Java LayoutManager to use
     */
    public LayoutManagerWrapper(LayoutManager layout) {
        this.layout = layout;
    }

    /**
     * @see rtpReceiver.DesktopLayoutManager#addLayoutWindow(java.awt.Window)
     */
    public void addLayoutWindow(Window win) {
        // Does Nothing
    }

    /**
     * @see rtpReceiver.DesktopLayoutManager#layoutDesktop
     *      (rtpReceiver.DesktopContainer)
     */
    public void layoutDesktop(DesktopContainer desktop) {
        Container subContainer = new Container();
        subContainer.setBounds(desktop.getBounds());
        subContainer.setLayout(layout);
        Window[] windows = desktop.getWindows();
        Component[] mappedWindows = new Component[windows.length];

        for (int i = 0; i < windows.length; i++) {
            if (windows[i].isVisible()) {
                mappedWindows[i] = new Container();
                mappedWindows[i].setSize(windows[i].getSize());
                mappedWindows[i].setMinimumSize(windows[i].getSize());
                mappedWindows[i].setMaximumSize(windows[i].getSize());
                mappedWindows[i].setPreferredSize(windows[i].getSize());
                subContainer.add(mappedWindows[i]);
            }
        }
        subContainer.doLayout();
        for (int i = 0; i < windows.length; i++) {
            if (windows[i].isVisible()) {
                windows[i].setBounds(mappedWindows[i].getBounds());
            }
        }
    }

    /**
     * @see rtpReceiver.DesktopLayoutManager#removeLayoutWindow
     *      (java.awt.Window)
     */
    public void removeLayoutWindow(Window win) {
        // Does Nothing
    }

}