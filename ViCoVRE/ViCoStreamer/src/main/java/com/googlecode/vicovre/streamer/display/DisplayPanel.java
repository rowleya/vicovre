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

import java.util.HashMap;
import java.util.Timer;

import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.rtcp.SourceDescription;
import javax.swing.JPanel;

import com.googlecode.vicovre.media.rtp.StreamListener;

public class DisplayPanel extends JPanel implements StreamListener {

    private HashMap<Long, AudioPanel> audioPanels =
        new HashMap<Long, AudioPanel>();

    private HashMap<Long, VideoPanel> videoPanels =
        new HashMap<Long, VideoPanel>();

    private HashMap<Long, Timer> timers = new HashMap<Long, Timer>();

    public DisplayPanel() {
        setLayout(new AVLayout());
    }

    public void addAudioStream(long ssrc, DataSource dataSource,
            AudioFormat format) {
        System.err.println("Adding audio stream from " + ssrc);
        AudioPanel panel = new AudioPanel(dataSource, false);
        synchronized (audioPanels) {
            audioPanels.put(ssrc, panel);
            add(panel);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new UpdateTask(panel), 0, 1000);
            timers.put(ssrc, timer);
        }
        validate();
    }

    public void addVideoStream(long ssrc, DataSource dataSource,
            VideoFormat format) {
        System.err.println("Adding video stream from " + ssrc);
        VideoPanel panel = new VideoPanel(dataSource, format.getEncoding(),
                false);
        synchronized (videoPanels) {
            videoPanels.put(ssrc, panel);
            add(panel);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new UpdateTask(panel), 0, 1000);
            timers.put(ssrc, timer);
        }
        validate();
    }

    public void removeAudioStream(long ssrc) {
        synchronized (audioPanels) {
            AudioPanel panel = audioPanels.get(ssrc);
            if (panel != null) {
                panel.end();
                remove(panel);
                audioPanels.remove(ssrc);
                timers.get(ssrc).cancel();
            }
        }
        validate();
    }

    public void removeVideoStream(long ssrc) {
        synchronized (videoPanels) {
            VideoPanel panel = videoPanels.get(ssrc);
            if (panel != null) {
                panel.end();
                remove(panel);
                videoPanels.remove(panel);
                timers.get(ssrc).cancel();
            }
        }
        validate();
    }

    public void setAudioStreamSDES(long ssrc, int item, String value) {
        synchronized (audioPanels) {
            AudioPanel panel = audioPanels.get(ssrc);
            if (panel != null) {
                if (item == SourceDescription.SOURCE_DESC_CNAME) {
                    panel.setCNAME(value);
                } else if (item == SourceDescription.SOURCE_DESC_NAME) {
                    panel.setName(value);
                }
            }
        }
    }

    public void setVideoStreamSDES(long ssrc, int item, String value) {
        synchronized (videoPanels) {
            VideoPanel panel = videoPanels.get(ssrc);
            if (panel != null) {
                if (item == SourceDescription.SOURCE_DESC_CNAME) {
                    panel.setCNAME(value);
                } else if (item == SourceDescription.SOURCE_DESC_NAME) {
                    panel.setName(value);
                }
            }
        }
    }
}
