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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;

import javax.media.CannotRealizeException;
import javax.media.Effect;
import javax.media.NoPlayerException;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.rtcp.SourceDescription;
import javax.sound.sampled.FloatControl;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.googlecode.vicovre.media.controls.PlayControl;
import com.googlecode.vicovre.media.rtp.StreamListener;
import com.googlecode.vicovre.media.ui.FileListener;
import com.googlecode.vicovre.media.ui.LocalStreamListener;
import com.googlecode.vicovre.streamer.web.ChangeDetectionEffect;
import com.googlecode.vicovre.streamer.web.StreamUpdateListener;
import com.googlecode.vicovre.streamer.web.VideoWebServer;

public class DisplayPanel extends JPanel implements StreamListener,
        LocalStreamListener, FileListener {

    private JSplitPane splitPane = null;

    private JPanel localStreams = new JPanel();

    private JPanel remoteStreams = new JPanel();

    private HashSet<Long> localSsrcs = new HashSet<Long>();

    private HashMap<DataSource, AudioPanel> localAudioPanels =
        new HashMap<DataSource, AudioPanel>();

    private HashMap<DataSource, VideoPanel> localVideoPanels =
        new HashMap<DataSource, VideoPanel>();

    private HashMap<DataSource, StreamUpdateListener> localStreamListeners =
        new HashMap<DataSource, StreamUpdateListener>();

    private HashMap<Long, AudioPanel> audioPanels =
        new HashMap<Long, AudioPanel>();

    private HashMap<Long, VideoPanel> videoPanels =
        new HashMap<Long, VideoPanel>();

    private boolean sendOnly = false;

    private HashMap<Long, Timer> timers = new HashMap<Long, Timer>();

    private HashMap<DataSource, Timer> localTimers =
        new HashMap<DataSource, Timer>();

    private VideoWebServer webServer = null;

    public DisplayPanel(VideoWebServer webServer) {
        this.webServer = webServer;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        localStreams.setLayout(new AVLayout());
        remoteStreams.setLayout(new AVLayout());

        JScrollPane localScroller = new JScrollPane(localStreams,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JScrollPane remoteScroller = new JScrollPane(remoteStreams,
             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
             JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        JPanel localPanel = new JPanel();
        localPanel.setLayout(new BoxLayout(localPanel, BoxLayout.Y_AXIS));
        JLabel localLabel = new JLabel("Local Streams");
        localLabel.setAlignmentX(LEFT_ALIGNMENT);
        localScroller.setAlignmentX(LEFT_ALIGNMENT);
        localPanel.add(localLabel);
        localPanel.add(localScroller);

        JPanel remotePanel = new JPanel();
        remotePanel.setLayout(new BoxLayout(remotePanel, BoxLayout.Y_AXIS));
        JLabel remoteLabel = new JLabel("Remote Streams");
        remoteLabel.setAlignmentX(LEFT_ALIGNMENT);
        remoteScroller.setAlignmentX(LEFT_ALIGNMENT);
        remotePanel.add(remoteLabel);
        remotePanel.add(remoteScroller);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                   remotePanel, localPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(340);
        splitPane.setResizeWeight(1.0);
        add(splitPane);
    }

    public void setSendOnly(boolean sendOnly) {
        this.sendOnly = sendOnly;
        if (sendOnly) {
            splitPane.setDividerLocation(0);
            splitPane.setEnabled(false);
            splitPane.setOneTouchExpandable(false);
            splitPane.setDividerSize(0);
        }
    }

    public void addAudioStream(long ssrc, DataSource dataSource,
            AudioFormat format) {
        synchronized (localSsrcs) {
            if (sendOnly || localSsrcs.contains(ssrc)) {
                return;
            }
        }
        AudioPanel panel = new AudioPanel(dataSource, false);
        synchronized (audioPanels) {
            audioPanels.put(ssrc, panel);
            remoteStreams.add(panel);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new UpdateTask(panel), 0, 1000);
            timers.put(ssrc, timer);
        }
        validate();
    }

    public void addVideoStream(long ssrc, DataSource dataSource,
            VideoFormat format) {
        synchronized (localSsrcs) {
            if (sendOnly || localSsrcs.contains(ssrc)) {
                return;
            }
        }
        VideoPanel panel = new VideoPanel(dataSource, format.getEncoding(),
                false);
        synchronized (videoPanels) {
            videoPanels.put(ssrc, panel);
            remoteStreams.add(panel);
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
                remoteStreams.remove(panel);
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
                remoteStreams.remove(panel);
                videoPanels.remove(ssrc);
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

    public void addLocalAudio(String name, DataSource dataSource,
            FloatControl volumeControl, long ssrc) throws NoPlayerException,
            CannotRealizeException, IOException {
        synchronized (localSsrcs) {
            localSsrcs.add(ssrc);
        }
        AudioPanel panel = new AudioPanel(dataSource, true);
        synchronized (localAudioPanels) {
            panel.setName(name);
            localAudioPanels.put(dataSource, panel);
            localStreams.add(panel);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new UpdateTask(panel), 0, 1000);
            localTimers.put(dataSource, timer);
        }
        validate();
    }

    public void addLocalVideo(String name, DataSource dataSource, long ssrc)
            throws IOException {
        synchronized (localSsrcs) {
            localSsrcs.add(ssrc);
        }
        StreamUpdateListener listener = webServer.getStream(
                name, name);
        ChangeDetectionEffect cdEffect = new ChangeDetectionEffect();
        cdEffect.addScreenListener(listener);
        VideoPanel panel = new VideoPanel(dataSource, "", false,
                new Effect[]{cdEffect});
        localStreamListeners.put(dataSource, listener);
        synchronized (localVideoPanels) {
            panel.setName(name);
            localVideoPanels.put(dataSource, panel);
            localStreams.add(panel);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new UpdateTask(panel), 0, 1000);
            localTimers.put(dataSource, timer);
        }
        validate();
    }

    public void removeLocalAudio(DataSource dataSource) {
        synchronized (localAudioPanels) {
            AudioPanel panel = localAudioPanels.get(dataSource);
            if (panel != null) {
                panel.end();
                localStreams.remove(panel);
                localAudioPanels.remove(dataSource);
                localTimers.get(dataSource).cancel();
            }
        }
        validate();
    }

    public void removeLocalVideo(DataSource dataSource) {
        synchronized (localVideoPanels) {
            VideoPanel panel = localVideoPanels.get(dataSource);
            if (panel != null) {
                panel.end();
                localStreams.remove(panel);
                localVideoPanels.remove(dataSource);
                localTimers.get(dataSource).cancel();
                StreamUpdateListener listener =
                    localStreamListeners.remove(dataSource);
                listener.streamStopped();
            }
        }
        validate();
    }

    public void addFile(String name, DataSource dataSource, long[] ssrcs,
            PlayControl control) {
        synchronized (localSsrcs) {
            for (long ssrc : ssrcs) {
                localSsrcs.add(ssrc);
            }
        }
        VideoPanel panel = new FilePanel(dataSource, "", control);
        synchronized (localVideoPanels) {
            panel.setName(name);
            localVideoPanels.put(dataSource, panel);
            localStreams.add(panel);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new UpdateTask(panel), 0, 1000);
            localTimers.put(dataSource, timer);
        }
        validate();
    }

    public void removeFile(DataSource dataSource) {
        removeLocalVideo(dataSource);
    }
}
