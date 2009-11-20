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

import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.utils.XmlIo;

public class LayoutReader {

    public static ReplayLayout readLayout(InputStream input,
            LayoutRepository layoutRepository, Recording recording)
            throws SAXException, IOException {
        ReplayLayout layout = new ReplayLayout(layoutRepository);
        Node doc = XmlIo.read(input);
        XmlIo.setString(doc, layout, "name");
        XmlIo.setLong(doc, layout, "time");
        XmlIo.setLong(doc, layout, "endTime");
        for (ReplayLayoutPosition position : layout.getLayoutPositions()) {
            String streamSsrc = XmlIo.readValue(doc,
                    "pos" + position.getName());
            Stream stream = recording.getStream(streamSsrc);
            layout.setStream(position.getName(), stream);
        }
        String[] audioStreams = XmlIo.readValues(doc, "audioStream");
        for (String streamSsrc : audioStreams) {
            Stream stream = recording.getStream(streamSsrc);
            layout.addAudioStream(stream);
        }
        return layout;
    }

    public static void writeLayout(ReplayLayout layout, OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<layout>");
        XmlIo.writeValue(layout, "name", writer);
        XmlIo.writeValue(layout, "time", writer);
        XmlIo.writeValue(layout, "endTime", writer);
        for (ReplayLayoutPosition position : layout.getLayoutPositions()) {
            XmlIo.writeValue("pos" + position.getName(),
                    position.getStream().getSsrc(), writer);
        }
        for (Stream stream : layout.getAudioStreams()) {
            XmlIo.writeValue("audioStream", stream.getSsrc(), writer);
        }
        writer.println("</layout>");
        writer.flush();
    }
}
