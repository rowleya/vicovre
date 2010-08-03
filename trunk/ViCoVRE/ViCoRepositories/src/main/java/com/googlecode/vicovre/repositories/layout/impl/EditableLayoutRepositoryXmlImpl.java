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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.googlecode.vicovre.repositories.layout.EditableLayoutRepository;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutExistsException;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.utils.ExtensionFilter;

public class EditableLayoutRepositoryXmlImpl
        implements EditableLayoutRepository {

    private HashMap<String, Layout> layouts = new HashMap<String, Layout>();

    private LayoutRepository readOnlyRepository = null;

    private File directory = null;

    public EditableLayoutRepositoryXmlImpl(LayoutRepository readOnlyRepository,
            String layoutDirectory) {
        this.readOnlyRepository = readOnlyRepository;
        this.directory = new File(layoutDirectory);
        directory.mkdirs();
        readLayouts();
    }

    private void readLayouts() {
        File[] files = directory.listFiles(new ExtensionFilter(".layout"));
        for (File file : files) {
            try {
                FileInputStream input = new FileInputStream(file);
                Layout layout = LayoutReader.readLayout(input);
                input.close();
                layouts.put(layout.getName(), layout);
            } catch (Exception e) {
                System.err.println("readLayouts Warning: " + e.getMessage());
            }
        }
    }

    public void addLayout(Layout layout) throws LayoutExistsException,
            IOException {
        if (readOnlyRepository.findLayout(layout.getName()) != null
                || layouts.containsKey(layout.getName())) {
            throw new LayoutExistsException(layout.getName());
        }
        FileOutputStream output = new FileOutputStream(
                new File(directory, layout.getName() + ".layout"));
        LayoutReader.writeLayout(layout, output);
        output.close();
        layouts.put(layout.getName(), layout);

    }

    public void editLayout(Layout layout) throws IOException {
        FileOutputStream output = new FileOutputStream(
                new File(directory, layout.getName() + ".layout"));
        LayoutReader.writeLayout(layout, output);
        output.close();
        layouts.put(layout.getName(), layout);
    }

    public Layout findLayout(String layoutName) {
        return layouts.get(layoutName);
    }

    public List<Layout> findLayouts() {
        return new Vector<Layout>(layouts.values());
    }



}
