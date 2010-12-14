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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.DefaultLayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.utils.XmlIo;

public class DefaultLayoutReader {

    public static DefaultLayout readLayout(InputStream input,
            LayoutRepository layoutRepository)
            throws SAXException, IOException {
        DefaultLayout layout = new DefaultLayout(layoutRepository);
        Node doc = XmlIo.read(input);
        XmlIo.setString(doc, layout, "name");
        XmlIo.setLong(doc, layout, "time");
        XmlIo.setLong(doc, layout, "endTime");
        for (DefaultLayoutPosition position : layout.getLayoutPositions()) {
            Node pos = XmlIo.readNode(doc, "pos" + position.getName());
            layout.setField(position.getName(),
                    BooleanFieldSetReader.readFieldSet(pos));
        }
        Node[] audioStreams = XmlIo.readNodes(doc, "audioStream");
        for (Node audioStream : audioStreams) {
            layout.addAudioStream(BooleanFieldSetReader.readFieldSet(
                    audioStream));
        }
        return layout;
    }

    public static void writeLayout(OutputStream output, DefaultLayout layout) {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<layout>");
        XmlIo.writeValue("name", layout.getName(), writer);
        XmlIo.writeValue("time", String.valueOf(layout.getTime()), writer);
        XmlIo.writeValue("endTime", String.valueOf(layout.getEndTime()),
                writer);
        for (DefaultLayoutPosition position : layout.getLayoutPositions()) {
            writer.println("<pos" + position.getName() + ">");
            BooleanFieldSetReader.writeFieldSet(writer, position.getFieldSet());
            writer.println("</pos" + position.getName() + ">");
        }
        for (DefaultLayoutPosition audio : layout.getAudioStreams()) {
            writer.println("<audioStream>");
            BooleanFieldSetReader.writeFieldSet(writer, audio.getFieldSet());
            writer.println("</audioStream>");
        }
        writer.println("</layout>");
        writer.flush();
    }
}