/*
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

package com.googlecode.vicovre.repositories.liveAnnotation.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.repositories.liveAnnotation.LiveAnnotationProperties;
import com.googlecode.vicovre.repositories.liveAnnotation.LiveAnnotationType;
import com.googlecode.vicovre.repositories.liveAnnotation.LiveAnnotationTypeRepository;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * An implementation of the LayoutRepository that uses an xml file
 *
 * @author Tobias M Schiebeck
 * @version 1.0
 */
public class LiveAnnotationTypeRepositoryXmlImpl implements
        LiveAnnotationTypeRepository {

    private HashMap<String, LiveAnnotationType> liveAnnotationTypes =
        new HashMap<String, LiveAnnotationType>();
    private List<String> liveAnnotationTypeNames = new Vector<String>();
    private LiveAnnotationProperties liveAnnotationProperties =
        new LiveAnnotationProperties();

    /**
     * Creates a new RtpTypeRepository
     *
     * @param file
     *            The file containing the known types
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public LiveAnnotationTypeRepositoryXmlImpl(String file)
            throws SAXException, IOException {
        Node doc = XmlIo.read(getClass().getResourceAsStream(file));
        Node properties = XmlIo.readNode(doc, "properties");
        if (properties != null) {
            String[] fields = XmlIo.readFields(properties);
            for (int i = 0; i < fields.length; i++) {
                if (!fields[i].equals("textColours")) {
                    Node node = XmlIo.readNode(properties, fields[i]);
                    Node width = node.getAttributes().getNamedItem("width");
                    Node height = node.getAttributes().getNamedItem("height");
                    if ((width != null) && (height != null)) {
                        liveAnnotationProperties.setSize(fields[i],
                                Integer.parseInt(width.getTextContent()),
                                Integer.parseInt(height.getTextContent()));
                    }
                }
            }
            Node textColours = XmlIo.readNode(properties, "textColours");
            String[] colours = XmlIo.readValues(textColours, "colour");
            for (String colour : colours) {
                liveAnnotationProperties.addTextColour(colour);
            }
        } else {
            throw new RuntimeException("invalid LiveAnnotationTypeRepository");
        }

        Node[] types = XmlIo.readNodes(doc, "liveAnnotationType");
        for (int i = 0; i < types.length; i++) {
            LiveAnnotationType liveAnnotationType = new LiveAnnotationType();
            String name = XmlIo.readAttr(types[i], "name", null);
            String type = XmlIo.readAttr(types[i], "type", name);
            String index = XmlIo.readAttr(types[i], "index", null);
            if (index != null) {
                liveAnnotationType.setIndex(Long.parseLong(index));
            }
            if (type == null) {
                throw new RuntimeException(
                        "invalid LiveAnnotationTypeRepository");
            }
            liveAnnotationType.setName(name);
            liveAnnotationType.setType(type);

            Node button = XmlIo.readNode(types[i], "button");
            if (button != null) {
                String image = XmlIo.readAttr(button, "image", null);
                String visible = XmlIo.readAttr(button, "visible", "true");
                liveAnnotationType.setButton(image, visible);
            }

            Node thumbnail = XmlIo.readNode(types[i], "thumbnail");
            if (thumbnail != null) {
                String imageName = XmlIo.readAttr(thumbnail, "name", null);
                liveAnnotationType.setThumbnail(imageName);
            }

            Node colour = XmlIo.readNode(types[i], "colour");
            if (colour != null) {
                String value = XmlIo.readAttr(colour, "value", null);
                liveAnnotationType.setColour(value);
            }

            Node[] fields = XmlIo.readNodes(types[i], "field");
            for (Node field : fields) {
                String fieldName = XmlIo.readAttr(field, "name", null);
                String[] attrs = XmlIo.readAttrs(field);
                for (String attr : attrs) {
                    String value = XmlIo.readAttr(field, attr, null);
                    liveAnnotationType.setFieldAttribute(
                            fieldName, attr, value);
                }
            }

            Node format = XmlIo.readNode(types[i], "format");
            if (format != null) {
                String[] children = XmlIo.readFields(format);
                for (String child : children) {
                    String value = XmlIo.readContent(format, child);
                    liveAnnotationType.setFormat(child, value);
                }
            }

            Node convertsTo = XmlIo.readNode(types[i], "convertsTo");
            if (convertsTo != null) {
                Node[] convertTypes = XmlIo.readNodes(convertsTo, "type");
                for (Node convert : convertTypes) {
                    String value = XmlIo.readAttr(convert, "name", null);
                    liveAnnotationType.addConversion(value);
                }
            }

            liveAnnotationTypes.put(liveAnnotationType.getName(),
                    liveAnnotationType);
            liveAnnotationTypeNames.add(liveAnnotationType.getName());
        }
    }

    public LiveAnnotationType findLiveAnnotationType(String liveAnnotationTypeName) {
        return liveAnnotationTypes.get(liveAnnotationTypeName);
    }

    public LiveAnnotationProperties getProperties() {
        return liveAnnotationProperties;
    }

    public List<String> getLiveAnnotationTypes() {
        return liveAnnotationTypeNames;
    }

}
