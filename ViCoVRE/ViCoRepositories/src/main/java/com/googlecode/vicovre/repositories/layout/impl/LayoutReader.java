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

package com.googlecode.vicovre.repositories.layout.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.utils.XmlIo;

public class LayoutReader {

    public static Layout readLayout(InputStream input)
            throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        return readLayout(doc);
    }

    public static Layout[] readLayouts(InputStream input)
            throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        Node[] layoutNodes = XmlIo.readNodes(doc, "layout");
        Layout[] layouts = new Layout[layoutNodes.length];
        for (int i = 0; i < layoutNodes.length; i++) {
            layouts[i] = readLayout(layoutNodes[i]);
        }
        return layouts;
    }

    private static Layout readLayout(Node node) {
        Layout layout = new Layout();
        String name = XmlIo.readAttr(node, "name", null);
        layout.setName(name);
        Node[] elements = XmlIo.readNodes(node, "element");
        List<LayoutPosition> layoutPostions = new Vector<LayoutPosition>();
        for (Node element : elements) {
            LayoutPosition pos = new LayoutPosition();
            pos.setName(XmlIo.readAttr(element, "name", null));
            pos.setX(Integer.parseInt(XmlIo.readAttr(element, "x", null)));
            pos.setY(Integer.parseInt(XmlIo.readAttr(element, "y", null)));
            pos.setWidth(Integer.parseInt(XmlIo.readAttr(element,
                    "width", null)));
            pos.setHeight(Integer.parseInt(XmlIo.readAttr(element,
                    "height", null)));
            String opacity = XmlIo.readAttr(element, "opacity", null);
            if (opacity != null) {
                pos.setOpacity(Double.parseDouble(opacity));
            }
            String assignable = XmlIo.readAttr(element, "assignable", null);
            if (assignable != null) {
                pos.setAssignable(Boolean.parseBoolean(assignable));
            }
            String changes = XmlIo.readAttr(element, "changes", null);
            if (changes != null) {
                pos.setChanges(Boolean.parseBoolean(changes));
            }
            String audio = XmlIo.readAttr(element, "audio", null);
            if (audio != null) {
                pos.setAudio(Boolean.parseBoolean(audio));
            }
            layoutPostions.add(pos);
        }
        layout.setStreamPostions(layoutPostions);
        return layout;
    }

    public static void writeLayout(Layout layout, OutputStream output) {
        PrintWriter writer = new PrintWriter(output);
        writeLayout(layout, writer);
        writer.close();
    }

    private static void writeLayout(Layout layout, PrintWriter writer) {
        writer.println("<layout name=\"" + layout.getName() + "\">");
        for (LayoutPosition position : layout.getStreamPositions()) {
            writer.print("<element name=\"" + position.getName() + "\"");
            writer.print(" x=\"" + position.getX() + "\"");
            writer.print(" y=\"" + position.getY() + "\"");
            writer.print(" width=\"" + position.getWidth() + "\"");
            writer.print(" height=\"" + position.getHeight() + "\"");
            writer.print(" opacity=\"" + position.getOpacity() + "\"");
            if (position.isAssignable()) {
                writer.print(" assignable=\"true\"");
            }
            if (position.hasChanges()) {
                writer.print(" changes=\"true\"");
            }
            if (position.hasAudio()) {
                writer.print(" audio=\"true\"");
            }
            writer.println("/>");
        }
        writer.println("</layout>");
    }
}
