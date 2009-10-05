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
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * A LayoutManager for Audio and Video Streams.
 *
 * Streams are arranged by CNAME. Note that components that are not audio, video
 * or inactive are not displayed!
 *
 * @author Andrew G D Rowley
 */
public class AVLayout implements LayoutManager {

    // The default width of the panel
    private static final int DEFAULT_MIN_WIDTH = 100;

    // The default height of the panel
    private static final int DEFAULT_MIN_HEIGHT = 100;

    // The initial x and y values
    private static final int START = 5;

    // The spacing value
    private static final int SPACING = 5;

    // The minimum height of the layout
    private int minHeight = DEFAULT_MIN_HEIGHT;

    // The minimum width of the layout
    private int minWidth = DEFAULT_MIN_WIDTH;

    /**
     * Creates a new AVLayout
     */
    public AVLayout() {
        // Does Nothing
    }

    /**
     * @see java.awt.LayoutManager#addLayoutComponent(java.lang.String,
     *      java.awt.Component)
     */
    public void addLayoutComponent(String name, Component component) {
        // Does Nothing
    }

    /**
     * @see java.awt.LayoutManager#layoutContainer(java.awt.Container)
     */
    public synchronized void layoutContainer(Container parent) {

        // Set up the positions
        int x = START;
        int y = START;
        int xmax = 0;

        // Get the components
        boolean enabled = parent.isEnabled();
        Component components[] = parent.getComponents();
        HashMap<String, Vector<VideoPanel>> videoComponents =
            new HashMap<String, Vector<VideoPanel>>();
        HashMap<String, Vector<AudioPanel>> audioComponents =
            new HashMap<String, Vector<AudioPanel>>();
        Vector<InactivePanel> inactiveComponents = new Vector<InactivePanel>();
        HashMap<String, Boolean> cnames = new HashMap<String, Boolean>();

        // Sort the components by type
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];

            // Store VideoPanels by CNAME
            if (component instanceof VideoPanel) {
                VideoPanel panel = (VideoPanel) component;
                Vector<VideoPanel> others = videoComponents.get(panel.getCNAME());
                if (others == null) {
                    cnames.put(panel.getCNAME(), true);
                    others = new Vector<VideoPanel>();
                }
                panel.setVisible(enabled);
                others.add(panel);
                videoComponents.put(panel.getCNAME(), others);
            } else if (component instanceof AudioPanel) {

                // Store AudioPanels by CNAME
                AudioPanel panel = (AudioPanel) component;
                Vector<AudioPanel> others = audioComponents.get(panel.getCNAME());
                if (others == null) {
                    cnames.put(panel.getCNAME(), true);
                    others = new Vector<AudioPanel>();
                }
                panel.setVisible(enabled);
                others.add(panel);
                audioComponents.put(panel.getCNAME(), others);
            } else if (component instanceof InactivePanel) {

                // Just list inactive Panels
                InactivePanel panel = (InactivePanel) component;
                inactiveComponents.add(panel);
                panel.setVisible(enabled);
            } else {

                // Hide other components
                component.setVisible(false);
            }
        }

        // Go through the components by CNAME
        if (enabled) {
            Iterator<String> iterator = cnames.keySet().iterator();
            while (iterator.hasNext()) {

                // Start at the left hand side
                x = START;
                String cname = iterator.next();

                // Go through the video components with this cname
                Vector<VideoPanel> videoList = videoComponents.get(cname);
                Dimension d = VideoPanel.getDefaultSize();
                if (videoList != null) {
                    for (int j = 0; j < videoList.size(); j++) {

                        // Display video panels with the same CNAME side by side
                        VideoPanel video = videoList.get(j);
                        d = video.getPreferredSize();
                        video.setLocation(x, y);
                        video.setSize(d);
                        x += d.getWidth() + SPACING;
                        if (x > xmax) {
                            xmax = x;
                        }
                    }
                    if (videoList.size() > 0) {
                        y += d.getHeight();
                    }
                }

                // Next line
                x = START;

                // Go through the audio panels with this cname
                Vector<AudioPanel> audioList = audioComponents.get(cname);
                if (audioList != null) {
                    for (int j = 0; j < audioList.size(); j++) {

                        // Display audio panels with the same CNAME side by side
                        AudioPanel audio = audioList.get(j);
                        d = audio.getPreferredSize();
                        audio.setLocation(x, y);
                        audio.setSize(d);
                        x += d.getWidth() + SPACING;
                        if (x > xmax) {
                            xmax = x;
                        }
                    }
                    if (audioList.size() > 0) {
                        y += d.getHeight() + SPACING;
                    }
                } else {

                    // If no panels exist with this cname, move up a bit
                    y += SPACING;
                }
            }

            // Display the inactive people in a list
            for (int i = 0; i < inactiveComponents.size(); i++) {
                x = START;
                InactivePanel inactive = inactiveComponents.get(i);
                Dimension d = inactive.getPreferredSize();
                inactive.setLocation(x, y);
                inactive.setSize(d);
                x += d.getWidth() + SPACING;
                if (x > xmax) {
                    xmax = x;
                }
                y += d.getHeight() + SPACING;
            }

            // Set the minimums according to where everything was displayed
            minWidth = xmax;
            minHeight = y;
        } else {
            minWidth = 0;
            minHeight = 0;
        }
    }

    /**
     * @see java.awt.LayoutManager#minimumLayoutSize(java.awt.Container)
     */
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(minWidth, minHeight);
    }

    /**
     * @see java.awt.LayoutManager#preferredLayoutSize(java.awt.Container)
     */
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(minWidth, minHeight);
    }

    /**
     * @see java.awt.LayoutManager#removeLayoutComponent(java.awt.Component)
     */
    public void removeLayoutComponent(Component component) {
        // Does Nothing
    }
}