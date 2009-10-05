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
 * 3) Neither the name of the University Manchester nor the names of its
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

package com.googlecode.vicovre.repositories.harvestFormat.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormat;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;

/**
 * An implementation of the HarvestFormatRepository that uses an xml file
 *
 * @author Andrew Rowley
 * @version 1.0
 */
public class HarvestFormatRepositoryXmlImpl implements HarvestFormatRepository {

    private HashMap<String, HarvestFormat> formats =
        new HashMap<String, HarvestFormat>();

    /**
     * Creates a new RtpTypeRepository
     *
     * @param file
     *            The file containing the known types
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public HarvestFormatRepositoryXmlImpl(String file) throws SAXException,
            IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(getClass().getResourceAsStream(file));
        NodeList list = document.getElementsByTagName("format");
        for (int i = 0; i < list.getLength(); i++) {
            HarvestFormat format = new HarvestFormat();
            Node node = list.item(i);
            NamedNodeMap attributes = node.getAttributes();
            format.setName(attributes.getNamedItem("name").getNodeValue());
            format.setDecodeClass(attributes.getNamedItem(
                    "decodeClass").getNodeValue());
            formats.put(format.getName(), format);
        }
    }

    public HarvestFormat findFormat(String formatName) {
        return formats.get(formatName);
    }

    public List<HarvestFormat> findFormats() {
        return new Vector<HarvestFormat>(formats.values());
    }

}
