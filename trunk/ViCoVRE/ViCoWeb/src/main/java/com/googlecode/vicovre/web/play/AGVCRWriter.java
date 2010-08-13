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

package com.googlecode.vicovre.web.play;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.format.VideoFormat;

import com.googlecode.vicovre.media.MemeticFileReader;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.LEDataOutputStream;

public class AGVCRWriter {

    private static final String[] HOSTS = new String[]{"224.0.0.1", "224.0.0.1",
            "224.0.0.1", "224.0.0.1"};

    private static final int[] PORTS = new int[]{57004, 57005, 57006, 57007};

    private static final int[] TTLS = new int[]{127, 127, 127, 127};

    private static final int[] MEDIA_TYPES = new int[]{1, 1, 2, 2};

    private static final int[] TYPES = new int[]{1, 2, 1, 2};


    private static final int MAX_PACKETS = 127;

    private static final long MAX_SIZE = Integer.MAX_VALUE * 2L;

    private LEDataOutputStream out = null;

    private RtpTypeRepository typeRepository;

    private Recording recording = null;

    private String[] streamIds = null;

    private long lastHeaderOffset = 0;

    public AGVCRWriter(RtpTypeRepository typeRepository, Recording recording,
            String[] streamIds, OutputStream output) {
        out = new LEDataOutputStream(output);
        this.typeRepository = typeRepository;
        this.recording = recording;
        this.streamIds = streamIds;
    }

    public void write() throws IOException {

        MemeticFileReader[] readers = new MemeticFileReader[streamIds.length];
        boolean[] readerFinished = new boolean[streamIds.length];
        Stream[] streams = new Stream[streamIds.length];
        boolean allReadersFinished = true;
        long minStartTime = Long.MAX_VALUE;
        long maxEndTime = 0;
        for (int i = 0; i < streamIds.length; i++) {
            Stream stream = recording.getStream(streamIds[i]);
            streams[i] = stream;
            File file = new File(recording.getDirectory(), streamIds[i]);
            readers[i] = new MemeticFileReader(file.getAbsolutePath(),
                    typeRepository);
            readers[i].streamSeek(0);
            readerFinished[i] = !readers[i].readNextPacket();
            if (!readerFinished[i]) {
                allReadersFinished = false;
            }
            minStartTime = Math.min(minStartTime, readers[i].getStartTime());
            maxEndTime = Math.max(maxEndTime,
                    streams[i].getEndTime().getTime());
        }
        for (int i = 0; i < readers.length; i++) {
            readers[i].setTimestampOffset(
                    (readers[i].getStartTime() - minStartTime) * 1000000);
        }

        writeFileHeader(minStartTime);
        writeStreamHeaders();

        while (!allReadersFinished) {

            // Find the next stream to read
            long minTimestamp = Long.MAX_VALUE;
            for (int i = 0; i < readers.length; i++) {
                if (!readerFinished[i]) {
                    long timestamp = readers[i].getTimestamp();
                    if (timestamp < minTimestamp) {
                        minTimestamp = timestamp;
                    }
                }
            }

            // Read all buffers at the minimum timestamp
            Vector<Buffer> buffers = new Vector<Buffer>();
            Vector<Integer> indices = new Vector<Integer>();
            Vector<Integer> rtpTypes = new Vector<Integer>();
            Vector<Long> realTimestamps = new Vector<Long>();
            for (int i = 0; i < readers.length; i++) {
                while ((readers[i].getTimestamp() == minTimestamp)
                        && !readerFinished[i]) {
                    Buffer buffer = readers[i].getBuffer();
                    Buffer clone = (Buffer) buffer.clone();
                    buffers.add(clone);
                    indices.add(i);
                    rtpTypes.add(streams[i].getRtpType().getId());
                    realTimestamps.add(readers[i].getRealTimestamp());
                    readerFinished[i] =
                        !readers[i].readNextPacket();
                }
            }

            // Write all the buffers at the minimum timestamp
            int position = 0;
            while (position < buffers.size()) {
                position = writeData(buffers, indices, rtpTypes, realTimestamps,
                        position, minTimestamp);
            }

            // Test if all streams have finished
            allReadersFinished = true;
            for (int i = 0; i < readers.length; i++) {
                if (!readerFinished[i]) {
                    allReadersFinished = false;
                    break;
                }
            }
        }

        long sdesSize = 0;
        for (int i = 0; i < streams.length; i++) {
            sdesSize += writeSDES(i, streams[i]);
        }

        writeFooter(streams.length, sdesSize, maxEndTime);
    }

    private void writeFooter(int noParticipants, long sdesSize, long maxEndTime)
            throws IOException {
        out.writeShort(0); // KEY
        out.writeShort(noParticipants);
        out.writeInt((int) (sdesSize & 0x00000000FFFFFFFFL));
        out.writeLong(maxEndTime / 1000);
        out.writeLong(lastHeaderOffset);
        out.writeShort(noParticipants);
        out.writeByte(0); // Pad 1
        out.writeByte(0); // Pad 2
        out.writeByte(0); // Pad 3
        out.writeByte(0); // Pad 4
        out.writeByte(0); // Pad 5
        out.writeByte(0); // Pad 6
    }

    private void writeItemSize(String item) throws IOException {
        if (item != null) {
            out.writeByte(item.length());
        } else {
            out.writeByte(0);
        }
    }

    private void writeItem(String item) throws IOException {
        if (item != null) {
            out.writeBytes(item);
        }
    }

    private long writeSDES(int index, Stream stream) throws IOException {
        long startBytes = out.getCount();
        out.writeInt(index);
        out.writeByte(stream.getRtpType().getId());
        if (stream.getMediaType().equalsIgnoreCase("Audio")) {
            out.writeByte(1);
        } else if (stream.getMediaType().equalsIgnoreCase("Video")) {
            out.writeByte(2);
        } else {
            out.writeByte(0);
        }
        writeItemSize(stream.getCname());
        out.writeByte(0); // Pad 1
        out.writeByte(0); // Pad 2
        out.writeByte(0); // Pad 3
        out.writeByte(0); // Pad 4
        out.writeByte(0); // Pad 5
        out.writeByte(0); // Pad 6
        out.writeByte(0); // Pad 7
        out.writeByte(0); // Pad 8
        out.writeByte(0); // Pad 9

        out.writeByte(0);
        writeItemSize(stream.getCname());
        writeItemSize(stream.getName());
        writeItemSize(stream.getEmail());
        writeItemSize(stream.getPhone());
        writeItemSize(stream.getLocation());
        writeItemSize(stream.getTool());
        writeItemSize(stream.getNote());
        out.writeByte(0); // SDES 8
        out.writeByte(0); // SDES 9
        out.writeByte(0); // SDES 10
        out.writeByte(0); // SDES 11
        out.writeByte(0); // SDES 12
        out.writeByte(0); // SDES 13
        out.writeByte(0); // SDES 14
        out.writeByte(0); // SDES 15

        writeItem(stream.getCname());
        writeItem(stream.getName());
        writeItem(stream.getEmail());
        writeItem(stream.getPhone());
        writeItem(stream.getLocation());
        writeItem(stream.getTool());
        writeItem(stream.getNote());
        writeItem(stream.getCname());

        return out.getCount() - startBytes;
    }

    private int writeData(Vector<Buffer> buffers, Vector<Integer> indices,
            Vector<Integer> rtpTypes, Vector<Long> realTimestamps,
            int position, long timestamp)
            throws IOException {
        long pos = out.getCount();

        out.writeShort(0); // KEY

        int noPackets = 0;
        long size = 32;
        boolean full = false;
        while (!full) {
            if (((position + noPackets) >= buffers.size())
                    || (noPackets >= MAX_PACKETS)) {
                full = true;
            } else {
                int length = buffers.get(noPackets + position).getLength();
                if ((size + length) < MAX_SIZE) {
                    size += length + 8;
                    noPackets += 1;
                } else {
                    full = true;
                }
            }
        }

        int noPacketsFlags = noPackets;
        out.writeByte(noPacketsFlags);

        out.writeByte(0); // Pad 1
        out.writeInt((int) (size & 0x00000000FFFFFFFFL)); // Length
        out.writeLong(timestamp / 1000);
        out.writeLong(pos);
        out.writeLong(lastHeaderOffset);
        lastHeaderOffset = pos;

        for (int i = position; i < (noPackets + position); i++) {
            Buffer buffer = buffers.get(i);
            int index = indices.get(i);
            int rtpType = rtpTypes.get(i);
            out.writeShort(0); // KEY
            out.writeShort(buffer.getLength());
            int streamFlags = 0;// | (1 << 7);
            if (buffer.getFormat() instanceof VideoFormat) {
                streamFlags = 2;
            } else {
                streamFlags = 0;
            }
            out.writeByte(streamFlags);
            int typeFlags = rtpType;
            if ((buffer.getFlags() & Buffer.FLAG_RTP_MARKER) > 0) {
                typeFlags |= 1 << 7;
            }
            out.writeByte(typeFlags);
            int sourceFlags = index;
            out.writeShort(sourceFlags);

            /*long realTimestamp = realTimestamps.get(i);
            out.writeInt((int) (realTimestamp & 0x00000000FFFFFFFFL));
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(0); */

            out.write((byte[]) buffer.getData(), buffer.getOffset(),
                    buffer.getLength());
        }

        return position + noPackets;
    }

    private void writeFileHeader(long minStartTime)
            throws IOException {
        out.writeByte(4); // VERSION
        out.writeByte(HOSTS.length); // NO STREAMS
        out.writeShort(0); // KEY
        out.writeByte(2); // PLATFORM = WINDOWS
        out.writeByte(2); // PROGRAM MAJOR
        out.writeByte(2); // PROGRAM MINOR
        out.writeByte(1); // PROGRAM UPDATE
        out.writeLong(minStartTime / 1000); // Start Time
        out.writeLong(32 + (16 * HOSTS.length) + (9 * HOSTS.length));
        out.writeByte(0); // BIG ENDIAN = FALSE
        out.writeByte(0); // Description Length
        out.writeByte(0); // Pad 1
        out.writeByte(0); // Pad 2
        out.writeByte(0); // Pad 3
        out.writeByte(0); // Pad 4
        out.writeByte(0); // Pad 5
        out.writeByte(0); // Pad 6
    }

    private void writeStreamHeaders() throws IOException {

        for (int i = 0; i < HOSTS.length; i++) {
            out.writeShort(0); // KEY
            out.writeByte(MEDIA_TYPES[i]);
            out.writeByte(TYPES[i]);
            out.writeByte(HOSTS[i].length()); // Host Length (Host = 224.0.0.1)
            out.writeByte(TTLS[i]); // TTL
            out.writeShort(PORTS[i]); // Port
            out.writeByte(0); // Description Length
            out.writeByte(0); // Encrypted
            out.writeByte(0); // Pad 2
            out.writeByte(0); // Pad 3
            out.writeByte(0); // Pad 4
            out.writeByte(0); // Pad 5
            out.writeByte(0); // Pad 6
            out.writeByte(0); // Pad 7
            out.writeBytes(HOSTS[i]); // Host Name
        }
    }
}
