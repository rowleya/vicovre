/**
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

package com.googlecode.vicovre.repositories.layout.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;

/**
 * An implementation of the LayoutRepository that uses an xml file
 *
 * @author Tobias M Schiebeck
 * @version 1.0
 */
public class LayoutRepositoryXmlImpl implements LayoutRepository {

    private HashMap<String, Layout> layouts = new HashMap<String, Layout>();

    /**
     * Creates a new RtpTypeRepository
     *
     * @param file
     *            The file containing the known types
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public LayoutRepositoryXmlImpl(String file) throws SAXException,
            IOException {
        InputStream input = getClass().getResourceAsStream(file);
        Layout[] layouts = LayoutReader.readLayouts(input);
        for (Layout layout : layouts) {
            this.layouts.put(layout.getName(), layout);
        }
    }

    /**
     * {@inheritDoc}
     * @see com.googlecode.vicovre.repositories.layout.LayoutRepository#
     *     findLayout(java.lang.String)
     */
    public Layout findLayout(final String layoutName) {
        return layouts.get(layoutName);
    }

   /**
    * {@inheritDoc}
    * @see com.googlecode.vicovre.repositories.layout.LayoutRepository#
    *     findLayouts()
    */
    public List<Layout> findLayouts() {
        return new Vector<Layout>(layouts.values());
    }

}
