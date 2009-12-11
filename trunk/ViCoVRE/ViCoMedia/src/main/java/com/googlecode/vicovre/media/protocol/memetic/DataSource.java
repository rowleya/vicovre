/*
 * @(#)MemeticDataSource.java
 * Created: 2 Nov 2007
 * Version: 1.0
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
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
 */

package com.googlecode.vicovre.media.protocol.memetic;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashSet;

import javax.media.Format;
import javax.media.SystemTimeBase;
import javax.media.Time;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.Positionable;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class DataSource extends PushBufferDataSource implements Positionable {

    // The rtp stream
    private DatagramForwarder[] rtpStreams = null;

    private StreamSource[] streamSources = null;

    private String filename = null;

    private long seek = 0;

    private double scale = 1.0;

    private boolean playing = false;

    private StreamSource minSource = null;

    private long duration = 0;

    private long currentTime = 0;

    private SystemTimeBase timeBase = new SystemTimeBase();

    private HashSet<StreamStartingListener> startListeners =
        new HashSet<StreamStartingListener>();

    private HashSet<StreamStoppingListener> stopListeners =
        new HashSet<StreamStoppingListener>();

    public DataSource(File... files) {
        StreamSource maxSource = null;
        long maxSourceEnd = 0;
        rtpStreams = new DatagramForwarder[files.length];
        streamSources = new StreamSource[files.length];
        for (int i = 0; i < files.length; i++) {
            rtpStreams[i] = new DatagramForwarder(timeBase);
            streamSources[i] = new StreamSource(this, files[i], i);
            if (minSource == null || (streamSources[i].getStartTime()
                    < minSource.getStartTime())) {
                minSource = streamSources[i];
            }
            long end = streamSources[i].getStartTime()
                + streamSources[i].getDuration();
            if (maxSource == null || (end > maxSourceEnd)) {
                maxSource = streamSources[i];
                maxSourceEnd = end;
            }
        }

        duration = maxSourceEnd - minSource.getStartTime();

        long baseTime = minSource.getStartTime();
        for (int i = 0; i < streamSources.length; i++) {
            System.err.println("Offset shift = " + (streamSources[i].getStartTime() - baseTime));
            streamSources[i].setOffsetShift(
                    streamSources[i].getStartTime() - baseTime);
        }
    }

    public void addStartingListener(StreamStartingListener listener) {
        startListeners.add(listener);
    }

    public void addStoppingListener(StreamStoppingListener listener) {
        stopListeners.add(listener);
    }

    public void setFormat(int i, Format format) {
        rtpStreams[i].setFormat(format);
    }

    /**
     * @see javax.media.protocol.PushDataSource#getStreams()
     */
    public PushBufferStream[] getStreams() {
        return rtpStreams;
    }

    /**
     * @see javax.media.protocol.DataSource#connect()
     */
    public void connect() throws IOException {
        if (getLocator() != null) {
            String locator = getLocator().getRemainder().substring(2);
            String[] parts = locator.split("\\?", 2);
            filename = parts[0];
            if (parts.length > 1) {
                String query = parts[1];
                String[] values = query.split("&");
                for (int i = 0; i < values.length; i++) {
                    String[] value = values[i].split("=", 2);
                    if (value[0].equals("scale")) {
                        scale = Double.parseDouble(value[1]);
                    } else if (value[0].equals("seek")) {
                        seek = Long.parseLong(value[1]);
                    }
                }
            }
            streamSources = new StreamSource[1];
            streamSources[0] = new StreamSource(this, new File(filename), 0);
            rtpStreams = new DatagramForwarder[1];
            rtpStreams[0] = new DatagramForwarder(timeBase);
            minSource = streamSources[0];
            try {
                rtpStreams[0].setFormat(streamSources[0].getRtpFormat());
            } catch (UnsupportedFormatException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * @see javax.media.protocol.DataSource#disconnect()
     */
    public void disconnect() {
        stop();
        for (DatagramForwarder stream : rtpStreams) {
            stream.close();
        }
    }

    /**
     * @see javax.media.protocol.DataSource#getContentType()
     */
    public String getContentType() {
        return "raw";
    }

    /**
     * @see javax.media.protocol.DataSource#getControl(java.lang.String)
     */
    public Object getControl(String cls) {
        return null;
    }

    /**
     * @see javax.media.protocol.DataSource#getControls()
     */
    public Object[] getControls() {
        return new Object[0];
    }

    /**
     * @see javax.media.protocol.DataSource#start()
     */
    public void start() {
        if (!playing) {
            playing = true;
            long allStartTime = System.currentTimeMillis() + 500;
            for (int i = 0; i < streamSources.length; i++) {
                streamSources[i].play(scale, seek, allStartTime);
            }
        }
    }

    /**
     * @see javax.media.protocol.DataSource#stop()
     */
    public void stop() {
        if (playing) {
            playing = false;
            seek = getCurrentTime();
            for (int i = 0; i < streamSources.length; i++) {
                streamSources[i].teardown();
            }
        }
    }

    /**
     * Handles an RTP Packet
     * @param packet The packet to handle
     * @param sourceId The id of the source sending the packet
     */
    public void handleRTPPacket(DatagramPacket packet, int sourceId) {
        rtpStreams[sourceId].handlePacket(packet);
    }

    /**
     * Seeks to a new time
     * @param seek The new time to seek to
     * @param scale The new scale to play at
     */
    public void seek(long seek, double scale) {
        currentTime = seek;
        this.seek = seek;
        this.scale = scale;
        boolean wasPlaying = playing;
        if (!playing) {
            currentTime = seek;
        }
        stop();
        if (wasPlaying) {
            start();
        }
    }

    protected synchronized void setCurrentTime(long time) {
        if (scale > 0) {
            if (time > currentTime) {
                currentTime = time;
            }
        } else {
            if (time < currentTime) {
                currentTime = time;
            }
        }

    }

    /**
     * Gets the current time of the playback
     * @return The current time in milliseconds
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * @see javax.media.protocol.DataSource#getDuration()
     */
    public Time getDuration() {
        return new Time(duration * 1000000);
    }

    /**
     *
     * @see javax.media.protocol.Positionable#isRandomAccess()
     */
    public boolean isRandomAccess() {
        return true;
    }

    /**
     *
     * @see javax.media.protocol.Positionable#setPosition(javax.media.Time, int)
     */
    public Time setPosition(Time where, int rounding) {
        boolean wasPlaying = playing;
        stop();
        seek = where.getNanoseconds() / 1000000;
        playing = wasPlaying;
        seek(seek, this.scale);
        return where;
    }

    protected void streamStarting(int id) {
        for (StreamStartingListener listener : startListeners) {
            listener.streamStarting(this, id);
        }
    }

    protected void streamStopping(int id) {
        for (StreamStoppingListener listener : stopListeners) {
            listener.streamStopping(this, id);
        }
    }

    public long getStartOffset(int id) {
        return streamSources[id].getStartTime() - minSource.getStartTime();
    }
}
