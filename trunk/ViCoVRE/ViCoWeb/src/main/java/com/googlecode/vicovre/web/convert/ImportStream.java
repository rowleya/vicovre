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

package com.googlecode.vicovre.web.convert;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Manager;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.SourceCloneable;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.screencapture.CaptureChangeListener;

public class ImportStream  {

    private boolean live = false;

    private long lastFrameNumber = -1;

    private DataSource dataSource = null;

    private Demultiplexer demuxer = null;

    private LiveDataSource liveDataSource = null;

    private SourceCloneable clonable = null;

    private Integer lockSync = new Integer(0);

    private boolean writeLocked = false;

    private boolean readLocked = false;

    private HashMap<Integer, HashMap<String, ChangeListener>> changeListeners =
        new HashMap<Integer, HashMap<String, ChangeListener>>();

    private StreamChangeListener[] streamChangeListeners = null;

    public ImportStream(boolean live) {
        this.live = live;
    }

    public void getWriteLock() {
        synchronized (lockSync) {
            while (writeLocked) {
                try {
                    lockSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
            writeLocked = true;
            lockSync.notifyAll();
        }
    }

    public void releaseWriteLock() {
        synchronized (lockSync) {
            writeLocked = false;
            lockSync.notifyAll();
        }
    }

    public void getReadLock() {
        synchronized (lockSync) {
            while (!writeLocked) {
                try {
                    lockSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }

            if (!live) {
                while (readLocked) {
                    try {
                        lockSync.wait();
                    } catch (InterruptedException e) {
                        // Does Nothing
                    }
                }
                readLocked = true;
                lockSync.notifyAll();
            }
        }
    }

    public void releaseReadLock() {
        synchronized (lockSync) {
            readLocked = false;
            lockSync.notifyAll();
        }
    }

    public static boolean isImageSource(String contentType) {
        return contentType.startsWith("image/");
    }

    public long getNextFrameNumber() {
        return lastFrameNumber + 1;
    }

    private void createLiveSource() throws IOException {
        System.err.println("Live stream created");
        liveDataSource = new LiveDataSource(dataSource);
        liveDataSource.start();
        LiveDataStream[] streams = (LiveDataStream[])
            liveDataSource.getStreams();
        streamChangeListeners = new StreamChangeListener[streams.length];
        for (int i = 0; i < streams.length; i++) {
            streamChangeListeners[i] = new StreamChangeListener(i, this,
                    streams[i]);
            streams[i].addCaptureChangeListener(streamChangeListeners[i]);
        }
    }

    public void addInputStream(InputStream inputStream,
            String contentType, long timestamp, long frame,
            long contentLength) throws IOException, UnsupportedFormatException {
        System.err.println("Adding input stream");
        lastFrameNumber = frame;

        if (isImageSource(contentType)) {
            System.err.println("Adding image stream");
            com.googlecode.vicovre.media.protocol.image.DataSource ds =
                (com.googlecode.vicovre.media.protocol.image.DataSource)
                    dataSource;
            if (ds == null) {
                System.err.println("Creating image stream");
                ds =
                   new com.googlecode.vicovre.media.protocol.image.DataSource(
                           live);
                dataSource = ds;
                if (live) {
                    createLiveSource();
                }
            }
            System.err.println("Reading image");
            ds.readImage(inputStream, timestamp, frame);
            System.err.println("Finished Reading image");
        } else {
            DataSource ds =
                new com.googlecode.vicovre.media.protocol.stream.DataSource(
                        inputStream, contentType, contentLength);
            demuxer = Misc.findDemultiplexer(ds);
            if (demuxer == null) {
                throw new UnsupportedFormatException(
                        new ContentDescriptor(contentType));
            }
            dataSource =
                new com.googlecode.vicovre.media.protocol.demuxer.DataSource(
                        demuxer);
            if (live) {
                createLiveSource();
            }
        }
    }

    public void setDataSource(DataSource dataSource) throws IOException {
        this.dataSource = dataSource;
        if (live) {
            liveDataSource = new LiveDataSource(dataSource);
            liveDataSource.start();
        }
    }

    public DataSource getDataSource() {
        if (clonable == null) {
            if (live) {
                clonable = (SourceCloneable) Manager.createCloneableDataSource(
                        liveDataSource);
            }
            clonable = (SourceCloneable) Manager.createCloneableDataSource(
                    dataSource);
        }
        return clonable.createClone();
    }

    public void close() {
        if (live) {
            if (liveDataSource != null) {
                try {
                    liveDataSource.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                liveDataSource = null;
            }
        }
        if (demuxer != null) {
            demuxer.close();
            demuxer = null;
        }
        if (dataSource != null) {
            try {
                dataSource.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataSource = null;
        }
        writeLocked = false;
        readLocked = false;
        lockSync.notifyAll();
    }

    public Format getFormat(int substream) {
        if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream[] streams =
                ((PushBufferDataSource) dataSource).getStreams();
            return streams[substream].getFormat();
        } else if (dataSource instanceof PullBufferDataSource) {
            PullBufferStream[] streams =
                ((PullBufferDataSource) dataSource).getStreams();
            return streams[substream].getFormat();
        }
        return null;
    }

    public int getNoStreams() {
        if (dataSource instanceof PushBufferDataSource) {
            return ((PushBufferDataSource) dataSource).getStreams().length;
        } else if (dataSource instanceof PullBufferDataSource) {
            return ((PullBufferDataSource) dataSource).getStreams().length;
        }
        return 0;
    }

    public void captureDone(int streamid) {
        HashMap<String, ChangeListener> streamListeners =
            changeListeners.get(streamid);
        if (streamListeners != null) {
            for (ChangeListener listener : streamListeners.values()) {
                listener.markChanged();
            }
        }
    }

    public byte[] getImage(int substream, String contentType)
            throws IOException {
        if (live) {
            if ((substream >= 0)
                    && (substream < streamChangeListeners.length)) {
                return streamChangeListeners[substream].getImage(contentType);
            }
            return null;
        }
        return null;
    }

    public ChangeListener getChangeListener(int substream, String changeId) {
        if (liveDataSource != null) {
            HashMap<String, ChangeListener> streamListeners =
                changeListeners.get(substream);
            if (streamListeners == null) {
                streamListeners = new HashMap<String, ChangeListener>();
                changeListeners.put(substream, streamListeners);
            }
            if ((changeId == null) || changeId.equals("")) {
                String id = UUID.randomUUID().toString();
                ChangeListener listener = new ChangeListener(id);
                streamListeners.put(id, listener);
                listener.markChanged();
                return listener;
            }
            return streamListeners.get(changeId);
        }
        return null;
    }
}
