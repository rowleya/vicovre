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

package com.googlecode.vicovre.codecs.ffmpeg.demuxer;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Demultiplexer;
import javax.media.ResourceUnavailableException;
import javax.media.Time;
import javax.media.Track;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.Positionable;

import com.googlecode.vicovre.codecs.ffmpeg.PixelFormat;
import com.googlecode.vicovre.codecs.ffmpeg.Utils;
import com.googlecode.vicovre.codecs.nativeloader.NativeLoader;

public class FFMPEGDemuxer implements Demultiplexer {

    private static enum CodecType {
        CODEC_TYPE_VIDEO,
        CODEC_TYPE_AUDIO,
        CODEC_TYPE_DATA,
        CODEC_TYPE_SUBTITLE,
        CODEC_TYPE_ATTACHMENT,
        CODEC_TYPE_NB
    };

    private static final String[] CONTENT_TYPES = new String[]{
        "video/x-msvideo",
        "video/mpeg",
        "video/quicktime",
        "content/unknown"
    };

    private DataSource dataSource = null;

    private final Integer bufferSync = new Integer(0);

    private Buffer bufferToRead = null;

    private int bufferToReadOffset = 0;

    private int bufferToReadLength = 0;

    private LinkedList<Buffer> preBuffers = new LinkedList<Buffer>();

    private boolean endOfDataSource = false;

    private DemuxerDataSink dataSink = null;

    private Time currentTime = new Time(0);

    private Time duration = DURATION_UNKNOWN;

    private FFMPEGTrack[] tracks = new FFMPEGTrack[0];

    private boolean inited = false;

    private boolean done = false;

    private long ref = -1;

    private boolean finishedProbe = false;

    public Time getDuration() {
        return duration;
    }

    public Time getMediaTime() {
        return currentTime;
    }

    public ContentDescriptor[] getSupportedInputContentDescriptors() {
        System.err.println("Getting supported content descriptors: ");
        ContentDescriptor[] descriptors =
            new ContentDescriptor[CONTENT_TYPES.length];
        for (int i = 0; i < CONTENT_TYPES.length; i++) {
            descriptors[i] = new ContentDescriptor(
                    CONTENT_TYPES[i].replace('/', '.').replace('-', '_'));
            System.err.println("    " + descriptors[i]);
        }
        return descriptors;
    }

    public Track[] getTracks() throws IOException {
        if (!inited) {
            init();
        }
        return tracks;
    }

    public boolean isPositionable() {
        return dataSource instanceof Positionable;
    }

    public boolean isRandomAccess() {
        return isPositionable();
    }

    public Time setPosition(Time time, int rounding) {
        if (dataSource instanceof Positionable) {
            return ((Positionable) dataSource).setPosition(time, rounding);
        }
        return currentTime;
    }

    private void init() throws IOException {
        System.err.println("Initializing");
        String url = dataSource.getLocator().getURL().toExternalForm();
        System.err.println("URL = " + url);
        ref = init(url, 10000);
        inited = true;
        if (ref <= 0) {
            throw new IOException("Could not initialize demuxer");
        }
        System.err.println("Done initializing");
        tracks = new FFMPEGTrack[getNoStreams(ref)];
        Vector<Track> tracksToLoad = new Vector<Track>();
        System.err.println(tracks.length + " tracks");
        long maxDuration = 0;
        for (int i = 0; i < tracks.length; i++) {
            int codecType = getCodecType(ref, i);
            if (codecType == CodecType.CODEC_TYPE_VIDEO.ordinal()) {
                long start = getStartTime(ref, i);
                long length = getStreamDuration(ref, i);
                System.err.println("Source " + i + " start = " + start);
                int outputSize = getOutputSize(ref, i);
                maxDuration = Math.max(maxDuration, start + length);
                setStreamOutputVideoFormat(ref, i, -1, -1, -1);
                int width = getVideoWidth(ref, i);
                int height = getVideoHeight(ref, i);
                int pixelFormat = getVideoPixelFormat(ref, i);
                VideoFormat format = Utils.getVideoFormat(
                        PixelFormat.find(pixelFormat),
                        new Dimension(width, height), -1);
                FFMPEGTrack track = new FFMPEGTrack(this, i, format,
                        new Time(start), new Time(length), outputSize);
                tracksToLoad.add(track);
                System.err.println("Source " + i + " format = " + track.getFormat());
            } else if (codecType == CodecType.CODEC_TYPE_AUDIO.ordinal()) {
                long start = getStartTime(ref, i);
                long length = getStreamDuration(ref, i);
                System.err.println("Source " + i + " start = " + start);
                int outputSize = getOutputSize(ref, i);
                maxDuration = Math.max(maxDuration, start + length);
                setStreamAudioDecoded(ref, i);
                int sampleRate = getAudioSampleRate(ref, i);
                int sampleSize = getAudioSampleSize(ref, i);
                int channels = getNoAudioChannels(ref, i);
                AudioFormat format = new AudioFormat(AudioFormat.LINEAR,
                        sampleRate, sampleSize, channels,
                        ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
                            ? AudioFormat.BIG_ENDIAN
                            : AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED);
                FFMPEGTrack track = new FFMPEGTrack(this, i, format,
                        new Time(start), new Time(length), outputSize);
                tracksToLoad.add(track);
                System.err.println("Source " + i + " format = " + track.getFormat());
            }
        }
        tracks = tracksToLoad.toArray(new FFMPEGTrack[0]);
        duration = new Time(getSourceDuration(ref));
        if ((duration.getNanoseconds() == 0)
                || (duration == DURATION_UNKNOWN)) {
            duration = new Time(maxDuration);
        }
    }

    public void start() throws IOException {
        // Does Nothing
    }

    public void stop() {
        // Does Nothing
    }

    public void close() {
        dataSink.close();
        synchronized (bufferSync) {
            done = true;
            bufferSync.notifyAll();
        }
        if (ref > 0) {
            dispose(ref);
            ref = -1;
        }
    }

    public String getName() {
        return "FFMPEGDemuxer";
    }

    public void open() throws ResourceUnavailableException {
        System.err.println("Opening demuxer");
        NativeLoader.loadLibrary(getClass(), "ffmpegj");
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String paramString) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public void setSource(DataSource dataSource) {
        System.err.println("Setting source to " + dataSource);
        this.dataSource = dataSource;
        dataSink = new DemuxerDataSink(this, dataSource, 0);
        dataSink.start();
    }

    protected void handleBuffer(Buffer buffer) {
        synchronized (bufferSync) {
            if (!done) {
                bufferToRead = buffer;
                bufferToReadOffset = buffer.getOffset();
                bufferToReadLength = buffer.getLength();
                bufferSync.notifyAll();
            }
            while (!done && (bufferToRead != null)) {
                try {
                    bufferSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }
    }

    protected void finishedProbe() {
        synchronized (bufferSync) {
            finishedProbe = true;
            bufferToRead.setOffset(bufferToReadOffset);
            bufferToRead.setLength(bufferToReadLength);
            endOfDataSource = false;
        }
    }

    protected Buffer readNextBuffer(int size) {
        synchronized (bufferSync) {
            if (bufferToRead != null) {
                if (bufferToRead.getLength() == 0) {
                    if (!finishedProbe) {
                        preBuffers.addLast((Buffer) bufferToRead.clone());
                    }
                    if (bufferToRead.isEOM()) {
                        endOfDataSource = true;
                    }
                    if (finishedProbe && !preBuffers.isEmpty()) {
                        bufferToRead = preBuffers.removeFirst();
                    } else {
                        bufferToRead = null;
                        bufferSync.notifyAll();
                    }
                }
            }

            if (endOfDataSource) {
                return null;
            }

            while (!done && (bufferToRead == null)) {
                try {
                    bufferSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
            if (!done) {
                if (bufferToRead.isEOM()) {
                    if (bufferToRead.isDiscard()) {
                        endOfDataSource = true;
                        return null;
                    }
                }

                Buffer buffer = new Buffer();
                buffer.copy(bufferToRead);
                int length = size;
                if (length > buffer.getLength()) {
                    length = buffer.getLength();
                } else {
                    buffer.setLength(length);
                }
                bufferToRead.setOffset(bufferToRead.getOffset() + length);
                bufferToRead.setLength(bufferToRead.getLength() - length);
                return buffer;
            }
        }
        return null;
    }

    protected boolean isEndOfSource() {
        return endOfDataSource;
    }

    protected boolean setStreamOutputVideoFormat(int stream,
            VideoFormat format) {
        PixelFormat pixFmt = Utils.getPixFormat(format);
        if (pixFmt == PixelFormat.PIX_FMT_NONE) {
            return false;
        }
        int width = -1;
        int height = -1;
        Dimension size = format.getSize();
        if (size != null) {
            width = size.width;
            height = size.height;
        }
        return setStreamOutputVideoFormat(ref, stream, pixFmt.getId(),
                width, height);
    }

    protected boolean setStreamAudioDecoded(int stream) {
        return setStreamAudioDecoded(ref, stream);
    }

    protected int getOutputSize(int stream) {
        return getOutputSize(ref, stream);
    }

    protected synchronized boolean readNextFrame(Buffer output, int stream) {
        return readNextFrame(ref, output, stream);
    }

    private native long init(String filename, int bufferSize);

    private native int getNoStreams(long ref);

    private native String getCodecName(long ref, int stream);

    private native int getCodecType(long ref, int stream);

    private native int getOutputSize(long ref, int stream);

    private native long getSourceDuration(long ref);

    private native long getStreamDuration(long ref, int stream);

    private native long getStartTime(long ref, int stream);

    private native boolean setStreamOutputVideoFormat(long ref,
            int stream, int pixelFmt, int width, int height);

    private native int getVideoWidth(long ref, int stream);

    private native int getVideoHeight(long ref, int stream);

    private native int getVideoPixelFormat(long ref, int stream);

    private native double getVideoFrameRate(long ref, int stream);

    private native boolean setStreamAudioDecoded(long ref,
            int stream);

    private native int getAudioSampleSize(long ref, int stream);

    private native int getNoAudioChannels(long ref, int stream);

    private native int getAudioSampleRate(long ref, int stream);

    private native boolean readNextFrame(long ref, Buffer output, int stream);

    private native void dispose(long ref);
}
