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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * An representative of the windows on the desktop
 *
 * @author Andrew G D Rowley
 */
public class DesktopContainer extends WindowAdapter
        implements Comparator<Rectangle> {

    // The only instance of this container allowed
    private static DesktopContainer instance = null;

    // The layout manager
    private DesktopLayoutManager layout = null;

    // A list of windows that have been added
    private Vector<Window> windows = new Vector<Window>();

    // The bounds of the desktop
    private Rectangle bounds = new Rectangle(0, 0, 0, 0);

    // The bounds of the individual monitors, from top-left to bottom-right
    private Rectangle monitorBounds[] = new Rectangle[0];

    // The primary window
    private Window primary = null;

    // The background window
    private Window background = null;

    // Creates a new DesktopContainer
    private DesktopContainer() {

        // Get the screens
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Vector<Rectangle> monitors = new Vector<Rectangle>();

        // Get the size of the overall desktop
        Rectangle rect = ge.getDefaultScreenDevice().getDefaultConfiguration()
                .getBounds();
        int minX = rect.x;
        int maxX = rect.x + rect.width;
        int minY = rect.y;
        int maxY = rect.y + rect.height;
        for (int i = 0; i < gs.length; i++) {
            rect = gs[i].getDefaultConfiguration().getBounds();
            if (rect.x < minX) {
                minX = rect.x;
            }
            if (rect.y < minY) {
                minY = rect.y;
            }
            if ((rect.x + rect.width) > maxX) {
                maxX = rect.x + rect.width;
            }
            if ((rect.y + rect.height) > maxY) {
                maxY = rect.y + rect.height;
            }

            monitors.add(rect);
        }

        // Set the bounds of the desktop
        bounds.setBounds(minX, minY, maxX - minX, maxY - minY);

        // Sort the bounds of the monitors
        Collections.sort(monitors, this);
        monitorBounds = monitors.toArray(new Rectangle[0]);
    }

    /**
     * Gets the current instance of the desktop container
     *
     * @return The instance
     */
    public static DesktopContainer getInstance() {
        if (instance == null) {
            instance = new DesktopContainer();
        }

        return instance;
    }

    /**
     * Returns the bounds of the desktop
     * @return the bounds of the desktop
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Adds a window to the desktop
     *
     * @param win The window to add
     */
    public void add(Window win) {
        windows.add(win);
        if (layout != null) {
            layout.addLayoutWindow(win);
        }
        win.addWindowListener(this);
        doLayout();
    }

    /**
     * Sets the layout of the desktop
     *
     * @param newLayout
     *            The layout to set
     */
    public void setLayout(LayoutManager newLayout) {
        layout = new LayoutManagerWrapper(newLayout);
        doLayout();
    }

    /**
     * Sets the layout of the desktop
     *
     * @param newLayout
     *            The layout to set
     */
    public void setLayout(DesktopLayoutManager newLayout) {
        layout = newLayout;
        doLayout();
    }

    /**
     * Removes a window from the desktop
     *
     * @param win
     *            The window to remove
     */
    public void remove(Window win) {
        win.removeWindowListener(this);
        windows.remove(win);
        if (layout != null) {
            layout.removeLayoutWindow(win);
        }
        doLayout();
    }

    /**
     * Refreshes the layout of the windows
     */
    public void doLayout() {
        if (layout != null) {
            layout.layoutDesktop(this);
        }
    }

    /**
     * Returns the layout manager
     * @return the layout manager
     */
    public DesktopLayoutManager getLayout() {
        return layout;
    }

    /**
     * Returns the windows in the desktop
     * @return An array of Windows
     */
    public Window[] getWindows() {
        return windows.toArray(new Window[0]);
    }

    /**
     * Returns the bounds of the monitors, ordered from top-left to bottom-right
     * in rows
     * @return an array of Rectangles
     */
    public Rectangle[] getMonitors() {
        return monitorBounds;
    }

    /**
     * A comparator for rectangles, such that the top-most, left-most rectangle
     * comes first
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Rectangle r1, Rectangle r2) {
        if (r1.y != r2.y) {
            return (r1.y - r2.y);
        }
        return (r1.x - r2.x);
    }

    /**
     * Sets the Primary window
     *
     * @param window
     */
    public void setPrimary(Window window) {
        if (window == background) {
            background = null;
        }
        primary = window;
        doLayout();
    }

    /**
     * Sets the Background window
     *
     * @param window
     */
    public void setBackground(Window window) {
        if (window == primary) {
            primary = null;
        }
        background = window;
        doLayout();
    }

    /**
     * Returns the current primary window
     * @return the primary Window
     */
    public Window getPrimaryWindow() {
        return primary;
    }

    /**
     * Returns the current background window
     * @return the background Window
     */
    public Window getBackgroundWindow() {
        return background;
    }

    /**
     *
     * @see java.awt.event.WindowListener#windowOpened
     *      (java.awt.event.WindowEvent)
     */
    public void windowOpened(WindowEvent e) {
        doLayout();
    }

    /**
     *
     * @see java.awt.event.WindowListener#windowClosed
     *      (java.awt.event.WindowEvent)
     */
    public void windowClosed(WindowEvent e) {
        doLayout();
    }
}