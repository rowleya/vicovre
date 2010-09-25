/**
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

package com.googlecode.vicovre.media.processor;

import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;

import javax.media.Buffer;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.Seekable;
import javax.media.protocol.SourceStream;
import javax.media.protocol.SourceTransferHandler;

import com.googlecode.vicovre.media.controls.BufferReadAheadControl;

/**
 * @author Andrew G D Rowley
 * @version 1.0
 */
public abstract class DataSink extends Thread implements SourceTransferHandler,
        BufferTransferHandler {

    public static final int SEEK_SET = 0;

    public static final int SEEK_CUR = 1;

    public static final int SEEK_END = 2;

    public static final int AV_SEEK_SIZE = 3;

    private static final int DATA_LENGTH = 10000;

    private DataSource dataSource = null;

    private int track = 0;

    private boolean done = false;

    private Buffer[] inputBuffer = null;

    private int currentBuffer = 0;

    private Integer currentBufferSync = new Integer(0);

    private byte[] data = new byte[DATA_LENGTH];

    private long length = SourceStream.LENGTH_UNKNOWN;

    private boolean started = false;

    private Integer startSync = new Integer(0);

    private LinkedList<Buffer> pushedBuffers = new LinkedList<Buffer>();

    private int maxBufferedBuffers = 1;

    private DataSinkHandleThread thread = new DataSinkHandleThread(this);

    /**
     * A DataSink for a track
     *
     * @param dataSource The data source
     * @param track The track to process
     */
    public DataSink(DataSource dataSource, int track) {
        this.dataSource = dataSource;
        this.track = track;
        BufferReadAheadControl readAhead = (BufferReadAheadControl)
            dataSource.getControl(BufferReadAheadControl.class.getName());
        if (readAhead != null) {
            maxBufferedBuffers = readAhead.getMaxBufferReadAhead();
        }
        inputBuffer = new Buffer[maxBufferedBuffers];
        for (int i = 0; i < inputBuffer.length; i++) {
            inputBuffer[i] = new Buffer();
        }
    }

    protected void waitForStart() {
        synchronized (startSync) {
            synchronized (this) {
                while (!started && !done) {
                    try {
                        startSync.wait();
                    } catch (InterruptedException e) {
                        // Does Nothing
                    }
                }
            }
        }
    }

    protected void addBuffer(Buffer buffer) {
        synchronized (pushedBuffers) {
            while (!done && (pushedBuffers.size() >= maxBufferedBuffers)) {
                try {
                    pushedBuffers.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
            if (!done) {
                pushedBuffers.addLast(buffer);
                pushedBuffers.notifyAll();
            }
        }
    }

    public void transferNextBuffer() {
        Buffer buffer = null;
        synchronized (pushedBuffers) {
            while (!done && pushedBuffers.isEmpty()) {
                try {
                    pushedBuffers.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
            if (!done) {
                buffer = pushedBuffers.removeFirst();
                pushedBuffers.notifyAll();
            }
        }
        if (buffer != null) {
            try {
                handleBuffer(buffer);
            } catch (EOFException e) {
                System.err.println("Connection closed");
                close();
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }
    }

    /**
     *
     * @see java.lang.Thread#run()
     */
    public void run() {
        try {
            dataSource.connect();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        thread.start();
        if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream[] streams = ((PushBufferDataSource)
                    dataSource).getStreams();
            streams[track].setTransferHandler(this);
            length = streams[track].getContentLength();
            try {
                dataSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (startSync) {
                started = true;
                startSync.notifyAll();
            }
        } else if (dataSource instanceof PushDataSource) {
            PushSourceStream[] streams = ((PushDataSource)
                    dataSource).getStreams();
            streams[track].setTransferHandler(this);
            length = streams[track].getContentLength();
            try {
                dataSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (startSync) {
                started = true;
                startSync.notifyAll();
            }
        } else if (dataSource instanceof PullBufferDataSource) {
            try {
                dataSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (startSync) {
                started = true;
                startSync.notifyAll();
            }
            PullBufferStream[] streams = ((PullBufferDataSource)
                    dataSource).getStreams();
            PullBufferStream stream = streams[track];
            length = streams[track].getContentLength();
            while (!done) {
                try {
                    currentBuffer = (currentBuffer + 1) % inputBuffer.length;
                    inputBuffer[currentBuffer].setData(null);
                    inputBuffer[currentBuffer].setLength(0);
                    inputBuffer[currentBuffer].setOffset(0);
                    inputBuffer[currentBuffer].setEOM(false);
                    inputBuffer[currentBuffer].setDiscard(false);
                    stream.read(inputBuffer[currentBuffer]);
                    addBuffer(inputBuffer[currentBuffer]);
                } catch (IOException e) {
                    e.printStackTrace();
                    synchronized (this) {
                        done = true;
                        notifyAll();
                    }
                }
            }
        } else if (dataSource instanceof PullDataSource) {
            try {
                dataSource.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (startSync) {
                started = true;
                startSync.notifyAll();
            }
            PullSourceStream[] streams = ((PullDataSource)
                    dataSource).getStreams();
            PullSourceStream stream = streams[track];
            length = streams[track].getContentLength();
            while (!done) {
                try {
                    currentBuffer = (currentBuffer + 1) % inputBuffer.length;
                    int bytesRead = stream.read(data, 0, data.length);
                    if (bytesRead != -1) {
                        inputBuffer[currentBuffer].setData(data);
                        inputBuffer[currentBuffer].setOffset(0);
                        inputBuffer[currentBuffer].setLength(bytesRead);
                        inputBuffer[currentBuffer].setEOM(false);
                        inputBuffer[currentBuffer].setDiscard(false);
                        addBuffer(inputBuffer[currentBuffer]);
                    } else {
                        inputBuffer[currentBuffer].setEOM(true);
                        inputBuffer[currentBuffer].setDiscard(true);
                        addBuffer(inputBuffer[currentBuffer]);
                    }
                } catch (IOException e) {
                    close();
                }
            }
        }
    }

    /**
     * Stops the processing
     *
     */
    public void close() {
        synchronized (this) {
            done = true;
            thread.close();
            notifyAll();
            synchronized (startSync) {
                startSync.notifyAll();
            }
        }
        if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream[] streams = ((PushBufferDataSource)
                    dataSource).getStreams();
            streams[track].setTransferHandler(null);
        } else if (dataSource instanceof PushDataSource) {
            PushSourceStream[] streams = ((PushDataSource)
                    dataSource).getStreams();
            streams[track].setTransferHandler(null);
        }

        try {
            dataSource.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataSource.disconnect();


        data = null;
        for (int i = 0; i < inputBuffer.length; i++) {
            inputBuffer[currentBuffer].setData(null);
        }
        System.err.println("Closed");
    }

    /**
     *
     * @see javax.media.protocol.SourceTransferHandler#transferData(
     *     javax.media.protocol.PushSourceStream)
     */
    public void transferData(PushSourceStream stream) {
        try {
            int buffer = 0;
            synchronized (currentBufferSync) {
                buffer = currentBuffer;
                currentBuffer = (currentBuffer + 1) % inputBuffer.length;
            }
            int bytesRead = stream.read(data, 0, data.length);
            if (bytesRead != -1) {
                inputBuffer[buffer].setData(data);
                inputBuffer[buffer].setOffset(0);
                inputBuffer[buffer].setLength(bytesRead);
                addBuffer(inputBuffer[buffer]);
            } else {
                inputBuffer[buffer].setEOM(true);
                inputBuffer[buffer].setDiscard(true);
                addBuffer(inputBuffer[buffer]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @see javax.media.protocol.BufferTransferHandler#transferData(
     *     javax.media.protocol.PushBufferStream)
     */
    public void transferData(PushBufferStream stream) {
        try {
            int buffer = 0;
            synchronized (currentBufferSync) {
                buffer = currentBuffer;
                currentBuffer = (currentBuffer + 1) % inputBuffer.length;
            }
            inputBuffer[buffer].setData(null);
            inputBuffer[buffer].setLength(0);
            inputBuffer[buffer].setOffset(0);
            stream.read(inputBuffer[buffer]);
            addBuffer(inputBuffer[buffer]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles
     * @param buffer The buffer to handle
     * @throws IOException
     */
    public abstract void handleBuffer(Buffer buffer) throws IOException;

    /**
     * Returns true if the data sink has finished writing
     * @return True if the data sink has finished writing
     */
    public boolean isDone() {
        return done;
    }

    public boolean isSeekable() {
        if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream[] streams = ((PushBufferDataSource)
                    dataSource).getStreams();
            return streams[track] instanceof Seekable;
        } else if (dataSource instanceof PushDataSource) {
            PushSourceStream[] streams = ((PushDataSource)
                    dataSource).getStreams();
            return streams[track] instanceof Seekable;
        } else if (dataSource instanceof PullBufferDataSource) {
            PullBufferStream[] streams = ((PullBufferDataSource)
                    dataSource).getStreams();
            return streams[track] instanceof Seekable;
        } else if (dataSource instanceof PullDataSource) {
            PullSourceStream[] streams = ((PullDataSource)
                    dataSource).getStreams();
            return streams[track] instanceof Seekable;
        }
        return false;
    }

    public long seek(long position, int whence) {
        Seekable seekable = null;
        SourceStream stream = null;
        if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream[] streams = ((PushBufferDataSource)
                    dataSource).getStreams();
            stream = streams[track];
        } else if (dataSource instanceof PushDataSource) {
            PushSourceStream[] streams = ((PushDataSource)
                    dataSource).getStreams();
            stream = streams[track];
        } else if (dataSource instanceof PullBufferDataSource) {
            PullBufferStream[] streams = ((PullBufferDataSource)
                    dataSource).getStreams();
            stream = streams[track];
        } else if (dataSource instanceof PullDataSource) {
            PullSourceStream[] streams = ((PullDataSource)
                    dataSource).getStreams();
            stream = streams[track];
        }
        if (stream instanceof Seekable) {
            seekable = (Seekable) stream;
        }
        if (seekable == null) {
            return -1;
        }
        long pos = position;
        if (whence == SEEK_CUR) {
            pos += seekable.tell();
        } else if (whence == SEEK_END) {
            if (length == SourceStream.LENGTH_UNKNOWN) {
                return -1;
            }
            pos += length;
        } else if (whence == AV_SEEK_SIZE) {
            if (length == SourceStream.LENGTH_UNKNOWN) {
                return -1;
            }
            return length;
        } else if (whence != SEEK_SET) {
            return -1;
        }
        pos = seekable.seek(pos);
        return pos;
    }
}
