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

package com.googlecode.vicovre.recordings.db.insecure;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.media.rtp.RTCPHeader;
import com.googlecode.vicovre.media.rtp.RTCPSDES;
import com.googlecode.vicovre.media.rtp.RTPHeader;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.XmlIo;

/**
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class StreamReader {

    private StreamReader() {
        // Does Nothing
    }


    public static Stream readStream(File directory, String ssrc,
            RtpTypeRepository rtpTypeRepository) throws IOException {
        Stream stream = new Stream(rtpTypeRepository);

        File streamMetadata = new File(directory, ssrc
                + RecordingConstants.STREAM_METADATA);
        try {
            if (streamMetadata.exists()) {
                FileInputStream metaInput = new FileInputStream(streamMetadata);
                readStreamMetadata(metaInput, stream);
                metaInput.close();
            }
        } catch (Exception e) {
            System.err.println("Warning: error reading metadata for stream "
                    + directory + " - " + ssrc);
            e.printStackTrace();
        }

        if ((stream.getSsrc() == null) || (stream.getStartTime() == null)
                || (stream.getEndTime() == null)
                || (stream.getRtpType() == null)) {

            File streamFile = new File(directory, ssrc);
            FileInputStream fileInput = new FileInputStream(streamFile);
            DataInputStream input = new DataInputStream(fileInput);
            FileChannel channel = fileInput.getChannel();
            long seconds = (input.readInt() & RTPHeader.UINT_TO_LONG_CONVERT);
            long uSeconds = (input.readInt() & RTPHeader.UINT_TO_LONG_CONVERT);
            long startTime = (seconds * 1000) + (uSeconds / 1000);

            File streamIndexFile = new File(directory,
                    ssrc + RecordingConstants.STREAM_INDEX);
            FileInputStream indexFileInput = new FileInputStream(streamIndexFile);
            DataInputStream indexInput = new DataInputStream(indexFileInput);
            FileChannel indexChannel = indexFileInput.getChannel();
            indexInput.readLong();
            long position = indexInput.readLong();
            indexChannel.position(streamIndexFile.length() - 8 - 8);
            long offset = indexInput.readLong();

            int type = -1;
            int length = 0;
            int rtpType = -1;
            try {
                channel.position(position);
                while (type != RecordingConstants.RTP_PACKET) {
                    length = input.readShort() & RTPHeader.USHORT_TO_INT_CONVERT;
                    type = input.readShort() & RTPHeader.USHORT_TO_INT_CONVERT;
                    input.readInt();
                    if (type != RecordingConstants.RTP_PACKET) {
                        channel.position(channel.position() + length);
                    }
                }
                byte[] data = new byte[length];
                input.readFully(data);
                RTPHeader header = new RTPHeader(data, 0, length);
                rtpType = header.getPacketType();
            } catch (EOFException e) {
                System.err.println("End of file reading stream");
            }

            stream.setSsrc(ssrc);
            stream.setStartTime(new Date(startTime));
            stream.setEndTime(new Date(startTime + offset));
            stream.setRtpType(rtpType);

            try {
                while (stream.getCname() == null
                        || stream.getName() == null) {
                    while (type != RecordingConstants.RTCP_PACKET) {
                        length = input.readShort()
                            & RTPHeader.USHORT_TO_INT_CONVERT;
                        type = input.readShort()
                            & RTPHeader.USHORT_TO_INT_CONVERT;
                        input.readInt();
                        if (type != RecordingConstants.RTCP_PACKET) {
                            channel.position(channel.position() + length);
                        }
                    }
                    byte[] data = new byte[length];
                    input.readFully(data);

                    RTCPHeader rtcpHeader = new RTCPHeader(data, 0, length);
                    int rtcpPos = 0;
                    while (rtcpHeader.getPacketType() != RTCPHeader.PT_SDES) {
                        rtcpPos += (rtcpHeader.getLength() + 1) * 4;
                        rtcpHeader = new RTCPHeader(data, rtcpPos,
                                length - rtcpPos);
                    }
                    try {
                        RTCPSDES sdes = new RTCPSDES(data,
                                rtcpPos + RTCPHeader.SIZE, length);
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
                    } catch (Exception e) {
                        // Do Nothing
                    }
                }

            } catch (EOFException e) {
                // Do Nothing
            } catch (Exception e) {
                e.printStackTrace();
            }

            fileInput.close();
            indexFileInput.close();

            FileOutputStream output = new FileOutputStream(streamMetadata);
            writeStreamMetadata(stream, output);
            output.close();
        }

        return stream;
    }

    /**
     * Reads a stream's metadata
     * @param input The stream from which to read the input
     * @return The stream read
     * @throws IOException
     * @throws SAXException
     */
    public static void readStreamMetadata(InputStream input,
            Stream stream)
    throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        XmlIo.setString(doc, stream, "ssrc");
        XmlIo.setDate(doc, stream, "startTime");
        XmlIo.setDate(doc, stream, "endTime");
        String rtpType = XmlIo.readValue(doc, "rtpType");
        if (rtpType != null) {
            stream.setRtpType(Integer.valueOf(rtpType));
        }
        XmlIo.setLong(doc, stream, "firstTimestamp");
        XmlIo.setLong(doc, stream, "packetsSeen");
        XmlIo.setLong(doc, stream, "packetsMissed");
        XmlIo.setLong(doc, stream, "bytes");
        XmlIo.setString(doc, stream, "cname");
        XmlIo.setString(doc, stream, "name");
        XmlIo.setString(doc, stream, "email");
        XmlIo.setString(doc, stream, "phone");
        XmlIo.setString(doc, stream, "location");
        XmlIo.setString(doc, stream, "tool");
        XmlIo.setString(doc, stream, "note");
    }

    /**
     * Writes a stream's metadata in a format that can be read by this reader
     * @param stream The stream to write
     * @param output The output
     */
    public static void writeStreamMetadata(Stream stream, OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<stream>");
        XmlIo.writeValue(stream, "ssrc", writer);
        XmlIo.writeDate(stream, "startTime", writer);
        XmlIo.writeDate(stream, "endTime", writer);
        XmlIo.writeValue("rtpType",
                String.valueOf(stream.getRtpType().getId()), writer);
        XmlIo.writeValue(stream, "firstTimestamp", writer);
        XmlIo.writeValue(stream, "packetsSeen", writer);
        XmlIo.writeValue(stream, "packetsMissed", writer);
        XmlIo.writeValue(stream, "bytes", writer);
        XmlIo.writeValue(stream, "cname", writer);
        XmlIo.writeValue(stream, "name", writer);
        XmlIo.writeValue(stream, "email", writer);
        XmlIo.writeValue(stream, "phone", writer);
        XmlIo.writeValue(stream, "location", writer);
        XmlIo.writeValue(stream, "tool", writer);
        XmlIo.writeValue(stream, "note", writer);
        writer.println("</stream>");
        writer.flush();
    }
}
