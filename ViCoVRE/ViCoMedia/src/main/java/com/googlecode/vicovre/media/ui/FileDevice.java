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

package com.googlecode.vicovre.media.ui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.RTPConnector;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;

import com.googlecode.onevre.ag.types.ClientProfile;
import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.controls.PlayControl;
import com.googlecode.vicovre.media.effect.CloneEffect;
import com.googlecode.vicovre.media.effect.CloneEffectDataSource;
import com.googlecode.vicovre.media.effect.TimeEffect;
import com.googlecode.vicovre.media.processor.SimpleProcessor;
import com.googlecode.vicovre.media.protocol.demuxer.DataSource;

public class FileDevice implements PlayControl {

    private String filename = null;

    private String deviceName = null;

    private RTPManager[] sendManagers = null;

    private SimpleProcessor[] processors = null;

    private SendStream[] sendStreams = null;

    private CloneEffectDataSource cloneSource = null;

    private TimeEffect timeEffect = new TimeEffect();

    private long[] ssrcs = null;

    private DataSource dataSource = null;

    private Demultiplexer demuxer = null;

    private ClientProfile profile = null;

    private boolean deviceStarted = false;

    private String tool = null;

    private boolean prepared = false;

    private VideoFormat lastVideoFormat = null;

    private AudioFormat lastAudioFormat = null;

    private FileListener listener = null;

    private boolean playing = false;

    public FileDevice(String filename, ClientProfile profile, String tool) {
        this.filename = filename;
        this.profile = profile;
        this.tool = tool;
        deviceName = new File(filename).getName();
    }

    public void test() throws NoDataSourceException, IOException {
        javax.media.protocol.DataSource ds =
            Manager.createDataSource(new MediaLocator("file:" + filename));
        demuxer = Misc.findDemultiplexer(ds);
        if (demuxer == null) {
            throw new IOException("Can't find demultiplexer for "
                    + ds.getContentType());
        }
        dataSource = new DataSource(demuxer);
        dataSource.connect();
    }

    public void prepare(
            RTPConnector videoConnector, RTPConnector audioConnector,
            VideoFormat videoFormat, AudioFormat audioFormat,
            int videoRtpType, int audioRtpType)
            throws NoDataSourceException, IOException,
            UnsupportedFormatException {
        if (prepared && (lastVideoFormat != null)
                && lastVideoFormat.equals(videoFormat)
                && (lastAudioFormat != null)
                && lastAudioFormat.equals(audioFormat)) {
            return;
        }

        stop();

        deviceStarted = false;
        lastVideoFormat = videoFormat;
        lastAudioFormat = audioFormat;

        if (dataSource == null) {
            test();
        }

        dataSource.start();

        PullBufferStream[] dataStreams = dataSource.getStreams();

        int noStreams = dataStreams.length;
        sendManagers = new RTPManager[noStreams];
        processors = new SimpleProcessor[noStreams];
        sendStreams = new SendStream[noStreams];
        cloneSource = new CloneEffectDataSource(noStreams);
        ssrcs = new long[noStreams];

        for (int i = 0; i < noStreams; i++) {
            RTPConnector rtpConnector = null;
            Format format = dataStreams[i].getFormat();
            Format outputFormat = null;
            int rtpType = 0;
            if (format instanceof VideoFormat) {
                rtpConnector = videoConnector;
                outputFormat = videoFormat;
                rtpType = videoRtpType;
            } else {
                rtpConnector = audioConnector;
                outputFormat = audioFormat;
                rtpType = audioRtpType;
            }

            processors[i] = new SimpleProcessor(format, outputFormat);
            if (!processors[i].insertEffect(cloneSource.getCloneEffect(i))) {
                System.err.println("Couldn't clone");
            }
            if (!processors[i].insertEffect(timeEffect)) {
                System.err.println("Couldn't adjust time");
            }

            sendManagers[i] = RTPManager.newInstance();
            sendManagers[i].addFormat(outputFormat, rtpType);
            sendManagers[i].initialize(rtpConnector);

            PushBufferDataSource data =
                processors[i].getDataOutput(dataSource, i);
            sendStreams[i] = sendManagers[i].createSendStream(data, 0);
            sendStreams[i].setSourceDescription(Misc.createSourceDescription(
                    profile, deviceName, tool));

            ssrcs[i] = sendStreams[i].getSSRC();
            if (ssrcs[i] < 0) {
                ssrcs[i] = ssrcs[i] + (((long) Integer.MAX_VALUE + 1) * 2);
            }
            prepared = true;
        }
    }

    private void stopDataSource() {
        try {
            timeEffect.pause();
            dataSource.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(FileListener listener) throws IOException {
        if (!deviceStarted && prepared) {
            this.listener = listener;
            if (listener != null) {
                listener.addFile(deviceName, cloneSource, ssrcs, this);
            }
            for (int i = 0; i < sendStreams.length; i++) {
                try {
                    sendStreams[i].start();
                    processors[i].start(dataSource, i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < sendStreams.length; i++) {
                final int iter = i;
                Thread t = new Thread() {
                    public void run() {
                        processors[iter].waitForFirstFrame();
                        stopDataSource();
                    }
                };
                t.start();
            }
            deviceStarted = true;
        } else if (!prepared) {
            throw new IOException("Device not prepared!");
        }
    }

    public void stop() {
        if (deviceStarted) {
            if (listener != null) {
                listener.removeFile(cloneSource);
            }
            try {
                dataSource.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < sendStreams.length; i++) {
                processors[i].stop();
                try {
                    sendStreams[i].stop();
                } catch (IOException e) {
                    // Do Nothing
                }
                sendStreams[i].close();
                sendManagers[i].removeTargets("Leaving");
                sendManagers[i].dispose();
            }

            deviceStarted = false;
        }
    }

    public Time getDuration() {
        return demuxer.getDuration();
    }

    public Time getPosition() {
        return demuxer.getMediaTime();
    }

    public void pause() {
        if (playing) {
            try {
                timeEffect.pause();
                dataSource.stop();
                playing = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void play() {
        if (!playing) {
            try {
                timeEffect.resume();
                dataSource.start();
                playing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Time seek(Time position) {
        Time mediaTime = demuxer.getMediaTime();
        Time newPos = demuxer.setPosition(position, 0);
        timeEffect.seek(mediaTime, newPos);
        return newPos;
    }

    public Component getControlComponent() {
        return null;
    }

}
