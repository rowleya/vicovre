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
import java.awt.Rectangle;
import java.io.IOException;

import javax.media.Control;
import javax.media.Effect;
import javax.media.Format;
import javax.media.NoPlayerException;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.renderer.VideoRenderer;
import javax.swing.JLabel;

import com.googlecode.vicovre.media.renderer.RGBRenderer;

/**
 * Displays a video stream in a separate window
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha2
 */
class VideoPlayer {

    // The renderer
    private RGBRenderer renderer;

    // The audio player (if needed)
    private AudioPlayer player;

    // The preview width
    private int previewwidth = 0;

    // The preview height
    private int previewheight = 0;

    /**
     * Creates a new VideoPlayer
     *
     * @param ds
     *            The datasource to play
     * @param previewwidth
     *            The width of the preview
     * @param previewheight
     *            The height of the preview
     * @throws IOException
     * @throws NoPlayerException
     */
    public VideoPlayer(DataSource ds, int previewwidth, int previewheight,
            Effect[] effects)
            throws NoPlayerException {
        this.previewwidth = previewwidth;
        this.previewheight = previewheight;

        int videoTrack = -1;
        int audioTrack = -1;
        Format videoFormat = null;
        Format audioFormat = null;

        if (ds instanceof PushBufferDataSource) {

            PushBufferStream[] datastreams =
                ((PushBufferDataSource) ds).getStreams();

            for (int i = 0; i < datastreams.length; i++) {
                Format format = datastreams[i].getFormat();
                if (format instanceof VideoFormat) {
                    if (videoTrack == -1) {
                        videoTrack = i;
                        videoFormat = format;
                    }
                } else if (datastreams[i].getFormat() instanceof AudioFormat) {
                    if (audioTrack == -1) {
                        audioTrack = i;
                        audioFormat = format;
                    }
                }
            }
        } else if (ds instanceof PullBufferDataSource) {
            PullBufferStream[] datastreams =
                ((PullBufferDataSource) ds).getStreams();
            for (int i = 0; i < datastreams.length; i++) {
                Format format = datastreams[i].getFormat();
                if (format instanceof VideoFormat) {
                    if (videoTrack == -1) {
                        videoTrack = i;
                        videoFormat = format;
                    }
                } else if (datastreams[i].getFormat() instanceof AudioFormat) {
                    if (audioTrack == -1) {
                        audioTrack = i;
                        audioFormat = format;
                    }
                }
            }
        }

        if (videoTrack != -1) {
            renderer = new RGBRenderer(effects);
            renderer.setDataSource(ds, videoTrack);
            if (renderer.setInputFormat(videoFormat) == null) {
                throw new NoPlayerException("Unsupported format "
                        + videoFormat);
            }
            renderer.getComponent().setVisible(false);
        }
        if (audioTrack != -1) {
        }
    }

    /**
     * Gets the preview component of the player
     * @return The preview graphical component
     */
    public Component getPreviewComponent() {
        Component comp = renderer.getPreviewComponent();
        return comp;
    }

    /**
     * Returns a component where the video is displayed
     * @return the graphical component for the video
     */
    public Component getVisualComponent() {
        return renderer.getComponent();
    }

    /**
     * Returns a control for this player
     *
     * @param classname
     *            The name of the class of the control
     * @return the control
     */
    public Control getControl(String classname) {
        return (Control) renderer.getControl(classname);
    }

    /**
     * Starts the playback
     */
    public void start() {
        if (renderer != null) {
            renderer.start();
        }
        if (player != null) {
            player.start();
        }
    }

    /**
     * Stops the playback
     */
    public void stop() {
        if (renderer != null) {
            renderer.stop();
        }
        if (player != null) {
            player.stop();
        }
    }

    /**
     * Waits for the first frame to become available
     */
    public void waitForFirstFrame() {
        renderer.waitForFirstFrame();
    }

    public boolean firstFrameSeen() {
        return renderer.firstFrameSeen();
    }
}
