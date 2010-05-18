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

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.media.Format;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;

import com.googlecode.vicovre.repositories.rtptype.RTPType;

public class ConvertSession {

    private boolean live = false;

    private String name = null;

    private HashMap<String, ImportStream> importStreams =
        new HashMap<String, ImportStream>();

    private HashMap<String, TransmitStream> transmitStreams =
        new HashMap<String, TransmitStream>();

    private Vector<StreamReceiver> receiveStreams =
        new Vector<StreamReceiver>();

    public ConvertSession(boolean live, String name) {
        this.live = live;
        this.name = name;
    }

    public List<String> getStreamIds() {
        return new Vector<String>(importStreams.keySet());
    }

    public ImportStream getStream(String streamId) {
        return getStream(streamId, false);
    }

    private ImportStream getStream(String streamId, boolean create) {
        ImportStream stream = null;
        synchronized (importStreams) {
            stream = importStreams.get(streamId);
            if ((stream == null) && create) {
                stream = new ImportStream(live);
                importStreams.put(streamId, stream);
            }
        }
        return stream;
    }

    public void addStream(DataSource dataSource, String streamId)
            throws IOException {
        ImportStream stream = getStream(streamId, true);
        stream.setDataSource(dataSource);
    }

    public void addStream(InputStream input, String contentType,
            String streamId, long timestamp, long timeclock, long frame,
            boolean inter, long contentLength)
            throws IOException, UnsupportedFormatException {
        ImportStream stream = getStream(streamId, true);
        stream.getWriteLock();
        long frameNo = frame;
        if (frame == -1) {
            frameNo = stream.getNextFrameNumber();
        }
        stream.addInputStream(input, contentType, timestamp, frameNo,
                contentLength);
        stream.releaseWriteLock();
    }

    public Format getFormat(String streamId, int substream)
            throws FileNotFoundException {
        ImportStream stream = getStream(streamId, false);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        return stream.getFormat(substream);
    }

    private TransmitStream createTransmitStream(String streamId, int substream,
            String name, String note)
            throws FileNotFoundException {
        ImportStream stream = getStream(streamId, false);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        TransmitStream transmitStream = new TransmitStream(
                stream.getDataSource(), substream);
        if ((name != null) && !name.equals("")) {
            transmitStream.setName(name);
        }
        if ((note != null) && !note.equals("")) {
            transmitStream.setNote(note);
        }
        return transmitStream;
    }

    private Format getFormat(RTPType rtpType, int width, int height) {
        Format format = null;
        if (width != -1 && height != -1) {
            if (rtpType.getFormat() instanceof VideoFormat) {
                format = new VideoFormat(rtpType.getFormat().getEncoding(),
                        new Dimension(width, height), -1, Format.byteArray, -1);
            }
        }
        return format;
    }

    public String sendStream(String streamId, int substream,
            String venue, String name, String note,
            RTPType rtpType, int width, int height)
            throws Exception {
        TransmitStream transmitStream = createTransmitStream(streamId,
                substream, name, note);
        Format format = getFormat(rtpType, width, height);
        transmitStream.transmit(venue, rtpType, format);
        String id = UUID.randomUUID().toString();
        transmitStreams.put(id, transmitStream);
        return id;
    }

    public String sendStream(String streamId, int substream,
            String address, int port, int ttl,
            String name, String note,
            RTPType rtpType, int width, int height)
            throws Exception {
        TransmitStream transmitStream = createTransmitStream(streamId,
                substream, name, note);
        Format format = getFormat(rtpType, width, height);
        transmitStream.transmit(address, port, ttl, rtpType, format);
        String id = UUID.randomUUID().toString();
        transmitStreams.put(id, transmitStream);
        return id;
    }

    public void receiveStreams(String venue) throws Exception {
        StreamReceiver receiver = new StreamReceiver(this, venue);
        receiveStreams.add(receiver);
    }

    public void receiveStreams(String address, int port) throws Exception {
        StreamReceiver receiver = new StreamReceiver(this, address, port);
        receiveStreams.add(receiver);
    }

    public ChangeListener getChangeListener(String streamId, int substream,
            String changeId) throws FileNotFoundException {
        ImportStream stream = getStream(streamId, false);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        return stream.getChangeListener(substream, changeId);
    }

    public String getName() {
        return name;
    }

    public void close() {
        for (String id : importStreams.keySet()) {
            ImportStream stream = importStreams.get(id);
            stream.close();
        }
        importStreams.clear();
        for (String id : transmitStreams.keySet()) {
            TransmitStream stream = transmitStreams.get(id);
            stream.stop();
        }
        transmitStreams.clear();
        for (StreamReceiver receiver : receiveStreams) {
            receiver.close();
        }
        receiveStreams.clear();
    }
}
