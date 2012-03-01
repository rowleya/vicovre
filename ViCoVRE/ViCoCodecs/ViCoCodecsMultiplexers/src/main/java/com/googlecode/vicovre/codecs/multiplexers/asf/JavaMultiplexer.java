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

package com.googlecode.vicovre.codecs.multiplexers.asf;

import java.awt.Component;
import java.awt.Dimension;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;

import com.googlecode.vicovre.codecs.utils.ByteArrayOutputStream;
import com.googlecode.vicovre.codecs.utils.LittleEndianHeaderOutputStream;
import com.googlecode.vicovre.media.controls.SetDurationControl;
import com.googlecode.vicovre.media.format.BitRateFormat;
import com.googlecode.vicovre.media.multiplexer.BasicMultiplexer;

public class JavaMultiplexer extends BasicMultiplexer
        implements SetDurationControl {

    private static final ContentDescriptor[] CONTENT_TYPES =
        new ContentDescriptor[]{
            new ContentDescriptor("video/x-ms-asf"),
            new ContentDescriptor("video/x-ms-wmv"),
            new ContentDescriptor("audio/x-ms-wma")};

    private static final long INDEX_TIME_INTERVAL = 1000;

    private static final long PREROLL = 3100;

    private static final int PACKET_SIZE = 3200;

    private static final int AUDIO_HEADER_SIZE = 30;

    private static final int AUDIO_SPREAD_SIZE = 8;

    private static final int VIDEO_HEADER_SIZE = 51;

    private static final int WAVE_FORMAT_MPEGLAYER3 = 0x0055;

    private static final int MP3_EXTRA_SIZE = 12;

    private static final int PACKET_HEADER_MIN_SIZE = 11;

    private static final int MULTI_PAYLOAD_HEADER_SIZE = 17;

    private static final int SINGLE_PAYLOAD_HEADER_SIZE = 15;

    private static final int SINGLE_PAYLOAD_LENGTH =
        PACKET_SIZE - PACKET_HEADER_MIN_SIZE - SINGLE_PAYLOAD_HEADER_SIZE;

    private static final int MULTI_PAYLOAD_LENGTH =
        PACKET_SIZE - PACKET_HEADER_MIN_SIZE
        - (MULTI_PAYLOAD_HEADER_SIZE * 2) - 1;

    private static final int MULTIPLE_PAYLOADS_PRESENT = 1;

    private static final int PADDING_LENGTH_FIELD_IS_WORD  = 0x10;

    private static final int PADDING_LENGTH_FIELD_IS_BYTE  = 0x08;

    private static final int REPLICATED_DATA_LENGTH_FIELD_IS_BYTE  = 0x01;

    private static final int OFFSET_LENGTH_FIELD_IS_DWORD = 0x0c;

    private static final int OBJECT_NUMBER_LENGTH_FIELD_IS_BYTE  = 0x10;

    private static final int STREAM_NUMBER_LENGTH_FIELD_IS_BYTE = 0x40;

    private static final int PAYLOAD_LENGTH_FIELD_IS_WORD = 0x80;

    private static final int FLAG_KEY_FRAME = 0x80;

    private static final int ERROR_CORRECTION = 0x80 | 0x2;

    private static final int PPI_PROPERTY_FLAGS =
        REPLICATED_DATA_LENGTH_FIELD_IS_BYTE |
        OFFSET_LENGTH_FIELD_IS_DWORD |
        OBJECT_NUMBER_LENGTH_FIELD_IS_BYTE |
        STREAM_NUMBER_LENGTH_FIELD_IS_BYTE;

    private static final int MP3_BITRATE = 128000;

    private static final int MP3_BLOCK_SIZE = 1152;

    private static final GUID ASF_HEADER_OBJECT = new GUID(
        0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11,
        0xA6, 0xD9, 0x00, 0xAA, 0x00, 0x62, 0xCE, 0x6C);

    private static final GUID ASF_FILE_PROPERTIES_OBJECT = new GUID(
        0xA1, 0xDC, 0xAB, 0x8C, 0x47, 0xA9, 0xCF, 0x11,
        0x8E, 0xE4, 0x00, 0xC0, 0x0C, 0x20, 0x53, 0x65);

    private static final GUID ASF_STREAM_PROPERTIES_OBJECT = new GUID(
        0x91, 0x07, 0xDC, 0xB7, 0xB7, 0xA9, 0xCF, 0x11,
        0x8E, 0xE6, 0x00, 0xC0, 0x0C, 0x20, 0x53, 0x65);

    private static final GUID ASF_AUDIO_MEDIA = new GUID(
        0x40, 0x9E, 0x69, 0xF8, 0x4D, 0x5B, 0xCF, 0x11,
        0xA8, 0xFD, 0x00, 0x80, 0x5F, 0x5C, 0x44, 0x2B);

    private static final GUID ASF_VIDEO_MEDIA = new GUID(
        0xC0, 0xEF, 0x19, 0xBC, 0x4D, 0x5B, 0xCF, 0x11,
        0xA8, 0xFD, 0x00, 0x80, 0x5F, 0x5C, 0x44, 0x2B);

    private static final GUID ASF_AUDIO_SPREAD = new GUID(
        0x50, 0xCD, 0xC3, 0xBF, 0x8F, 0x61, 0xCF, 0x11,
        0x8B, 0xB2, 0x00, 0xAA, 0x00, 0xB4, 0xE2, 0x20);

    private static final GUID ASF_NO_ERROR_CORRECTION = new GUID(
        0x00, 0x57, 0xFB, 0x20, 0x55, 0x5B, 0xCF, 0x11,
        0xA8, 0xFD, 0x00, 0x80, 0x5F, 0x5C, 0x44, 0x2B);

    private static final GUID ASF_HEADER_EXTENSION_OBJECT = new GUID(
        0xb5, 0x03, 0xbf, 0x5f, 0x2E, 0xA9, 0xCF, 0x11,
        0x8e, 0xe3, 0x00, 0xc0, 0x0c, 0x20, 0x53, 0x65);

    private static final GUID ASF_RESERVED_1 = new GUID(
        0x11, 0xd2, 0xd3, 0xab, 0xBA, 0xA9, 0xCF, 0x11,
        0x8e, 0xe6, 0x00, 0xc0, 0x0c, 0x20, 0x53, 0x65);

    private static final GUID ASF_DATA_OBJECT = new GUID(
        0x36, 0x26, 0xb2, 0x75, 0x8E, 0x66, 0xCF, 0x11,
        0xa6, 0xd9, 0x00, 0xaa, 0x00, 0x62, 0xce, 0x6c);

    private static final GUID ASF_CODEC_LIST_OBJECT = new GUID(
        0x40, 0x52, 0xD1, 0x86, 0x1D, 0x31, 0xD0, 0x11,
        0xA3, 0xA4, 0x00, 0xA0, 0xC9, 0x03, 0x48, 0xF6);

    private static final GUID ASF_RESERVED_2 = new GUID(
        0x41, 0x52, 0xd1, 0x86, 0x1D, 0x31, 0xD0, 0x11,
        0xa3, 0xa4, 0x00, 0xa0, 0xc9, 0x03, 0x48, 0xf6);

    private static final GUID ASF_SIMPLE_INDEX_OBJECT = new GUID(
        0x90, 0x08, 0x00, 0x33, 0xB1, 0xE5, 0xCF, 0x11,
        0x89, 0xF4, 0x00, 0xA0, 0xC9, 0x03, 0x49, 0xCB);

    private static final GUID FILE_ID = new GUID(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);

    private boolean headerWritten = false;

    private long duration = 0;

    private long offset = 0;

    private long packetsToWrite = 0;

    private boolean[] trackSeen = null;

    private int tracksToSee = 0;

    private Integer tracksSync = new Integer(0);

    private int[] sequence = null;

    private Integer packetSync = new Integer(0);

    private byte[] packet = new byte[PACKET_SIZE];

    private int packetBytesWritten = 0;

    private long packetFirstTimestamp = -1;

    private long packetLastTimestamp = -1;

    private int packetNoPayloads = 0;

    private boolean multiPayloadPacket = false;

    private boolean packetFull = false;

    private int currentHeaderSize = 0;

    private ByteArrayOutputStream packetStream = new ByteArrayOutputStream(
            packet, 0, packet.length);

    private LinkedList<Buffer> bufferedBuffers = new LinkedList<Buffer>();

    private LinkedList<Integer> bufferedTracks = new LinkedList<Integer>();

    private LittleEndianHeaderOutputStream indexEntries =
        new LittleEndianHeaderOutputStream(0, 0);

    private int noIndexEntries = 0;

    private int nextIndexTime = 0;

    private int lastKeyFramePacket = 0;

    private int lastKeyFrameNoPackets = 0;

    private int noPackets = 0;

    private int maxPacketsPerKeyFrame = 0;

    private boolean processing = false;

    private boolean lastRead = false;

    public JavaMultiplexer() {
        super(CONTENT_TYPES,
            new Format[]{
                new VideoFormat("msmpeg4"),
                new AudioFormat(AudioFormat.MPEGLAYER3)
            },
            32);
    }

    public int setNumTracks(int numtracks) {
        int tracks = super.setNumTracks(numtracks);
        trackSeen = new boolean[tracks];
        tracksToSee = tracks;
        sequence = new int[tracks];
        return tracks;
    }

    private void writeHeader(DataOutputStream out) throws IOException {

        // Find the bitrate and file length
        int bitrate = 0;
        int maxrate = 0;
        long dataSize = 0;
        for (int i = 0; i < getNoTracks(); i++) {
            Format format = getTrackFormat(i);
            if (format instanceof BitRateFormat) {
                BitRateFormat brf = (BitRateFormat) format;
                bitrate += brf.getBitRate();
                maxrate += brf.getBitRate() + brf.getTolerance();
            }
        }
        dataSize = (maxrate / 8) * (duration / 10000000);
        packetsToWrite = dataSize / (PACKET_SIZE - MULTI_PAYLOAD_HEADER_SIZE);
        if ((dataSize % (PACKET_SIZE - MULTI_PAYLOAD_HEADER_SIZE)) != 0) {
            packetsToWrite += 1;
        }
        dataSize = packetsToWrite * PACKET_SIZE;

        Header header = new Header(ASF_HEADER_OBJECT);
        header.writeInt(3 + getNoTracks()); // Number of Header Objects
        header.write(0x1); // Reserved
        header.write(0x2); // Reserved

        // The File Properties Object
        long creationDate = System.currentTimeMillis() * 10000;
        creationDate += 116444736000000000L;
        Header fileProps = new Header(ASF_FILE_PROPERTIES_OBJECT);
        fileProps.write(FILE_ID);
        fileProps.writeLong(0); // File size (ignored)
        fileProps.writeLong(creationDate); // Creation date
        fileProps.writeLong(0);//packetsToWrite); // Data Packets count
        fileProps.writeLong(duration + (PREROLL * 10000));
        fileProps.writeLong(0); // Send Duration
        fileProps.writeLong(PREROLL); // PreRoll (buffer time)
        fileProps.writeInt(2);  // Flags
        fileProps.writeInt(PACKET_SIZE);
        fileProps.writeInt(PACKET_SIZE);
        fileProps.writeInt(bitrate);  // Bit Rate
        header.write(fileProps);

        // Header Extension Object
        Header extHeader = new Header(ASF_HEADER_EXTENSION_OBJECT);
        extHeader.write(ASF_RESERVED_1);
        extHeader.writeShort(6);
        extHeader.writeInt(0);
        header.write(extHeader);

        // Stream properties object
        for (int i = 0; i < getNoTracks(); i++) {
            Header streamProps = new Header(ASF_STREAM_PROPERTIES_OBJECT);
            Format format = getTrackFormat(i);
            if (format instanceof AudioFormat) {
                streamProps.write(ASF_AUDIO_MEDIA);
                streamProps.write(ASF_AUDIO_SPREAD);
            } else {
                streamProps.write(ASF_VIDEO_MEDIA);
                streamProps.write(ASF_NO_ERROR_CORRECTION);
            }
            streamProps.writeLong(0); // Time Offset
            if (format instanceof AudioFormat) {
                streamProps.writeInt(AUDIO_HEADER_SIZE);
                streamProps.writeInt(AUDIO_SPREAD_SIZE);
            } else {
                streamProps.writeInt(VIDEO_HEADER_SIZE);
                streamProps.writeInt(0); // Enc size
            }
            streamProps.writeShort(i + 1); // Stream number
            streamProps.writeInt(0); // Reserved
            if (format instanceof AudioFormat) {
                AudioFormat af = (AudioFormat) format;
                streamProps.writeShort(WAVE_FORMAT_MPEGLAYER3);
                streamProps.writeShort(af.getChannels());
                streamProps.writeInt((int) af.getSampleRate());
                streamProps.writeInt(MP3_BITRATE / 8);
                streamProps.writeShort(MP3_BLOCK_SIZE);
                streamProps.writeShort(0);
                streamProps.writeShort(MP3_EXTRA_SIZE);

                // MP3 Extra data
                streamProps.writeShort(1); // wID
                streamProps.writeInt(2); // fdwFlags
                streamProps.writeShort(MP3_BLOCK_SIZE); // Block size
                streamProps.writeShort(1); // Frames per block
                streamProps.writeShort(1393); // Codec delay

                // Error correction
                streamProps.writeByte(1);
                streamProps.writeShort(MP3_BLOCK_SIZE); // Virtual Packet Size
                streamProps.writeShort(MP3_BLOCK_SIZE); // Virtual Chunk Size 
                streamProps.writeShort(1);
                streamProps.writeByte(0);
            } else {
                VideoFormat vf = (VideoFormat) format;
                Dimension size = vf.getSize();
                streamProps.writeInt(size.width);
                streamProps.writeInt(size.height);
                streamProps.writeByte(2);
                streamProps.writeShort(40);
                streamProps.writeInt(40);
                streamProps.writeInt(size.width);
                streamProps.writeInt(size.height);
                streamProps.writeShort(1);
                streamProps.writeShort(24);
                streamProps.writeBytes("MP43");
                streamProps.writeInt(size.width * size.height * 3);
                streamProps.writeInt(0);
                streamProps.writeInt(0);
                streamProps.writeInt(0);
                streamProps.writeInt(0);
            }
            header.write(streamProps);
        }

        Header codecList = new Header(ASF_CODEC_LIST_OBJECT);
        codecList.write(ASF_RESERVED_2);
        codecList.writeInt(getNoTracks());
        for (int i = 0; i < getNoTracks(); i++) {
            Format format = getTrackFormat(i);
            if (format instanceof AudioFormat) {
                codecList.writeShort(0x0002);
            } else {
                codecList.writeShort(0x0001);
            }
            codecList.writeShort(format.getEncoding().length() + 1);
            codecList.writeChars(format.getEncoding());
            codecList.writeShort(0); // Unicode Null termination
            codecList.writeShort(0); // Codec Description Length
            if (format instanceof AudioFormat) {
                codecList.writeShort(0x2); // Codec Information Length
                codecList.writeShort(0x55); // Codec Information
            } else {
                codecList.writeShort(0x4); // Codec Information Length
                codecList.writeBytes("MP43"); // Codec Information
            }
        }
        header.write(codecList);

        out.write(header.getBytes());

        LittleEndianHeaderOutputStream dataHeader =
            new LittleEndianHeaderOutputStream(0, 0);
        dataHeader.write(ASF_DATA_OBJECT.getBytes());
        dataHeader.writeLong(dataSize + 50);
        dataHeader.write(FILE_ID.getBytes());
        dataHeader.writeLong(packetsToWrite);
        dataHeader.writeShort(0x0101);
        byte[] headerBytes = dataHeader.getBytes();
        out.write(headerBytes);
    }

    private void writePayloadHeader(int track, long timestamp, int objectSize,
            int objectOffset, int length, boolean key) throws IOException {
        LittleEndianHeaderOutputStream out =
            new LittleEndianHeaderOutputStream(0, 0);

        int keyFrame = 0;
        if (key) {
            keyFrame = FLAG_KEY_FRAME;
        }
        out.writeByte((track + 1) | keyFrame);

        out.writeByte(sequence[track]);                    // Modia Object No
        out.writeInt(objectOffset);                 // Offset into Media Object
        out.writeByte(8);                           // Replicated Data Length
        out.writeInt(objectSize);                   // Replicated Data - size of Media Object
        out.writeInt((int) (timestamp + PREROLL));  // Replicated Data - presentation time of Media Object
        if (multiPayloadPacket) {                   // Payload Length
            out.writeShort(length);
        }
        packetStream.write(out.getBytes());
    }

    private void flush() {
        packetFull = true;
        packetBytesWritten = 0;
        packetSync.notifyAll();
        while (packetFull && !isDone()) {
            try {
                packetSync.wait();
            } catch (InterruptedException e) {
                // Do Nothing
            }
        }
        packetFirstTimestamp = -1;
        packetLastTimestamp = -1;
        packetNoPayloads = 0;
        multiPayloadPacket = false;
        packetStream.reset();
    }

    private int doProcess(Buffer buf, int trk) {
        synchronized (packetSync) {
            while (processing && !isDone()) {
                try {
                    packetSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
            processing = true;
            byte[] data = (byte[]) buf.getData();
            int offset = buf.getOffset();
            int length = buf.getLength();
            long timestamp = (buf.getTimeStamp() / 1000000) - this.offset;
            boolean key = (buf.getFlags() & Buffer.FLAG_KEY_FRAME) > 0;
            if (buf.getFormat() instanceof AudioFormat) {
                key = false;
            }
            int startPacket = noPackets;
            int endPacket = startPacket;

            while (length > 0) {
                int space = 0;
                if (packetFirstTimestamp == -1) {
                    if (length < MULTI_PAYLOAD_LENGTH) {
                        multiPayloadPacket = true;
                        space = MULTI_PAYLOAD_LENGTH - 1;
                    } else {
                        space = SINGLE_PAYLOAD_LENGTH;
                    }
                    packetFirstTimestamp = timestamp;
                } else {
                    space = packetStream.getSpace() - MULTI_PAYLOAD_HEADER_SIZE
                        - PACKET_HEADER_MIN_SIZE - 1;
                    if ((space < length)
                            && (buf.getFormat() instanceof AudioFormat)) {
                        flush();
                        startPacket += 1;
                        continue;
                    }
                }


                int packetLength = length;

                if (space > 0) {
                    if (packetLength > space) {
                        packetLength = space;
                    } else if (packetLength == (space - 1)) {
                        packetLength = space - 2;
                    }

                    try {
                        writePayloadHeader(trk, timestamp, buf.getLength(),
                            offset - buf.getOffset(), packetLength, key);
                        packetStream.write(data, offset, packetLength);
                    } catch (IOException e) {
                        e.printStackTrace();
                        processing = false;
                        packetSync.notifyAll();
                        return BUFFER_PROCESSED_FAILED;
                    }
                    packetLastTimestamp = timestamp;
                    packetNoPayloads += 1;
                } else {
                    packetLength = 0;
                }

                offset += packetLength;
                length -= packetLength;

                if (!multiPayloadPacket) {
                    flush();
                    if (length > 0) {
                        endPacket += 1;
                    }
                } else if (packetStream.getSpace() <= (MULTI_PAYLOAD_HEADER_SIZE
                        + PACKET_HEADER_MIN_SIZE + 1)) {
                    flush();
                    if (length > 0) {
                        endPacket += 1;
                    }
                }
            }

            if (key) {
                int noPacketsSent = (endPacket - startPacket) + 1;
                if (noPacketsSent > maxPacketsPerKeyFrame) {
                    maxPacketsPerKeyFrame = noPacketsSent;
                }
                lastKeyFramePacket = startPacket;
                lastKeyFrameNoPackets = noPacketsSent;
            }

            while (timestamp > nextIndexTime) {
                try {
                    indexEntries.writeInt(lastKeyFramePacket);
                    indexEntries.writeShort(lastKeyFrameNoPackets);
                    noIndexEntries += 1;
                    nextIndexTime += INDEX_TIME_INTERVAL;
                } catch (IOException e) {
                    // Do Nothing
                }

            }

            sequence[trk]++;
            processing = false;
            packetSync.notifyAll();
            return BUFFER_PROCESSED_OK;
        }
    }

    public int process(Buffer buf, int trk) {
        synchronized (tracksSync) {
            if (tracksToSee > 0) {
                bufferedBuffers.addLast((Buffer) buf.clone());
                bufferedTracks.addLast(trk);
                if (!trackSeen[trk]) {
                    trackSeen[trk] = true;
                    tracksToSee--;
                    setInputFormat(buf.getFormat(), trk);
                    if (tracksToSee > 0) {
                        return BUFFER_PROCESSED_OK;
                    }
                    tracksSync.notifyAll();
                    while (!headerWritten) {
                        try {
                            tracksSync.wait();
                        } catch (InterruptedException e) {
                            // Does Nothing
                        }
                    }
                }
            }
        }
        if (!bufferedBuffers.isEmpty()) {
            while (!bufferedBuffers.isEmpty()) {
                Buffer buffer = bufferedBuffers.removeFirst();
                int track = bufferedTracks.removeFirst();
                if (doProcess(buffer, track) == BUFFER_PROCESSED_FAILED) {
                    return BUFFER_PROCESSED_FAILED;
                }
            }
            return BUFFER_PROCESSED_OK;
        }
        return doProcess(buf, trk);
    }

    private void writePacketHeader(DataOutputStream out) throws IOException {
        int paddingSize = packetStream.getSpace();
        paddingSize -= PACKET_HEADER_MIN_SIZE;
        if (multiPayloadPacket) {
            paddingSize -= 1;
        }

        LittleEndianHeaderOutputStream header =
            new LittleEndianHeaderOutputStream(0, 0);

        header.writeByte(ERROR_CORRECTION);
        header.writeByte(0);
        header.writeByte(0);

        int flags = 0;
        if (multiPayloadPacket) {
            flags |= MULTIPLE_PAYLOADS_PRESENT;
        }
        if (paddingSize > 0) {
            if (paddingSize > 256) {
                flags |= PADDING_LENGTH_FIELD_IS_WORD;
            } else {
                flags |= PADDING_LENGTH_FIELD_IS_BYTE;
            }
        }

        header.writeByte(flags);
        header.writeByte(PPI_PROPERTY_FLAGS);
        if (paddingSize > 0) {
            if (paddingSize > 256) {
                paddingSize -= 2;
                header.writeShort(paddingSize);
            } else {
                paddingSize -= 1;
                header.writeByte(paddingSize);
            }
        }


        header.writeInt((int) packetFirstTimestamp);
        header.writeShort((int) (packetLastTimestamp - packetFirstTimestamp));
        if (multiPayloadPacket) {
            header.writeByte(packetNoPayloads | PAYLOAD_LENGTH_FIELD_IS_WORD);
        }
        byte[] headerBytes = header.getBytes();
        out.write(headerBytes);
        Arrays.fill(packet, packet.length - paddingSize - headerBytes.length,
                packet.length, (byte) 0);
        noPackets += 1;
        packetsToWrite -= 1;
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(
                buf, off, len);
        DataOutputStream out = new DataOutputStream(bytes);

        synchronized (tracksSync) {
            if (!headerWritten) {
                while ((tracksToSee > 0) && !isDone()) {
                    try {
                        tracksSync.wait();
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
                if (isDone()) {
                    return -1;
                }

                writeHeader(out);
                out.flush();
                bytes.flush();
                headerWritten = true;
                tracksSync.notifyAll();
                return bytes.getCount();
            }
        }

        synchronized (packetSync) {
            while (!packetFull && !isDone()) {
                try {
                    packetSync.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }

            if (isDone()) {
                if (!lastRead) {
                    return readIndex(buf, off, len);
                }
                return 0;
            }

            int headerSize = 0;
            if (packetBytesWritten == 0) {
                writePacketHeader(out);
                out.flush();
                bytes.flush();
                packetStream.flush();
                headerSize = bytes.getCount();
                currentHeaderSize = headerSize;
            }

            int toWrite = PACKET_SIZE - currentHeaderSize
                - packetBytesWritten;
            if (toWrite > bytes.getSpace()) {
                toWrite = bytes.getSpace();
            }
            bytes.write(packet, packetBytesWritten, toWrite);
            packetBytesWritten += toWrite;
            if (packetBytesWritten >= (PACKET_SIZE - currentHeaderSize)) {
                packetFull = false;
                packetSync.notifyAll();
            }
            bytes.flush();

            return toWrite + headerSize;
        }

    }

    protected int read(byte[] buf, int off, int len, Buffer buffer, int track)
            throws IOException {
        return 0;
    }

    private int readIndex(byte[] buf, int off, int len) throws IOException {

        // Write the index
        Header index = new Header(ASF_SIMPLE_INDEX_OBJECT);
        index.write(FILE_ID);
        index.writeLong(INDEX_TIME_INTERVAL * 10000);
        index.writeInt(maxPacketsPerKeyFrame);
        index.writeInt(noIndexEntries);
        index.write(indexEntries.getBytes());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(buf, off, len);
        bytes.write(index.getBytes());
        lastRead = true;
        return bytes.getCount();
    }

    protected int readLast(byte[] buf, int off, int len) throws IOException {
        return -1;
    }

    public String getName() {
        return "ASF Multiplexer";
    }

    public void setOffset(Time offset) {
        this.offset = offset.getNanoseconds() / 1000000;
    }

    public void setDuration(Time duration) {
        this.duration = duration.getNanoseconds() / 100;
    }

    public Component getControlComponent() {
        return null;
    }

    public Object getControl(String s) {
        if (s.equals(SetDurationControl.class.getName())) {
            return this;
        }
        return null;
    }

    public Object[] getControls() {
        return new Object[]{this};
    }

    public void close() {
        System.err.println("Written " + noPackets + " packets");
        System.err.println("Writing " + packetsToWrite + " extra packets");
        synchronized (packetSync) {
            while (packetsToWrite > 0) {
                flush();
                try {
                    writePayloadHeader(0, 0, 0, 0, 0, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                packetFirstTimestamp = 0;
                packetLastTimestamp = 0;
            }
        }

        super.close();
        synchronized (packetSync) {
            packetSync.notifyAll();
        }
        synchronized (tracksSync) {
            tracksSync.notifyAll();
        }
    }

}
