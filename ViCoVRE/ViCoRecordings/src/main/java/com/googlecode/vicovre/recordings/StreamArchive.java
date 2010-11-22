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

package com.googlecode.vicovre.recordings;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.channels.FileChannel;
import java.util.Date;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.media.rtp.RTCPHeader;
import com.googlecode.vicovre.media.rtp.RTCPSDES;
import com.googlecode.vicovre.media.rtp.RTPHeader;
import com.googlecode.vicovre.media.screencapture.ScreenChangeDetector;
import com.googlecode.vicovre.repositories.rtptype.RTPType;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

/**
 * Stores RTP Data to Disk
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class StreamArchive {

    // The number of bytes in a short
    private static final int BYTES_PER_SHORT = 2;

    // The number of bytes in an int
    private static final int BYTES_PER_INT = 4;

    // The number of bytes in an address
    private static final int BYTES_PER_ADDRESS = 4;

    // The number of milliseconds in a second
    private static final int MS_PER_SEC = 1000;

    // The number of bytes in a word
    private static final int BYTES_PER_WORD = 4;

    // The maximum RTP number for RTCP conflict avoidance
    private static final int MAX_RTCP_CONFLICT = 76;

    // The minimum RTP number for RTCP conflict avoidance
    private static final int MIN_RTCP_CONFLICT = 72;

    // The manager of this archive
    private RecordArchiveManager archiveMgr = null;

    // The start time of the stream
    private long startTime = 0;

    // The RTP identifier of the stream
    private long ssrc = 0;

    // The last RTP sequence number seen
    private int lastRtpSeq = 0;

    // The last RTP timestamp
    private long lastTimestamp = -1;

    // True if the RTP sequence number has been initialised
    private boolean rtpSeqInit = false;

    // The total number of bytes recorded
    private long totalBytes = 0;

    // The total number of packets not recorded
    private long totalMissedPackets = 0;

    // The total number of packets recorded
    private long totalPacketsSeen = 0;

    // The total size of the recorded file
    private long fileSize = 0;

    // True if some bad IO has occurred
    private boolean bBadFileIO = false;

    // The name of the file holding the stream
    private String streamFilename = "";

    private File directory = null;

    // The control channel of the stream file
    private FileChannel streamFileControl = null;

    // The data channel of the stream file
    private DataOutputStream streamFile = null;

    // The name of the index of the stream
    private String indexFilename = "";

    // The data channel of the index file
    private DataOutputStream indexFile = null;

    // True if output writing has started
    private boolean writingOutput = false;

    // The type of the data
    private int type = -1;

    // The time that the current packet was recieved
    private long packetRecievedTime = 0;

    // The packet timestamp of the current packet
    private long packetTimestamp = 0;

    // The recording to which this stream belongs
    private Stream stream = null;

    // The type repository
    private RtpTypeRepository typeRepository = null;

    private ScreenChangeDetector changeDetector = null;


    /**
     * Creates a new StreamArchive
     *
     * @param archiveMgr The manager of this archive
     * @param directory The directory to store in
     * @param ssrc The RTP stream id to record
     * @param typeRepository The RTP Type repository
     * @throws IOException
     */
    public StreamArchive(RecordArchiveManager archiveMgr, File directory,
            long ssrc, RtpTypeRepository typeRepository) {

        this.stream = new Stream(typeRepository);
        this.archiveMgr = archiveMgr;
        this.ssrc = ssrc;
        this.typeRepository = typeRepository;
        this.directory = directory;

        // Work out the names of the files
        String slash = System.getProperty("file.separator");
        streamFilename = directory.getAbsolutePath() + slash + ssrc;
        indexFilename = directory.getAbsolutePath() + slash + ssrc
            + RecordingConstants.STREAM_INDEX;
    }

    /**
     * Stops the recording
     * @throws IOException
     */
    public void terminate() throws IOException {

        // Close the actual data archive file
        if (streamFile != null) {
            streamFile.close();
        }

        // Write final data
        writeFinalInfo();

        // Close the index file
        if (indexFile != null) {
            indexFile.close();
        }

        if (changeDetector != null) {
            changeDetector.close();
        }
    }

    private void calculateMissedPackets(RTPHeader packetHeader) {
        // Initialise the RTP sequence if it hasn't been done
        if (!rtpSeqInit) {
            lastRtpSeq = packetHeader.getSequence() - 1;
            rtpSeqInit = true;
        }

        // Set the RTP sequence, taking account of wrapping
        lastRtpSeq++;
        if (lastRtpSeq > RTPHeader.MAX_SEQUENCE) {
            lastRtpSeq = 0;
        }

        // If the packet sequence and the current sequence are not the same, we
        // missed one
        if (packetHeader.getSequence() != lastRtpSeq) {
            if (packetHeader.getSequence() < lastRtpSeq) {
                totalMissedPackets += packetHeader.getSequence()
                        + ((RTPHeader.MAX_SEQUENCE + 1) - lastRtpSeq);
            } else {
                totalMissedPackets +=
                    (packetHeader.getSequence() - lastRtpSeq) + 1;
            }
            lastRtpSeq = packetHeader.getSequence();
        }
    }

    /**
     * Handles an incoming RTP packet
     *
     * @param packet The packet to handle
     * @param time The time at which the packet arrived
     * @throws IOException
     */
    public void handleRTPPacket(DatagramPacket packet, long time)
            throws IOException {

        RTPHeader packetHeader = new RTPHeader(packet);

        // Reject packets that have invalid data in them
        if ((packetHeader.getPacketType() >= MIN_RTCP_CONFLICT)
                && (packetHeader.getPacketType() <= MAX_RTCP_CONFLICT)) {
            return;
        }
        long offset = 0;
        packetRecievedTime = time;
        if (bBadFileIO) {
            return;
        }

        // Initialization block -- we haven't started writing yet
        if (!writingOutput) {

            writingOutput = true;

            // This puts the initial stuff in the actual archive
            writeFileHeader();

            // This starts the info/index files
            writeInitialInfo();

            // Write out the timestamp for this first RTP packet in the file.
            stream.setFirstTimestamp(packetHeader.getTimestamp());


        }

        // Set the statistics
        totalBytes += packet.getLength();
        totalPacketsSeen++;

        calculateMissedPackets(packetHeader);

        offset = packetRecievedTime - startTime;
        lastTimestamp = packetTimestamp;
        packetTimestamp = packetHeader.getTimestamp();

        if (type == -1) {
            type = packetHeader.getPacketType();
            RTPType rtpType = typeRepository.findRtpType(type);
            stream.setRtpType(rtpType);
            /*if (rtpType.getFormat() instanceof VideoFormat) {
                try {
                    changeDetector =
                        new ScreenChangeDetector(directory,
                                String.valueOf(ssrc),
                                typeRepository, packetHeader.getPacketType());
                } catch (Throwable e) {
                    System.err.println(
                        "Could not initialize ScreenChangeDetector: "
                        + e.getMessage());
                    changeDetector = null;
                }
            } */
        }

        if (type == packetHeader.getPacketType()) {

            // Store the packet
            writePacket(packet, RecordingConstants.RTP_PACKET, offset);

            if (changeDetector != null) {
                try {
                    changeDetector.process(packetHeader, packet, offset);
                } catch (Throwable e) {
                    e.printStackTrace();
                    changeDetector = null;
                }
            }
        }

    }

    /**
     * Handles an incoming RTCP packet
     *
     * @param packet The packet to handle
     * @param time The time the packet arrived
     */
    public void handleRTCPPacket(DatagramPacket packet, long time) {
        int offset = packet.getOffset();
        int read = 0;
        packetRecievedTime = time;

        // Don't do anything if there has been a file error
        if (bBadFileIO) {
            return;
        }

        // If output is being written, save the packet
        if (writingOutput) {
            long off = packetRecievedTime - startTime;
            writePacket(packet, RecordingConstants.RTCP_PACKET, off);
        }

        // Go through all the attached RTCP packets and handle them too
        try {
            while (offset < (packet.getLength()
                    + packet.getOffset())) {
                RTCPHeader header = new RTCPHeader(packet.getData(),
                        offset, packet.getLength() - read);
                int length = (header.getLength() + 1) * BYTES_PER_WORD;
                read += RTCPHeader.SIZE;
                offset += RTCPHeader.SIZE;
                processSubpacket(header, packet.getData(), offset);
                offset += length - RTCPHeader.SIZE;
                read += length - RTCPHeader.SIZE;
            }
        } catch (EOFException e) {
            // Do Nothing - happens regularly
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processSubpacket(RTCPHeader packetHeader, byte[] packet,
            int offset) {

        // If there is a file error, stop
        if (bBadFileIO) {
            return;
        }

        switch (packetHeader.getPacketType()) {
        // Ignore RR packets
        case RTCPHeader.PT_RR:
            break;

        // Ignore BYE packets
        case RTCPHeader.PT_BYE:
            break;

        case RTCPHeader.PT_SDES:
            RTCPSDES sdes = new RTCPSDES(packet, offset,
                    (packetHeader.getLength() + 1) * 4);
            if (sdes.getCname() != null) {
                stream.setCname(sdes.getCname());
            }
            if (sdes.getName() != null) {
                stream.setName(sdes.getName());
            }
            if (sdes.getEmail() != null) {
                stream.setEmail(sdes.getEmail());
            }
            if (sdes.getPhone() != null) {
                stream.setPhone(sdes.getPhone());
            }
            if (sdes.getLocation() != null) {
                stream.setLocation(sdes.getLocation());
            }
            if (sdes.getTool() != null) {
                stream.setTool(sdes.getTool());
            }
            if (sdes.getNote() != null) {
                stream.setNote(sdes.getNote());
            }
        break;

        // Ignore SR Packets
        case RTCPHeader.PT_SR:
            break;

        // Ignore APP Packets
        case RTCPHeader.PT_APP:
            break;

        // Ignore anything you don't know about
        default:
            break;
        }
    }

    // Spit out info we know about the start of the stream
    private void writeInitialInfo() {
        try {
            FileOutputStream indexFileO = new FileOutputStream(indexFilename,
                    true);

            // Store the ssrc and start time of the stream
            stream.setSsrc(String.valueOf(ssrc));
            stream.setStartTime(new Date(startTime));

            // Start the index file
            indexFile = new DataOutputStream(indexFileO);
        } catch (IOException e) {
            e.printStackTrace();
            bBadFileIO = true;
        }
    }

    // Spit out the end-of-stream summary information:
    private void writeFinalInfo() {
        stream.setEndTime(packetRecievedTime);
        stream.setPacketsSeen(totalPacketsSeen);
        stream.setPacketsMissed(totalMissedPackets);
        stream.setBytes(totalBytes);
    }

    // Opens the stream file for writing
    private void openFile() {
        try {
            if (streamFile == null) {
                FileOutputStream streamFileO = new FileOutputStream(
                        streamFilename, true);
                streamFile = new DataOutputStream(streamFileO);
                streamFileControl = streamFileO.getChannel();
            }
        } catch (IOException e) {
            e.printStackTrace();
            bBadFileIO = true;
        }
    }

    // Writes the index file
    private void writeIndex(long offset) {
        try {

            // If we're writing output to the stream file
            if (writingOutput && packetTimestamp != lastTimestamp) {

                // figure out where we are in the main file
                long pos = streamFileControl.position();

                // Write our index
                indexFile.writeLong(offset);
                indexFile.writeLong(pos);
                indexFile.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            bBadFileIO = true;
        }
    }

    // Writes the header of the stream file
    private void writeFileHeader() {
        try {
            long seconds = 0;
            long uSeconds = 0;
            byte[] ipaddress = new byte[BYTES_PER_ADDRESS];
            startTime = packetRecievedTime;
            openFile();

            seconds = (startTime / MS_PER_SEC);
            uSeconds = ((startTime - (seconds * MS_PER_SEC)) * MS_PER_SEC);
            streamFile.writeInt((int) seconds);
            streamFile.writeInt((int) uSeconds);
            streamFile.write(ipaddress, 0, BYTES_PER_ADDRESS);
            streamFile.writeShort(0);

            // Add to the file size 2 ints, 4 bytes and a short
            fileSize += BYTES_PER_INT + BYTES_PER_INT + BYTES_PER_ADDRESS
                + BYTES_PER_SHORT;

        } catch (IOException e) {
            e.printStackTrace();
            bBadFileIO = true;
        }
    }

    // Writes a packet to disk
    private void writePacket(DatagramPacket packet, int type, long offset) {
        if (((archiveMgr != null) && !archiveMgr.isRecording())
                || (packet.getLength() == 0)) {
            return;
        }

        try {
            if (type == 0) {
                writeIndex(offset);
            }
            streamFile.writeShort(packet.getLength());
            streamFile.writeShort(type);
            streamFile.writeInt((int) offset);

            // Add 2 shorts and an int to the file
            fileSize += BYTES_PER_SHORT + BYTES_PER_SHORT + BYTES_PER_INT;

            streamFile.write(packet.getData(), packet.getOffset(),
                    packet.getLength());
            fileSize += packet.getLength();
        } catch (IOException e) {
            e.printStackTrace();
            bBadFileIO = true;
        }
    }

    /**
     * Gets the start time of the archive
     * @return The start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the current duration of the stream
     *
     * @return the duration in ms
     */
    public long getDuration() {
        return packetRecievedTime - startTime;
    }

    /**
     * Gets the stream information
     * @return The stream
     */
    public Stream getStream() {
        return stream;
    }
}
