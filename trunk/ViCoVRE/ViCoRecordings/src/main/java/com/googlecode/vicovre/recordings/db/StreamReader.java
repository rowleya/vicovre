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

package com.googlecode.vicovre.recordings.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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

    /**
     * Reads a stream
     * @param input The stream from which to read the input
     * @return The recording read
     * @throws IOException
     * @throws SAXException
     */
    public static Stream readStream(InputStream input,
            RtpTypeRepository typeRepository)
    throws SAXException, IOException {
        Stream stream = new Stream(typeRepository);
        Node doc = XmlIo.read(input);
        XmlIo.setString(doc, stream, "ssrc");
        XmlIo.setDate(doc, stream, "startTime");
        XmlIo.setDate(doc, stream, "endTime");
        XmlIo.setLong(doc, stream, "firstTimestamp");
        XmlIo.setLong(doc, stream, "firstTimestamp");
        XmlIo.setLong(doc, stream, "packetsSeen");
        XmlIo.setLong(doc, stream, "packetsMissed");
        XmlIo.setLong(doc, stream, "bytes");
        stream.setRtpType(Integer.valueOf(XmlIo.readValue(doc, "rtpType")));
        XmlIo.setString(doc, stream, "cname");
        XmlIo.setString(doc, stream, "name");
        XmlIo.setString(doc, stream, "email");
        XmlIo.setString(doc, stream, "phone");
        XmlIo.setString(doc, stream, "location");
        XmlIo.setString(doc, stream, "tool");
        XmlIo.setString(doc, stream, "note");
        return stream;
    }

    /**
     * Writes a stream in a format that can be read by this reader
     * @param stream The stream to write
     * @param output The output
     */
    public static void writeStream(Stream stream, OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<stream>");
        XmlIo.writeValue(stream, "ssrc", writer);
        XmlIo.writeDate(stream, "startTime", writer);
        XmlIo.writeDate(stream, "endTime", writer);
        XmlIo.writeValue(stream, "firstTimestamp", writer);
        XmlIo.writeValue(stream, "packetsSeen", writer);
        XmlIo.writeValue(stream, "packetsMissed", writer);
        XmlIo.writeValue(stream, "bytes", writer);
        XmlIo.writeValue("rtpType",
                String.valueOf(stream.getRtpType().getId()),
                writer);
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
